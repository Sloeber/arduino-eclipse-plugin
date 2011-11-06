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
 * $Id: BaseToolInfo.java 851 2010-08-07 19:37:00Z innot $
 *******************************************************************************/
/**
 * 
 */
package it.baeyens.avreclipse.core.toolinfo;

import it.baeyens.arduino.globals.ArduinoConst;
import it.baeyens.avreclipse.AVRPlugin;

import org.eclipse.cdt.managedbuilder.core.ITool;
import org.eclipse.cdt.managedbuilder.core.ManagedBuildManager;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

/**
 * @author U043192
 * 
 */
public abstract class BaseToolInfo {

	private String	fCommandName	= null;

	protected BaseToolInfo(String toolid) {
		// First: Get the command name from the toolchain
		ITool tool = ManagedBuildManager.getExtensionTool(toolid);
		if (tool == null)
		{
			IStatus status = new Status(Status.ERROR, ArduinoConst.CORE_PLUGIN_ID,	"Failed to get toolpad of "  + toolid, null);
			AVRPlugin.getDefault().log(status);
		  return;
		}
			fCommandName = tool.getToolCommand();
			if (fCommandName.startsWith("-")) {
				// remove leading "-" in command name
				// (used to suppress "make" exit on errors)
				fCommandName = fCommandName.substring(1);
			}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see it.baeyens.avreclipse.core.toolinfo.IToolInfo#getToolPath()
	 */
	public IPath getToolPath() {
		// Base implementation. Override as necessary.
		return null;
	}

	public String getCommandName() {
		return fCommandName;
	}
}
