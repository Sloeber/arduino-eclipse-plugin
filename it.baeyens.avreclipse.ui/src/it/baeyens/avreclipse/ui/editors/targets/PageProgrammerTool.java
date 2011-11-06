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
 * $Id: PageProgrammerTool.java 851 2010-08-07 19:37:00Z innot $
 *     
 *******************************************************************************/

package it.baeyens.avreclipse.ui.editors.targets;

import it.baeyens.arduino.globals.ArduinoConst;
import it.baeyens.avreclipse.core.targets.IProgrammerTool;
import it.baeyens.avreclipse.core.targets.ITargetConfigChangeListener;
import it.baeyens.avreclipse.core.targets.ITargetConfigConstants;
import it.baeyens.avreclipse.core.targets.ITargetConfiguration;
import it.baeyens.avreclipse.core.targets.ITargetConfigurationWorkingCopy;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.SharedHeaderFormEditor;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.forms.widgets.TableWrapData;
import org.eclipse.ui.forms.widgets.TableWrapLayout;


/**
 * 
 * @author Thomas Holland
 * @since 2.4
 * 
 */
public class PageProgrammerTool extends BasePage implements ITargetConfigChangeListener {

	private final static String						TITLE	= "Programmer Tool";

	/**
	 * The target configuration this editor page works on. The target config is final and con not be
	 * changed after instantiation of the page. This is the 'model' for the managed form.
	 */
	final private ITargetConfigurationWorkingCopy	fTCWC;

	private ITCEditorPart							fSectionPart;

	private IManagedForm							fManagedForm;

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
	public PageProgrammerTool(SharedHeaderFormEditor editor) {
		super(editor, ArduinoConst.programmertool, TITLE);

		// Get the TargetConfiguration from the editor input.
		IEditorInput ei = editor.getEditorInput();
		fTCWC = (ITargetConfigurationWorkingCopy) ei
				.getAdapter(ITargetConfigurationWorkingCopy.class);

		fTCWC.addPropertyChangeListener(this);

	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.forms.editor.FormPage#dispose()
	 */
	@Override
	public void dispose() {
		if (fTCWC != null) {
			fTCWC.removePropertyChangeListener(this);
		}
		// TODO Auto-generated method stub
		super.dispose();
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.eclipse.ui.forms.editor.FormPage#createFormContent(org.eclipse.ui.forms.IManagedForm)
	 */
	@Override
	protected void createFormContent(IManagedForm managedForm) {

		fManagedForm = managedForm;

		Composite body = managedForm.getForm().getBody();
		body.setLayout(new TableWrapLayout());

		SectionProgrammerTool programmerToolPart = new SectionProgrammerTool();
		programmerToolPart.setMessageManager(getMessageManager());
		managedForm.addPart(programmerToolPart);
		registerPart(programmerToolPart);
		programmerToolPart.getControl().setLayoutData(new TableWrapData(TableWrapData.FILL_GRAB));

		createExtensibleContent();

		// ... and give the 'model' to the managed form which will cause the dynamic parts of the
		// form to be rendered.
		managedForm.setInput(fTCWC);

	}

	private void createExtensibleContent() {

		if (fSectionPart != null) {
			// Remove the previous settings part
			fManagedForm.removePart(fSectionPart);
			unregisterPart(fSectionPart);
			fSectionPart.dispose();
			fSectionPart = null;
			fManagedForm.reflow(true);
		}

		IProgrammerTool tool = fTCWC.getProgrammerTool();
		String toolid = tool.getId();
		ITCEditorPart part = SettingsExtensionManager.getDefault().getSettingsPartForTool(toolid);

		if (part == null) {
			// No extension with a GUI for the tool available
			// Create a dummy part that contains just a label to inform the user that there are no
			// settings for this tool.
			part = createDummyPart(toolid);
		}
		part.setMessageManager(getMessageManager());
		fManagedForm.addPart(part);
		part.setFormInput(fTCWC);
		registerPart(part);
		part.getControl().setLayoutData(new TableWrapData(TableWrapData.FILL_GRAB));
		part.setFormInput(fTCWC);
		fSectionPart = part;
		fManagedForm.reflow(true);

		// Setting the part name does not change anything. Probably because the MulitPageEditorPart
		// does not listen to Part Property change events.
		// I leave this code in case the current behavior gets changed in future Eclipse versions.
		String partname = "Programmer: " + tool.getName();
		setPartName(partname);

	}

	@Override
	public void attributeChange(ITargetConfiguration config, String attribute, String oldvalue,
			String newvalue) {

		if (ITargetConfigConstants.ATTR_PROGRAMMER_TOOL_ID.equals(attribute)
				|| ITargetConfigConstants.ATTR_GDBSERVER_ID.equals(attribute)) {
			// The programmer tool has been changed
			createExtensibleContent();
		}

	}

	private ITCEditorPart createDummyPart(String tool) {

		return new AbstractTCSectionPart() {

			@Override
			protected void createSectionContent(Composite parent, FormToolkit toolkit) {
			}

			@Override
			protected String[] getPartAttributes() {
				return new String[] {};
			}

			@Override
			protected String getTitle() {
				return "Settings";
			}

			@Override
			protected String getDescription() {
				return "No Settings available";
			}

			@Override
			protected int getSectionStyle() {
				return Section.SHORT_TITLE_BAR | Section.EXPANDED | Section.CLIENT_INDENT;
			}

			@Override
			protected void refreshSectionContent() {
				// Do nothing
			}

		};

	}
}
