package net.rychkov.lab.widgets.dal.repository;

import net.rychkov.lab.widgets.dal.model.Page;
import net.rychkov.lab.widgets.dal.model.Widget;
import net.rychkov.lab.widgets.dal.model.WidgetDelta;

import javax.transaction.NotSupportedException;
import java.util.Collection;
import java.util.NoSuchElementException;

public interface WidgetRepository {

    /**
     * Get widget by ID
     * @param id Widget ID
     * @return Widget (or null)
     */
    Widget get(int id);

    /**
     * Get collection of all widgets in repository
     * @return Collection of widgets
     */
    Collection<Widget> getAll();

    /**
     * Get slice of all widgets in repository
     * @param pageNum Page number
     * @param pageSize Elements per page
     * @return Page of widgets
     */
    Page<Widget> getAll(int pageNum, int pageSize);

    /**
     * Get collection of all widgets in repository, ordered by z-coordinate (ASC)
     * @return Collection of widgets
     */
    Collection<Widget> getAllOrderByZ();

    /**
     * Get slice of all widgets in repository, ordered by z-coordinate (ASC)
     * @param pageNum Page number
     * @param pageSize Elements per page
     * @return Page of widgets
     */
    Page<Widget> getAllOrderByZ(int pageNum, int pageSize);

    /**
     * Get widgets that fall entirely into the region
     * @param x1 Left border (included)
     * @param y1 Top border (included)
     * @param x2 Right border (included)
     * @param y2 Bottom border (included)
     * @return Collection of widgets
     * @throws NotSupportedException Repository implementation don't support this filter
     */
    Collection<Widget> getFilteredByRectangle(int x1, int y1, int x2, int y2) throws NotSupportedException;

    /**
     * Start transaction, witch change repository state (throw add, update, remove)
     * @return Transaction
     */
    WidgetRepositoryTransaction BeginTransaction();

    /**
     * Add widget to repository
     * @param delta WidgetDelta contains values for setting to properties and nulls for defaults
     * @return Created widget
     * @throws ConstraintViolationException Constrains violation like non unique z-coordinate
     */
    Widget add(WidgetDelta delta) throws ConstraintViolationException;

    /**
     * Remove widget from repository
     * @param id Widget ID
     * @return Removed widget
     */
    Widget remove(int id);

    /**
     * Update widget in repository
     * @param widgetId Widget ID
     * @param widgetDelta WidgetDelta contains values for setting to properties and nulls for others
     * @return Updated widget
     * @throws IllegalArgumentException Illegal widgetDelta like null
     * @throws NoSuchElementException No widget with such ID in repository
     * @throws ConstraintViolationException Constrain violation like non unique z-coordinate
     */
    Widget update(int widgetId, WidgetDelta widgetDelta)
            throws IllegalArgumentException, NoSuchElementException, ConstraintViolationException;
}
