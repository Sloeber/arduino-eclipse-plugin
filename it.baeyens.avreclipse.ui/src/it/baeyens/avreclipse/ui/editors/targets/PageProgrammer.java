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
 * $Id: PageProgrammer.java 851 2010-08-07 19:37:00Z innot $
 *     
 *******************************************************************************/

package it.baeyens.avreclipse.ui.editors.targets;

import it.baeyens.arduino.globals.ArduinoConst;
import it.baeyens.avreclipse.core.targets.ITargetConfigurationWorkingCopy;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.SharedHeaderFormEditor;
import org.eclipse.ui.forms.widgets.TableWrapData;
import org.eclipse.ui.forms.widgets.TableWrapLayout;


/**
 * The GUI code container for the Target Configuration editor.
 * <p>
 * This page is the one and only page in the {@link TargetConfigurationEditor}. It implements the
 * <code>IFormPart</code> interface to utilize the managed form API of Eclipse.
 * 
 * </p>
 * 
 * @author Thomas Holland
 * @since 2.4
 * 
 */
public class PageProgrammer extends BasePage {

	private final static String						TITLE	= "Programmer Hardware";

	/**
	 * The target configuration this editor page works on. The target config is final and con not be
	 * changed after instantiation of the page. This is the 'model' for the managed form.
	 */
	final private ITargetConfigurationWorkingCopy	fTCWC;

	/**
	 * Create a new EditorPage.
	 * <p>
	 * The page has the id from the {@link #ID} identifier and the fixed title string {@link #TITLE}
	 * .
	 * </p>
	 * 
	 * @param editor
	 *            Parent FormEditor
	 */
	public PageProgrammer(SharedHeaderFormEditor editor) {
		super(editor, ArduinoConst.programmer, TITLE);

		// Get the TargetConfiguration from the editor input.
		IEditorInput ei = editor.getEditorInput();
		fTCWC = (ITargetConfigurationWorkingCopy) ei
				.getAdapter(ITargetConfigurationWorkingCopy.class);
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.eclipse.ui.forms.editor.FormPage#createFormContent(org.eclipse.ui.forms.IManagedForm)
	 */
	@Override
	protected void createFormContent(IManagedForm managedForm) {

		// fill the fixed parts of the form...
		Composite body = managedForm.getForm().getBody();
		body.setLayout(new TableWrapLayout());

		{
			SectionProgrammer programmerPart = new SectionProgrammer();
			programmerPart.setMessageManager(getMessageManager());
			managedForm.addPart(programmerPart);
			registerPart(programmerPart);
			programmerPart.getControl().setLayoutData(new TableWrapData(TableWrapData.FILL_GRAB));
		}

		{
			SectionHostInterface hostInterfacePart = new SectionHostInterface();
			hostInterfacePart.setMessageManager(getMessageManager());
			managedForm.addPart(hostInterfacePart);
			registerPart(hostInterfacePart);
			hostInterfacePart.getControl()
					.setLayoutData(new TableWrapData(TableWrapData.FILL_GRAB));
		}

		{
			SectionTargetInterface targetInterfacePart = new SectionTargetInterface();
			targetInterfacePart.setMessageManager(getMessageManager());
			managedForm.addPart(targetInterfacePart);
			registerPart(targetInterfacePart);
			targetInterfacePart.getControl().setLayoutData(
					new TableWrapData(TableWrapData.FILL_GRAB));
		}

		// ... and give the 'model' to the managed form which will cause the dynamic parts of the
		// form to be rendered.
		managedForm.setInput(fTCWC);
	}

}
