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
package io.sloeber.core.ui;

import org.eclipse.cdt.core.parser.util.StringUtil;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.preferences.ConfigurationScope;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.Hyperlink;
import org.eclipse.ui.preferences.ScopedPreferenceStore;

import io.sloeber.common.Common;
import io.sloeber.common.Const;
import io.sloeber.core.api.BoardsManager;

public class LinkPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

    private Text urlsText;
    BooleanFieldEditor upDateJsons;

    public LinkPreferencePage() {
	super(org.eclipse.jface.preference.FieldEditorPreferencePage.GRID);
	setDescription(Messages.json_maintain);
	setPreferenceStore(new ScopedPreferenceStore(ConfigurationScope.INSTANCE, Const.NODE_ARDUINO));
    }

    @Override
    public boolean performOk() {
	BoardsManager.setBoardsPackageURL(this.urlsText.getText().split(System.lineSeparator()));
	return super.performOk();
    }

    @Override
    protected void performDefaults() {
	super.performDefaults();
	this.urlsText.setText(BoardsManager.getBoardsPackageURLs());
    }

    @Override
    protected void createFieldEditors() {
	String selectedJsons[] = BoardsManager.getBoardsPackageURLList();
	final Composite parent = getFieldEditorParent();
	// Composite control = new Composite(parent, SWT.NONE);
	Label title = new Label(parent, SWT.UP);
	title.setFont(parent.getFont());

	title.setText(Messages.ui_url_for_package_index_file);
	title.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING));

	this.urlsText = new Text(parent, SWT.MULTI | SWT.V_SCROLL | SWT.BORDER | SWT.WRAP);
	GridData gd = new GridData(GridData.FILL_BOTH);
	this.urlsText.setLayoutData(gd);
	this.urlsText.setText(StringUtil.join(selectedJsons, System.lineSeparator()));

	this.upDateJsons = new BooleanFieldEditor(BoardsManager.getUpdateJasonFilesKey(), Messages.json_update,
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
    public void init(IWorkbench arg0) {
	// Nothing to do here

    }

}
