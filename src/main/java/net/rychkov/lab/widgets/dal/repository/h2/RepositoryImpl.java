package net.rychkov.lab.widgets.dal.repository.h2;

import net.rychkov.lab.widgets.dal.model.Widget;
import net.rychkov.lab.widgets.dal.model.WidgetDelta;
import net.rychkov.lab.widgets.dal.repository.ConstraintViolationException;
import net.rychkov.lab.widgets.dal.repository.WidgetRepository;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Repository;

import javax.transaction.NotSupportedException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

@Repository("h2")
public class RepositoryImpl implements WidgetRepository {

    /**
     * JPA repository
     */
    private final DbWidgetRepository db;

    ClassLoader cl;

    /**
     * Sort for z-coordinate
     */
    private final Sort zSort;

    /**
     * Transactions lock (only one transaction at same time)
     */
    private final ReentrantLock txLock;

    public RepositoryImpl(DbWidgetRepository db) {
        this.db = db;
        this.zSort = Sort.by(Sort.Direction.ASC, "z");
        txLock = new ReentrantLock();
    }

    // region Read

    public net.rychkov.lab.widgets.dal.model.Page<Widget> getAll(Pageable pageable) {
        Page<Widget> page = db.findAll(pageable);

        net.rychkov.lab.widgets.dal.model.Page<Widget> result =
                new net.rychkov.lab.widgets.dal.model.Page<Widget>(
                        pageable.getPageNumber(),
                        pageable.getPageSize(),
                        page.getTotalPages(),
                        (int)page.getTotalElements(),
                        page.get().collect(Collectors.toList())
                );

        return result;

    }

    @Override
    public Collection<Widget> getAllOrderByZ() {
        return db.findAll(zSort);
    }

    @Override
    public net.rychkov.lab.widgets.dal.model.Page<Widget> getAllOrderByZ(int pageNum, int pageSize) {
        return getAll(PageRequest.of(pageNum,pageSize, zSort));
    }

    @Override
    public Integer getMaxZ() {
        return db.getMaxZ();
    }

    @Override
    public Collection<Widget> getFilteredByRectangle(int x1, int y1, int x2, int y2) throws NotSupportedException {
        throw new NotSupportedException();
    }

    @Override
    public Widget get(int id) {
        return db.findById(id).orElse(null);
    }

    // endregion

    // region Write

    @Override
    public Widget add(WidgetDelta widgetDelta) throws ConstraintViolationException {

        if(widgetDelta==null) {
            throw new IllegalArgumentException();
        }

        Widget newWidget = widgetDelta.createNewWidget(0);
        db.saveAndFlush(newWidget);
        return newWidget;
    }

    @Override
    public Collection<Widget> addAll(Collection<WidgetDelta> deltas) throws ConstraintViolationException {
        try {
            Collection<Widget> result = db.saveAll(deltas.stream().map(d -> d.createNewWidget(0)).collect(Collectors.toList()));
            db.flush();
            return result;
        }
        catch(org.hibernate.exception.ConstraintViolationException e) {
            throw new ConstraintViolationException(e);
        }
    }

    @Override
    public Widget remove(int id) {
        Widget widget = db.getOne(id);
        db.deleteById(id);
        db.flush();
        return widget;
    }

    @Override
    public Collection<Widget> removeAll(Collection<Integer> ids) {
        Collection<Widget> result = db.findAllById(ids);
        db.deleteAll(result);
        db.flush();
        return result;
    }

    @Override
    public Widget update(int widgetId, WidgetDelta widgetDelta) throws IllegalArgumentException, NoSuchElementException, ConstraintViolationException {

        if(widgetDelta==null) {
            throw new IllegalArgumentException();
        }

        Widget origin = db.getOne(widgetId);
        Widget newWidget = widgetDelta.createUpdatedWidget(origin);
        db.saveAndFlush(newWidget);
        return newWidget;
    }

    @Override
    public Collection<Widget> updateAll(Map<Integer, WidgetDelta> changes) throws IllegalArgumentException, NoSuchElementException, ConstraintViolationException {

        List<Widget> origin = db.findAllById(changes.keySet());
        List<Widget> updated = origin.stream().map(w -> changes.get(w.getId()).createUpdatedWidget(w)).collect(Collectors.toList());
        db.saveAll(updated);
        db.flush();
        return updated;
    }

    // endregion
}
