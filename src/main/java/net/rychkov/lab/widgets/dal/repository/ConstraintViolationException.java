package net.rychkov.lab.widgets.dal.repository;

public class ConstraintViolationException extends Exception {

    public ConstraintViolationException() { super(); }

    public ConstraintViolationException(String message) { super(message); }

    public ConstraintViolationException(Exception e) { super(e); }
}
