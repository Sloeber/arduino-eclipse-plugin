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
 * $Id: AvrdudeToolFactory.java 851 2010-08-07 19:37:00Z innot $
 *     
 *******************************************************************************/

package it.baeyens.avreclipse.core.targets.tools;

import it.baeyens.avreclipse.core.targets.ITargetConfiguration;
import it.baeyens.avreclipse.core.targets.ITargetConfigurationTool;
import it.baeyens.avreclipse.core.targets.IToolFactory;
import it.baeyens.avreclipse.core.targets.ToolManager;

/**
 * @author Thomas Holland
 * @since
 * 
 */
public class AvrdudeToolFactory implements IToolFactory {

	/*
	 * (non-Javadoc)
	 * @see it.baeyens.avreclipse.core.targets.IToolFactory#getId()
	 */
	public String getId() {
		return AvrdudeTool.ID;
	}

	/*
	 * (non-Javadoc)
	 * @see it.baeyens.avreclipse.core.targets.IToolFactory#getName()
	 */
	public String getName() {
		return AvrdudeTool.NAME;
	}

	/*
	 * (non-Javadoc)
	 * @see it.baeyens.avreclipse.core.targets.IToolFactory#isType(java.lang.String)
	 */
	public boolean isType(String tooltype) {

		// AvrdudeTool is a programmer tool

		if (ToolManager.AVRPROGRAMMERTOOL.equals(tooltype)) {
			return true;
		}

		return false;
	}

	/*
	 * (non-Javadoc)
	 * @see  it.baeyens.avreclipse.core.targets.IToolFactory#createTool(it.baeyens.avreclipse.core.targets
	 * .ITargetConfiguration)
	 */
	public ITargetConfigurationTool createTool(ITargetConfiguration hc) {
		return new AvrdudeTool(hc);
	}

}
