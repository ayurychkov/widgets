package net.rychkov.lab.widgets.dal.repository.CustomInMemory;

import net.rychkov.lab.widgets.dal.model.Widget;
import net.rychkov.lab.widgets.dal.model.WidgetDelta;

import java.util.Collection;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.Function;

public class FieldIndex<TField> extends RepositoryIndex {

    private final SortedMap<TField, Integer> map;

    private final Function<Widget, TField> fieldSelector;

    private final Function<WidgetDelta, TField> fieldSelectorForDelta;

    public FieldIndex(final String name, boolean unique, Function<Widget, TField> fieldSelector, Function<WidgetDelta, TField> fieldSelectorForDelta) {
        super(name, unique);
        map = new TreeMap<>();
        this.fieldSelector = fieldSelector;
        this.fieldSelectorForDelta = fieldSelectorForDelta;
    }

    @Override
    public void add(final Widget widget) {
        map.put(fieldSelector.apply(widget), widget.getId());
    }

    @Override
    public void remove(final Widget widget) {
        map.remove(fieldSelector.apply(widget));
    }

    @Override
    public boolean isAffected(final WidgetDelta changes) {
        return changes!=null && fieldSelectorForDelta.apply(changes)!=null;
    }

    @Override
    public Collection<Integer> get() {
        return map.values();
    }

    @Override
    public boolean checkConstrainsViolation(final WidgetDelta widget) {
        TField key = fieldSelectorForDelta.apply(widget);
        return map.containsKey(key);
    }

    public int size() {
        return map.size();
    }

    public TField lastKey() {
        return map.lastKey();
    }
}
