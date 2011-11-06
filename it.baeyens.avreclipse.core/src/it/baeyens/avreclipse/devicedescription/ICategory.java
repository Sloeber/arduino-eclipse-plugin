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
 * $Id: ICategory.java 851 2010-08-07 19:37:00Z innot $
 *     
 *******************************************************************************/
package it.baeyens.avreclipse.devicedescription;

/**
 * Describes a category of a AVR DeviceDescription.
 * <p>
 * The category stands for items like "registers", "ports" etc. A category has
 * some {@link IEntry} children and sub-children, describing for example a
 * single register and (optionally) a single bit within that register.
 * </p>
 * <p>
 * It is the top visible level in a tabbed view and provides the labels for the
 * column data of its children.
 * </p>
 * <p>
 * All child handling methods are inherited from {@link IEntry}
 * 
 * @author Thomas Holland
 * 
 */
public interface ICategory extends IEntry {

	/** 
	 * Returns the number of columns children of this ICategory have data for.
	 * This includes the standard columns 0 and 1 for "name" and "description".
	 * 
	 * @return Number of columns supported by this ICategory.
	 */
	public int getColumnCount();

	/**
	 * Get the labels of the columns.
	 * <p>
	 * Each IEntry keeps - besides its name and usually its description - some
	 * additional data in the form of columns. This method returns labels for
	 * the columns.
	 * </p>
	 * <p>
	 * It is up to the implementors to ensure that the labels actually match the
	 * data stored in the column data of the children.
	 * </p>
	 */
	public String[] getColumnLabels();

	/**
	 * Get the default width of each column in chars.
	 * <p>
	 * To give the viewer some idea on how wide the columns should be, this methods returns
	 * the number of characters that each column will display (at the default setting).
	 * The user can adjust the widths of the columns, so ultimate precision is not required.
	 * 
	 * @return Number of characters to display in each column.
	 */
	public int[] getColumnDefaultWidths();
	
}
