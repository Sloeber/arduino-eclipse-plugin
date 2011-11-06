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
 * $Id: IDeviceDescription.java 851 2010-08-07 19:37:00Z innot $
 *     
 *******************************************************************************/
package it.baeyens.avreclipse.devicedescription;

import java.io.Serializable;
import java.util.Comparator;
import java.util.List;

/**
 * Describes a single AVR Device.
 * <p>
 * This is the root of a tree structure. The IDevice has some {@link ICategory}
 * children, containing the different sections of the description like
 * registers, ports etc. Each category has some {@link IEntry} children and
 * sub-children, describing for example a single register and (optionally) a
 * single bit within that register.
 * </p>
 * 
 * @author Thomas Holland
 * 
 */
public interface IDeviceDescription {

	/**
	 * Sets the name of the Device.
	 */
	public void setName(String name);

	/**
	 * Gets the name of the Device.
	 */
	public String getName();

	/**
	 * Get the different Categories that this IDeviceDescription has.
	 * <p>
	 * The returned List is a copy of the internal list and may be modified
	 * (e.g. sorted) as required.
	 * </p>
	 * 
	 * @return A List Object.
	 */
	public List<ICategory> getCategories();

	/**
	 * Get the source files for this IDeviceDescription
	 * <p>
	 * Only the path relative to some base directory is given. Call
	 * {@link IDeviceDescriptionProvider#getBasePath()} to get the base path.
	 * </p>
	 * 
	 * @return List of Strings with the pathnames.
	 */
	public List<String> getSourcesList();
	
	/**
	 * Compare two IDeviceDescription Objects. Used for sorting the list of
	 * devices.
	 */
	public static class DeviceComperator implements Comparator<IDeviceDescription>, Serializable {

		private static final long serialVersionUID = -3408289142416567290L;

		public int compare(IDeviceDescription o1, IDeviceDescription o2) {
			return o1.getName().compareTo(o2.getName());
		}
	}

}
