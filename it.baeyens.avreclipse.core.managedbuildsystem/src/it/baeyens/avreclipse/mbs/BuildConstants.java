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
 * $Id: BuildConstants.java 851 2010-08-07 19:37:00Z innot $
 *     
 *******************************************************************************/
package it.baeyens.avreclipse.mbs;

/**
 * Default names and values for the AVR Eclipse Plugin.
 * 
 * <p>Currently two names with their corresponding toolchain option id's are 
 * defined. One for the Target MCU type and one for the Target MCU
 * Clock Frequency.</p>
 * 
 * <p>They are used as
 * <ul>
 * 	<li>name for the <code>valueHandlerExtraArgument</code> attribute of the corresponding
 * 		option in the plugin.xml </li>
 *  <li>name of the generated <code>BuildMacro</code></li>
 *  <li>name of the generated <code>Configuration</code> environment variable</li>
 * </ul>
 * 
 * @author Thomas Holland
 * @version 1.0
 */
public interface BuildConstants {

	/** Name of the extraArgument / buildMacro / environment variable. Set to {@value} */
	public static String TARGET_MCU_NAME = "AVRTARGETMCU";

	/** Name of the extraArgument / buildMacro / Environment Variable. Set to {@value} */
	public static String TARGET_FCPU_NAME = "AVRTARGETFCPU";
}
