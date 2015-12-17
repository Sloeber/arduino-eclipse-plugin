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
package it.baeyens.arduino.managers.ui;

import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import it.baeyens.arduino.managers.ArduinoManager;
import it.baeyens.arduino.managers.ArduinoPreferences;

public class ArduinoPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

	private Text urlsText;

	@Override
	public void init(IWorkbench workbench) {
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
		desc.setText("Enter URLs for package_index.json files one per line.");

		urlsText = new Text(control, SWT.BORDER|SWT.MULTI);
		urlsText.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		urlsText.setText(ArduinoPreferences.getBoardUrls());

		return control;
	}

	@Override
	public boolean performOk() {
		ArduinoPreferences.setBoardUrls(urlsText.getText());
		ArduinoManager.loadIndices(true);
		return true;
	}

	@Override
	protected void performDefaults() {
		String defaultBoardUrl = ArduinoPreferences.getDefaultBoardUrls();
		urlsText.setText(defaultBoardUrl);
		ArduinoPreferences.setBoardUrls(defaultBoardUrl);
		super.performDefaults();
	}

}
