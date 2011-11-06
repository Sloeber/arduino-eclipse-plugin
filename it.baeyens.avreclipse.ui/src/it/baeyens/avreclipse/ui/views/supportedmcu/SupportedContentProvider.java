/*******************************************************************************
 * 
 * Copyright (c) 2008, 2010 Thomas Holland (thomas@innot.de) and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the GNU Public License v3
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     Thomas Holland - initial API and implementation
 *     
 * $Id: SupportedContentProvider.java 851 2010-08-07 19:37:00Z innot $
 *     
 *******************************************************************************/
package it.baeyens.avreclipse.ui.views.supportedmcu;

import it.baeyens.avreclipse.core.IMCUProvider;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.Viewer;


/**
 * Content Provider for the MCUListView.
 * <p>
 * As there is no real model to build on, this Content Provider also acts as a model for the
 * MCUListView.
 * </p>
 * 
 * @author Thomas Holland
 * @since 2.2
 */
public class SupportedContentProvider implements IStructuredContentProvider {

	/** List of all known MCU id values */
	private List<String>	fMasterMCUList	= null;

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.viewers.IStructuredContentProvider#getElements(java.lang.Object)
	 */
	@Override
	public Object[] getElements(Object inputElement) {
		// return an Array of Strings with all known MCU ID values.

		// If the Master MCU List has not yet created (or a reload via
		// #inputChanged() was forced), the Masterlist is build by querying all
		// given
		// IMCUProviders.
		if (fMasterMCUList == null) {
			Set<IMCUProvider> providers = new HashSet<IMCUProvider>();

			for (MCUListColumn provider : MCUListColumn.values()) {
				providers.add(provider.getMCUProvider());
			}
			Set<String> masterlist = getMCUMasterList(providers);
			List<String> sortedlist = new ArrayList<String>(masterlist);
			Collections.sort(sortedlist);
			fMasterMCUList = sortedlist;
		}
		return fMasterMCUList.toArray();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.viewers.IContentProvider#dispose()
	 */
	@Override
	public void dispose() {
		// Nothing to dispose of
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.viewers.IContentProvider#inputChanged(org.eclipse.jface.viewers.Viewer,
	 *      java.lang.Object, java.lang.Object)
	 */
	@Override
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		// reload the master MCU list on the next invocation of getElements
		fMasterMCUList = null;
	}

	/**
	 * @return A Set with all known MCU id values
	 */
	public Set<String> getMasterMCUList() {
		// return a set of the master mcu list
		return new HashSet<String>(fMasterMCUList);
	}

	/**
	 * Merges the supported MCU lists from all providers into one masterlist, which has all MCU ID
	 * values supported by at least one provider
	 * 
	 * @param providers
	 *            A <code>Set</code> with all IMCUProviders to query
	 * @return A <code>Set</code> with all known MCU ID values
	 */
	private Set<String> getMCUMasterList(Set<IMCUProvider> providers) {
		// build a "master" mcu id list from all providers
		// implemented as a Set to easily filter duplicates
		Set<String> masterlist = new HashSet<String>();

		for (IMCUProvider provider : providers) {
			if (provider != null) {
				try {
					Set<String> mcus = provider.getMCUList();
					if (mcus != null)
						masterlist.addAll(mcus);
				} catch (IOException e) {
					// providers that throw an exception (probably because a tool is not found) are
					// ignored.
				}
			}
		}
		return masterlist;
	}

}
