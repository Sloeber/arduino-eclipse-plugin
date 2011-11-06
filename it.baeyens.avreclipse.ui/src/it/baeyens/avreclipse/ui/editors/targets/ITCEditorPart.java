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
 * $Id: ITCEditorPart.java 851 2010-08-07 19:37:00Z innot $
 *     
 *******************************************************************************/

package it.baeyens.avreclipse.ui.editors.targets;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.forms.IFormPart;
import org.eclipse.ui.forms.IMessageManager;

/**
 * Extension of the <code>IFormPart</code> Interface for the Target Configuration Editor.
 * <p>
 * This interface adds some APIs to <code>IFormPart</code>:
 * <ol>
 * <li>The notion of attributes that this form part can edit.<br/>
 * <br/>
 * <p>
 * There is one method for this:
 * <ul>
 * <li>The {@link #setFocus(String)} method to set the focus on the control for the given attribute.
 * </li>
 * </ul>
 * </p>
 * <br/>
 * </li>
 * <li>Additional lifecycle methods to allow the form part to be created after instantiation with
 * the default constructor.<br/>
 * <br/>
 * <p>
 * There are two methods for this:
 * <ul>
 * <li>{@link #setParent(Composite)} to set the parent for the control to be generated in the
 * {@link #initialize(org.eclipse.ui.forms.IManagedForm)} method.</li>
 * <li>{@link #getControl()} to get the control that was created in the
 * {@link #initialize(org.eclipse.ui.forms.IManagedForm)} method.</li>
 * </ul>
 * With this the interface can be used for Eclipse extension point implementations, which use the
 * default constructor to instantiate the class.
 * </p>
 * <br/>
 * </li>
 * <li>The {@link #setMessageManager(IMessageManager)} method to change to a message manager
 * different from the one supplied by the managed form. This is used to have any messages generated
 * by this part to be shown in the shared header of the target configuration editor.</li>
 * </ol>
 *</p>
 *<p>
 * The {@link AbstractTCSectionPart} is the default implementation of this interface and should be
 * used instead of implementing this interface directly.
 * </p>
 * 
 * @author Thomas Holland
 * @since 2.4
 * 
 */
public interface ITCEditorPart extends IFormPart {

	//
	// Attribute API
	//

	/**
	 * Set the focus to the control that can change the given attribute.
	 * 
	 * @param attribute
	 *            A target configuration attribute.
	 * @return <code>false</code> if this form part has no controls for the attribute.
	 */
	public boolean setFocus(String attribute);

	//
	// Life Cycle API
	//

	/**
	 * Set the parent composite to be used for creating this form part.
	 * <p>
	 * This method must be called before {@link #initialize(org.eclipse.ui.forms.IManagedForm)}.
	 * Otherwise the body of the managed form will be used as the parent.
	 * </p>
	 * <p>
	 * Calls to this method after <code>initialize()</code> won't have any effect.
	 * </p>
	 * 
	 * @param parent
	 */
	public void setParent(Composite parent);

	/**
	 * Get the control containing this form part.
	 * <p>
	 * The control is a child of the parent given with the {@link #setParent(Composite)} method (of
	 * of the managed form body if <code>setParent()</code> was not called before
	 * <code>initialize()</code>.
	 * </p>
	 * 
	 * @return The control for this form part or <code>null</code> if the form part has not yet been
	 *         initialized.
	 */
	public Control getControl();

	//
	// Message API
	//

	/**
	 * Change the default message manager to a different one.
	 * <p>
	 * Implementations must use the given message manager instead of the the one from
	 * <code>getManagedForm().getMessageManager()</code>.
	 * </p>
	 * 
	 * @param manager
	 */
	public void setMessageManager(IMessageManager manager);

}