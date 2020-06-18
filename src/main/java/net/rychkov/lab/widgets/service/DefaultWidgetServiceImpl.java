package net.rychkov.lab.widgets.service;

import net.rychkov.lab.widgets.api.model.WidgetCreateRequest;
import net.rychkov.lab.widgets.api.model.WidgetUpdateRequest;
import net.rychkov.lab.widgets.dal.model.*;
import net.rychkov.lab.widgets.dal.repository.ConstraintViolationException;
import net.rychkov.lab.widgets.dal.repository.WidgetRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.transaction.NotSupportedException;
import javax.validation.constraints.NotNull;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class DefaultWidgetServiceImpl implements WidgetService {

    /**
     * Widget repository
     */
    private final WidgetRepository repository;

    // Best solution - move LockService outside and inject as bean
    // because it can move to another service/server (like as lock throw DB locks)
    // but not now
    /**
     * Used for shifting widgets (z conflicts)
     * Write only lock
     */
    private final ReentrantLock zLock;

    @Value("${widgets.pagesize}")
    private final Integer pageSize = 10;

    /**
     * z-coordinate step for new widgets (max z of existed widgets plus step)
     * will help in future updates
     */
    private static final int Z_STEP = 16;

    public DefaultWidgetServiceImpl(@Qualifier("repository") WidgetRepository repository) {
        this.repository = repository;
        zLock = new ReentrantLock();
    }

    /**
     * Lock z-index changes
     */
    private void lockZ() {
        zLock.lock();
    }

    /**
     * Unlock z-index changes
     */
    private void unlockZ() {
        zLock.unlock();
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
    private void shift(WidgetDelta widgetToBeInserted, final List<Widget> all) throws ConstraintViolationException {
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

                Map<Integer, WidgetDelta> updateQueue = new HashMap<>();

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
                    updateQueue.put(all.get(i).getId(), changes);
                }

                if((float)updateQueue.size()/allSize>0.5) {
                    // optimisation: force rebuild z-index (only part)
                    updateQueue.clear();
                    for(int i=all.size()-1; i>=insertPosition; i--) {
                        WidgetDelta widgetDelta = new WidgetDelta(null, null,widgetToBeInserted.getZ()+Z_STEP*(i-insertPosition+1), null, null);
                        updateQueue.put(all.get(i).getId(), widgetDelta);
                        //repository.update(all.get(i).getId(), widgetDelta);
                    }
                }

                repository.updateAll(updateQueue);

            }

        }
    }

    @Override
    public Widget addWidget(WidgetCreateRequest widget) {

        // mapping
            WidgetDelta delta = new WidgetDelta();
        delta.setHeight(widget.getHeight());
        delta.setWidth(widget.getWidth());
        delta.setX(widget.getX());
        delta.setY(widget.getY());
        delta.setZ(widget.getZ());


        // adding
        lockZ();
        try {
            
            if (delta.getZ()==null) {

                Integer maxZ = repository.getMaxZ();

                // if no widgets in repository (maxZ==null) - set 0 z-coordinate
                // else set max z-coordinate of widgets plus Z_STEP constant
                delta.setZ(maxZ!=null ? maxZ+Z_STEP : 0);
            }
            else {
                List<Widget> all = new ArrayList<>(repository.getAllOrderByZ());
                int allSize = all.size();

                // z-coordinate not set
                shift(delta, all);
            }

            Widget result = repository.add(delta);

            return result;
        }
        catch(ConstraintViolationException e) {
            throw new RuntimeException(e);
        }
        finally {
            unlockZ();
        }
    }

    @Override
    public Widget removeWidget(int id){
        lockZ();
        try {
            return repository.remove(id);
        }
        finally {
            unlockZ();
        }
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

        Widget origin = repository.get(widgetId);

        if (origin == null) {
            throw new NoSuchElementException("No widget found");
        }

        lockZ();

        try {

            // no z-coordinate changes
            if (delta.getZ() == origin.getZ()) {
                Widget result = repository.update(widgetId, delta);
                return result;
            }

            List<Widget> all = new ArrayList(repository.getAllOrderByZ());
            all.remove(origin);
            int insertPosition = findInsertPosition(delta.getZ(), all);

            // no z-coordinate conflicts
            if (insertPosition>=all.size() || all.get(insertPosition).getZ() > delta.getZ()) {
                Widget result = repository.update(widgetId, delta);
                return result;
            }

            // z-coordinate conflict
            shift(delta, all);

            Widget result = repository.update(widgetId, delta);

            return result;
        }
        catch(ConstraintViolationException e) {
            throw new RuntimeException(e);
        }
        finally {
            unlockZ();
        }

    }
}
