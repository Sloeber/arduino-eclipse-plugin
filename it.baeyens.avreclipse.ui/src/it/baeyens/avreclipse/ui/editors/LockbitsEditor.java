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
 * $Id: LockbitsEditor.java 851 2010-08-07 19:37:00Z innot $
 *     
 *******************************************************************************/
package it.baeyens.avreclipse.ui.editors;


/**
 * The Lockbits File Editor.
 * <p>
 * The only difference to the {@link FusesEditor} is the different memory type, so this class has
 * only one method to get the type.
 * </p>
 * 
 * @see FusesEditor
 * 
 * @author Thomas Holland
 * @since 2.3
 * 
 */
public class LockbitsEditor extends FusesEditor {

	// @Override
	// protected FuseType getType() {
	// return FuseType.LOCKBITS;
	// }
}
