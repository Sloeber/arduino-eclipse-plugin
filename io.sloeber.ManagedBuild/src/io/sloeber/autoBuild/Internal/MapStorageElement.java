/*******************************************************************************
 * Copyright (c) 2007, 2016 Intel Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * Intel Corporation - Initial API and implementation
 * James Blackburn (Broadcom Corp.)
 * IBM Corporation
 *******************************************************************************/
package io.sloeber.autoBuild.Internal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.cdt.core.settings.model.ICStorageElement;
import org.eclipse.core.runtime.CoreException;

public class MapStorageElement implements ICStorageElement {
	private HashMap<String, String> fMap;
	private String fName;
	private MapStorageElement fParent;
	private static final String CHILDREN_KEY = "?children?"; //$NON-NLS-1$
	private static final String NAME_KEY = "?name?"; //$NON-NLS-1$
	private static final String VALUE_KEY = "?value?"; //$NON-NLS-1$
	private List<MapStorageElement> fChildren = new ArrayList<>();
	private String fValue;

	public MapStorageElement(String name, MapStorageElement parent) {
		fName = name;
		fParent = parent;
		fMap = new HashMap<>();
	}

	public MapStorageElement(Map<String, String> map, MapStorageElement parent) {
		fName = map.get(getMapKey(NAME_KEY));
		fValue = map.get(getMapKey(VALUE_KEY));
		fMap = new HashMap<>(map);
		fParent = parent;

		String children = map.get(getMapKey(CHILDREN_KEY));
		if (children != null) {
			Set<String> childrenStrList = decodeList(children);
				for (String curChild:childrenStrList) {
					Map<String, String> childMap = decodeMap(curChild);
					MapStorageElement child = createChildElement(childMap);
					fChildren.add(child);
				}
		}
	}

	protected MapStorageElement createChildElement(Map<String, String> childMap) {
		return new MapStorageElement(childMap, this);
	}

	protected String getMapKey(String name) {
		return name;
	}

	public Map<String, String> toStringMap() {
		@SuppressWarnings("unchecked")
		Map<String, String> map = (Map<String, String>) fMap.clone();
		if (fName != null)
			map.put(getMapKey(NAME_KEY), fName);
		else
			map.remove(getMapKey(NAME_KEY));

		if (fValue != null)
			map.put(getMapKey(VALUE_KEY), fValue);
		else
			map.remove(getMapKey(VALUE_KEY));

		int size = fChildren.size();
		if (size != 0) {
			List<String> childrenStrList = new ArrayList<>(size);
			for (int i = 0; i < size; i++) {
				MapStorageElement child = fChildren.get(i);
				Map<String, String> childStrMap = child.toStringMap();
				String str = encodeMap(childStrMap);
				childrenStrList.add(str);
			}

			String childrenStr = encodeList(childrenStrList);
			map.put(getMapKey(CHILDREN_KEY), childrenStr);
		} else {
			map.remove(getMapKey(CHILDREN_KEY));
		}

		return map;
	}

	protected boolean isSystemKey(String key) {
		return key.indexOf('?') == 0 && key.lastIndexOf('?') == key.length() - 1;
	}

	@Override
	public void clear() {
		fMap.clear();
	}

	@Override
	public ICStorageElement createChild(String name) {
		MapStorageElement child = createChildElement(name);
		fChildren.add(child);
		return child;
	}

	protected MapStorageElement createChildElement(String name) {
		return new MapStorageElement(name, this);
	}

	@Override
	public String getAttribute(String name) {
		Object o = fMap.get(getMapKey(name));
		if (o instanceof String)
			return (String) o;
		return null;
	}

	@Override
	public boolean hasAttribute(String name) {
		return fMap.containsKey(getMapKey(name));
	}

	@Override
	public ICStorageElement[] getChildren() {
		return fChildren.toArray(new MapStorageElement[fChildren.size()]);
	}

	@Override
	public ICStorageElement[] getChildrenByName(String name) {
		List<ICStorageElement> children = new ArrayList<>();
		for (ICStorageElement child : fChildren)
			if (name.equals(child.getName()))
				children.add(child);
		return new ICStorageElement[children.size()];
	}

	@Override
	public boolean hasChildren() {
		return !fChildren.isEmpty();
	}

	@Override
	public String getName() {
		return fName;
	}

	@Override
	public ICStorageElement getParent() {
		return fParent;
	}

	@Override
	public void removeChild(ICStorageElement child) {
		fChildren.remove(child);
		if (child instanceof MapStorageElement) {
			((MapStorageElement) child).removed();
		}
	}

