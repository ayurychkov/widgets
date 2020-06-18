package net.rychkov.lab.widgets.dal.repository.CustomInMemory;

import net.rychkov.lab.widgets.dal.model.Page;
import net.rychkov.lab.widgets.dal.model.Widget;
import net.rychkov.lab.widgets.dal.model.WidgetDelta;
import net.rychkov.lab.widgets.dal.repository.ConstraintViolationException;
import net.rychkov.lab.widgets.dal.repository.WidgetRepository;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@Repository("customInMemory")
public class RepositoryImpl implements WidgetRepository {

    /**
     * List of repository indexes
     * !!! Important: write-locking of multiple index MUST be in sequence of this array,
     * otherwise - deadlocks possible
     */
    private final List<RepositoryIndex> indexes;

    /**
     * Index for filter 'by rectangle'
     */
    private final ComplexIndex xyIndex;

    /**
     * Sorted by z-field
     */
    private final FieldIndex<Integer> zIndex;

    /**
     * Map id-widget
     */
    private final ConcurrentMap<Integer, AtomicReference<Widget>> widgets;

    /**
     * Sequence for widget's id
     */
    private final AtomicInteger idSequence;

    public RepositoryImpl() {

        idSequence = new AtomicInteger(1);

        widgets = new ConcurrentHashMap<>();

        zIndex = new FieldIndex<>("Z",true, Widget::getZ, WidgetDelta::getZ);
        xyIndex = new ComplexIndex();
        indexes = Arrays.asList(zIndex, xyIndex);
    }

    //region Read

    @Override
    public Widget get(int id) {
        AtomicReference<Widget> holder = widgets.get(id);
        return holder!=null ? holder.get() : null;
    }

    @Override
    public Collection<Widget> getAllOrderByZ() {
        zIndex.readLock();
        try {
            return zIndex.get().stream().map(widgetId -> widgets.get(widgetId).get()).collect(Collectors.toList());
        }
        finally {
            zIndex.readUnlock();
        }
    }

    @Override
    public Page<Widget> getAllOrderByZ(int pageNum, int pageSize) {
        zIndex.readLock();
        try {

            int elementCount = zIndex.size();

            return new Page<>(
                    pageNum,
                    pageSize,
                    (elementCount / pageSize) + 1,
                    elementCount,
                    zIndex.get().stream().skip(pageNum * pageSize).limit(pageSize)
                            .map(widgetId -> widgets.get(widgetId).get()).collect(Collectors.toList())
            );
        }
        finally {
            zIndex.readUnlock();
        }
    }

    @Override
    public Integer getMaxZ() {
        zIndex.readLock();
        try {
            // todo: make field?
            return zIndex.lastKey();
        }
        finally {
            zIndex.readUnlock();
        }
    }

    @Override
    public Collection<Widget> getFilteredByRectangle(int x1, int y1, int x2, int y2) {

        xyIndex.readLock();
        try {
            return xyIndex.getFilteredByRectangle(x1, y1, x2, y2).stream()
                    .map(widgetId -> widgets.get(widgetId).get())
                    .collect(Collectors.toList());
        }
        finally {
            xyIndex.readUnlock();
        }
    }

    //endregion

    //region Write

    @Override
    public Widget add(final WidgetDelta widgetDelta) throws ConstraintViolationException {

        // check validation
        if(widgetDelta==null) {
            throw new IllegalArgumentException("widgetDelta must be not null");
        }

        if(widgetDelta.getZ()==null) {
            throw new NullPointerException("Widget must have none null z-coordinate");
        }

        ArrayList<RepositoryIndex> lockedIndexes = new ArrayList<>();

        Widget createdWidget = null;

        try {
            // write-lock affected indexes in sequence of indexes array
            for (RepositoryIndex i : indexes) {

                i.writeLock();
                lockedIndexes.add(i);

                if(i.isUnique() && i.checkConstrainsViolation(widgetDelta)) {
                    throw new ConstraintViolationException("Not unique for " + i.getName());
                }
            }

            // create widget
            int newId = idSequence.getAndAdd(1);
            createdWidget = widgetDelta.createNewWidget(newId);

            widgets.put(newId, new AtomicReference<>(createdWidget));

            // add to affected indexes
            for (RepositoryIndex i : lockedIndexes) {
                i.add(createdWidget);
            }

        }
        finally {
            // unlock locked indexes
            for(RepositoryIndex i : lockedIndexes) {
                i.writeUnlock();
            }
        }

        return createdWidget;

    }

    @Override
    public Collection<Widget> addAll(Collection<WidgetDelta> deltas) throws ConstraintViolationException {

        if(! deltas.stream().map(d -> d.getZ()!=null).reduce((l,r) -> l && r).orElse(true) ) {
            throw new NullPointerException("Widget must have none null z-coordinate");
        }

        ArrayList<RepositoryIndex> lockedIndexes = new ArrayList<>();

        List<Widget> createdWidgets = new ArrayList<>();

        try {
            // write-lock affected indexes in sequence of indexes array
            for (RepositoryIndex i : indexes) {

                i.writeLock();
                lockedIndexes.add(i);

                if(i.isUnique()) {
                    if(! deltas.stream().map(i::checkConstrainsViolation).reduce((l, r) -> l && r).orElse(true) ) {
                        throw new ConstraintViolationException("Not unique for " + i.getName());
                    }
                }
            }

            // create widgets
            for(WidgetDelta wd : deltas) {
                Widget widget = wd.createNewWidget(idSequence.getAndAdd(1));
                createdWidgets.add(widget);
                widgets.put(widget.getId(), new AtomicReference<>(widget));
            }



            // add to affected indexes
            for (RepositoryIndex i : lockedIndexes) {
                createdWidgets.forEach(i::add);
            }

        }
        finally {
            // unlock locked indexes
            for(RepositoryIndex i : lockedIndexes) {
                i.writeUnlock();
            }
        }

        return createdWidgets;
    }

