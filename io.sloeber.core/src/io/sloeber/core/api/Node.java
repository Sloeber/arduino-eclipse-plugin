package io.sloeber.core.api;

public interface Node {
    boolean hasChildren();

    Object[] getChildren();

    Object getParent();

    String getName();
}