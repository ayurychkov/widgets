package net.rychkov.lab.widgets.service;

import net.rychkov.lab.widgets.api.model.WidgetCreateRequest;
import net.rychkov.lab.widgets.api.model.WidgetUpdateRequest;
import net.rychkov.lab.widgets.dal.model.Page;
import net.rychkov.lab.widgets.dal.model.Widget;
import net.rychkov.lab.widgets.dal.repository.ConstraintViolationException;

import javax.transaction.NotSupportedException;
import java.util.Collection;
import java.util.NoSuchElementException;

public interface WidgetService {

    /**
     * Get widget by ID
     * @param id Widget ID
     * @return Widget
     */
    Widget getWidget(int id);

    /**
     * Get collection of all widgets in repository
     * @return Collection of widgets
     */
    Collection<Widget> getAllWidgets();

    /**
     * Get slice of all widgets in repository (using page size from config)
     * @param pageNum Page number
     * @return Page of widgets
     */
    Page<Widget> getAllWidgets(int pageNum);

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
     * Add widget to repository
     * @param widget WidgetCreateRequest contains values for setting to properties and nulls for defaults
     * @return Created widget
     */
    Widget addWidget(final WidgetCreateRequest widget);

    /**
     * Update widget in repository
     * @param widgetId Widget ID
     * @param widget WidgetUpdateRequest contains values for setting to properties and nulls for others
     * @return Updated widget
     * @throws IllegalArgumentException Illegal widgetDelta like null
     * @throws NoSuchElementException No widget with such ID in repository
     * @throws ConstraintViolationException Constrain violation like non unique z-coordinate
     */
    Widget updateWidget(int widgetId, final WidgetUpdateRequest widget) throws IllegalArgumentException, NoSuchElementException, ConstraintViolationException;

    /**
     * Remove widget from repository
     * @param id Widget ID
     * @return Removed widget
     */
    Widget removeWidget(int id);
}
