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
 * $Id: IOptionsChangeListener.java 851 2010-08-07 19:37:00Z innot $
 *******************************************************************************/
/**
 * 
 */
package it.baeyens.avreclipse.mbs;

/**
 * @author Thomas
 *
 */
public interface IOptionsChangeListener {

	public void optionChanged(String option, String newvalue);
	
}
