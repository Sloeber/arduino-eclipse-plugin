/*******************************************************************************
 * 
 * Copyright (c) 2007, 2010 Thomas Holland (thomas@innot.de) and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the GNU Public License v3
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     Thomas Holland - initial API and implementation
 *     
 * $Id: BaseEntry.java 851 2010-08-07 19:37:00Z innot $
 *     
 *******************************************************************************/
package it.baeyens.avreclipse.devicedescription.avrio;

import it.baeyens.avreclipse.devicedescription.IEntry;

import java.util.ArrayList;
import java.util.List;


/**
 * This abstract class implements all methods of the IEntry interface.
 * Also some additional methods used by all Entries of the AVR io.h devicemodel are included. 
 * 
 * @author Thomas Holland
 * 
 */
public abstract class BaseEntry implements IEntry {

	private IEntry fParent;
	private String fName;
	private String fDescription;

	private List<IEntry> fChildren = null;

	public BaseEntry() {
		fChildren = new ArrayList<IEntry>();
	}

	/**
	 * Convenience Constructor that automatically adds this Object to its
	 * parent.
	 * 
	 * @param parent
	 *            The Parent IEntry
	 */
	public BaseEntry(IEntry parent) {
		this();
		fParent = parent;
		if (parent != null) {
			parent.addChild(this);
		}
	}

	public void setName(String name) {
		fName = name;
	}

	public String getName() {
		if (fName != null) {
			return fName;
		}
		return "";
	}

	// Tree handling methods

	public List<IEntry> getChildren() {
		// make a copy of the children list, as the caller may modify this list
		// (e.g. sorting)
		if (fChildren != null) {
			return new ArrayList<IEntry>(fChildren);
		}
		return null;
	}

	public void setParent(IEntry parent) {
		fParent = parent;
	}

	public IEntry getParent() {
		return fParent;
	}

	public boolean hasChildren() {
		return fChildren.size() > 0;
	}

	public void addChild(IEntry child) {
		if (child != null) {
			fChildren.add(child);
			child.setParent(this);
		}
	}

	// Column data handling methods

	public void setColumnData(int index, String data) {
		switch (index) {
		case 0:
			fName = data;
			break;
		case 1:
			fDescription = data;
			break;
		}
	}

	/**
	 * Returns the data for columns 0 (Name) and 1 (Description)
	 * 
	 * @param index 0 to get the name and 1 to get the description of this entry
	 * @return String Object with the data
	 * 
	 * @see IEntry#getColumnData(int)
	 * 
	 */
	public String getColumnData(int index) {
		// the two items name and description are common to all AVRioh
		// Categories
		switch (index) {
		case 0:
			return getName();
		case 1:
			return getDescription();
		}
		return null;
	}

	// Methods not inherited from the IEntry Interface
	// These are methods to set/get Column data elements that
	// are common to all AVRioh entries

	/**
	 * Sets the description of this Element This is just a convenience method
	 * for <code>setColumnData(1, description)</code>
	 */
	protected void setDescription(String description) {
		fDescription = description;
	}

	/**
	 * Gets the description of this Element. This is just a convenience method
	 * for <code>getColumnData(1)</code>
	 * 
	 * @return String with the description of the entry.
	 */
	protected String getDescription() {
		if (fDescription != null)
			return fDescription;
		return "";
	}




	@Override
	public String toString() {
		// For the debugger
		return fName;
	}
}
