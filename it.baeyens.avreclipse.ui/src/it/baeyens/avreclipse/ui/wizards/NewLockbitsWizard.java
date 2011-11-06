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
 * $Id: NewLockbitsWizard.java 851 2010-08-07 19:37:00Z innot $
 *     
 *******************************************************************************/
package it.baeyens.avreclipse.ui.wizards;

import it.baeyens.avreclipse.core.toolinfo.fuses.FuseType;

/**
 * This is a sample new wizard. Its role is to create a new file resource in the provided container.
 * If the container resource (a folder or a project) is selected in the workspace when the wizard is
 * opened, it will accept it as the target container. The wizard creates one file with the extension
 * "fuses". If a sample multi-fWizardPage editor (also available as a template) is registered for
 * the same extension, it will be able to open it.
 */

public class NewLockbitsWizard extends NewFusesWizard {

	@Override
	protected FuseType getType() {
		return FuseType.LOCKBITS;
	}
}