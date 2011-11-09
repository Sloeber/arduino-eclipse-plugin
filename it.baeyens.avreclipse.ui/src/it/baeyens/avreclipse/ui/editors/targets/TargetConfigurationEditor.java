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
 * $Id: TargetConfigurationEditor.java 851 2010-08-07 19:37:00Z innot $
 *     
 *******************************************************************************/

package it.baeyens.avreclipse.ui.editors.targets;

import it.baeyens.arduino.ArduinoConst;
import it.baeyens.avreclipse.core.targets.ITargetConfigurationWorkingCopy;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.IMessage;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.editor.SharedHeaderFormEditor;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.ScrolledForm;


/**
 * @author Thomas Holland
 * @since
 * 
 */
public class TargetConfigurationEditor extends SharedHeaderFormEditor {

	private ITargetConfigurationWorkingCopy	fWorkingCopy;

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.forms.editor.FormEditor#init(org.eclipse.ui.IEditorSite,
	 * org.eclipse.ui.IEditorInput)
	 */
	@Override
	public void init(IEditorSite site, IEditorInput editorInput) throws PartInitException {
		if (!(editorInput instanceof TCEditorInput)) {
			throw new PartInitException("Invalid Input: Must be an AVR Hardware Configuration");
		}

		super.init(site, editorInput);

		// Use the name of the Configuration as a part name
		setPartName(editorInput.getName());

		// Description is not required as it should be obvious to the user what he is editing.
		// setContentDescription("Edit Target Configuration");

		// Get a working copy of the target configuration which will be the model for the actual
		// pages.
		fWorkingCopy = (ITargetConfigurationWorkingCopy) getEditorInput().getAdapter(
				ITargetConfigurationWorkingCopy.class);
		if (fWorkingCopy == null) {
			throw new PartInitException("Could not create a editable hardware configuration object");
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.forms.editor.FormEditor#addPages()
	 */
	@Override
	protected void addPages() {
		List<FormPage> pages = new ArrayList<FormPage>();
		FormPage page;

		try {
			page = new PageMain(this);
			addPage(page);
			pages.add(page);

			page = new PageProgrammer(this);
			addPage(page);
			pages.add(page);

			page = new PageProgrammerTool(this);
			addPage(page);
			pages.add(page);

			page = new PageGDBServerTool(this);
			addPage(page);
			pages.add(page);

		} catch (PartInitException e) {
			//
		}

		// The following is a hack, but I have found no other solution -- do you have one?
		//
		// Problem: I want all problems/errors/warnings of the target configuration to be shown in
		// the shared header as soon as the editor is opened.
		// But the MessageManager associates messages (= errors and warnings) with the control
		// that is passed in the addMessage() method. So to show errors/warnings we need the
		// controls that generate them. But normally the controls of a page won't be created until
		// that page is actually activated by the user.
		//
		// Solution: call createPartControl() on all pages right away.
		// With this all controls of the editor in all pages are created right away and the
		// FormParts can start adding/removing messages to the shared header of this editor.
		// This is a hack because it mimics the internal behavior of the FormEditor class. If the
		// FormEditor class changes its internals this may break!
		for (int i = 0; i < pages.size(); i++) {
			page = pages.get(i);
			if (page.getPartControl() == null) {
				page.createPartControl(getContainer());
				setControl(i, page.getPartControl());
				page.getPartControl().setMenu(getContainer().getMenu());
			}

		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.part.EditorPart#doSave(org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	public void doSave(IProgressMonitor monitor) {

		// Convert the given monitor into a progress instance
		SubMonitor progress = SubMonitor.convert(monitor, 12);
		try {

			// Tell all pages to commit their changes to the target configuration
			commitPages(true);
			progress.worked(1);

			fWorkingCopy.doSave();
			progress.worked(10);

			firePropertyChange(PROP_DIRTY);

			// Update the part title (in case the target configuration name has been changed)
			setPartName(fWorkingCopy.getName());
			progress.worked(1);

		} catch (IOException ioe) {
			showErrorDialog("Could not save hardware configuration", ioe);
		} finally {
			monitor.done();
		}

	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.part.EditorPart#isSaveAsAllowed()
	 */
	@Override
	public boolean isSaveAsAllowed() {
		// TargetConfigurations can not be saved under a different name.
		return false;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.part.EditorPart#doSaveAs()
	 */
	@Override
	public void doSaveAs() {
		// Save as not supported for target configurations
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.eclipse.ui.forms.editor.SharedHeaderFormEditor#createHeaderContents(org.eclipse.ui.forms
	 * .IManagedForm)
	 */
	@Override
	protected void createHeaderContents(IManagedForm headerForm) {
		final ScrolledForm sform = headerForm.getForm();
		sform.setText("Target Configuration");
		getToolkit().decorateFormHeading(sform.getForm());

		// Add a hypertext listener that will set the focus to the control that reported the error.
		// In case of multiple problems it will jump to the last problem in the list (which seems to
		// be the first one that was added.)
		sform.getForm().addMessageHyperlinkListener(new HyperlinkAdapter() {
			@Override
			public void linkActivated(HyperlinkEvent e) {
				Object href = e.getHref();
				if (href instanceof IMessage[]) {
					IMessage[] messages = (IMessage[]) href;
					IMessage message = messages[messages.length - 1];

					Object data = message.getData();
					if (data != null && (data instanceof String)) {
						String attribute = (String) data;
						selectReveal(attribute);
						return;
					}
				}
			}
		});

	}

	/**
	 * Show a standard error message dialog with the given message and exception.
	 * 
	 * @param message
	 * @param exception
	 *            may be <code>null</code>
	 */
	private void showErrorDialog(String message, Throwable exception) {
		IStatus status = new Status(IStatus.ERROR, ArduinoConst.UI_PLUGIN_ID, message, exception);
		ErrorDialog dialog = new ErrorDialog(this.getContainer().getShell(),
				"Hardware Configuration Error", null, status, IStatus.ERROR);
		dialog.open();
	}
}
