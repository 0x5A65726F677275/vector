package com.artofvector.workflow.model;

public final class Port {

    public enum Direction {
        INPUT, OUTPUT
    }

    private final String id;
    private final String label;
    private final Direction direction;

    public Port(String id, String label, Direction direction) {
        this.id = id;
        this.label = label;
        this.direction = direction;
    }

    public String id() {
        return id;
    }

    public String label() {
        return label;
    }

    public Direction direction() {
        return direction;
    }

    public boolean isInput() {
        return direction == Direction.INPUT;
    }

    public boolean isOutput() {
        return direction == Direction.OUTPUT;
    }
}
