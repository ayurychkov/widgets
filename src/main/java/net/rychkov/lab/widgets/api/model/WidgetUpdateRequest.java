package net.rychkov.lab.widgets.api.model;

public class WidgetUpdateRequest {

    private Integer x;
    private Integer y;
    private Integer z;

    private Integer width;
    private Integer height;

    public WidgetUpdateRequest() {}

    public WidgetUpdateRequest(Integer x, Integer y, Integer z, Integer width, Integer height) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.width = width;
        this.height = height;
    }

    //region Getters-Setters

    public Integer getX() {
        return x;
    }

    public void setX(Integer x) {
        this.x = x;
    }

    public Integer getY() {
        return y;
    }

    public void setY(Integer y) {
        this.y = y;
    }

    public Integer getZ() {
        return z;
    }

    public void setZ(Integer z) {
        this.z = z;
    }

    public Integer getWidth() {
        return width;
    }

    public void setWidth(Integer width) {
        this.width = width;
    }

    public Integer getHeight() {
        return height;
    }

    public void setHeight(Integer height) {
        this.height = height;
    }

    //endregion
}
