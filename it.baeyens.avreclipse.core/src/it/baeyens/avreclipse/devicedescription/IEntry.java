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
 * $Id: IEntry.java 851 2010-08-07 19:37:00Z innot $
 *     
 *******************************************************************************/

package it.baeyens.avreclipse.devicedescription;

import java.io.Serializable;
import java.util.Comparator;
import java.util.List;

/**
 * Holds the description data for a single element of a IDeviceDescription.
 * <p>
 * This may be a single port or register, or a single bit thereof. The data for
 * each entry are arbitrary <code>String</code> Objects, named columns because
 * they will be displayed as columns by the view. Only column 0 has a fixed
 * meaning and always contains the name of the entry. The parent
 * {@link ICategory} element is responsible for assigning labels to the first
 * and the other column data fields.
 * </p>
 * <p>
 * The IEnries (and its extension ICategory, are organized in a tree and have
 * the usual methods to create and manage the tree. As this tree is static (no
 * dynamic changes after creation), there are no methods to remove children.
 * </p>
 * 
 * @author Thomas Holland
 * 
 */
public interface IEntry {

	/**
	 * Sets the name of the entry.
	 * <p>
	 * This has to be stored as column 0 in the columnar data.
	 * </p>
	 * 
	 * @param name
	 *            The name of the entry.
	 */
	public void setName(String name);

	/**
	 * Gets the name of the entry or an empty String ("") if no name has been
	 * assigned yet.
	 * <p>
	 * This is just a convenience method for <code>getColumnData(0)</code>.
	 * </p>
	 * 
	 * @return String Object
	 */
	public String getName();

	// Tree Management methods

	/**
	 * Checks if this entry has children.
	 * <p>
	 * This is just a convenience method for <code>(getChildren() != null). </p>
	 * @return boolean true if this object has children.
	 */
	public boolean hasChildren();

	/**
	 * Returns a List of all child entries or <code>null</code> if this entry
	 * has no children.
	 * <p>
	 * The list does not has to have a special order. It is up to the caller to
	 * sort the list as required (or according to user input).
	 * </p>
	 * <p>
	 * The returned List is a copy of the internal list and may be modified
	 * (e.g. sorted) as required.
	 * </p>
	 * 
	 * @return Array of IEntry objects or <code>null</code>
	 */
	public List<IEntry> getChildren();

	/**
	 * Returns the parent of this entry or <code>null</code> if this is the
	 * root entry.
	 * 
	 * @return IEntry object or <code>null</code>
	 */
	public IEntry getParent();

	/**
	 * Sets the parent of this child.
	 * 
	 * @param parent
	 *            The new IEntry parent.
	 */
	public void setParent(IEntry parent);

	/**
	 * Adds an IEntry as child to this entry.
	 * <p>
	 * It will automatically set the parent of the new child to this object.
	 * </p>
	 * 
	 * @param child
	 */
	public void addChild(IEntry child);

	// Methods to set/get the column data

	/**
	 * Changes the data field for column <code>index</code> to the given
	 * String.
	 * <p>
	 * Column 0 is reserved for the name, all other columns are up to the
	 * implementation (but the name can be overwritten by this method. Index
	 * values not supported will be ignored.
	 * </p>
	 * 
	 * @param index
	 *            Column number for the data.
	 * @param data
	 *            The new value.
	 */
	public void setColumnData(int index, String data);

	/**
	 * Returns the String data stored at the given index.
	 * <p>
	 * If no data is available for this index an empty String Object ("") will
	 * be returned. if the index is not supported, <code>null</code> is
	 * returned.
	 * </p>
	 * 
	 * @param index
	 *            Column number for the data.
	 * @return String Object or <code>null</code> if index out of bounds
	 */
	public String getColumnData(int index);
	
	// convenience class

	/**
	 * Compare two IEntry Objects according to the given column index. Used for
	 * sorting a list of entries.
	 * 
	 */
	public static class EntryColumnComperator implements Comparator<IEntry>, Serializable {

		private static final long serialVersionUID = 7886833825709323939L;

		private int fIndex = 0;

		/**
		 * New IEntry comperator which compares two IEntry Object according to
		 * the data in column <code>index</code>. Use <code>0</code> to compare 
		 * according to the name of the entries.
		 * 
		 * @param index
		 *            The column that will be compared
		 */
		public EntryColumnComperator(int index) {
			fIndex = index;
		}

		public int compare(IEntry o1, IEntry o2) {
			return o1.getColumnData(fIndex).compareTo(o2.getColumnData(fIndex));
		}
	}
}
