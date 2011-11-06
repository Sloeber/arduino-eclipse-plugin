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
 * $Id: AbstractActionPart.java 851 2010-08-07 19:37:00Z innot $
 *     
 *******************************************************************************/
package it.baeyens.avreclipse.ui.editors;

import it.baeyens.avreclipse.core.toolinfo.fuses.ByteValues;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.forms.AbstractFormPart;
import org.eclipse.ui.forms.IManagedForm;


/**
 * A <code>IFormPart</code> that adds one or more actions to the form toolbar.
 * <p>
 * This part can be created and used like a normal <code>IFormPart</code>. Subclasses provide the
 * action to be performed. If the Action changes the <code>ByteValues</code> form input it will
 * fire an Selection event via the ManagedForm to inform other parts that the input
 * <code>ByteValues</code> have changed.
 * </p>
 * 
 * @author Thomas Holland
 * @since 2.3
 * 
 */
public abstract class AbstractActionPart extends AbstractFormPart {

	/**
	 * The model for this <code>ActionPart</code>. Other than the normal FormParts this will be
	 * modified immediately when the MCU is changed.
	 */
	private ByteValues	fByteValues;

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.forms.AbstractFormPart#initialize(org.eclipse.ui.forms.IManagedForm)
	 */
	@Override
	public void initialize(IManagedForm form) {
		super.initialize(form);
		// Get the toolbar manager of the form and add our actions to it.
		IToolBarManager toolbarmanager = form.getForm().getToolBarManager();
		IAction[] allactions = getAction();
		for (IAction action : allactions) {
			if (action == null) {
				toolbarmanager.add(new Separator());
			} else {
				toolbarmanager.add(action);
			}
		}

		form.getForm().updateToolBar();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.forms.AbstractFormPart#setFormInput(java.lang.Object)
	 */
	@Override
	public boolean setFormInput(Object input) {
		if (input instanceof ByteValues) {
			fByteValues = (ByteValues) input;
			return true;
		}

		return false;
	}

	/**
	 * Notify other Part of the ManagedForm that the values in the source <code>ByteValues</code>
	 * has changed.
	 * <p>
	 * The other parts must implement the <code>IPartSelectionListener</code> interface to receive
	 * the notification. The <code>ISelection</code> required by the ManagedForm is not used and
	 * is just an empty implementation.
	 * </p>
	 */
	protected void notifyForm() {
		getManagedForm().fireSelectionChanged(this, new ISelection() {
			@Override
			public boolean isEmpty() {
				return false;
			}
		});

	}

	/**
	 * Get the <code>ByteValues</code> input object of this form.
	 * 
	 * @return
	 */
	protected ByteValues getByteValues() {
		return fByteValues;
	}

	/**
	 * Create the actions.
	 * <p>
	 * The actions are added to the form toolbar. If the returned array has a <code>null</code> in
	 * it, then a toolbar separator is added in its place.
	 * </p>
	 * 
	 * @return An <code>IAction</code> that can be added to the form toolbar.
	 */
	abstract protected IAction[] getAction();

}
