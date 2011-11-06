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
 * $Id: IMCUProvider.java 851 2010-08-07 19:37:00Z innot $
 *     
 *******************************************************************************/
package it.baeyens.avreclipse.core;

import java.io.IOException;
import java.util.Set;

/**
 * Methods for accessing MCU id values for a tool that has a list of supported MCUs.
 * <p>
 * This interface is used by the Supported MCU View to get a list of all MCU id values the
 * implementor supports.
 * </p>
 * 
 * @author Thomas Holland
 * @since 2.2
 * 
 */
public interface IMCUProvider {

	/**
	 * Returns a <code>Set</code> of all MCU id values the implementor supports.
	 * 
	 * @return <code>Set&lt;String&gt;</code> with the supported MCU id values
	 */
	public Set<String> getMCUList() throws IOException;

	/**
	 * Test if the implementor supports the given MCU id.
	 * 
	 * @param mcuid
	 *            String with a MCU id
	 * @return <code>true</code> if the implementor supports this id, <code>false</code>
	 *         otherwise.
	 */
	public boolean hasMCU(String mcuid);

	/**
	 * Returns any information the implementor associates with the given MCU id.
	 * 
	 * @param mcuid
	 *            String with a MCU id
	 * @return <code>String</code> with some information about the MCU or <code>null</code> if
	 *         no information is available or the MCU id is unknown.
	 */
	public String getMCUInfo(String mcuid);

}