    @Override
    public Widget remove(int widgetId) {

        if(widgets.get(widgetId)==null) {
            throw new NoSuchElementException("No widgets with id "+widgetId);
        }

        ArrayList<RepositoryIndex> lockedIndexes = new ArrayList<>();

        Widget deletedWidget = null;

        try {
            // write-lock affected indexes in sequence of indexes array
            for (RepositoryIndex i : indexes) {
                i.writeLock();
                lockedIndexes.add(i);
            }


            deletedWidget = widgets.remove(widgetId).get();

            // add to affected indexes
            for (RepositoryIndex i : lockedIndexes) {
                i.remove(deletedWidget);
            }

        }
        finally {
            // unlock locked indexes
            for(RepositoryIndex i : lockedIndexes) {
                i.writeUnlock();
            }
        }

        return deletedWidget;
    }

    @Override
    public Collection<Widget> removeAll(Collection<Integer> ids) {

        ArrayList<RepositoryIndex> lockedIndexes = new ArrayList<>();

        List<Widget> deletedWidgets = new ArrayList<>();

        try {
            // write-lock affected indexes in sequence of indexes array
            for (RepositoryIndex i : indexes) {
                i.writeLock();
                lockedIndexes.add(i);
            }

            for (Integer id : ids) {
                deletedWidgets.add(widgets.remove(id).get());
            }

            // remove from affected indexes
            for (RepositoryIndex i : lockedIndexes) {
                deletedWidgets.forEach(i::remove);
            }

        }
        finally {
            // unlock locked indexes
            for(RepositoryIndex i : lockedIndexes) {
                i.writeUnlock();
            }
        }

        return deletedWidgets;
    }

    /**
     * Update widget by delta
     * @param widgetId Widget id
     * @param widgetDelta Widget changed properties (other - null)
     * @return Updated widget full description
     * @throws IllegalArgumentException WidgetDelta must be not null
     * @throws NoSuchElementException No widget with such id in repository
     */
    @Override
    public Widget update(int widgetId, final WidgetDelta widgetDelta)
            throws IllegalArgumentException, NoSuchElementException, ConstraintViolationException {

        if(widgetDelta==null) {
            throw new IllegalArgumentException("widgetDelta must be not null");
        }

        ArrayList<RepositoryIndex> lockedIndexes = new ArrayList<>();

        Widget changedWidget = null;

        try {
            // write-lock affected indexes in sequence of indexes array
            for (RepositoryIndex i : indexes) {
                if(i.isAffected(widgetDelta)) {
                    i.writeLock();
                    lockedIndexes.add(i);

                    if (i.isUnique() && i.checkConstrainsViolation(widgetDelta)) {
                        throw new ConstraintViolationException("Not unique for " + i.getName());
                    }
                }
            }

            // remove origin from indexes
            Widget origin = widgets.get(widgetId).get();
            for(RepositoryIndex i : lockedIndexes) {
                i.remove(origin);
            }

            // update widget
            changedWidget = widgets.get(widgetId).updateAndGet(widgetDelta::applyTo);

            // add to affected indexes
            for (RepositoryIndex i : lockedIndexes) {
                i.add(changedWidget);
            }

        }
        finally {
            // unlock locked indexes
            for(RepositoryIndex i : lockedIndexes) {
                i.writeUnlock();
            }
        }

        return changedWidget;
    }

    @Override
    public Collection<Widget> updateAll(Map<Integer, WidgetDelta> changes) throws IllegalArgumentException, NoSuchElementException, ConstraintViolationException {

        ArrayList<RepositoryIndex> lockedIndexes = new ArrayList<>();

        List<Widget> changedWidgets = new ArrayList<>();

        try {
            // write-lock affected indexes in sequence of indexes array
            for (RepositoryIndex i : indexes) {
                if(changes.values().stream().map(i::isAffected).reduce( (l,r) -> l || r ).orElse(false)) {
                    i.writeLock();
                    lockedIndexes.add(i);
                }
            }


            List<Widget> origin = changes.keySet().stream().map(id -> widgets.get(id).get()).collect(Collectors.toList());

            // remove origin from indexes
            for (RepositoryIndex lockedIndex : lockedIndexes) {
                for (Widget w : origin) {
                    if (lockedIndex.isAffected(changes.get(w.getId()))) {
                        lockedIndex.remove(w);
                    }
                }
            }

            // update widget
            for (Widget w : origin) {
                changedWidgets.add(widgets.get(w.getId()).updateAndGet(changes.get(w.getId())::applyTo));
            };

            // add to affected indexes
            for (RepositoryIndex lockedIndex : lockedIndexes) {
                for (Widget w : changedWidgets) {
                    if (lockedIndex.isAffected(changes.get(w.getId()))) {
                        lockedIndex.add(w);
                    }
                }
            }
        }
        finally {
            // unlock locked indexes
            for(RepositoryIndex i : lockedIndexes) {
                i.writeUnlock();
            }
        }

        return changedWidgets;
    }

    //endregion
}
