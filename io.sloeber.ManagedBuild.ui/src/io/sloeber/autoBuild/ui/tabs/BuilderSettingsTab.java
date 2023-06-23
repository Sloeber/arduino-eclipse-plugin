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
import org.eclipse.cdt.ui.newui.AbstractCPropertyTab;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import io.sloeber.autoBuild.api.IBuildRunner;
import io.sloeber.autoBuild.ui.internal.Messages;

/**
 * @noextend This class is not intended to be subclassed by clients.
 * @noinstantiate This class is not intended to be instantiated by clients.
 */
public class BuilderSettingsTab extends AbstractAutoBuildPropertyTab {

    private Button myUseDefaultBuildCommandButton;
    private Combo myBuilderTypeCombo;
    private Text myBuildCmdText;
    private Button myBuildCommandButton;
    private Button myAutoGenMakefileButton;
    private Text myBuildFolderText;

    @Override
    public void createControls(Composite parent) {
        super.createControls(parent);
        usercomp.setLayout(new GridLayout(1, false));

        // Builder group
        Group g1 = setupGroup(usercomp, Messages.BuilderSettingsTab_0, 3, GridData.FILL_HORIZONTAL);
        setupLabel(g1, Messages.BuilderSettingsTab_1, 1, GridData.BEGINNING);
        myBuilderTypeCombo = new Combo(g1, SWT.READ_ONLY | SWT.DROP_DOWN | SWT.BORDER);
        setupControl(myBuilderTypeCombo, 2, GridData.FILL_HORIZONTAL);

        myBuilderTypeCombo.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                IBuildRunner buildRunner = (IBuildRunner) myBuilderTypeCombo.getData(myBuilderTypeCombo.getText());
                myAutoConfDesc.setBuildRunner(buildRunner);
                updateButtons();
            }
        });

        myUseDefaultBuildCommandButton = setupCheck(g1, Messages.BuilderSettingsTab_4, 3, GridData.BEGINNING);
        myUseDefaultBuildCommandButton.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                myAutoConfDesc.setUseDefaultBuildCommand(myUseDefaultBuildCommandButton.getSelection());
                updateButtons();
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent e) {
                // TODO Auto-generated method stub
                return;
            }
        });

        setupLabel(g1, Messages.BuilderSettingsTab_5, 1, GridData.BEGINNING);

        myBuildCmdText = setupText(g1, 1, GridData.FILL_HORIZONTAL);
        myBuildCmdText.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                String buildCommand = myBuildCmdText.getText().trim();
                myAutoConfDesc.setCustomBuildCommand(buildCommand);
            }
        });
        myBuildCommandButton = setupButton(g1, VARIABLESBUTTON_NAME, 1, GridData.END);
        myBuildCommandButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                String x = AbstractCPropertyTab.getVariableDialog(getShell(),
                        myAutoConfDesc.getCdtConfigurationDescription());
                if (x != null)
                    myBuildCmdText.insert(x);
            }
        });

        setupLabel(g1, Messages.BuilderSettingsTab_Configure_Build_Arguments_In_the_Behavior_tab, 2,
                GridData.BEGINNING);

        Group g2 = setupGroup(usercomp, Messages.BuilderSettingsTab_6, 2, GridData.FILL_HORIZONTAL);
        ((GridLayout) (g2.getLayout())).makeColumnsEqualWidth = true;

        myAutoGenMakefileButton = setupCheck(g2, Messages.BuilderSettingsTab_7, 1, GridData.BEGINNING);
        myAutoGenMakefileButton.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                myAutoConfDesc.setGenerateMakeFilesAUtomatically(myAutoGenMakefileButton.getSelection());
                updateButtons();
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent e) {
                // TODO Auto-generated method stub
                return;
            }
        });

        // Build location group
        Group group_dir = setupGroup(usercomp, Messages.BuilderSettingsTab_21, 2, GridData.FILL_HORIZONTAL);
        setupLabel(group_dir, Messages.BuilderSettingsTab_22, 1, GridData.BEGINNING);
        myBuildFolderText = setupText(group_dir, 1, GridData.FILL_HORIZONTAL);
        myBuildFolderText.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                myAutoConfDesc.setBuildFolderString(myBuildFolderText.getText());
            }
        });
        Composite c = new Composite(group_dir, SWT.NONE);
        setupControl(c, 2, GridData.FILL_HORIZONTAL);
        GridLayout f = new GridLayout(4, false);
        c.setLayout(f);
        Label dummy = new Label(c, 0);
        dummy.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        Button b_dirWsp = setupBottomButton(c, WORKSPACEBUTTON_NAME);
        b_dirWsp.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                String x = getWorkspaceDirDialog(getShell(), EMPTY_STR);
                if (x != null)
                    myBuildFolderText.setText(x);
            }
        });
        Button b_dirFile = setupBottomButton(c, FILESYSTEMBUTTON_NAME);
        b_dirFile.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                String x = getFileSystemDirDialog(getShell(), EMPTY_STR);
                if (x != null)
                    myBuildFolderText.setText(x);
            }
        });
        Button b_dirVars = setupBottomButton(c, VARIABLESBUTTON_NAME);
        b_dirVars.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                String x = AbstractCPropertyTab.getVariableDialog(getShell(),
                        myAutoConfDesc.getCdtConfigurationDescription());
                if (x != null)
                    myBuildFolderText.insert(x);
            }
        });
    }

    private void updateDisplayedData() {
        //as each autoConfDesc can contain a different set of builders
        //we need to replace all of them
        myBuilderTypeCombo.removeAll();
        for (IBuildRunner buildRunner : myAutoConfDesc.getCompatibleBuildRunners()) {
            myBuilderTypeCombo.add(buildRunner.getName());
            myBuilderTypeCombo.setData(buildRunner.getName(), buildRunner);
        }
        IBuildRunner buildRunner = myAutoConfDesc.getBuildRunner();
        myBuilderTypeCombo.setText(buildRunner.getName());
        myBuildCmdText.setText(myAutoConfDesc.getBuildCommand(true));
        myBuildFolderText.setText(myAutoConfDesc.getBuildFolderString());
        myAutoGenMakefileButton.setSelection(myAutoConfDesc.generateMakeFilesAUtomatically());
        myUseDefaultBuildCommandButton.setSelection(myAutoConfDesc.useDefaultBuildCommand());
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

        IBuildRunner buildRunner = myAutoConfDesc.getBuildRunner();
        myUseDefaultBuildCommandButton.setEnabled(buildRunner.supportsCustomCommand());
        boolean enableCustomBuildcmd = buildRunner.supportsCustomCommand() && !myAutoConfDesc.useDefaultBuildCommand();
        myBuildCmdText.setEnabled(enableCustomBuildcmd);
        myBuildCommandButton.setEnabled(enableCustomBuildcmd);
        myAutoGenMakefileButton.setEnabled(buildRunner.supportsMakeFiles());
    }

    private static Button setupBottomButton(Composite c, String name) {
        Button b = new Button(c, SWT.PUSH);
        b.setText(name);
        GridData fd = new GridData(GridData.CENTER);
        fd.minimumWidth = BUTTON_WIDTH;
        b.setLayoutData(fd);
        return b;
    }

    // This page can be displayed for project only
    @Override
    public boolean canBeVisible() {
        return page.isForProject() || page.isForPrefs();
    }

    @Override
    protected void performDefaults() {
        //		if (icfg instanceof IMultiConfiguration) {
        //			IConfiguration[] cfs = (IConfiguration[]) ((IMultiConfiguration) icfg).getItems();
        //			for (int i = 0; i < cfs.length; i++) {
        ////				IBuilder b = cfs[i].getEditableBuilder();
        ////				BuildBehaviourTab.copyBuilders(b.getSuperClass(), b);
        //			}
        //		} 
        ////		else
        ////			BuildBehaviourTab.copyBuilders(bldr.getSuperClass(), bldr);
        //		updateData(getResDesc());
    }

}
