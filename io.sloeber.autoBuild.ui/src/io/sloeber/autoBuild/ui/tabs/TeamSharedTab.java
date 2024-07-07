/*******************************************************************************
 * Copyright (c) 2007, 2011 Intel Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * Intel Corporation - Initial API and implementation
 * IBM Corporation
 * Dmitry Kozlov (CodeSourcery) - save build output preferences (bug 294106)
 * Andrew Gvozdev (Quoin Inc)   - Saving build output implemented in different way (bug 306222)
 * Jan Baeyens                  - adapted and rewritten for autoBuild also removed multiconfig feature
 *******************************************************************************/
package io.sloeber.autoBuild.ui.tabs;

import static io.sloeber.autoBuild.helpers.api.AutoBuildConstants.*;
import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

import org.eclipse.cdt.core.settings.model.ICResourceDescription;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.List;

import io.sloeber.autoBuild.ui.internal.Messages;

/**
 * @noextend This class is not intended to be subclassed by clients.
 * @noinstantiate This class is not intended to be instantiated by clients.
 */
public class TeamSharedTab extends AbstractAutoBuildPropertyTab {

	private Button myShareConfigButton;
	private List myExclusions;
	private Button myAddExclusionButton;
	private Button myRemoveExclusionButton;

	@Override
	public void createControls(Composite parent) {
		super.createControls(parent);
		usercomp.setLayout(new GridLayout(1, false));

		// Builder group
		Group g1 = setupGroup(usercomp, Messages.ShareConfigTitle, 2,
				GridData.FILL_HORIZONTAL | GridData.FILL_VERTICAL);
		myShareConfigButton = setupCheck(g1, Messages.shareConfigButton, 2, GridData.BEGINNING);
		Group g3 = setupGroup(g1, EMPTY_STR, 1, SWT.FILL | GridData.FILL_HORIZONTAL | GridData.FILL_VERTICAL);
		myExclusions = new List(g3, GridData.FILL_BOTH);
		GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
		myExclusions.setLayoutData(gridData);
		Group g4 = setupGroup(g1, EMPTY_STR, 1, GridData.VERTICAL_ALIGN_BEGINNING);

		int mode = GridData.BEGINNING | GridData.VERTICAL_ALIGN_BEGINNING;
		myAddExclusionButton = setupButton(g4, Messages.addExclusion, 1, mode);
		myRemoveExclusionButton = setupButton(g4, Messages.removeExclusion, 1, mode);
		myExclusions.addSelectionListener(new SelectionListener() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				myRemoveExclusionButton.setEnabled(myExclusions.getSelectionCount() > 0);

			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				// Nothing to be done

			}
		});
		myShareConfigButton.addSelectionListener(new SelectionListener() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				myAutoConfDesc.setTeamShared(myShareConfigButton.getSelection());
				updateButtons();
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				return;
			}
		});

		myRemoveExclusionButton.addSelectionListener(new SelectionListener() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				String[] selected = myExclusions.getSelection();
				if (selected.length == 0) {
					return;
				}
				Set<String> allItem = new TreeSet<>(Arrays.asList(myExclusions.getItems()));
				allItem.removeAll(Arrays.asList(selected));
				myAutoConfDesc.setCustomTeamExclusionKeys(allItem);
				updateDisplayedData();
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				// Nothing to be done
			}
		});

		myAddExclusionButton.addSelectionListener(new SelectionListener() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				JFrame frame = new JFrame();
				String text = JOptionPane.showInputDialog(frame, Messages.ProvideExclusionKey);
				if (text != null) {
					Set<String> exclusiosn = getCustomTeamExclusionKeys();
					exclusiosn.add(text);
					myAutoConfDesc.setCustomTeamExclusionKeys(exclusiosn);
					updateDisplayedData();
				}
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				// nothing to do her
			}
		});
	}

	private void updateDisplayedData() {
		boolean isShared = myAutoConfDesc.isTeamShared();
		myShareConfigButton.setSelection(isShared);
		myAddExclusionButton.setEnabled(isShared);
		myExclusions.setEnabled(isShared);
		myExclusions.removeAll();
		if (isShared) {
			String prefix=myAutoConfDesc.getCdtConfigurationDescription().getName()+DOT;
			Set<String> exclusions = getCustomTeamExclusionKeys();
			for (String curExclusion : exclusions) {
				myExclusions.add(prefix+curExclusion);
			}
			myRemoveExclusionButton.setEnabled(myExclusions.getSelectionCount() > 0);
		} else {
			myRemoveExclusionButton.setEnabled(isShared);

		}
	}

	private Set<String> getCustomTeamExclusionKeys() {
		Set<String> ret = myAutoConfDesc.getCustomTeamExclusionKeys();
		if (ret == null) {
			ret = myAutoConfDesc.getDefaultTeamExclusionKeys();
		}
		return ret;
	}

	@Override
	public void updateData(ICResourceDescription cfgd) {
		super.updateData(cfgd);
		updateDisplayedData();
	}

	/**
	 * sets widgets states
	 */
	@Override
	protected void updateButtons() {
		updateDisplayedData();
	}

	// This page can be displayed for project only
	@Override
	public boolean canBeVisible() {
		return page.isForProject() || page.isForPrefs();
	}

	@Override
	protected void performDefaults() {
		myAutoConfDesc.setTeamShared(false);
		myAutoConfDesc.setCustomTeamExclusionKeys(null);
		updateDisplayedData();
	}

}
