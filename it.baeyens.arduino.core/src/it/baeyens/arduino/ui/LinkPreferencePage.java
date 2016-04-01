/*******************************************************************************
 * Copyright (c) 2015 QNX Software Systems and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     QNX Software Systems - Initial API and implementation
 *******************************************************************************/
package it.baeyens.arduino.ui;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.preferences.ConfigurationScope;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.Hyperlink;
import org.eclipse.ui.preferences.ScopedPreferenceStore;

import it.baeyens.arduino.common.Common;
import it.baeyens.arduino.common.ConfigurationPreferences;
import it.baeyens.arduino.common.Const;
import it.baeyens.arduino.managers.Manager;

public class LinkPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

    public LinkPreferencePage() {
	super(org.eclipse.jface.preference.FieldEditorPreferencePage.GRID);
	setDescription(Messages.json_maintain);
	setPreferenceStore(new ScopedPreferenceStore(ConfigurationScope.INSTANCE, Const.NODE_ARDUINO));
    }

    private MultiLineTextFieldEditor urlsText;
    BooleanFieldEditor upDateJsons;
    Set<String> oldSelectedJsons;

    @Override
    public boolean performOk() {
	this.oldSelectedJsons = new HashSet<>(Arrays.asList(ConfigurationPreferences.getBoardURLList()));
	this.urlsText.store();
	deleteJsonFilesAsNeeded();

	Manager.loadIndices(true);
	return super.performOk();
    }

    private void deleteJsonFilesAsNeeded() {
	Set<String> toDeleteJsons = this.oldSelectedJsons;
	Set<String> newSelectedJsons = new HashSet<>(Arrays.asList(ConfigurationPreferences.getBoardURLList()));
	if (toDeleteJsons == null) {
	    Common.log(new Status(IStatus.ERROR, Const.CORE_PLUGIN_ID,
		    "Previous jason files are null. This should not happen.", null)); //$NON-NLS-1$
	    return;
	}

	if (this.upDateJsons.getBooleanValue()) {
	    toDeleteJsons.addAll(newSelectedJsons);
	} else // only delete the removed ones
	{
	    toDeleteJsons.removeAll(newSelectedJsons);
	}

	for (String curJson : toDeleteJsons) {
	    File localFile = Manager.getLocalFileName(curJson);
	    if (localFile.exists()) {
		localFile.delete();
	    }
	}
    }

    @Override
    protected void performDefaults() {
	super.performDefaults();
	String defaultBoardUrl = Const.DEFAULT_MANAGER_BOARD_URLS;
	this.urlsText.setStringValue(defaultBoardUrl);
	ConfigurationPreferences.setBoardURLs(defaultBoardUrl);

    }

    @Override
    protected void createFieldEditors() {
	final Composite parent = getFieldEditorParent();
	// Composite control = new Composite(parent, SWT.NONE);

	this.urlsText = new MultiLineTextFieldEditor(Const.KEY_MANAGER_BOARD_URLS,
		Messages.ui_url_for_package_index_file, parent);
	addField(this.urlsText);

	this.upDateJsons = new BooleanFieldEditor(Const.KEY_UPDATE_JASONS, Messages.json_update,
		BooleanFieldEditor.DEFAULT, parent);
	addField(this.upDateJsons);
	final Hyperlink link = new Hyperlink(parent, SWT.NONE);
	link.setText(Messages.json_find);
	link.setHref("https://github.com/arduino/Arduino/wiki/Unofficial-list-of-3rd-party-boards-support-urls"); //$NON-NLS-1$
	link.setUnderlined(true);
	link.addHyperlinkListener(new HyperlinkAdapter() {
	    @Override
	    public void linkActivated(HyperlinkEvent he) {
		try {
		    org.eclipse.swt.program.Program.launch(link.getHref().toString());
		} catch (IllegalArgumentException e) {
		    Common.log(new Status(IStatus.ERROR, Const.CORE_PLUGIN_ID, Messages.json_browser_fail, e));
		}
	    }
	});

    }

    @Override
    public void init(IWorkbench workbench) {
	// TODO Auto-generated method stub

    }

}
