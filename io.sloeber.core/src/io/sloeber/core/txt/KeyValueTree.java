package io.sloeber.core.txt;

import java.util.AbstractMap.SimpleEntry;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import io.sloeber.core.common.Const;

public class KeyValueTree extends Const {

    private String value;
    private String key;
    private LinkedHashMap<String, KeyValueTree> children;
    private KeyValueTree parent;

    public static KeyValueTree createTxtRoot() {
        return new KeyValueTree(null, null);
    }

    private KeyValueTree(String newKey, String newValue) {
        children = new LinkedHashMap<>();
        key = newKey;
        value = newValue;
        parent = null;
    }

    public KeyValueTree getParent() {
        return this.parent;
    }

    public LinkedHashMap<String, KeyValueTree> getChildren() {
        return this.children;
    }

    public KeyValueTree addChild(String newKey, String newValue) {
        KeyValueTree child = new KeyValueTree(newKey, newValue);
        child.parent = this;
        children.put(newKey, child);
        return child;
    }

    public KeyValueTree addChild(String newKey) {
        return addChild(newKey, null);
    }

    public String getKey() {
        return this.key;
    }

    public String getValue() {
        return this.value;
    }

    public void setValue(String data) {
        this.value = data;
    }

    @Override
    public String toString() {
        return getKey() + EQUAL + getValue();
    }


    private String toStringLine(KeyValueTree theRoot) {
        if ((null == getValue()) || (this == theRoot)) {
            // leaves without a value should not be logged
            return EMPTY;
        }
        String seperator = EMPTY;
        String ret = EQUAL + getValue();
        KeyValueTree current = this;
        while ((current.getParent() != null) && (current != theRoot)) {
            ret = current.getKey() + seperator + ret;
            current = current.getParent();
            seperator = DOT;
        }
        return ret + NEWLINE;
    }

    Map<String, String> toKeyValues(boolean addParents) {
        KeyValueTree theRoot = this;
        if (addParents) {
            theRoot = null;
        }
        return toKeyValues(EMPTY, theRoot);
    }

    public Map<String, String> toKeyValues(String prefix, boolean addParents) {
        KeyValueTree theRoot = this;
        if (addParents) {
            theRoot = null;
        }
        return toKeyValues(prefix, theRoot);
    }

    public String dump() {
        return dump(this);
    }

    private String dump(KeyValueTree theRoot) {

        String stringRepresentation = toStringLine(theRoot);
        for (KeyValueTree node : getChildren().values()) {
            stringRepresentation += node.dump(theRoot);
        }
        return stringRepresentation;
    }

    private Map<String, String> toKeyValues(String prefix, KeyValueTree theRoot) {

        Map<String, String> ret = new HashMap<>();
        Entry<String, String> firstEntry = this.toKeyValue(prefix, theRoot);
        if (firstEntry != null) {
            ret.put(firstEntry.getKey(), firstEntry.getValue());
        }
        for (KeyValueTree node : getChildren().values()) {
            if ((theRoot == this) && MENU.equals(node.getKey())) {
                // skip menu entries at the first level
                continue;
            }
            ret.putAll(node.toKeyValues(prefix, theRoot));
        }
        return ret;
    }

    private Entry<String, String> toKeyValue(String prefix, KeyValueTree theRoot) {
        if (this == theRoot || null == key || null == value) {
            return null;
        }

        String seperator = EMPTY;
        String keyString = EMPTY;
        KeyValueTree current = this;
        while ((current.getParent() != null) && (current != theRoot)) {
            keyString = current.getKey() + seperator + keyString;
            current = current.getParent();
            seperator = DOT;
        }

        return new SimpleEntry<>(prefix + keyString, value);

    }

    /**
     * Get the child based on a key
     * 
     * @param childKey
     *            (can contain dots)
     * @return the child corresponding to the key or a empty child if the key is not
     *         found
     */
    public KeyValueTree getChild(String childKey) {
        KeyValueTree child = getChildInternal(childKey);
        if (null == child) {
            return createTxtRoot();
        }
        return child;
    }

    /**
     * Search the child with this key and return the value associated with this
     * child
     * 
     * @param childKey
     *            the key to the child. this key can contain dots to get subchildren
     * 
     * @return the value of the found node or an empty string
     */
    public String getValue(String childKey) {
        KeyValueTree child = getChildInternal(childKey);
        if (null == child) {
            return EMPTY;
        }
        return child.value;
    }

    private KeyValueTree getChildInternal(String childKey) {
        String[] subKeys = childKey.split("\\."); //$NON-NLS-1$
        KeyValueTree curChild = this;
        for (String curSubKey : subKeys) {
            curChild = curChild.children.get(curSubKey);
            if (null == curChild) {
                return null;
            }
        }
        return curChild;
    }

    public void addValue(String theKey, String TheValue) {
        String[] subKeys = theKey.split("\\."); //$NON-NLS-1$
        KeyValueTree curChild = this;
        for (String curSubKey : subKeys) {
            KeyValueTree nextChild = curChild.children.get(curSubKey);
            if (null == nextChild) {
                nextChild = curChild.addChild(curSubKey);
            }
            curChild = nextChild;
        }
        curChild.setValue(TheValue);
    }

}