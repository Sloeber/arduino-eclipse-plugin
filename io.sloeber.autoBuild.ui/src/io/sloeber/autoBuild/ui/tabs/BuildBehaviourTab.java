/*******************************************************************************
 * Copyright (c) 2007, 2012 Intel Corporation and others.
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
 *******************************************************************************/
package io.sloeber.autoBuild.ui.tabs;

import static io.sloeber.autoBuild.integration.AutoBuildConstants.*;

import java.text.MessageFormat;

import org.eclipse.cdt.core.settings.model.ICResourceDescription;
import org.eclipse.cdt.ui.newui.AbstractCPropertyTab;
import org.eclipse.swt.SWT;
import org.eclipse.swt.accessibility.AccessibleAdapter;
import org.eclipse.swt.accessibility.AccessibleEvent;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Text;
import io.sloeber.autoBuild.api.IBuildRunner;
import io.sloeber.autoBuild.ui.internal.Messages;

/**
 * @noextend This class is not intended to be subclassed by clients.
 * @noinstantiate This class is not intended to be instantiated by clients.
 */
public class BuildBehaviourTab extends AbstractAutoBuildPropertyTab {

    private class BuildTargetCombo {
        Button myCheckBoxButton;
        Text myEntryField;
        Button myVariablesButton;

        BuildTargetCombo(Composite g4, String groupLabel, SelectionAdapter checkListener, ModifyListener textListener) {
            Font font = g4.getFont();
            myCheckBoxButton = new Button(g4, SWT.CHECK);
            myCheckBoxButton.setText(groupLabel);
            myCheckBoxButton.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1));
            myCheckBoxButton.setFont(font);
            myCheckBoxButton.addSelectionListener(checkListener);

            myEntryField = new Text(g4, SWT.SINGLE | SWT.BORDER);
            myEntryField.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
            myEntryField.setFont(font);
            myEntryField.addModifyListener(textListener);

