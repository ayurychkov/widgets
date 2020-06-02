package net.rychkov.lab.widgets.dal.model;

import org.springframework.data.annotation.Immutable;

import java.util.Collection;

@Immutable
public final class Page<T> {
    private final int pageNum;
    private final int pageSize;
    private final int pageCount;
    private final int elementCount;
    private final Collection<T> elements;

    public Page(int pageNum, int pageSize, int pageCount, int elementCount, final Collection<T> elements) {
        this.pageNum = pageNum;
        this.pageSize = pageSize;
        this.pageCount = pageCount;
        this.elementCount = elementCount;
        this.elements = elements;
    }

    public int getPageNum() {
        return pageNum;
    }

    public int getPageSize() {
        return pageSize;
    }

    public int getPageCount() {
        return pageCount;
    }

    public int getElementCount() {
        return elementCount;
    }

    public Collection<T> getElements() {
        return elements;
    }
}
