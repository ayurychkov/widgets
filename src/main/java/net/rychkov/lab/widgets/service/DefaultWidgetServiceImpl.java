package net.rychkov.lab.widgets.service;

import net.rychkov.lab.widgets.api.model.WidgetCreateRequest;
import net.rychkov.lab.widgets.api.model.WidgetUpdateRequest;
import net.rychkov.lab.widgets.dal.model.Page;
import net.rychkov.lab.widgets.dal.model.Widget;
import net.rychkov.lab.widgets.dal.model.WidgetChange;
import net.rychkov.lab.widgets.dal.model.WidgetDelta;
import net.rychkov.lab.widgets.dal.repository.ConstraintViolationException;
import net.rychkov.lab.widgets.dal.repository.WidgetRepository;
import net.rychkov.lab.widgets.dal.repository.WidgetRepositoryTransaction;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.transaction.NotSupportedException;
import javax.validation.constraints.NotNull;
import java.util.*;

@Service
public class DefaultWidgetServiceImpl implements WidgetService {

    /**
     * Widget repository
     */
    private final WidgetRepository repository;

    @Value("${widgets.pagesize}")
    private Integer pageSize = 10;

    /**
     * z-coordinate step for new widgets (max z of existed widgets plus step)
     * will help in future updates
     */
    private static final int Z_STEP = 16;

    public DefaultWidgetServiceImpl(@Qualifier("repository") WidgetRepository repository) {
        this.repository = repository;
    }

    /**
     * Find position for inserting widget with z-coordinate in z-index
     * @param z z-coordinate of widget
     * @param index Z-index (not null)
     * @return Position for inserting (index.size() for inserting after last element)
     */
    private int findInsertPosition(int z,@NotNull List<Widget> index) {
        int firstIndex = 0;
        int lastIndex = index.size() - 1;

        while(firstIndex <= lastIndex) {
            int middleIndex = (firstIndex + lastIndex) / 2;

            if (index.get(middleIndex).getZ() < z) {
                // position after last element (middleIndex - last element) or position after middleIndex
                if(index.size()<=middleIndex+1 || index.get(middleIndex+1).getZ() >= z) {
                    return middleIndex+1;
                }
                else {
                    // try right part
                    firstIndex = middleIndex + 1;
                }
            }
            else if (index.get(middleIndex).getZ() >= z) {
                // position before first element (middleIndex - first element) or position at middleIndex
                if(middleIndex==0 || index.get(middleIndex-1).getZ() < z) {
                    return middleIndex;
                }
                else {
                    // try left part
                    lastIndex = middleIndex - 1;
                }
            }
        }

        // empty array - insert at first position
        return 0;
    }

    /**
     * Shift widgets by change z-coordinate
     * @param widgetToBeInserted Widget for inserting - cause of shifting
     * @param all Ordered by z collection of all widgets
     * @throws ConstraintViolationException Unique z constraint violation
     */
    private void shift(WidgetDelta widgetToBeInserted, List<Widget> all) throws ConstraintViolationException {
        int allSize = all.size();

        if(allSize==0) {
            // for first element in repository set z to 0
            widgetToBeInserted.setZ(0);
        }
        else {
            int insertPosition = findInsertPosition(widgetToBeInserted.getZ(), all);

            if(insertPosition==0) {
                // first element
                widgetToBeInserted.setZ(all.get(0).getZ()-Z_STEP);
            }
            else if(insertPosition==all.size()) {
                // last element
                widgetToBeInserted.setZ(all.get(allSize-1).getZ()+Z_STEP);
            }
            else {
                // middle element
                int z = widgetToBeInserted.getZ();

                ArrayList<WidgetChange> updateQueue = new ArrayList<>();

                for(int i = insertPosition; i<allSize; i++) {

                    // no needs for cascade changes of z - break
                    if(all.get(i).getZ()>z) {
                        break;
                    }

                    // need to change z of existed widget (unique constraint)
                    // and go to next widget

                    z += i+1==allSize ? Z_STEP : Math.round(((float)all.get(i+1).getZ()-z)/2);
                    WidgetDelta changes = new WidgetDelta();
                    changes.setZ(z);
                    updateQueue.add(new WidgetChange(all.get(i).getId(), changes));
                }

                if((float)updateQueue.size()/allSize>0.5) {
                    // optimisation: force rebuild z-index (only part)
                    for(int i=all.size()-1; i>=insertPosition; i--) {
                        WidgetDelta widgetDelta = new WidgetDelta(null, null,widgetToBeInserted.getZ()+Z_STEP*(i-insertPosition+1), null, null);
                        repository.update(all.get(i).getId(), widgetDelta);
                    }
                }
                else {
                    // backward iterating for change z-coordinate of widgets with collision
                    for (int i = updateQueue.size() - 1; i >= 0; i--) {
                        WidgetChange change = updateQueue.get(i);
                        repository.update(change.getId(), change.getDelta());
                    }
                }
            }

        }
    }

