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
 * $Id: DeviceListContentProvider.java 851 2010-08-07 19:37:00Z innot $
 *     
 *******************************************************************************/
package it.baeyens.avreclipse.ui.views.avrdevice;

import it.baeyens.avreclipse.core.util.AVRMCUidConverter;
import it.baeyens.avreclipse.devicedescription.IDeviceDescriptionProvider;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.Viewer;


public class DeviceListContentProvider implements IStructuredContentProvider {

	private IDeviceDescriptionProvider	fDMprovider	= null;

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.viewers.IContentProvider#inputChanged(org.eclipse.jface.viewers.Viewer,
	 *      java.lang.Object, java.lang.Object)
	 */
	@Override
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		fDMprovider = (IDeviceDescriptionProvider) newInput;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.viewers.IStructuredContentProvider#getElements(java.lang.Object)
	 */
	@Override
	public Object[] getElements(Object inputElement) {
		Set<String> devicesset = null;
		try {
			devicesset = fDMprovider.getMCUList();
		} catch (IOException e) {
			// do nothing. deviceset remains at null which causes the method to fail gracefully.
		}
		if (devicesset == null) {
			// if the list is null, an internal Provider Error has occurred.
			String[] empty = { "" };
			return empty;
		}

		// Convert to an List so that it can be sorted
		List<String> devices = new ArrayList<String>(devicesset);
		Collections.sort(devices);
		// Convert the IDs to names
		String[] nameslist = new String[devices.size()];
		int i = 0;
		for (String deviceid : devices) {
			nameslist[i] = AVRMCUidConverter.id2name(deviceid);
			i++;
		}
		return nameslist;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.viewers.IContentProvider#dispose()
	 */
	@Override
	public void dispose() {
		// Nothing to dispose
	}

}
