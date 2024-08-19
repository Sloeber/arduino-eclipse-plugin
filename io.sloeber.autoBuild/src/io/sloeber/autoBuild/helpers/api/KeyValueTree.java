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
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;

public class KeyValueTree {

	private String myValue;
	private String myKey;
	private LinkedHashMap<String, KeyValueTree> myChildren =null;
	private KeyValueTree myParent;

	public static KeyValueTree createRoot() {
		return new KeyValueTree((String)null,(String) null);
	}

	private KeyValueTree(String newKey, String newValue) {
		myChildren = new  LinkedHashMap<>();
		myKey = newKey;
		myValue = newValue;
		myParent = null;
	}

	/**
	 * Copy constructor from this location (parent of source is ignored
	 * Note this copy constructor assumes the strings themselves are not changed.
	 *
	 * @param source
	 */
	public KeyValueTree(KeyValueTree source) {
		this( source, null);
	}

	private KeyValueTree(KeyValueTree source, KeyValueTree parent) {
		myChildren = new LinkedHashMap<>();
		myKey = source.myKey;
		myValue = source.myValue;
		myParent = parent;
		copyChildren(source.myChildren);
	}

	private void copyChildren(Map<String, KeyValueTree> children) {
		if(children==null) {
			return;
		}
		for(Entry<String, KeyValueTree> curChild:children.entrySet()) {
			myChildren.put(curChild.getKey(),new KeyValueTree(curChild.getValue(),this));
		}
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
		return addChild(newKey,(String) null);
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

	public Map<String, String> toKeyValues(String prefix) {
		return toKeyValues(prefix, this);
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

	public IResource getResource(IProject project) {
		String resourceID = getValue(KEY_RESOURCE);
		String resourceType = getValue(KEY_RESOURCE_TYPE);
		switch (resourceType) {
		case KEY_FILE:
			return project.getFile(resourceID);
		case KEY_FOLDER:
			return project.getFolder(resourceID);
		default:
		case KEY_PROJECT:
			return project;
		}
	}

	public KeyValueTree addChild(String key, IResource resource) {
		if(resource==null) {
			return null;
		}
		String resourceID = resource.getProjectRelativePath().toString();
		KeyValueTree ret = addChild(String.valueOf(key));
		ret.addValue(KEY_RESOURCE, resourceID);
		if (resource instanceof IFolder) {
			ret.addValue(KEY_RESOURCE_TYPE, KEY_FOLDER);
		}
		if (resource instanceof IFile) {
			ret.addValue(KEY_RESOURCE_TYPE, KEY_FILE);
		}
		if (resource instanceof IProject) {
			ret.addValue(KEY_RESOURCE_TYPE, KEY_PROJECT);
		}
		return ret;
	}
}