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
 * $Id: MCUDefaultsActionPart.java 851 2010-08-07 19:37:00Z innot $
 *     
 *******************************************************************************/
package it.baeyens.avreclipse.ui.editors;

import it.baeyens.avreclipse.ui.actions.ActionType;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;


/**
 * A <code>IFormPart</code> that adds an action to the form toolbar to set the values to the
 * factory defaults.
 * 
 * @see AbstractActionPart
 * 
 * @author Thomas Holland
 * @since 2.3
 * 
 */
public class MCUDefaultsActionPart extends AbstractActionPart {


	@Override
	protected IAction[] getAction() {

		ActionType type = ActionType.DEFAULTS;

		Action defaultAction = new Action() {

			@Override
			public void run() {

				getByteValues().setDefaultValues();
				notifyForm();
				markDirty();
			}
		};

		type.setupAction(defaultAction);

		IAction[] allactions = new IAction[1];
		allactions[0] = defaultAction;

		return allactions;

	}

}