    @Override
    @Transactional
    public Widget addWidget(WidgetCreateRequest widget) {

        // mapping
            WidgetDelta delta = new WidgetDelta();
        delta.setHeight(widget.getHeight());
        delta.setWidth(widget.getWidth());
        delta.setX(widget.getX());
        delta.setY(widget.getY());
        delta.setZ(widget.getZ());

        WidgetRepositoryTransaction tx = repository.BeginTransaction();;

        // adding
        try {

            List<Widget> all = new ArrayList<>(repository.getAllOrderByZ());
            int allSize = all.size();

            // z-coordinate not set
            if(delta.getZ()==null) {
                // if no widgets in repository - set 0 z-coordinate
                // else set z-coordinate of last widget (in sorted list) plus Z_STEP constant
                delta.setZ(allSize==0 ? 0 : all.get(allSize-1).getZ()+Z_STEP);
            }
            else {
                shift(delta, all);
            }


            Widget result = repository.add(delta);

            tx.commit();
            return result;
        }
        catch(ConstraintViolationException e) {
            if (tx != null) {
                tx.rollback();
            }
            throw new RuntimeException(e);
        }
        catch(Exception e) {
            if (tx != null) {
                tx.rollback();
            }
            throw e;
        }
    }

    @Override
    @Transactional
    public Widget removeWidget(int id){
        return repository.remove(id);
    }

    @Override
    public Widget getWidget(int id) {
        return repository.get(id);
    }

    @Override
    public Collection<Widget> getAllWidgets() {
        return repository.getAllOrderByZ();
    }

    @Override
    public Page<Widget> getAllWidgets(int pageNum) {
        return repository.getAllOrderByZ(pageNum, pageSize);
    }

    @Override
    public Collection<Widget> getFilteredByRectangle(int x1, int y1, int x2, int y2) throws NotSupportedException {

        // verify
        if(x1>x2 || y1>y2) {
            throw new IllegalArgumentException();
        }

        return repository.getFilteredByRectangle(x1,y1,x2,y2);
    }

    @Override
    @Transactional
    public Widget updateWidget(int widgetId, WidgetUpdateRequest widget)
            throws IllegalArgumentException, NoSuchElementException, ConstraintViolationException {

        WidgetDelta delta = new WidgetDelta();
        delta.setHeight(widget.getHeight());
        delta.setWidth(widget.getWidth());
        delta.setX(widget.getX());
        delta.setY(widget.getY());
        delta.setZ(widget.getZ());

        // no z-coordinate update
        if(delta.getZ()==null) {
            return repository.update(widgetId, delta);
        }

        WidgetRepositoryTransaction tx = repository.BeginTransaction();
        Widget origin = repository.get(widgetId);

        // nothing to update - no widget with widgetId found
        if (origin == null) {
            throw new NoSuchElementException("No widget found");
        }

        try {

            // no z-coordinate changes
            if (delta.getZ() == origin.getZ()) {
                Widget result = repository.update(widgetId, delta);
                tx.commit();
                return result;
            }

            List<Widget> all = new ArrayList(repository.getAllOrderByZ());
            all.remove(origin);
            int insertPosition = findInsertPosition(delta.getZ(), all);

            // no z-coordinate conflicts
            if (insertPosition>=all.size() || all.get(insertPosition).getZ() > delta.getZ()) {
                Widget result = repository.update(widgetId, delta);
                tx.commit();
                return result;
            }

            // z-coordinate conflict
            shift(delta, all);

            Widget result = repository.update(widgetId, delta);

            tx.commit();
            return result;
        }
        catch(ConstraintViolationException e) {
            if (tx != null) {
                tx.rollback();
            }
            throw new RuntimeException(e);
        }
        catch(Exception e) {
            if (tx != null) {
                tx.rollback();
            }
            throw e;
        }

    }
}
