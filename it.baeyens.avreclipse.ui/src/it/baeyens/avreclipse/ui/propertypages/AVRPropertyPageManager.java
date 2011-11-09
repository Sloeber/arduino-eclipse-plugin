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
 * $Id: AVRPropertyPageManager.java 851 2010-08-07 19:37:00Z innot $
 *     
 *******************************************************************************/
package it.baeyens.avreclipse.ui.propertypages;

import it.baeyens.arduino.ArduinoConst;
import it.baeyens.avreclipse.core.properties.AVRProjectProperties;
import it.baeyens.avreclipse.core.properties.ProjectPropertyManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.cdt.core.settings.model.ICResourceDescription;
import org.eclipse.cdt.managedbuilder.core.IConfiguration;
import org.eclipse.cdt.managedbuilder.core.ManagedBuildManager;
import org.eclipse.cdt.ui.newui.CDTPropertyManager;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.PropertyPage;
import org.osgi.service.prefs.BackingStoreException;


/**
 * Manages the list of {@link AbstractAVRPage} for a property dialog.
 * <p>
 * This manager keeps track of all open property pages. Each page registers itself by calling the
 * {@link #getPropertyManager(PropertyPage, IProject)} method.
 * </p>
 * <p>
 * All data and all methods are static. This is no problem, because the property dialog is modal, so
 * only one property dialog = one session can be open at a time.
 * </p>
 * <p>
 * This class is very similar to and supplements the
 * <code>CDTPropertyManager</class>, which manages the list of all CDT <code>AbstractPage</code>s.</p> 
 * 
 * @see CDTPropertyManager
 * 
 * @author Thomas Holland
 * @since 2.2
 *
 */
public class AVRPropertyPageManager {

	/** List of all open Property Pages */
	private static List<PropertyPage>					fPages	= new ArrayList<PropertyPage>();

	/** The Project for which the properties are edited */
	private static IProject								fProject;

	/** The Project Property Manager for the current project */
	private static ProjectPropertyManager				fPropertiesManager;

	/** The Project Properties for the current project */
	private static AVRProjectProperties					fProjectProps;

	/** Cache of build config Properties for the current project. */
	private static Map<String, AVRProjectProperties>	fConfigPropertiesMap;

	/**
	 * Gets the the {@link ProjectPropertyManager} for the project and registers the given page in
	 * the list of property pages.
	 * <p>
	 * On the first call to this method for a new or a different project, a new session is
	 * initiated.
	 * </p>
	 * 
	 * @param page
	 *            <code>AbstractAVRPropertyPage</code> to register in this manager.
	 * @param project
	 *            The current project.
	 * @return The <code>ProjectPropertyManager</code> for the given project.
	 */
	public static ProjectPropertyManager getPropertyManager(PropertyPage page, IProject project) {

		// If no pages registered start a new static session
		if (fPages.size() == 0) {
			fProject = null;
			fPropertiesManager = null;
		}

		// Remember the page and add dispose listener to the page so we know
		// when it is closed
		if (!fPages.contains(page)) {
			fPages.add(page);
			page.getControl().addDisposeListener(fDisposeListener);
		}

		// Check if a new project has been selected
		if (fProject == null || !project.equals(fProject)) {
			fProject = project;
			fPropertiesManager = null;
		}

		// Check if a new properties object is required
		if (fPropertiesManager == null) {
			fPropertiesManager = ProjectPropertyManager.getPropertyManager(project);
			fProjectProps = null;
			fConfigPropertiesMap = new HashMap<String, AVRProjectProperties>();
		}

		return fPropertiesManager;
	}

	/**
	 * Save all modifications to the properties to the properties storage and remove the page from
	 * the manager.
	 * <p>
	 * Also the list of "per config" properties is synchronized with the list of existing build
	 * configurations, so AVR properties for deleted build configurations will be deleted as well.
	 * </p>
	 * 
	 * @param page
	 *            Originating page.
	 * @param allconfigs
	 *            Array with all build configuration description objects.
	 */
	public static void performOK(PropertyPage page, ICConfigurationDescription[] allconfigs) {
		try {
			fPropertiesManager.save(); // saves the perConfig flag

			if (fProjectProps != null) {
				fProjectProps.save(); // Saves the global project properties
			}

			// and the config properties
			if (fConfigPropertiesMap != null) {
				for (AVRProjectProperties props : fConfigPropertiesMap.values()) {
					props.save();
				}
			}

			if (allconfigs != null) {
				// Convert the given array of ConfigurationDescriptions into a list of configuration
				// id values and call the ProjectPropertyManager.sync() method to remove
				// configuration properties for deleted build configurations.
				List<String> allcfgids = new ArrayList<String>(allconfigs.length);
				for (ICConfigurationDescription cfgd : allconfigs) {
					IConfiguration cfg = ManagedBuildManager.getConfigurationForDescription(cfgd);
					allcfgids.add(cfg.getId());
				}

				fPropertiesManager.sync(allcfgids);
			}
		} catch (BackingStoreException e) {
			IStatus status = new Status(IStatus.ERROR, ArduinoConst.CORE_PLUGIN_ID,
					"Could not write project properties to the preferences.", e);

			ErrorDialog.openError(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(),
					"AVR Properties Error", null, status);
			// We continue, even if the props could not be saved, because we still need to remove
			// the page.
		}

		removePage(page);

	}

