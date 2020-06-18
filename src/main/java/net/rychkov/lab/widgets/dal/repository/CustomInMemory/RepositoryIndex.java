package net.rychkov.lab.widgets.dal.repository.CustomInMemory;

import net.rychkov.lab.widgets.dal.model.Widget;
import net.rychkov.lab.widgets.dal.model.WidgetDelta;

import java.util.Collection;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public abstract class RepositoryIndex {

    private final ReentrantReadWriteLock lock;

    private final boolean unique;

    private final String name;

    public RepositoryIndex(final String name, boolean unique) {
        this.lock = new ReentrantReadWriteLock();
        this.unique = unique;
        this.name = name;
    }

    public boolean isUnique() {
        return this.unique;
    }

    public String getName() {
        return this.name;
    }

    public void readLock() {
        lock.readLock().lock();
    }

    public void readUnlock() {
        lock.readLock().unlock();
    }

    public void writeLock() {
        lock.writeLock().lock();
    }

    public void writeUnlock() {
        lock.writeLock().unlock();
    }

    public boolean checkConstrainsViolation(final WidgetDelta widget) {return false;}

    public abstract void add(final Widget data);

    public abstract void remove(final Widget data);

    public abstract boolean isAffected(final WidgetDelta changes);

    public abstract Collection<Integer> get();
}
