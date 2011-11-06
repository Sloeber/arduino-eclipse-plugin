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
 * $Id: IVecsCategory.java 851 2010-08-07 19:37:00Z innot $
 *     
 *******************************************************************************/
package it.baeyens.avreclipse.devicedescription.avrio;

import it.baeyens.avreclipse.devicedescription.ICategory;
import it.baeyens.avreclipse.devicedescription.IEntry;

import java.util.Collections;
import java.util.List;


/**
 * Implements a ICategory for Interrupt Vector Elements.
 * 
 * @author Thomas Holland
 * 
 * @see PortCategory
 * @see RegisterCategory
 */
public class IVecsCategory extends BaseEntry implements ICategory {

	// The indices for Register Entry column fields
	final static int			IDX_NAME		= 0;
	final static int			IDX_SIGNAME		= 1;
	final static int			IDX_DESCRIPTION	= 2;
	final static int			IDX_VECTOR		= 3;

	// The labels for Register Entry column data fields
	final static String			STR_NAME		= "Name";
	final static String			STR_SIGNAME		= "Old Name";
	final static String			STR_DESCRIPTION	= "Description";
	final static String			STR_VECTOR		= "Vector";

	final static String[]		fLabels			= { STR_NAME, STR_SIGNAME, STR_DESCRIPTION,
			STR_VECTOR							};
	final static int[]			fDefaultWidths	= { 20, 20, 35, 7 };

	public final static String	CATEGORY_NAME	= "Interrupts";

	/**
	 * Instantiate a new IVecsCategory. The name is fixed to {@value #CATEGORY_NAME}
	 */
	public IVecsCategory() {
		super.setName(CATEGORY_NAME);
	}

	/**
	 * @return the name of this Category (fixed to {@value #CATEGORY_NAME})
	 */
	@Override
	public String getName() {
		return CATEGORY_NAME;
	}

	public int getColumnCount() {
		return fLabels.length;
	}

	public String[] getColumnLabels() {
		String[] labels = new String[fLabels.length];
		System.arraycopy(fLabels, 0, labels, 0, fLabels.length);
		return labels;
	}

	public int[] getColumnDefaultWidths() {
		int[] widths = new int[fDefaultWidths.length];
		System.arraycopy(fDefaultWidths, 0, widths, 0, fDefaultWidths.length);
		return widths;
	}

	@Override
	public List<IEntry> getChildren() {
		// The list of IVecs looks better when sorted
		// TODO: remove as soon as the view knows how to sort according to user
		// input
		List<IEntry> tmplist = super.getChildren();
		Collections.sort(tmplist, new IEntry.EntryColumnComperator(0));
		return tmplist;
	}
}
