package net.rychkov.lab.widgets.dal.repository.h2;

import net.rychkov.lab.widgets.dal.model.Widget;
import net.rychkov.lab.widgets.dal.model.WidgetDelta;
import net.rychkov.lab.widgets.dal.repository.ConstraintViolationException;
import net.rychkov.lab.widgets.dal.repository.WidgetRepository;
import net.rychkov.lab.widgets.dal.repository.WidgetRepositoryTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import javax.transaction.NotSupportedException;
import java.util.Collection;
import java.util.Date;
import java.util.NoSuchElementException;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

@Repository("h2")
public class RepositoryImpl implements WidgetRepository {

    /**
     * JPA repository
     */
    private final DbWidgetRepository db;

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
    public Collection<Widget> getAll() {
        return db.findAll();
    }

    @Override
    public net.rychkov.lab.widgets.dal.model.Page<Widget> getAll(int pageNum, int pageSize) {
        return getAll(PageRequest.of(pageNum,pageSize));
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
    public Collection<Widget> getFilteredByRectangle(int x1, int y1, int x2, int y2) throws NotSupportedException {
        throw new NotSupportedException();
    }

    @Override
    public Widget get(int id) {
        return db.findById(id).orElse(null);
    }

    // endregion

    // region Transaction

    @Override
    public WidgetRepositoryTransaction BeginTransaction() {
        txLock.lock();
        return new Transaction(0, this);
    }

    public void txCommit(Transaction tx) {
        db.flush();
        txLock.unlock();
    }

    public void txRollback(Transaction tx) {
        txLock.unlock();
    }

    // endregion

    // region Write

    @Override
    public Widget add(WidgetDelta widgetDelta) throws ConstraintViolationException {

        if(widgetDelta==null) {
            throw new IllegalArgumentException();
        }

        Widget newWidget = widgetDelta.createNewWidget(0);
        db.save(newWidget);
        return newWidget;
    }

    @Override
    public Widget remove(int id) {
        Widget widget = db.getOne(id);
        db.deleteById(id);
        return widget;
    }

    @Override
    public Widget update(int widgetId, WidgetDelta widgetDelta) throws IllegalArgumentException, NoSuchElementException, ConstraintViolationException {

        if(widgetDelta==null) {
            throw new IllegalArgumentException();
        }

        Widget origin = db.getOne(widgetId);
        Widget newWidget = widgetDelta.createUpdatedWidget(origin);
        db.save(newWidget);
        return newWidget;
    }

    // endregion
}
