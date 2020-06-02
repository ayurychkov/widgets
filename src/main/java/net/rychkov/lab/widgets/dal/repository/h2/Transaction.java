package net.rychkov.lab.widgets.dal.repository.h2;

import net.rychkov.lab.widgets.dal.repository.h2.RepositoryImpl;
import net.rychkov.lab.widgets.dal.repository.WidgetRepositoryTransaction;

public class Transaction implements WidgetRepositoryTransaction {

    private final net.rychkov.lab.widgets.dal.repository.h2.RepositoryImpl repository;

    private final long id;

    public Transaction(long txId, RepositoryImpl repository) {
        this.id = txId;
        this.repository = repository;
    }

    @Override
    public long getId() {
        return id;
    }

    @Override
    public void commit() {
        repository.txCommit(this);
    }

    @Override
    public void rollback() {
        repository.txRollback(this);
    }

}
