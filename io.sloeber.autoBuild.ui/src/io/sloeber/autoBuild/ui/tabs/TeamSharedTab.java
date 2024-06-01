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

import org.eclipse.cdt.core.settings.model.ICResourceDescription;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import io.sloeber.autoBuild.ui.internal.Messages;

/**
 * @noextend This class is not intended to be subclassed by clients.
 * @noinstantiate This class is not intended to be instantiated by clients.
 */
public class TeamSharedTab extends AbstractAutoBuildPropertyTab {

    private Button myShareConfigButton;

    @Override
    public void createControls(Composite parent) {
        super.createControls(parent);
        usercomp.setLayout(new GridLayout(1, false));

        // Builder group
        Group g1 = setupGroup(usercomp, Messages.ShareConfigTitle, 3, GridData.FILL_HORIZONTAL);
        myShareConfigButton = setupCheck(g1, Messages.shareConfigButton, 3, GridData.BEGINNING);
        myShareConfigButton.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                myAutoConfDesc.setTeamShared(myShareConfigButton.getSelection());
                updateButtons();
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent e) {
                // TODO Auto-generated method stub
                return;
            }
        });

    }

    private void updateDisplayedData() {
    	myShareConfigButton.setSelection(myAutoConfDesc.isTeamShared());
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
    	myShareConfigButton.setSelection(myAutoConfDesc.isTeamShared());
    }


    // This page can be displayed for project only
    @Override
    public boolean canBeVisible() {
        return page.isForProject() || page.isForPrefs();
    }

    @Override
    protected void performDefaults() {
    	myShareConfigButton.setSelection(false);
    }

}
