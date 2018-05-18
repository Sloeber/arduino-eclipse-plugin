/*******************************************************************************
 * Copyright (c) 2015 QNX Software Systems and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package io.sloeber.core.managers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
@SuppressWarnings("unused") 
public class HierarchicalProperties {

    private String value;
    private Map<String, HierarchicalProperties> children;

    public HierarchicalProperties() {
    }

    public HierarchicalProperties(Properties properties) {
	for (Map.Entry<Object, Object> entry : properties.entrySet()) {
	    String key = (String) entry.getKey();
	    String value1 = (String) entry.getValue();
	    putProperty(key, value1);
	}
    }

    public String getProperty(String qualifiedKey) {
	if (this.children == null) {
	    return null;
	}

	int i = qualifiedKey.indexOf('.');
	if (i < 0) {
	    HierarchicalProperties child = this.children.get(qualifiedKey);
	    return child != null ? child.getValue() : null;
	}
	String key = qualifiedKey.substring(0, i);
	HierarchicalProperties child = this.children.get(key);
	if (child != null) {
	    String childKey = qualifiedKey.substring(i + 1);
	    return child.getProperty(childKey);
	}
	return null;
    }

    public void putProperty(String qualifiedKey, String value1) {
	if (this.children == null) {
	    this.children = new HashMap<>();
	}

	int i = qualifiedKey.indexOf('.');
	if (i < 0) {
	    HierarchicalProperties child = this.children.get(qualifiedKey);
	    if (child == null) {
		child = new HierarchicalProperties();
		this.children.put(qualifiedKey, child);
		child.setValue(value1);
	    }
	} else {
	    String key = qualifiedKey.substring(0, i);
	    HierarchicalProperties child = this.children.get(key);
	    if (child == null) {
		child = new HierarchicalProperties();
		this.children.put(key, child);
	    }
	    String childKey = qualifiedKey.substring(i + 1);
	    child.putProperty(childKey, value1);
	}
    }

    public String getValue() {
	return this.value;
    }

    public void setValue(String value) {
	this.value = value;
    }

    public Map<String, HierarchicalProperties> getChildren() {
	return this.children;
    }

    public HierarchicalProperties getChild(String key) {
	return this.children != null ? this.children.get(key) : null;
    }

    public void putChild(String key, HierarchicalProperties node) {
	if (this.children == null) {
	    this.children = new HashMap<>();
	}
	this.children.put(key, node);
    }

    public List<HierarchicalProperties> listChildren() {
	int size = 0;
	for (Map.Entry<String, HierarchicalProperties> entry : this.children.entrySet()) {
	    try {
		int i = Integer.parseInt(entry.getKey());
		if (i + 1 > size) {
		    size = i + 1;
		}
	    } catch (NumberFormatException e) {
		// ignore
	    }
	}

	ArrayList<HierarchicalProperties> list = new ArrayList<>(size);
	for (Map.Entry<String, HierarchicalProperties> entry : this.children.entrySet()) {
	    try {
		int i = Integer.parseInt(entry.getKey());
		list.set(i, entry.getValue());
	    } catch (NumberFormatException e) {
		// ignore
	    }
	}
	return list;
    }

    public void setChildren(List<HierarchicalProperties> list) {
	this.children.clear();
	for (int i = 0; i < list.size(); i++) {
	    HierarchicalProperties node = list.get(i);
	    if (node != null) {
		this.children.put(Integer.toString(i), node);
	    }
	}
    }

    public Properties flatten() {
	Properties properties = new Properties();
	flatten(null, this, properties);
	return properties;
    }

    private static void flatten(String prefix, HierarchicalProperties tree, Properties props) {
	if (tree.getValue() != null && prefix != null) {
	    props.put(prefix, tree.getValue());
	}

	if (tree.getChildren() != null) {
	    for (Map.Entry<String, HierarchicalProperties> entry : tree.getChildren().entrySet()) {
		String childPrefix = entry.getKey();
		if (prefix != null) {
		    childPrefix = prefix + "." + childPrefix; //$NON-NLS-1$
		}
		flatten(childPrefix, entry.getValue(), props);
	    }
	}
    }

}
