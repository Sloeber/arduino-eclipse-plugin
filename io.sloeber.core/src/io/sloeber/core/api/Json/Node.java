package io.sloeber.core.api.Json;

import org.eclipse.core.runtime.IPath;

public abstract class Node {
    public boolean hasChildren() {
        Node[] children = getChildren();
        if (children == null)
            return false;
        return children.length > 0;
    };

    abstract public Node[] getChildren();

    abstract public Node getParent();

    abstract public String getName();

    abstract public String getID();

    @SuppressWarnings("static-method")
    public IPath getInstallPath() {
        return null;
    };
}