            GridData gd = new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1);
            gd.minimumWidth = BUTTON_WIDTH;
            myVariablesButton = new Button(g4, SWT.PUSH);
            myVariablesButton.setText(VARIABLESBUTTON_NAME);
            myVariablesButton.setLayoutData(gd);
            myVariablesButton.setFont(font);
            myVariablesButton.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent event) {
                    String x = AbstractCPropertyTab.getVariableDialog(getShell(),
                            myAutoConfDesc.getCdtConfigurationDescription());
                    if (x != null)
                        myEntryField.insert(x);
                }
            });
        }

        public void setEnabled(boolean check, boolean entry) {
            myCheckBoxButton.setEnabled(check);
            myEntryField.setEnabled(check && entry);
            myVariablesButton.setEnabled(check && entry);
        }
    }

    private static final int SPINNER_MAX_VALUE = 10000;
    private static final int SPINNER_MIN_VALUE = 2;

    private Button myUseStandardBuildArgumentsButton;
    private Button myUseCustomBuildArgumentsButton;
    private Text myBuildArgumentsText;
    private Button myStopOnErrorButton;
    private Button myUseParallelBuildButton;

    private Button myParalOpt_OptimalButton;
    private Button myParalOpt_SpecificButton;
    private Button myParalOpt_UnlimitedButton;
    private Spinner myParalOpt_NumberSpinner;

    private BuildTargetCombo myAutoBuildCombo;
    private BuildTargetCombo myIncreBuildCombo;
    private BuildTargetCombo myCleanBuildCombo;
    private Button myBuildArgumentsVarButton;

    @Override
    public void createControls(Composite parent) {
        super.createControls(parent);
        usercomp.setLayout(new GridLayout(1, false));
        Font font = usercomp.getFont();

        // Build setting group
        Group grp_buildSettings = setupGroup(usercomp, Messages.BuildBehaviourTab_Header, 2, GridData.FILL_HORIZONTAL);
        GridLayout gl = new GridLayout(2, true);
        gl.verticalSpacing = 0;
        gl.marginWidth = 0;
        grp_buildSettings.setLayout(gl);

        myUseStandardBuildArgumentsButton = new Button(grp_buildSettings, SWT.RADIO);
        myUseStandardBuildArgumentsButton.setText(Messages.BuildBehaviourTab_Use_standard_build_arguments);
        setupControl(myUseStandardBuildArgumentsButton, 3, GridData.BEGINNING);
        myUseStandardBuildArgumentsButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                myAutoConfDesc.setUseStandardBuildArguments(true);
                updateButtons();
            }
        });

        Composite c1 = new Composite(grp_buildSettings, SWT.NONE);
        setupControl(c1, 1, GridData.FILL_BOTH);
        GridData gd = (GridData) c1.getLayoutData();
        gd.verticalSpan = 2;
        gd.verticalIndent = 0;
        c1.setLayoutData(gd);
        gl = new GridLayout(1, false);
        c1.setLayout(gl);

        //        myStopOnErrorButton = setupCheck(c1, Messages.BuilderSettingsTab_10, 1, GridData.BEGINNING);
        //        ((GridData) (myStopOnErrorButton.getLayoutData())).horizontalIndent = 15;
        myStopOnErrorButton = new Button(c1, SWT.CHECK);
        myStopOnErrorButton.setText(Messages.BuilderSettingsTab_10);
        gd = new GridData(SWT.LEFT);
        gd.horizontalSpan = 1;
        gd.horizontalIndent = 15;
        myStopOnErrorButton.setLayoutData(gd);
        myStopOnErrorButton.setFont(font);
        myStopOnErrorButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                myAutoConfDesc.setStopOnFirstBuildError(myStopOnErrorButton.getSelection());
            }
        });

        Composite c2 = new Composite(grp_buildSettings, SWT.NONE);
        setupControl(c2, 1, GridData.FILL_BOTH);
        gl = new GridLayout(1, false);
        c2.setLayout(gl);

        myUseParallelBuildButton = new Button(c2, SWT.CHECK);
        myUseParallelBuildButton.setText(Messages.BuilderSettingsTab_EnableParallelBuild);
        setupControl(myUseParallelBuildButton, 1, GridData.BEGINNING);
        myUseParallelBuildButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                myAutoConfDesc.setIsParallelBuild(myUseParallelBuildButton.getSelection());
                updateButtons();
            }
        });

        Composite c3 = new Composite(grp_buildSettings, SWT.NONE);
        setupControl(c3, 1, GridData.FILL_BOTH);
        gl = new GridLayout(2, false);
        gl.verticalSpacing = 0;
        gl.marginWidth = 0;
        gl.marginHeight = 0;
        c3.setLayout(gl);

        Integer one = Integer.valueOf(1);
        myParalOpt_OptimalButton = new Button(c3, SWT.RADIO);
        myParalOpt_OptimalButton.setText(MessageFormat.format(Messages.BuilderSettingsTab_UseOptimalJobs, one));
        setupControl(myParalOpt_OptimalButton, 2, GridData.BEGINNING);
        ((GridData) (myParalOpt_OptimalButton.getLayoutData())).horizontalIndent = 15;
        myParalOpt_OptimalButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                if (myParalOpt_OptimalButton.getSelection()) {
                    myAutoConfDesc.setParallelizationNum(PARRALLEL_BUILD_OPTIMAL_JOBS);
                    updateButtons();
                }
            }
        });

        myParalOpt_SpecificButton = new Button(c3, SWT.RADIO);
        myParalOpt_SpecificButton.setText(Messages.BuilderSettingsTab_UseParallelJobs);
        setupControl(myParalOpt_SpecificButton, 1, GridData.BEGINNING);
        ((GridData) (myParalOpt_SpecificButton.getLayoutData())).horizontalIndent = 15;
        myParalOpt_SpecificButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                if (myParalOpt_SpecificButton.getSelection()) {
                    myAutoConfDesc.setParallelizationNum(myParalOpt_NumberSpinner.getSelection());
                    updateButtons();
                }
            }
        });

        myParalOpt_NumberSpinner = new Spinner(c3, SWT.BORDER);
        setupControl(myParalOpt_NumberSpinner, 1, GridData.BEGINNING);
        myParalOpt_NumberSpinner.setValues(SPINNER_MIN_VALUE, SPINNER_MIN_VALUE, SPINNER_MAX_VALUE, 0, 1, 10);
        myParalOpt_NumberSpinner.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                myAutoConfDesc.setParallelizationNum(myParalOpt_NumberSpinner.getSelection());
                updateButtons();
            }
        });
        myParalOpt_NumberSpinner.getAccessible().addAccessibleListener(new AccessibleAdapter() {
            @Override
            public void getName(AccessibleEvent e) {
                e.result = Messages.BuilderSettingsTab_UseParallelJobs;
            }
        });
        myParalOpt_NumberSpinner.setToolTipText(Messages.BuilderSettingsTab_UseParallelJobs);

        myParalOpt_UnlimitedButton = new Button(c3, SWT.RADIO);
        myParalOpt_UnlimitedButton.setText(Messages.BuilderSettingsTab_UseUnlimitedJobs);
        setupControl(myParalOpt_UnlimitedButton, 2, GridData.BEGINNING);
        ((GridData) (myParalOpt_UnlimitedButton.getLayoutData())).horizontalIndent = 15;
        myParalOpt_UnlimitedButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                if (myParalOpt_UnlimitedButton.getSelection()) {
                    myAutoConfDesc.setParallelizationNum(PARRALLEL_BUILD_UNLIMITED_JOBS);
                    updateButtons();
                }
            }
        });

        myUseCustomBuildArgumentsButton = new Button(grp_buildSettings, SWT.RADIO);
        myUseCustomBuildArgumentsButton.setText(Messages.BuildBehaviourTab_Use_custom_build_arguments);
        setupControl(myUseCustomBuildArgumentsButton, 3, GridData.BEGINNING);
        myUseCustomBuildArgumentsButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                myAutoConfDesc.setUseStandardBuildArguments(false);
                updateButtons();
            }
        });

        Composite c0 = setupComposite(grp_buildSettings, 3, GridData.FILL_BOTH);
        setupControl(c0, 2, GridData.FILL_BOTH);
        setupLabel(c0, Messages.BuildBehaviourTab_Build_arguments, 1, GridData.BEGINNING);
        //myBuildArgumentsText = setupBlock(c0, myUseCustomBuildArgumentsButton);
        myBuildArgumentsText = setupText(c0, 1, GridData.FILL_HORIZONTAL);
        myBuildArgumentsVarButton = setupButton(c0, VARIABLESBUTTON_NAME, 1, GridData.END);
        myBuildArgumentsVarButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                String x = AbstractCPropertyTab.getVariableDialog(getShell(),
                        myAutoConfDesc.getCdtConfigurationDescription());
                if (x != null)
                    myBuildArgumentsText.insert(x);
            }
        });

        myBuildArgumentsText.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                String arguments = myBuildArgumentsText.getText().trim();
                myAutoConfDesc.setCustomBuildArguments(arguments);
            }
        });

        Group g4 = setupGroup(usercomp, Messages.BuilderSettingsTab_14, 3, GridData.FILL_HORIZONTAL);

        Label l = new Label(g4, SWT.NONE);
        l.setText(Messages.BuilderSettingsTab_workbench_build_type);
        l.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false, 1, 1));
        l.setFont(font);

        l = new Label(g4, SWT.NONE);
        l.setText(Messages.BuilderSettingsTab_make_build_target);
        l.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, true, false, 2, 1));
        l.setFont(font);

        myAutoBuildCombo = new BuildTargetCombo(g4, Messages.BuilderSettingsTab_auto_build, new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                myAutoConfDesc.setAutoBuildEnabled(myAutoBuildCombo.myCheckBoxButton.getSelection());
                updateButtons();
            }
        }, new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                myAutoConfDesc.setAutoMakeTarget(myAutoBuildCombo.myEntryField.getText());
            }
        });

        myIncreBuildCombo = new BuildTargetCombo(g4, Messages.BuilderSettingsTab_19, new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                myAutoConfDesc.setIncrementalBuildEnable(myIncreBuildCombo.myCheckBoxButton.getSelection());
                updateButtons();
            }
        }, new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                myAutoConfDesc.setIncrementalMakeTarget(myIncreBuildCombo.myEntryField.getText());
            }
        });

        myCleanBuildCombo = new BuildTargetCombo(g4, Messages.BuilderSettingsTab_20, new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                myAutoConfDesc.setCleanBuildEnable(myCleanBuildCombo.myCheckBoxButton.getSelection());
                updateButtons();
            }
        }, new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                myAutoConfDesc.setCleanMakeTarget(myCleanBuildCombo.myEntryField.getText());
            }
        });

    }

    private void updateDisplayedData() {
        int parallelnum = myAutoConfDesc.getParallelizationNum();
        int optimalNum = myAutoConfDesc.getOptimalParallelJobNum();
        switch (parallelnum) {
        case PARRALLEL_BUILD_UNLIMITED_JOBS:
            myParalOpt_NumberSpinner.setSelection(optimalNum);
            myParalOpt_UnlimitedButton.setSelection(true);
            break;
        case PARRALLEL_BUILD_OPTIMAL_JOBS:
            myParalOpt_NumberSpinner.setSelection(optimalNum);
            myParalOpt_OptimalButton.setSelection(true);
            break;
        default:
            myParalOpt_NumberSpinner.setSelection(parallelnum);
            myParalOpt_SpecificButton.setSelection(true);
        }

        myUseParallelBuildButton.setSelection(myAutoConfDesc.isParallelBuild());
        setTriSelection(myUseStandardBuildArgumentsButton, myAutoConfDesc.useStandardBuildArguments());
        setTriSelection(myUseCustomBuildArgumentsButton, !myAutoConfDesc.useStandardBuildArguments());
        setTriSelection(myStopOnErrorButton, myAutoConfDesc.stopOnFirstBuildError());
        myBuildArgumentsText.setText(myAutoConfDesc.getCustomBuildArguments());

        myAutoBuildCombo.myCheckBoxButton.setSelection(myAutoConfDesc.isAutoBuildEnabled());
        myIncreBuildCombo.myCheckBoxButton.setSelection(myAutoConfDesc.isIncrementalBuildEnabled());
        myCleanBuildCombo.myCheckBoxButton.setSelection(myAutoConfDesc.isCleanBuildEnabled());

        myAutoBuildCombo.myEntryField.setText(myAutoConfDesc.getAutoMakeTarget());
        myIncreBuildCombo.myEntryField.setText(myAutoConfDesc.getIncrementalMakeTarget());
        myCleanBuildCombo.myEntryField.setText(myAutoConfDesc.getCleanMakeTarget());

        //On this page we can enable the fields based on buildrunner in this call
        //as this page does not allow changing the build runner
        IBuildRunner runner = myAutoConfDesc.getBuildRunner();
        myUseStandardBuildArgumentsButton.setEnabled(runner.supportsCustomCommand());
        myUseCustomBuildArgumentsButton.setEnabled(runner.supportsCustomCommand());
        myStopOnErrorButton.setEnabled(runner.supportsStopOnError());

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
        IBuildRunner runner = myAutoConfDesc.getBuildRunner();
        boolean isMake = runner.supportsMakeFiles();
        if (myAutoConfDesc.useStandardBuildArguments()) {
            myStopOnErrorButton.setEnabled(runner.supportsStopOnError());
            myBuildArgumentsText.setEnabled(false);
            myBuildArgumentsVarButton.setEnabled(false);
            updateParallelBlock();
        } else {
            myStopOnErrorButton.setEnabled(false);
            myBuildArgumentsText.setEnabled(runner.supportsCustomCommand());
            myBuildArgumentsVarButton.setEnabled(runner.supportsCustomCommand());

            myUseParallelBuildButton.setEnabled(false);
            myParalOpt_OptimalButton.setEnabled(false);
            myParalOpt_SpecificButton.setEnabled(false);
            myParalOpt_UnlimitedButton.setEnabled(false);
        }
        myAutoBuildCombo.setEnabled(runner.supportsAutoBuild(), myAutoConfDesc.isAutoBuildEnabled() && isMake);
        myIncreBuildCombo.setEnabled(runner.supportsIncrementalBuild(),
                myAutoConfDesc.isIncrementalBuildEnabled() && isMake);
        myCleanBuildCombo.setEnabled(runner.supportsCleanBuild(), myAutoConfDesc.isCleanBuildEnabled() && isMake);

    }

    private void updateParallelBlock() {
        IBuildRunner buildRunner = myAutoConfDesc.getBuildRunner();

        boolean isParallelSupported = buildRunner.supportsParallelBuild();
        boolean isParallelOn = myAutoConfDesc.isParallelBuild();

        myUseParallelBuildButton.setEnabled(isParallelSupported);

        if (!isParallelSupported) {
            return;
        }

        myParalOpt_OptimalButton.setEnabled(isParallelOn);
        myParalOpt_SpecificButton.setEnabled(isParallelOn);
        myParalOpt_UnlimitedButton.setEnabled(isParallelOn);

        myParalOpt_NumberSpinner.setEnabled(myAutoConfDesc.getParallelizationNum() >= 0);
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
        //				IAutoBuildConfigurationDescription b = cfs[i].getEditableBuilder();
        //				copyBuilders(b.getSuperClass(), b);
        //			}
        //		} else
        //			copyBuilders(bldr.getSuperClass(), bldr);
        //		updateData(getResDesc());
    }

}
