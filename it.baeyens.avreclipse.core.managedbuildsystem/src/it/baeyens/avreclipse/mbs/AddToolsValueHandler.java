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
 * $Id: AddToolsValueHandler.java 851 2010-08-07 19:37:00Z innot $
 *     
 *******************************************************************************/
package it.baeyens.avreclipse.mbs;

import org.eclipse.cdt.managedbuilder.core.BuildException;
import org.eclipse.cdt.managedbuilder.core.IBuildObject;
import org.eclipse.cdt.managedbuilder.core.IConfiguration;
import org.eclipse.cdt.managedbuilder.core.IFolderInfo;
import org.eclipse.cdt.managedbuilder.core.IHoldsOptions;
import org.eclipse.cdt.managedbuilder.core.IOption;
import org.eclipse.cdt.managedbuilder.core.ITool;
import org.eclipse.cdt.managedbuilder.core.ManagedBuildManager;
import org.eclipse.cdt.managedbuilder.core.ManagedOptionValueHandler;
import org.eclipse.cdt.managedbuilder.internal.core.Tool;
import org.eclipse.cdt.managedbuilder.internal.core.ToolChain;

/**
 * Handle changes of additional build target options.
 * 
 * <p>
 * This class is registered as a <code>valueHandler</code> by the options of
 * the base toolchain in the AVR Eclipse plugin. All changes to the option
 * implementing this handler will cause a call to the <code>handleValue</code>
 * method of this class.
 * </p>
 * <p>
 * The value of the <code>valueHandlerExtraArgument</code> attribute in the
 * option element is used as the Id of the tool to be added to / removed from
 * the toolchain.
 * </p>
 * <p>
 * This class is extended from
 * {@link org.eclipse.cdt.managedbuilder.core.ManagedOptionValueHandler}, which
 * covers the other methods of the
 * {@link org.eclipse.cdt.managedbuilder.core.IManagedOptionValueHandler}
 * interface.
 * </p>
 * 
 * @author Thomas Holland
 * @version 1.0
 */
public class AddToolsValueHandler extends ManagedOptionValueHandler {

	/** Array of all available tools on this computer.*/
	private final ITool[] alltools = ManagedBuildManager.getRealTools();

	/**
	 * Handle Option Change events.
	 * 
	 * <p>
	 * Adjust the ToolChain according to the selected option. <code>extraArgument</code>
	 * has the id of the tool to be added or removed from the winAVR toolchain, depending
	 * on the boolean value of the option.
	 * </p>
	 * 
	 * @see org.eclipse.cdt.managedbuilder.core.IManagedOptionValueHandler#handleValue(org.eclipse.cdt.managedbuilder.core.IBuildObject,
	 *      org.eclipse.cdt.managedbuilder.core.IHoldsOptions,
	 *      org.eclipse.cdt.managedbuilder.core.IOption, java.lang.String, int)
	 */
	@Override
	public boolean handleValue(IBuildObject configuration,
			IHoldsOptions holder, IOption option, String extraArgument,
			int event) {

		Boolean value = null;
		try {
			value = option.getBooleanValue();
		} catch (BuildException e1) {
			// something wrong with the plugin.xml
			e1.printStackTrace();
			return false;
		}
		if (value == null) {
			return false;
		}

		// At the EVENT_LOAD event the toolchain exists, but has not been correctly 
		// initialized. Especially the options have not been set to their persistent / 
		// default state and are always at <code>true</code>. Not usable, although 
		// it would be nice to adjust the toolchain to the defaults with this event,
		// as it is always generated at eclipse load time. For the time being the 
		// defaults have to be done via &lt;enablement&gt; elements in the plugin.xml.  
		if (event == EVENT_LOAD) {
			return false;
		}

		// sanity check, could only happen with mistakes in the plugin.xml
		if (extraArgument == null) {
			return false;
		}

		// The IFolderInfo API has the required modifyToolChain() method.
		IFolderInfo fi = null;
		if (configuration instanceof IConfiguration) {
			fi = ((IConfiguration) configuration).getRootFolderInfo();
		} else if (configuration instanceof IFolderInfo) {
			fi = (IFolderInfo) configuration;
		} else {
			return false;
		}

//		System.out
//				.println("\n\n------------------- AddTools handlevalue() -------------------");
//		System.out.print("event=" + event + ", ");
//		System.out.print(value.booleanValue() == true ? "add " : "remove ");
//		System.out.println(extraArgument + "  ToolchainID: "
//				+ fi.getToolChain().getId());
//		System.out.print("Toolchain before: ");
//		dumpTC(fi.getFilteredTools());

		if (value) {
			/*
			 * Add tool to toolchain
			 */
			if (fi.getToolsBySuperClassId(extraArgument).length != 0) {
				// tool already in toolchain
				return false;
			}

			ITool[] add = new ITool[1];		// modifyToolChain() requires an Array of Tools as Argument

			// find the tool in the list of all tools
			for (int i = 0; i < alltools.length; i++) {
				ITool tool = alltools[i];
				while (tool.getSuperClass() != null) {
					tool = tool.getSuperClass();
				}
				if (extraArgument.equals(tool.getId())) {
					// found it
					add[0] = tool;
					break;
				}
			}

			if (add[0] != null) {
//				System.out.println("internal toolid = " + add[0].getId());
				try {
					fi.modifyToolChain(new ITool[0], add);
				} catch (BuildException e) {
					// What can cause this exception ?
					e.printStackTrace();
					return false;
				}
//				System.out.println(add[0].getId() + " added");

				/* Small hack to get the tool to the end of the list
				 * IFolderInfo.modifyToolChain() makes no guarantees
				 * about the ordering of added tools. Removing / Adding 
				 * numerous times will result in an predetermined but 
				 * uncontrollable tool order (probably the order of the
				 * internal HashMap or something).
				 * This might break in future versions of CDT as it uses
				 * an undocumented internal API.
				 */
				if (fi.getToolChain() instanceof ToolChain) {
					ToolChain tc = (ToolChain) fi.getToolChain();
					// The toolchain has a new instance of the tool we added.
					// find it and remove it and add it again to move it to 
					// the end of the internal list.
					ITool[] tctools = tc.getToolsBySuperClassId(add[0].getId());

					if (tctools.length != 0) {
						tc.removeTool((Tool) tctools[0]);
						tc.addTool((Tool) tctools[0]);
					}
				}
			}
//			System.out.print("Toolchain after: ");
//			dumpTC(fi.getFilteredTools());
			
		} else {
			/*
			 * remove tool from toolchain
			 */
			ITool[] remove = fi.getToolsBySuperClassId(extraArgument);

			if (remove.length != 0) {
//				System.out.println("internal toolid = " + remove[0].getId());

				try {
					fi.modifyToolChain(remove, new ITool[0]);
				} catch (BuildException e) {
					// What can cause this exception ?
					e.printStackTrace();
					return false;
				}
//				System.out.println(remove[0].getId() + " removed");
			}
//			System.out.print("Toolchain after: ");
//			dumpTC(fi.getFilteredTools());
		}

		return false;
	}

	/**
	 * Dump array of ITools
	 * 
	 * Used for debugging.
	 * 
	 * @param tools
	 */
//	private void dumpTC(ITool[] tools) {
//		System.out.print("[");
//		for (int i = 0; i < tools.length; i++) {
//			System.out.print(ManagedBuildManager.getRealTool(tools[i]).getId()
//					.substring(12)
//					+ ", ");
//		}
//		System.out.println("]");
//
//	}
}