	/**
	 * Cancel all modifications and remove the page from the manager.
	 * 
	 * @param page
	 *            Originating page.
	 */
	public static void performCancel(PropertyPage page) {
		fConfigPropertiesMap.clear();
		fProjectProps = null;
		removePage(page);
	}

	/**
	 * Remove the given <code>AbstractAVRPage</code> from the manager.
	 * <p>
	 * Once the last page has been removed from this manager, the current session is closed.
	 * </p>
	 * 
	 * @param page
	 *            Page to remove from the manager.
	 */
	private static void removePage(PropertyPage page) {

		if (fPages.contains(page)) {
			fPages.remove(page);
		}

		if (fPages.size() == 0) {
			// all pages have been disposed
			fPropertiesManager = null;
			fProject = null;
			fProjectProps = null;
			fConfigPropertiesMap = null;
		}

	}

	/**
	 * @return A <code>List</code> of all managed pages.
	 */
	public static List<PropertyPage> getPages() {
		return fPages;
	}

	/**
	 * Gets the project properties for a given <code>ICResourceDescription</code>.
	 * <p>
	 * The avr project properties object is cached so that it can be retrieved multiple times
	 * without being sync'd with the underlying preferencestore. The first call to this method will
	 * return a freshly sync'd object.
	 * </p>
	 * 
	 * @param resdesc
	 * @return
	 */
	public static AVRProjectProperties getConfigProperties(ICResourceDescription resdesc) {
		IConfiguration buildcfg = getConfigFromConfigDesc(resdesc);
		AVRProjectProperties props = null;
		// Added the cache in response to Bug 2050945: Read Back of "Enable Individual Settings ..."
		// The ProjectPropertyManager will now always sync any property objects it hands out.
		// Syncing means that all changes made are lost, so we cache the objects in order to keep
		// all modifications until either the performOK() or the performCancel() method is
		// called.
		if (fConfigPropertiesMap.containsKey(buildcfg.getId())) {
			props = fConfigPropertiesMap.get(buildcfg.getId());
		} else {
			props = fPropertiesManager.getConfigurationProperties(buildcfg);
			fConfigPropertiesMap.put(buildcfg.getId(), props);
		}
		return props;
	}

	public static AVRProjectProperties getConfigPropertiesNoCache(ICResourceDescription resdesc) {
		// This method is used in the performApply() methods of the Tabs.
		// In order to save only the contents of a single tab it gets a new Properties object
		// directly from the preference store. The tab can then modify its individual settings, and
		// saves it immediately.
		IConfiguration buildcfg = getConfigFromConfigDesc(resdesc);
		return fPropertiesManager.getConfigurationProperties(buildcfg, false);
	}

	/**
	 * Get the current per project properties.
	 * 
	 * @return
	 */
	public static AVRProjectProperties getProjectProperties() {
		if (fProjectProps == null) {
			fProjectProps = fPropertiesManager.getProjectProperties();
		}
		return fProjectProps;
	}

	/**
	 * Convenience method to get an <code>IConfiguration</code> from an
	 * <code>ICResourceDescription</code>
	 * 
	 * @param resdesc
	 *            An <code>ICResourceDescription</code>
	 * @return <code>IConfiguration</code> associated with the given Description.
	 */
	private static IConfiguration getConfigFromConfigDesc(ICResourceDescription resdesc) {
		ICConfigurationDescription cfgDes = resdesc.getConfiguration();
		IConfiguration conf = ManagedBuildManager.getConfigurationForDescription(cfgDes);
		return conf;
	}

	/**
	 * Listener to remove disposed pages from the manager.
	 */
	private static DisposeListener	fDisposeListener	= new MyDisposeListener();

	private static class MyDisposeListener implements DisposeListener {
		@Override
		public void widgetDisposed(DisposeEvent e) {
			Widget w = e.widget;
			for (PropertyPage page : fPages) {
				if (page.getControl().equals(w)) {
					fPages.remove(page);
					break;
				}
			}

			if (fPages.size() == 0) {
				// all pages have been disposed
				fPropertiesManager = null;
				fProject = null;
				fProjectProps = null;
				fConfigPropertiesMap = null;
			}
		}
	};
}
