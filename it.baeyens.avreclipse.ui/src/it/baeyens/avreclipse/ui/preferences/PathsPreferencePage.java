/*******************************************************************************
 * 
 * Copyright (c) 2007, 2010 Thomas Holland (thomas@innot.de) and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the GNU Public License v3
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     Thomas Holland - initial API and implementation
 *     
 * $Id: PathsPreferencePage.java 851 2010-08-07 19:37:00Z innot $
 *     
 *******************************************************************************/
package it.baeyens.avreclipse.ui.preferences;

import it.baeyens.avreclipse.core.preferences.AVRPathsPreferences;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;


/**
 * Paths Preference page of the AVR Eclipse plugin.
 * <p>
 * This page manages two preferences:
 * <ul>
 * <li>The "no scan at startup" flag to inhibit the background scan for changed system paths.</li>
 * <li>The path settings for all paths required by the plugin.</li>
 * </ul>
 * </p>
 * <p>
 * Most of the real work of path management is done in the {@link AVRPathsFieldEditor} included on
 * this page.
 * </p>
 * 
 * @author Thomas Holland
 * @since 2.1
 */

public class PathsPreferencePage extends FieldEditorPreferencePage implements
		IWorkbenchPreferencePage {

	private IPreferenceStore	fPreferenceStore	= null;
	private boolean mIsDirty = false;
	
	@Override
	public void propertyChange(PropertyChangeEvent event) {
		mIsDirty=true;
	}
	
	@Override
	public boolean performOk() {
		if (mIsDirty) return super.performOk();
		return true;
	}

	public PathsPreferencePage() {
		super(GRID);

		// Get the instance scope path preference store
		fPreferenceStore = AVRPathsPreferences.getPreferenceStore();
		setPreferenceStore(fPreferenceStore);
		setDescription("Path Settings for the AVR Eclipse Plugin");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.preference.FieldEditorPreferencePage#createFieldEditors()
	 */
	@Override
	public void createFieldEditors() {

		// This page has two fields:
		// The first one to inhibit the startup search for changed system paths
		// The second to edit all paths.

		Composite parent = getFieldEditorParent();

		Label filler = new Label(parent, SWT.NONE);
		filler.setLayoutData(new GridData(SWT.FILL, SWT.NONE, true, false, 2, 1));

		// Startup search inhibit

		BooleanFieldEditor autoScanBoolean = new BooleanFieldEditor(
				AVRPathsPreferences.KEY_NOSTARTUPSCAN,
				"Disable search for system paths at startup", BooleanFieldEditor.DEFAULT, parent);
		addField(autoScanBoolean);

		Composite note = createNoteComposite(JFaceResources.getDialogFont(), parent, "Note:",
				"If disabled, a manual rescan may be required when a new avr-gcc toolchain has been installed.\n");
		note.setLayoutData(new GridData(SWT.FILL, SWT.NONE, true, false, 2, 1));

		filler = new Label(parent, SWT.NONE);
		filler.setLayoutData(new GridData(SWT.FILL, SWT.NONE, true, false, 2, 1));

		// Path editor field control.

		AVRPathsFieldEditor pathEditor = new AVRPathsFieldEditor(parent);
		addField(pathEditor);

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.IWorkbenchPreferencePage#init(org.eclipse.ui.IWorkbench)
	 */
	@Override
	public void init(IWorkbench workbench) {
		// nothing to init
	}

}
