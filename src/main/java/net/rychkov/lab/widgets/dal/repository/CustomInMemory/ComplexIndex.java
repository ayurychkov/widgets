package net.rychkov.lab.widgets.dal.repository.CustomInMemory;

import net.rychkov.lab.widgets.dal.model.Widget;
import net.rychkov.lab.widgets.dal.model.WidgetDelta;

import java.util.*;

public class ComplexIndex extends RepositoryIndex {

    private final TreeMap<Integer, TreeMap<Integer, ArrayList<Integer>>> map;

    public ComplexIndex() {
        super("Spatial index",false);
        map = new TreeMap<>();
    }

    //region Point helpers

    private void addPoint(int x, int y, int widgetId) {
        map.computeIfAbsent(x, k -> new TreeMap<>())
                .computeIfAbsent(y, k -> new ArrayList<>())
                .add(widgetId);
    }

    private void deletePoint(int x, int y, int widgetId) {
        TreeMap<Integer, ArrayList<Integer>> yLayer = map.get(x);
        ArrayList<Integer> widgets = yLayer.get(y);
        widgets.remove(widgets.indexOf(widgetId));

        // clear if needed
        if (widgets.isEmpty()) {
            yLayer.remove(y);
        }

        if(yLayer.isEmpty()) {
            map.remove(x);
        }
    }

    //endregion

    @Override
    public void add(final Widget widget) {
        // left top corner
        addPoint(widget.getX()-widget.getWidth()/2, widget.getY()-widget.getHeight()/2, widget.getId());
        // right bottom corner
        addPoint(widget.getX()+widget.getWidth()/2, widget.getY()+widget.getHeight()/2, widget.getId());
    }

    @Override
    public void remove(final Widget widget) {
        // left top corner
        deletePoint(widget.getX()-widget.getWidth()/2, widget.getY()-widget.getHeight()/2, widget.getId());
        // right bottom corner
        deletePoint(widget.getX()+widget.getWidth()/2, widget.getY()+widget.getHeight()/2, widget.getId());
    }

    @Override
    public boolean isAffected(final WidgetDelta changes) {
        return changes!=null &&
                (
                    changes.getX()!=null ||
                    changes.getY()!=null ||
                    changes.getWidth()!=null ||
                    changes.getHeight()!=null
                );
    }

    @Override
    public Collection<Integer> get() {

        Set<Integer> result = new HashSet<>();

        map.values().forEach(x->
                x.values().forEach(result::addAll)
        );

        return result;
    }

    public Collection<Integer> getFilteredByRectangle(int x1, int y1, int x2, int y2) {

        // include high borders
        int includedBottomBorder = y2+1;
        int includedRightBorder = x2+1;

        HashSet<Integer> hash = new HashSet<>();
        ArrayList<Integer> result = new ArrayList<>();

        Collection<TreeMap<Integer, ArrayList<Integer>>> y_rows =
                map.tailMap(x1).headMap(includedRightBorder).values();

        y_rows.forEach(x->
                x.tailMap(y1).headMap(includedBottomBorder).values().forEach(y->
                        y.forEach(w->
                        {
                            if (!hash.add(w)) {
                                result.add(w);
                            }
                        })
                )
        );

        return result;
    }
}
