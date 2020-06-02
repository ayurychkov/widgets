package net.rychkov.lab.widgets.dal.repository;

import net.rychkov.lab.widgets.dal.model.Widget;
import net.rychkov.lab.widgets.dal.model.WidgetDelta;

import java.util.Collection;
import java.util.NoSuchElementException;

public interface WidgetRepositoryTransaction {

    long getId();

    void commit();

    void rollback();
}
