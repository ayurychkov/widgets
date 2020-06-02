package net.rychkov.lab.widgets.dal.model;

public class WidgetChange {

    private int id;

    private WidgetDelta delta;

    public WidgetDelta getDelta() {
        return delta;
    }

    public void setDelta(WidgetDelta delta) {
        this.delta = delta;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public WidgetChange(int id, WidgetDelta delta) {
        this.id = id;
        this.delta = delta;
    }
}
