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
 * $Id: AVRPlugin.java 851 2010-08-07 19:37:00Z innot $
 *******************************************************************************/
/****************************************************************************
 * 
 * AVRPlugin.java
 * 
 * This file is part of AVR Eclipse Plugin.
 *
 ****************************************************************************/

/* $Id: AVRPlugin.java 851 2010-08-07 19:37:00Z innot $ */

package it.baeyens.avreclipse;

import it.baeyens.arduino.globals.ArduinoConst;
import it.baeyens.avreclipse.core.preferences.AVRPathsPreferences;
import it.baeyens.avreclipse.core.targets.ToolManager;

import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleManager;
import org.eclipse.ui.console.MessageConsole;
import org.osgi.framework.BundleContext;


/**
 * The activator class controls the plug-in life cycle
 */
public class AVRPlugin extends Plugin {

	// The plug-in ID
	
	public static final String	DEFAULT_CONSOLE	= "AVR Eclipse Plugin Log";

	// The shared instance
	private static AVRPlugin	plugin;

	/**
	 * The constructor
	 */
	public AVRPlugin() {
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;

		// Rescan all system paths (unless the "No startup scan" flag has been set
		AVRPathsPreferences.scanAllPaths();

		ToolManager toolmanager = ToolManager.getDefault();
		String[] extpoints = toolmanager.getExtensionPointIDs();
		for (String ext : extpoints) {
			Platform.getExtensionRegistry().addListener(toolmanager, ext);
		}

	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
	 */
	@Override
	public void stop(BundleContext context) throws Exception {
		ToolManager toolmanager = ToolManager.getDefault();
		Platform.getExtensionRegistry().removeListener(toolmanager);

		plugin = null;
		super.stop(context);
	}

	/**
	 * Returns the shared instance
	 * 
	 * @return the shared instance
	 */
	public static AVRPlugin getDefault() {
		return plugin;
	}

	/**
	 * Log the given status and print it to the err Stream if debugging is enabled.
	 * 
	 * @param status
	 */
	public void log(IStatus status) {
		ILog log = getLog();
		if (status.getSeverity() >= Status.WARNING) {
			log.log(status);
		}
		if (isDebugging()) {
			System.err.print(ArduinoConst.CORE_PLUGIN_ID + ": " + status.getMessage());
			if (status.getCode() != 0) {
				System.err.print("(" + status.getCode() + ")");
			}
			System.out.println("");
			if (status.getException() != null) {
				status.getException().printStackTrace();
			}
		}
	}

	/**
	 * Gets the default console for this plugin.
	 * <p>
	 * This console is used for logging internal information, like the output of the external tools
	 * called internally (not by user interaction). This Console is only for background information
	 * and debugging and should never be actively brought to the front.
	 * </p>
	 * 
	 * @return The default <code>MessageConsole</code>
	 */
	public MessageConsole getDefaultConsole() {
		return getConsole(DEFAULT_CONSOLE);
	}

	/**
	 * Gets the console with the given name.
	 * <p>
	 * This is a convenience method to get a console with the given name.
	 * </p>
	 * <p>
	 * If the console already exists, a reference is returned, otherwise a new
	 * <code>MessageConsole</code> with the given name is created, added to the ConsoleManager, and
	 * returned.
	 * </p>
	 * 
	 * @param name
	 *            The name of the Console
	 * @return A <code>MessageConsole</code> with the given name
	 */
	public MessageConsole getConsole(String name) {
		// Get a list of all known Consoles from the Global Console Manager and
		// see if a Console with the given name already exists.
		IConsoleManager conman = ConsolePlugin.getDefault().getConsoleManager();
		IConsole[] allconsoles = conman.getConsoles();
		for (IConsole console : allconsoles) {
			if (console.getName().equals(name)) {
				return (MessageConsole) console;
			}
		}
		// Console not found - create a new one
		MessageConsole newconsole = new MessageConsole(name, null);
		conman.addConsoles(new IConsole[] { newconsole });
		return newconsole;
	}
}
