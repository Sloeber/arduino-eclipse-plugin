/*******************************************************************************
 * 
 * Copyright (c) 2009, 2010 Thomas Holland (thomas@innot.de) and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the GNU Public License v3
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     Thomas Holland - initial API and implementation
 *     
 * $Id: ToolManager.java 851 2010-08-07 19:37:00Z innot $
 *     
 *******************************************************************************/

package it.baeyens.avreclipse.core.targets;

import it.baeyens.arduino.globals.ArduinoConst;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IRegistryEventListener;
import org.eclipse.core.runtime.Platform;


/**
 * Manages the tools for the target configuration.
 * <p>
 * This class manages the
 * </p>
 * <p>
 * This class implements the singleton pattern. There is only one instance of this class, accessible
 * with {@link #getDefault()}.
 * </p>
 * 
 * @author Thomas Holland
 * @since 2.4
 * 
 */
public class ToolManager implements IRegistryEventListener {

	private static ToolManager			fInstance;

	private final static String			NAMESPACE			= ArduinoConst.CORE_PLUGIN_ID;
	public final static String			EXTENSIONPOINT		= NAMESPACE + ".targetToolFactories";
	private final static String			ELEMENT_FACTORY		= "toolfactory";

	public final static String			AVRPROGRAMMERTOOL	= "avr.tool.programmer";
	public final static String			AVRGDBSERVER		= "avr.tool.gdbserver";

	private Map<String, IToolFactory>	fFactoryRegistry;

	private Map<String, Long>			fInterfaceLastCallMap;

	/**
	 * Get the default tool manager.
	 * 
	 * @return Default tool manager instance.
	 */
	public static ToolManager getDefault() {
		if (fInstance == null) {
			fInstance = new ToolManager();
		}
		return fInstance;
	}

	// prevent instantiation
	private ToolManager() {

	}

	/**
	 * Get a list of all extension point ids used for the target tools.
	 * <p>
	 * The list is used by the AVRPlugin class to register the toolmanager as a listener for
	 * additions/removal of tool extension plugins.
	 * </p>
	 * 
	 * @return Array with the unique extension points for the toolmanager.
	 */
	public String[] getExtensionPointIDs() {
		String[] extpoints = new String[1];
		extpoints[0] = EXTENSIONPOINT;
		return extpoints;
	}

	/**
	 * Get the name of the tool with the given id.
	 * <p>
	 * This is the same as <code>getTool(targetconfig, id).getName()</code>, but without needing a
	 * target configuration.
	 * </p>
	 * 
	 * @param id
	 *            A tool id vaue
	 * @return The human readable name of the tool, or <code>null</code> if no tool with the given
	 *         id exists.
	 */
	public String getToolName(String id) {

		String name = null;

		Map<String, IToolFactory> registry = getRegistry();

		if (registry.containsKey(id)) {
			IToolFactory factory = registry.get(id);
			name = factory.getName();
		}

		return name;
	}

	/**
	 * Get the tool with the given id for the given hardware configuration.
	 * 
	 * @param hc
	 *            The hardware configuration the tool is applicable for.
	 * @param id
	 *            The tool id value.
	 * @return The tool, or <code>null</code> if no tool exists for the given id.
	 */
	public ITargetConfigurationTool getTool(ITargetConfiguration hc, String id) {
		Map<String, IToolFactory> registry = getRegistry();
		ITargetConfigurationTool tool = null;

		if (registry.containsKey(id)) {
			IToolFactory factory = registry.get(id);
			tool = factory.createTool(hc);
		}

		return tool;
	}

	/**
	 * Get an array with the id values of all available tools.
	 * 
	 * @param tooltype
	 *            Type of tool. If <code>null</code> all tools are returned.
	 * @return List with tool id values.
	 */
	public List<String> getAllTools(String tooltype) {
		Map<String, IToolFactory> registry = getRegistry();
		List<String> resultids = new ArrayList<String>();

		for (IToolFactory factory : registry.values()) {
			if (tooltype == null || factory.isType(tooltype)) {
				resultids.add(factory.getId());
			}
		}
		return resultids;
	}

	/**
	 * Remember the system time the given programmer port was last accessed.
	 * 
	 * @param programmerport
	 *            The name of the port, e.g. <code>/dev/usb</code>
	 * @param lastfinish
	 *            last access time in ms (from <code>System.currentTimeMillis()</code>)
	 */
	public synchronized void setLastAccess(String programmerport, long lastfinish) {
		if (fInterfaceLastCallMap == null) {
			fInterfaceLastCallMap = new HashMap<String, Long>();
		}
		fInterfaceLastCallMap.put(programmerport, lastfinish);
	}

	/**
	 * Get the system time of the last access to the given programmer port.
	 * 
	 * @param programmerport
	 *            The name of the port, e.g. <code>/dev/usb</code>
	 * @return the system time (in millis) of the last access, or <code>0</code> if the port has not
	 *         been accessed before.
	 */
	public synchronized long getLastAccess(String programmerport) {
		Long lastfinish = 0L;
		if (fInterfaceLastCallMap != null) {
			if (fInterfaceLastCallMap.containsKey(programmerport)) {
				lastfinish = fInterfaceLastCallMap.get(programmerport);
			}
		}

		return lastfinish;
	}

	/**
	 * Load all gdbserverTool extensions.
	 * <p>
	 * The list is cached until some extensions are either added or removed
	 * </p>
	 * 
	 * @see #added(IExtension[])
	 * 
	 */
	private Map<String, IToolFactory> getRegistry() {
		if (fFactoryRegistry == null) {
			fFactoryRegistry = new HashMap<String, IToolFactory>();

			IConfigurationElement[] elements = Platform.getExtensionRegistry()
					.getConfigurationElementsFor(EXTENSIONPOINT);

			for (IConfigurationElement element : elements) {

				String type = element.getName();
				if (ELEMENT_FACTORY.equalsIgnoreCase(type)) {
					// Get an instance of the implementing class
					// and add it to the registry
					Object obj;
					try {
						obj = element.createExecutableExtension("class");
					} catch (CoreException e) {
						// TODO log exception
						continue;
					}

					if (obj instanceof IToolFactory) {
						IToolFactory factory = (IToolFactory) obj;
						String id = factory.getId();
						fFactoryRegistry.put(id, factory);
					} else {
						// invalid class
						// TODO: log exception
					}
				}
			}
		}
		return fFactoryRegistry;
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.eclipse.core.runtime.IRegistryEventListener#added(org.eclipse.core.runtime.IExtension[])
	 */
	public void added(IExtension[] extensions) {
		// Check if the extensions matches the extension used by this manager.
		// To keep things simple we just invalidate the current list of known extensions so that the
		// list will be regenerated the next time getTool() is called.
		for (IExtension ext : extensions) {
			if (ext.getUniqueIdentifier().equals(EXTENSIONPOINT)) {
				fFactoryRegistry = null;
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.eclipse.core.runtime.IRegistryEventListener#added(org.eclipse.core.runtime.IExtensionPoint
	 * [])
	 */
	public void added(IExtensionPoint[] extensionPoints) {
		// Don't care if any extension points have changed
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.eclipse.core.runtime.IRegistryEventListener#removed(org.eclipse.core.runtime.IExtension
	 * [])
	 */
	public void removed(IExtension[] extensions) {
		// remove or add doesn't matter for our simple implementation.
		added(extensions);
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.eclipse.core.runtime.IRegistryEventListener#removed(org.eclipse.core.runtime.IExtensionPoint
	 * [])
	 */
	public void removed(IExtensionPoint[] extensionPoints) {
		// Don't care if any extension points have changed
	}

}
