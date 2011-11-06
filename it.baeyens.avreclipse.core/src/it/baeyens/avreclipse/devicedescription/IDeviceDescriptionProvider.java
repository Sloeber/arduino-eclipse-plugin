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
 * $Id: IDeviceDescriptionProvider.java 851 2010-08-07 19:37:00Z innot $
 *     
 *******************************************************************************/
package it.baeyens.avreclipse.devicedescription;

import it.baeyens.avreclipse.core.IMCUProvider;

import org.eclipse.core.runtime.IPath;


/**
 * An <code>IDeviceDescriptionProvider</code>
 * 
 * @author Thomas Holland
 * 
 */
public interface IDeviceDescriptionProvider extends IMCUProvider {

	/**
	 * Returns a {@link IDeviceDescription} Object for the Device with the given
	 * name.
	 * 
	 * The name must be one of the names in the list returned by
	 * {@link #getDeviceList()}.
	 * 
	 * @param name
	 *            The name of the requested device.
	 * @return {@link IDeviceDescription} Object of <code>null</code> if the
	 *         name was not known or any error occurred reading / parsing the
	 *         underlying source file(s)
	 */
	public IDeviceDescription getDeviceDescription(String name);

	/**
	 * Get the Path of the directory where the device description comes from.
	 * 
	 * This method should be used together with
	 * {@link IDeviceDescription#getSourcesList()} to get a fully qualified path
	 * to the original source. This then can be used by the viewer to open the
	 * corresponding file upon user request.
	 * 
	 * @return IPath to the base directory
	 */
	public IPath getBasePath();

	/**
	 * Returns any stored error messages.
	 * 
	 * If either {@link #getMCUList()} {@link #getMCUInfo(String)} returns
	 * null, this method can be called to retrieve a message describing the
	 * error.
	 * 
	 * @return String The stored error message or null if no error stored.
	 */
	public String getErrorMessage();

	/**
	 * Adds a Provider change listener to this Provider.
	 * 
	 * The listener is called whenever the internal data for the provider has
	 * changed, e.g. when the user changes some preference settings.
	 * 
	 * @param pcl
	 *            The IProviderChangeListener to add
	 */
	public void addProviderChangeListener(IProviderChangeListener pcl);

	/**
	 * Removes a Provider change listener from this provider.
	 * 
	 * @param pcl
	 *            The IProviderChangeListener to remove
	 */
	public void removeProviderChangeListener(IProviderChangeListener pcl);
}
