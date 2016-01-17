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

import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import it.baeyens.arduino.common.ArduinoConst;
import it.baeyens.arduino.common.ConfigurationPreferences;
import it.baeyens.arduino.managers.ArduinoManager;


public class ArduinoLinkPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

    private Text urlsText;

    @Override
    public void init(IWorkbench workbench) {
	// no code needed
    }

    @Override
    protected Control createContents(Composite parent) {
	Composite control = new Composite(parent, SWT.NONE);
	control.setLayout(new GridLayout());

	Text desc = new Text(control, SWT.READ_ONLY | SWT.WRAP);
	GridData layoutData = new GridData(SWT.FILL, SWT.FILL, true, false);
	layoutData.widthHint = 500;
	desc.setLayoutData(layoutData);
	desc.setBackground(parent.getBackground());
	desc.setText(Messages.ui_url_for_package_index_file);

	this.urlsText = new Text(control, SWT.BORDER | SWT.MULTI);
	this.urlsText.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
	this.urlsText.setText(ConfigurationPreferences.getBoardURLs());

	return control;
    }

    @Override
    public boolean performOk() {
	ConfigurationPreferences.setBoardURLs(this.urlsText.getText());
	ArduinoManager.loadIndices(true);
	return true;
    }

    @Override
    protected void performDefaults() {
	String defaultBoardUrl = ArduinoConst.DEFAULT_ARDUINO_MANAGER_BOARD_URLS;
	this.urlsText.setText(defaultBoardUrl);
	ConfigurationPreferences.setBoardURLs(defaultBoardUrl);
	super.performDefaults();
    }

}