	private void removed() {
		fParent = null;
	}

	@Override
	public void removeAttribute(String name) {
		fMap.remove(getMapKey(name));
	}

	@Override
	public void setAttribute(String name, String value) {
		fMap.put(getMapKey(name), value);
	}

	public static HashMap<String, String> decodeMap(String value) {
		Set<String> list = decodeList(value);
		HashMap<String, String> map = new HashMap<>();
		char escapeChar = '\\';

		for (String curString: list) {
			StringBuilder line = new StringBuilder(curString);
			int lndx = 0;
			while (lndx < line.length()) {
				if (line.charAt(lndx) == '=') {
					if (line.charAt(lndx - 1) == escapeChar) {
						// escaped '=' - remove '\' and continue on.
						line.deleteCharAt(lndx - 1);
					} else {
						break;
					}
				}
				lndx++;
			}
			map.put(line.substring(0, lndx),					line.substring(lndx + 1));
		}

		return map;

	}

	public static Set<String> decodeList(String value) {
		Set<String> ret =  new HashSet<>();
		if (value != null) {
			StringBuilder envStr = new StringBuilder(value);
			String escapeChars = "|\\"; //$NON-NLS-1$
			char escapeChar = '\\';
			try {
				while (envStr.length() > 0) {
					int ndx = 0;
					while (ndx < envStr.length()) {
						if (escapeChars.indexOf(envStr.charAt(ndx)) != -1) {
							if (envStr.charAt(ndx - 1) == escapeChar) {
								// escaped '|' - remove '\' and continue on.
								envStr.deleteCharAt(ndx - 1);
								if (ndx == envStr.length()) {
									break;
								}
							}
							if (envStr.charAt(ndx) == '|')
								break;
						}
						ndx++;
					}
					StringBuilder line = new StringBuilder(envStr.substring(0, ndx));
					/*					int lndx = 0;
										while (lndx < line.length()) {
											if (line.charAt(lndx) == '=') {
												if (line.charAt(lndx - 1) == escapeChar) {
													// escaped '=' - remove '\' and continue on.
													line.deleteCharAt(lndx - 1);
												} else {
													break;
												}
											}
											lndx++;
										}
					*/
					ret.add(line.toString());
					envStr.delete(0, ndx + 1);
				}
			} catch (StringIndexOutOfBoundsException e) {
			}
		}
		return ret;
	}

	public static String encodeMap(Map<String, String> values) {
		Iterator<Entry<String, String>> entries = values.entrySet().iterator();
		StringBuilder str = new StringBuilder();
		while (entries.hasNext()) {
			Entry<String, String> entry = entries.next();
			str.append(escapeChars(entry.getKey(), "=|\\", '\\')); //$NON-NLS-1$
			str.append("="); //$NON-NLS-1$
			str.append(escapeChars(entry.getValue(), "|\\", '\\')); //$NON-NLS-1$
			str.append("|"); //$NON-NLS-1$
		}
		return str.toString();
	}

	public static String encodeList(List<String> values) {
		StringBuilder str = new StringBuilder();
		Iterator<String> entries = values.iterator();
		while (entries.hasNext()) {
			String entry = entries.next();
			str.append(escapeChars(entry, "|\\", '\\')); //$NON-NLS-1$
			str.append("|"); //$NON-NLS-1$
		}
		return str.toString();
	}

	public static String escapeChars(String string, String escapeChars, char escapeChar) {
		StringBuilder str = new StringBuilder(string);
		for (int i = 0; i < str.length(); i++) {
			if (escapeChars.indexOf(str.charAt(i)) != -1) {
				str.insert(i, escapeChar);
				i++;
			}
		}
		return str.toString();
	}

	@Override
	public String getValue() {
		return fValue;
	}

	@Override
	public void setValue(String value) {
		fValue = value;
	}

	@Override
	public ICStorageElement importChild(ICStorageElement el) throws UnsupportedOperationException {
		// TODO
		throw new UnsupportedOperationException();
	}

	@Override
	public String[] getAttributeNames() {
		List<String> list = new ArrayList<>(fMap.size());
		Set<Entry<String, String>> entrySet = fMap.entrySet();
		for (Entry<String, String> entry : entrySet) {
			String key = entry.getKey();
			if (!isSystemKey(key)) {
				list.add(key);
			}
		}

		return list.toArray(new String[list.size()]);
	}

	@Override
	public ICStorageElement createCopy() throws UnsupportedOperationException, CoreException {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean equals(ICStorageElement other) {
		throw new UnsupportedOperationException();
	}
}
