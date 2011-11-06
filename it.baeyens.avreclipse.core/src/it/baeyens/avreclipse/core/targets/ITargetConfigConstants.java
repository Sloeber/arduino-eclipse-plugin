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
 * $Id: ITargetConfigConstants.java 851 2010-08-07 19:37:00Z innot $
 *     
 *******************************************************************************/

package it.baeyens.avreclipse.core.targets;

import it.baeyens.avreclipse.core.targets.tools.AvrdudeTool;
import it.baeyens.avreclipse.core.targets.tools.NoneToolFactory;

/**
 * The common attributes of a target configuration and their default values.
 * 
 * @author Thomas Holland
 * @since 2.4
 * 
 */
public interface ITargetConfigConstants {

	// General (Name and description)
	public final static String	ATTR_NAME				= "name";
	public final static String	DEF_NAME				= "New target";

	public final static String	ATTR_DESCRIPTION		= "description";
	public final static String	DEF_DESCRIPTION			= "";

	// Target Hardware
	public final static String	ATTR_MCU				= "mcu";
	public final static String	DEF_MCU					= "atmega16";

	public final static String	ATTR_FCPU				= "fcpu";
	public final static int		DEF_FCPU				= 1000000;

	// Programmer device
	public final static String	ATTR_PROGRAMMER_ID		= "programmer";
	public final static String	DEF_PROGRAMMER_ID		= "stk500v2";

	// Host interface
	public final static String	ATTR_HOSTINTERFACE		= "hostinterface";
	public final static String	DEF_HOSTINTERFACE		= "SERIAL";

	public final static String	ATTR_PROGRAMMER_PORT	= "port";
	public final static String	DEF_PROGRAMMER_PORT		= "";

	public final static String	ATTR_PROGRAMMER_BAUD	= "baud";
	public final static String	DEF_PROGRAMMER_BAUD		= "";

	public final static String	ATTR_BITBANGDELAY		= "bitbangdelay";
	public final static String	DEF_BITBANGDELAY		= "";

	public final static String	ATTR_PAR_EXITSPEC		= "exitspec";
	public final static String	DEF_PAR_EXITSPEC		= "";

	public final static String	ATTR_USB_DELAY			= "usbdelay";
	public final static String	DEF_USB_DELAY			= "";

	// Target interface
	public final static String	ATTR_JTAG_CLOCK			= "jtagclock";
	public final static String	DEF_JTAG_CLOCK			= "0";

	public final static String	ATTR_DAISYCHAIN_ENABLE	= "jtagdaisychain";
	public final static String	DEF_DAISYCHAIN_ENABLE	= "false";

	public final static String	ATTR_DAISYCHAIN_UB		= "unitsBefore";
	public final static String	DEF_DAISYCHAIN_UB		= "0";

	public final static String	ATTR_DAISYCHAIN_UA		= "unitsAfter";
	public final static String	DEF_DAISYCHAIN_UA		= "0";

	public final static String	ATTR_DAISYCHAIN_BB		= "bitsBefore";
	public final static String	DEF_DAISYCHAIN_BB		= "0";

	public final static String	ATTR_DAISYCHAIN_BA		= "bitsAfter";
	public final static String	DEF_DAISYCHAIN_BA		= "0";

	// Uploader tool
	public final static String	ATTR_PROGRAMMER_TOOL_ID	= "programmertool";
	public final static String	DEF_PROGRAMMER_TOOL_ID	= AvrdudeTool.ID;

	// GDBServer tool
	public final static String	ATTR_GDBSERVER_ID		= "gdbservertool";
	public final static String	DEF_GDBSERVER_ID		= NoneToolFactory.ID;

}
