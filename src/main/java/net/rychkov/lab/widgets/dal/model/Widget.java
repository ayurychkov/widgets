package net.rychkov.lab.widgets.dal.model;

import org.springframework.data.annotation.Immutable;
import org.springframework.data.annotation.LastModifiedDate;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;

/**
 * Widget(rectangle) on plane
 * has ID, XY-coordinates (top left corner), Width, Height,
 * z-coordinate (higher - closer to observer) and last modification date
 */
@Immutable
@Entity
@Access(AccessType.FIELD)
public final class Widget implements Cloneable, Serializable {

    /**
     * Widget ID
     */
    @Id
    @Column
    @GeneratedValue
    private int id;

    /**
     * X-coordinate of center
     */
    @Column
    private int x;

    /**
     * Y-coordinate of center
     */
    @Column
    private int y;

    /**
     * Z-coordinate
     */
    @Column
    private int z;

    /**
     * Width (x-dimension)
     */
    @Column
    private int width;

    /**
     * Height (y-dimension)
     */
    @Column
    private int height;

    /**
     * Last modification date
     */
    @Column
    @LastModifiedDate
    private Date lastModificationDate;

    public Widget() {}

    /**
     * @param id Widget ID
     * @param x X-coordinate of center
     * @param y y-coordinate of center
     * @param z z-coordinate
     * @param width Widget width (x-dimension)
     * @param height Widget height (y-dimension)
     * @param lastModificationDate Last modification date
     */
    public Widget(int id, int x, int y, int z, int width, int height, Date lastModificationDate) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.z = z;
        this.width = width;
        this.height = height;
        this.lastModificationDate = lastModificationDate;
    }

    /**
     * Get widget ID
     * @return ID
     */
    public int getId() {
        return id;
    }

    /**
     * Get widget x-coordinate (rectangle center)
     * @return x-coordinate
     */
    public int getX() {
        return x;
    }

    /**
     * Get widget y-coordinate (rectangle center)
     * @return y-coordinate
     */
    public int getY() {
        return y;
    }

    /**
     * Get widget z-coordinate (higher the value, the higher the widget lies on the plane)
     * @return z-coordinate
     */
    public int getZ() {
        return z;
    }

    /**
     * Get widget width
     * @return Width
     */
    public int getWidth() {
        return width;
    }

    /**
     * Get widget height
     * @return Height
     */
    public int getHeight() {
        return height;
    }

    /**
     * Get widget's last modification date
     * @return Last modification date
     */
    public Date getLastModificationDate() {
        return lastModificationDate;
    }

    /**
     * Get widget deep clone
     * @return Deep clone
     */
    @Override
    public Widget clone() {
        try {
            return (Widget) super.clone();
        }
        catch(CloneNotSupportedException e) {
            return new Widget(this.id, this.x, this.y, this.z, this.width, this.height, this.lastModificationDate);
        }
    }
}
