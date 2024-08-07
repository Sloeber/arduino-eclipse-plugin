package io.sloeber.core.api.Json;

public abstract class Node {
    public boolean hasChildren() {
        Node[] children = getChildren();
        if (children == null)
            return false;
        return children.length > 0;
    }

    abstract public Node[] getChildren();

    abstract public Node getParent();

    abstract public String getNodeName();

    abstract public String getID();

}
