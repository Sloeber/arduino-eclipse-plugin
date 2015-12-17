/*******************************************************************************
 * Copyright (c) 2015 QNX Software Systems and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package it.baeyens.arduino.managers.ui;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import it.baeyens.arduino.managers.ArduinoBoard;
import it.baeyens.arduino.managers.ArduinoManager;
import it.baeyens.arduino.managers.ArduinoPlatform;
import it.baeyens.arduino.ui.Activator;

public class ArduinoBoardsPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

	private Table table;
	private Button installButton;
	private Set<ArduinoBoard> toInstall = new HashSet<>();

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
		desc.setText("Select a board you would like to install and click Install and then OK or Apply to install the SDK and Tools for that board. By doing so you agree to the licenses of the libraries and tools. For more information, see http://arduino.cc.");

		Composite comp = new Composite(control, SWT.NONE);
		GridLayout layout = new GridLayout(2, false);
		layout.marginWidth = 0;
		comp.setLayout(layout);
		comp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		Composite tableComp = new Composite(comp, SWT.NONE);
		tableComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		table = new Table(tableComp, SWT.SINGLE | SWT.BORDER | SWT.V_SCROLL | SWT.FULL_SELECTION);
		table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		table.setHeaderVisible(true);
		table.setLinesVisible(true);

		TableColumn packageColumn = new TableColumn(table, SWT.LEAD);
		packageColumn.setText("Board");

		TableColumn platformColumn = new TableColumn(table, SWT.LEAD);
		platformColumn.setText("Platform");

		TableColumn installedColumn = new TableColumn(table, SWT.LEAD);
		installedColumn.setText("Installed");

		TableColumnLayout tableLayout = new TableColumnLayout();
		tableLayout.setColumnData(packageColumn, new ColumnWeightData(5, 150, true));
		tableLayout.setColumnData(platformColumn, new ColumnWeightData(5, 150, true));
		tableLayout.setColumnData(installedColumn, new ColumnWeightData(2, 75, true));
		tableComp.setLayout(tableLayout);

		table.addListener(SWT.Selection, new Listener() {
			@Override
			public void handleEvent(Event event) {
				updateButtons();
			}
		});

		Composite buttonComp = new Composite(comp, SWT.NONE);
		buttonComp.setLayout(new GridLayout());
		buttonComp.setLayoutData(new GridData(SWT.BEGINNING, SWT.BEGINNING, false, false));

		installButton = new Button(buttonComp, SWT.PUSH);
		installButton.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));
		installButton.setText("Install");
		installButton.addListener(SWT.Selection, new Listener() {
			@Override
			public void handleEvent(Event event) {
				for (TableItem item : table.getSelection()) {
					ArduinoBoard board = (ArduinoBoard) item.getData();
					toInstall.add(board);
					item.setText(2, "selected");
					updateButtons();
				}
			}
		});

		updateTable();
		updateButtons();

		return control;
	}

	private void updateTable() {
		if (table == null || table.isDisposed()) {
			return;
		}

		table.removeAll();

		try {
			List<ArduinoBoard> boards = ArduinoManager.getBoards();
			Collections.sort(boards, new Comparator<ArduinoBoard>() {
				public int compare(ArduinoBoard o1, ArduinoBoard o2) {
					return o1.getName().compareTo(o2.getName());
				}
			});

			for (ArduinoBoard board : boards) {
				TableItem item = new TableItem(table, SWT.NONE);
				item.setData(board);
				item.setText(0, board.getName());
				item.setText(1, board.getPlatform().getName());
				String msg;
				if (toInstall.contains(board)) {
					msg = "selected";
				} else {
					msg = board.getPlatform().isInstalled() ? "yes" : "no";
				}
				item.setText(2, msg);
			}
		} catch (CoreException e) {
			Activator.log(e);
		}
	}

	private void updateButtons() {
		if (table == null || table.isDisposed()) {
			return;
		}

		boolean enable = false;
		for (TableItem item : table.getSelection()) {
			ArduinoBoard board = (ArduinoBoard) item.getData();
			if (toInstall.contains(board)) {
				continue;
			}
			ArduinoPlatform platform = board.getPlatform();
			if (!platform.isInstalled()) {
				enable = true;
			}
		}
		installButton.setEnabled(enable);
	}

	@Override
	public boolean performOk() {
		new Job("Installing Arduino Board Platforms") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				Set<ArduinoPlatform> platforms = new HashSet<>();
				for (ArduinoBoard board : toInstall) {
					platforms.add(board.getPlatform());
				}

				MultiStatus status = new MultiStatus(Activator.getId(), 0, "Installing Arduino Board Platforms",
						null);
				for (ArduinoPlatform platform : platforms) {
					status.add(platform.install(monitor));
				}

				toInstall.clear();

				if (table != null && !table.isDisposed()) {
					table.getDisplay().asyncExec(new Runnable() {
						@Override
						public void run() {
							updateTable();
						}
					});
				}

				return status;
			}
		}.schedule();
		return true;
	}

}
