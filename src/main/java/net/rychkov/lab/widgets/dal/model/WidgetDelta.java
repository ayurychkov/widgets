package net.rychkov.lab.widgets.dal.model;

import javax.validation.constraints.NotNull;
import java.util.Date;

/**
 * Difference between two widgets
 * has differences of XYZ-coordinates, width and height
 */
public class WidgetDelta {

    private Integer x;
    private Integer y;
    private Integer z;
    private Integer width;
    private Integer height;

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

    public WidgetDelta() {};

    public WidgetDelta(Integer x, Integer y, Integer z, Integer width, Integer height ) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.width = width;
        this.height = height;
    }

    /**
     * Has changes for any field
     * @return true - any field not null, otherwise - false
     */
    public boolean hasChanges() {
        return x!=null || y!=null || z!=null || width!=null || height!=null;
    }

    /**
     * Check applying this delta to widget has any effects (change any fields)
     * @param widget Widget for check applying
     * @return true - if applying has effects on widget, otherwise - false
     */
    public boolean checkChanging(Widget widget) {
        return !(
                (x==null || x==widget.getX()) &&
                (y==null || y==widget.getY()) &&
                (z==null || z==widget.getZ()) &&
                (width==null || width==widget.getWidth()) &&
                (height==null || height==widget.getHeight())
        );
    }

    @Override
    public boolean equals(Object o) {
        if(o.getClass()==WidgetDelta.class) {
            WidgetDelta other = (WidgetDelta)o;
            return x==other.x && y==other.y && z==other.z && width==other.width && height==other.height;
        }

        return super.equals(o);
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        result.append("{");

        result.append("x:");
        result.append(this.x);

        result.append(", y:");
        result.append(this.y);

        result.append(", z:");
        result.append(this.z);

        result.append(", width:");
        result.append(this.width);

        result.append(", height:");
        result.append(this.height);

        result.append("}");

        return result.toString();
    }

    /**
     * Create widget by delta
     * @param id Widget ID
     * @return Created widget
     */
    public Widget createNewWidget(int id) {
        // no changes
        if(!this.hasChanges()) {
            return null;
        }

        // create new widget by changes (no origin widget)
        return new Widget(
                id,
                this.getX()!=null ? this.getX() : 0,
                this.getY()!=null ? this.getY() : 0,
                this.getZ()!=null ? this.getZ() : 0,
                this.getWidth()!=null ? this.getWidth() : 0,
                this.getHeight()!=null ? this.getHeight() : 0,
                new Date()
        );
    }

    public Widget applyTo(Widget origin) {
        return new Widget(
                origin.getId(),
                this.getX()!=null ? this.getX() : origin.getX(),
                this.getY()!=null ? this.getY() : origin.getY(),
                this.getZ()!=null ? this.getZ() : origin.getZ(),
                this.getWidth()!=null ? this.getWidth() : origin.getWidth(),
                this.getHeight()!=null ? this.getHeight() : origin.getHeight(),
                new Date()
        );
    }

    /**
     * Create updated widget (new version) by delta or return null - if nothing changed
     * @param origin Original widget
     * @return Updated widget or null - if nothing changed
     */
    public Widget createUpdatedWidget(@NotNull Widget origin) {

        // no changes
        if(!this.hasChanges()) {
            return null;
        }

        // no changes
        if(!checkChanging(origin)) {
            return null;
        }

        // create new widget by original widget and changes
        return new Widget(
                origin.getId(),
                this.getX()!=null ? this.getX() : origin.getX(),
                this.getY()!=null ? this.getY() : origin.getY(),
                this.getZ()!=null ? this.getZ() : origin.getZ(),
                this.getWidth()!=null ? this.getWidth() : origin.getWidth(),
                this.getHeight()!=null ? this.getHeight() : origin.getHeight(),
                new Date()
        );
    }


}
