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
 * $Id: ITargetConfigurationTool.java 851 2010-08-07 19:37:00Z innot $
 *     
 *******************************************************************************/

package it.baeyens.avreclipse.core.targets;

import it.baeyens.avreclipse.core.avrdude.AVRDudeException;

import java.util.Set;


/**
 * @author Thomas Holland
 * @since 2.4
 * 
 */
public interface ITargetConfigurationTool extends IAttributeProvider {

	public String getId();

	public String getName();

	public String getVersion() throws AVRDudeException;

	public Set<String> getMCUs() throws AVRDudeException;

	public Set<String> getProgrammers() throws AVRDudeException;

	public IProgrammer getProgrammer(String id) throws AVRDudeException;
}
