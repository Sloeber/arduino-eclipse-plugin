package io.sloeber.autoBuild.helpers.api;

import static io.sloeber.autoBuild.helpers.api.AutoBuildConstants.*;

import java.util.AbstractMap.SimpleEntry;
import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.apache.commons.io.FileUtils;

public class KeyValueTree {

	private String myValue;
	private String myKey;
	private Map<String, KeyValueTree> myChildren;
	private KeyValueTree myParent;

	public static KeyValueTree createRoot() {
		return new KeyValueTree(null, null);
	}

	private KeyValueTree(String newKey, String newValue) {
		myChildren = new TreeMap<>();
		myKey = newKey;
		myValue = newValue;
		myParent = null;
	}

	public KeyValueTree getParent() {
		return this.myParent;
	}

	public Map<String, KeyValueTree> getChildren() {
		return this.myChildren;
	}

	public KeyValueTree addChild(String newKey, String newValue) {
		KeyValueTree child = new KeyValueTree(newKey, newValue);
		child.myParent = this;
		myChildren.put(newKey, child);
		return child;
	}

	public KeyValueTree addChild(String newKey) {
		return addChild(newKey, null);
	}

	public String getKey() {
		return this.myKey;
	}

	public String getValue() {
		return this.myValue;
	}

	public void setValue(String data) {
		this.myValue = data;
	}

	@Override
	public String toString() {
		return getKey() + EQUAL + getValue();
	}

	private String toStringLine(KeyValueTree theRoot) {
		if ((null == getValue()) || (this == theRoot)) {
			// leaves without a value should not be logged
			return EMPTY_STRING;
		}
		String seperator = EMPTY_STRING;
		String ret = EQUAL + getValue();
		KeyValueTree current = this;
		while ((current.getParent() != null) && (current != theRoot)) {
			ret = current.getKey() + seperator + ret;
			current = current.getParent();
			seperator = DOT;
		}
		return ret + NEWLINE;
	}

	public Map<String, String> toKeyValues(boolean addParents) {
		KeyValueTree theRoot = this;
		if (addParents) {
			theRoot = null;
		}
		return toKeyValues(EMPTY_STRING, theRoot);
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

		Map<String, String> ret = new LinkedHashMap<>();
		Entry<String, String> firstEntry = this.toKeyValue(prefix, theRoot);
		if (firstEntry != null) {
			ret.put(firstEntry.getKey(), firstEntry.getValue());
		}
		for (KeyValueTree node : getChildren().values()) {
			ret.putAll(node.toKeyValues(prefix, theRoot));
		}
		return ret;
	}

	private Entry<String, String> toKeyValue(String prefix, KeyValueTree theRoot) {
		if (this == theRoot || null == myKey || null == myValue) {
			return null;
		}

		String seperator = EMPTY_STRING;
		String keyString = EMPTY_STRING;
		KeyValueTree current = this;
		while ((current.getParent() != null) && (current != theRoot)) {
			keyString = current.getKey() + seperator + keyString;
			current = current.getParent();
			seperator = DOT;
		}

		return new SimpleEntry<>(prefix + keyString, myValue);

	}

	/**
	 * Get the child based on a key
	 *
	 * @param childKey (can contain dots)
	 * @return the child corresponding to the key or a empty child if the key is not
	 *         found
	 */
	public KeyValueTree getChild(String childKey) {
		KeyValueTree child = getChildInternal(childKey);
		if (null == child) {
			return createRoot();
		}
		return child;
	}

	/**
	 * Search the child with this key and return the value associated with this
	 * child
	 *
	 * @param childKey the key to the child. this key can contain dots to get
	 *                 subchildren
	 *
	 * @return the value of the found node or an empty string
	 */
	public String getValue(String childKey) {
		KeyValueTree child = getChildInternal(childKey);
		if (null == child) {
			return EMPTY_STRING;
		}
		return child.myValue;
	}

	private KeyValueTree getChildInternal(String childKey) {
		String[] subKeys = childKey.split("\\."); //$NON-NLS-1$
		KeyValueTree curChild = this;
		for (String curSubKey : subKeys) {
			curChild = curChild.myChildren.get(curSubKey);
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
			KeyValueTree nextChild = curChild.myChildren.get(curSubKey);
			if (null == nextChild) {
				nextChild = curChild.addChild(curSubKey);
			}
			curChild = nextChild;
		}
		curChild.setValue(TheValue);
	}

	/**
	 * given a key value file add the key values to this object
	 *
	 * @param boardsFileName
	 * @return
	 * @throws IOException
	 */
	public void mergeFile(File boardsFileName) throws IOException {
		if (!boardsFileName.exists()) {
			return;
		}

		String curConfigsText = FileUtils.readFileToString(boardsFileName, AUTOBUILD_CONFIG_FILE_CHARSET);
		String[] lines = curConfigsText.split(NEWLINE);
		for (String line : lines) {
			if ((line.length() == 0) || (line.charAt(0) == '#'))
				continue;

			String[] lineParts = line.split("=", 2); //$NON-NLS-1$
			if (lineParts.length == 2) {
				String key = lineParts[0].trim();
				String value = lineParts[1].trim();
				addValue(key, value);
			}
		}
	}

	public void removeChild(String name) {
		myChildren.remove(name);

	}

	public void removeKey(String deleteKey) {
		String[] subKeys = deleteKey.split("\\."); //$NON-NLS-1$
		KeyValueTree parentKeyTree = this;
		KeyValueTree foundKeyTree = this;
		for (String curSubKey : subKeys) {
			parentKeyTree=foundKeyTree;
			foundKeyTree = foundKeyTree.myChildren.get(curSubKey);
			if (null == foundKeyTree) {
				return ;
			}
		}
		parentKeyTree.myChildren.remove(foundKeyTree.getKey());
	}

}