/*******************************************************************************
 * Copyright (c) 2010 Thomas Holland (thomas@innot.de) and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the GNU Public License v3
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 *
 * Contributors:
 *     Thomas Holland - initial API and implementation
 *     
 * $Id: Convert21.java 851 2010-08-07 19:37:00Z innot $
 *******************************************************************************/
/**
 * 
 */
package it.baeyens.avreclipse.mbs.converter;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Vector;

import org.eclipse.cdt.managedbuilder.core.IBuildObject;
import org.eclipse.cdt.managedbuilder.core.IConfiguration;
import org.eclipse.cdt.managedbuilder.core.IHoldsOptions;
import org.eclipse.cdt.managedbuilder.core.IManagedProject;
import org.eclipse.cdt.managedbuilder.core.IOption;
import org.eclipse.cdt.managedbuilder.core.ITool;
import org.eclipse.cdt.managedbuilder.core.IToolChain;
import org.eclipse.cdt.managedbuilder.core.ManagedBuildManager;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.osgi.service.prefs.BackingStoreException;

import it.baeyens.arduino.globals.ArduinoConst;
import it.baeyens.avreclipse.AVRPlugin;
import it.baeyens.avreclipse.core.natures.AVRProjectNature;
import it.baeyens.avreclipse.core.properties.AVRProjectProperties;
import it.baeyens.avreclipse.core.properties.ProjectPropertyManager;

/**
 * @author Thomas
 * 
 */
public class Convert21 {

	private final static String				OLD_AVRTARGET_ID	= "avrtarget";

	private static ProjectPropertyManager	fProjProps			= null;

	public static IBuildObject convert(IBuildObject buildObj, String fromId) {

		IManagedProject mproj = (IManagedProject) buildObj;

		// get the project property store

		fProjProps = ProjectPropertyManager.getPropertyManager((IProject) mproj.getOwner());

		// go through all configurations of the selected Project and
		// check the options needing an update
		IConfiguration[] cfgs = mproj.getConfigurations();
		if ((cfgs != null) && (cfgs.length > 0)) { // Sanity Check
			for (int i = 0; i < cfgs.length; i++) {
				IConfiguration currcfg = cfgs[i];

				// remove deprecated toolchain options
				IToolChain tc = currcfg.getToolChain();
				checkOptions(tc, currcfg);

				// Check all tools for deprecated options
				ITool[] tools = currcfg.getTools();
				for (int n = 0; n < tools.length; n++) {
					checkOptions(tools[n], currcfg);
				}

			} // for configurations

			// Save the (modified) Buildinfo
			IProject project = (IProject) mproj.getOwner();
			ManagedBuildManager.saveBuildInfo(project, true);

			// Save the new project properties
			try {
				fProjProps.save();
			} catch (BackingStoreException e) {
				// TODO Auto-generated catch block
				// I will fix this once the converter gets a little GUI
				e.printStackTrace();
			}
		}

		// Add AVR Nature to the project
		IProject project = (IProject) mproj.getOwner();
		try {
			AVRProjectNature.addAVRNature(project);
		} catch (CoreException ce) {
			// TODO: Once a converter GUI is implemented change this to an error dialog.
			// addAVRNature() should not cause an Exception, but just in case we log it.
			IStatus status = new Status(IStatus.ERROR, ArduinoConst.CORE_PLUGIN_ID,
					"Could not add AVR nature to project [" + project.toString() + "]", ce);
			AVRPlugin.getDefault().log(status);
		}
		return buildObj;
	}

	/**
	 * @param tools
	 */
	@SuppressWarnings("unchecked")
	private static void checkOptions(IHoldsOptions optionholder, IConfiguration buildcfg) {

		// Get the Project Properties for the given Configuration
		AVRProjectProperties props = fProjProps.getConfigurationProperties(buildcfg, true);
		boolean changeperconfig = false;

		// we need to use reflections to call the private method
		// "getOptionsList" because getOptions filters all invalid
		// options, which are just the ones we need for removal
		Vector optionlist = new Vector(0);
		Class<?> c = optionholder.getClass().getSuperclass();
		try {
			Method getoptionlist = c.getDeclaredMethod("getOptionList", (Class<?>[]) null);
			getoptionlist.setAccessible(true);
			Object returnvalue = getoptionlist.invoke(optionholder, (Object[]) null);
			if (returnvalue instanceof Vector) {
				optionlist = (Vector) returnvalue;
			}
		} catch (SecurityException e) {
			return;
		} catch (NoSuchMethodException e) {
			return;
		} catch (IllegalArgumentException e) {
			return;
		} catch (IllegalAccessException e) {
			return;
		} catch (InvocationTargetException e) {
			return;
		}

		Object[] allopts = optionlist.toArray();
		// Step thru all options and remove the deprecated ones
		for (int k = 0; k < allopts.length; k++) {
			IOption curropt = (IOption) allopts[k];

			// remove 2.0.x toolchain options
			if (curropt.getId().startsWith("it.baeyens.avreclipse.toolchain.options.target.mcutype")) {
				// get the selected target mcu and set the project property
				// accordingly
				String selectedmcuid = (String) curropt.getValue();
				String mcutype = selectedmcuid.substring(selectedmcuid.lastIndexOf(".") + 1);
				props.setMCUId(mcutype);
				changeperconfig = true;
				optionholder.removeOption(curropt);
				continue;
			}

			if (curropt.getId().startsWith("it.baeyens.avreclipse.toolchain.options.target.fcpu")) {
				// get the selected fcpu and set the project property
				// accordingly
				String fcpu = (String) curropt.getValue();
				props.setFCPU(fcpu);
				changeperconfig = true;
				optionholder.removeOption(curropt);
				continue;
			}

			// remove 2.0.x avrtarget options
			if (curropt.getId().indexOf(OLD_AVRTARGET_ID) != -1) {
				optionholder.removeOption(curropt);
				continue;
			}

			// remove old debug option from 2.0.0
			if (curropt.getName() != null) {
				if (curropt.getName().endsWith("(-g)")) {
					optionholder.removeOption(curropt);
					continue;
				}
			}

			// remove any other invalid options
			if (!curropt.isValid()) {
				optionholder.removeOption(curropt);
				continue;
			}

		} // for options

		if (changeperconfig) {
			fProjProps.setPerConfig(true);
		}

		try {
			props.save();
		} catch (BackingStoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
