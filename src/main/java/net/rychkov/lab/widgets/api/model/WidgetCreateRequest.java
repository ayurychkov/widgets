package net.rychkov.lab.widgets.api.model;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

public class WidgetCreateRequest {

    @NotNull(message = "Please provide widget x-coordinate")
    private Integer x;

    @NotNull(message = "Please provide widget y-coordinate")
    private Integer y;

    @NotNull(message = "Please provide widget z-coordinate")
    private Integer z;

    @NotNull(message = "Please provide widget width")
    @Min(1)
    private Integer width;

    @NotNull(message = "Please provide widget height")
    @Min(1)
    private Integer height;

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

    public WidgetCreateRequest() {}

    public WidgetCreateRequest(Integer x, Integer y, Integer z, Integer width, Integer height) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.width = width;
        this.height = height;
    }

    //endregion
}
