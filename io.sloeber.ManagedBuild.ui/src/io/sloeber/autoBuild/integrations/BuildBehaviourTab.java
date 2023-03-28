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
package io.sloeber.autoBuild.integrations;

import java.text.MessageFormat;

import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.cdt.core.settings.model.ICMultiConfigDescription;
import org.eclipse.cdt.core.settings.model.ICMultiItemsHolder;
import org.eclipse.cdt.core.settings.model.ICResourceDescription;
import org.eclipse.cdt.ui.newui.AbstractCPropertyTab;
import org.eclipse.cdt.ui.newui.ICPropertyProvider;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.swt.SWT;
import org.eclipse.swt.accessibility.AccessibleAdapter;
import org.eclipse.swt.accessibility.AccessibleEvent;
import org.eclipse.swt.accessibility.AccessibleListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Widget;

import io.sloeber.autoBuild.api.AutoBuildProject;
import io.sloeber.autoBuild.api.IAutoBuildConfigurationDescription;
import io.sloeber.autoBuild.ui.internal.Messages;

/**
 * @noextend This class is not intended to be subclassed by clients.
 * @noinstantiate This class is not intended to be instantiated by clients.
 */
public class BuildBehaviourTab extends AbstractAutoBuildPropertyTab {

    public final static String BUILD_TARGET_INCREMENTAL = ARGS_PREFIX + ".build.target.inc"; //$NON-NLS-1$
    public final static String BUILD_TARGET_AUTO = ARGS_PREFIX + ".build.target.auto"; //$NON-NLS-1$
    public final static String BUILD_TARGET_CLEAN = ARGS_PREFIX + ".build.target.clean"; //$NON-NLS-1$
    private static final int SPINNER_MAX_VALUE = 10000;
    private static final int SPINNER_MIN_VALUE = 2;

    private static final int TRI_STATES_SIZE = 7;
    // Widgets
    private Button r_useStandardBuildArguments;
    private Button r_useCustomBuildArguments;
    private Text t_buildArguments;

    // 3
    private Button b_stopOnError; // 3
    private Button b_parallel; // 3

    private Button b_parallelOptimal;
    private Button b_parallelSpecific;
    private Button b_parallelUnlimited;
    private Spinner s_parallelNumber;

    private Group grp_buildSettings;

    private Label title2;
    private Button b_autoBuild; // 3
    private Text t_autoBuild;
    private Button b_cmdBuild; // 3
    private Text t_cmdBuild;
    private Button b_cmdClean; // 3
    private Text t_cmdClean;

    private boolean canModify = true;

    protected final int cpuNumber = Runtime.getRuntime().availableProcessors();

    @Override
    public void createControls(Composite parent) {
        super.createControls(parent);
        usercomp.setLayout(new GridLayout(1, false));

        // Build setting group
        grp_buildSettings = setupGroup(usercomp, Messages.BuilderSettingsTab_9, 2, GridData.FILL_HORIZONTAL);
        GridLayout gl = new GridLayout(2, true);
        gl.verticalSpacing = 0;
        gl.marginWidth = 0;
        grp_buildSettings.setLayout(gl);

        r_useStandardBuildArguments = setupRadio(grp_buildSettings,
                Messages.BuildBehaviourTab_Use_standard_build_arguments, 3, GridData.BEGINNING);

        Composite c1 = new Composite(grp_buildSettings, SWT.NONE);
        setupControl(c1, 1, GridData.FILL_BOTH);
        GridData gd = (GridData) c1.getLayoutData();
        gd.verticalSpan = 2;
        gd.verticalIndent = 0;
        c1.setLayoutData(gd);
        gl = new GridLayout(1, false);
        c1.setLayout(gl);

        b_stopOnError = setupCheck(c1, Messages.BuilderSettingsTab_10, 1, GridData.BEGINNING);
        ((GridData) (b_stopOnError.getLayoutData())).horizontalIndent = 15;

        Composite c2 = new Composite(grp_buildSettings, SWT.NONE);
        setupControl(c2, 1, GridData.FILL_BOTH);
        gl = new GridLayout(1, false);
        c2.setLayout(gl);

        b_parallel = setupCheck(c2, Messages.BuilderSettingsTab_EnableParallelBuild, 1, GridData.BEGINNING);

        Composite c3 = new Composite(grp_buildSettings, SWT.NONE);
        setupControl(c3, 1, GridData.FILL_BOTH);
        gl = new GridLayout(2, false);
        gl.verticalSpacing = 0;
        gl.marginWidth = 0;
        gl.marginHeight = 0;
        c3.setLayout(gl);

        b_parallelOptimal = new Button(c3, SWT.RADIO);
        b_parallelOptimal.setText(MessageFormat.format(Messages.BuilderSettingsTab_UseOptimalJobs, 1));
        setupControl(b_parallelOptimal, 2, GridData.BEGINNING);
        ((GridData) (b_parallelOptimal.getLayoutData())).horizontalIndent = 15;
        b_parallelOptimal.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                if (b_parallelOptimal.getSelection()) {
                    setParallelDef(true);
                    setParallelNumber(IAutoBuildConfigurationDescription.PARRALLEL_BUILD_OPTIMAL_JOBS);
                    updateButtons();
                }
            }
        });

        b_parallelSpecific = new Button(c3, SWT.RADIO);
        b_parallelSpecific.setText(Messages.BuilderSettingsTab_UseParallelJobs);
        setupControl(b_parallelSpecific, 1, GridData.BEGINNING);
        ((GridData) (b_parallelSpecific.getLayoutData())).horizontalIndent = 15;
        b_parallelSpecific.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                if (b_parallelSpecific.getSelection()) {
                    setParallelDef(true);
                    setParallelNumber(s_parallelNumber.getSelection());
                    updateButtons();
                }
            }
        });

        s_parallelNumber = new Spinner(c3, SWT.BORDER);
        setupControl(s_parallelNumber, 1, GridData.BEGINNING);
        s_parallelNumber.setValues(cpuNumber, SPINNER_MIN_VALUE, SPINNER_MAX_VALUE, 0, 1, 10);
        s_parallelNumber.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                setParallelDef(true);
                setParallelNumber(s_parallelNumber.getSelection());
                updateButtons();
            }
        });
        s_parallelNumber.getAccessible().addAccessibleListener(new AccessibleAdapter() {
            @Override
            public void getName(AccessibleEvent e) {
                e.result = Messages.BuilderSettingsTab_UseParallelJobs;
            }
        });
        s_parallelNumber.setToolTipText(Messages.BuilderSettingsTab_UseParallelJobs);

        b_parallelUnlimited = new Button(c3, SWT.RADIO);
        b_parallelUnlimited.setText(Messages.BuilderSettingsTab_UseUnlimitedJobs);
        setupControl(b_parallelUnlimited, 2, GridData.BEGINNING);
        ((GridData) (b_parallelUnlimited.getLayoutData())).horizontalIndent = 15;
        b_parallelUnlimited.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                if (b_parallelUnlimited.getSelection()) {
                    setParallelDef(true);
                    setParallelNumber(Integer.MAX_VALUE);
                    updateButtons();
                }
            }
        });

        r_useCustomBuildArguments = setupRadio(grp_buildSettings, Messages.BuildBehaviourTab_Use_custom_build_arguments,
                3, GridData.BEGINNING);
        Composite c0 = setupComposite(grp_buildSettings, 3, GridData.FILL_BOTH);
        setupControl(c0, 2, GridData.FILL_BOTH);
        setupLabel(c0, Messages.BuildBehaviourTab_Build_arguments, 1, GridData.BEGINNING);
        t_buildArguments = setupBlock(c0, r_useCustomBuildArguments);
        t_buildArguments.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                if (!canModify)
                    return;
                setArguments(t_buildArguments.getText().trim());
            }
        });

        // Workbench behaviour group
        AccessibleListener makeTargetLabelAccessibleListener = new AccessibleAdapter() {
            @Override
            public void getName(AccessibleEvent e) {
                e.result = Messages.BuilderSettingsTab_16;
            }
        };
        Group g4 = setupGroup(usercomp, Messages.BuilderSettingsTab_14, 3, GridData.FILL_HORIZONTAL);
        setupLabel(g4, Messages.BuilderSettingsTab_15, 1, GridData.BEGINNING);
        title2 = setupLabel(g4, Messages.BuilderSettingsTab_16, 2, GridData.BEGINNING);
        b_autoBuild = setupCheck(g4, Messages.BuilderSettingsTab_17, 1, GridData.BEGINNING);
        t_autoBuild = setupBlock(g4, b_autoBuild);
        t_autoBuild.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                if (canModify)
                    setBuildAttribute(BUILD_TARGET_AUTO, t_autoBuild.getText());
            }
        });
        t_autoBuild.getAccessible().addAccessibleListener(makeTargetLabelAccessibleListener);
        setupLabel(g4, Messages.BuilderSettingsTab_18, 3, GridData.BEGINNING);
        b_cmdBuild = setupCheck(g4, Messages.BuilderSettingsTab_19, 1, GridData.BEGINNING);
        t_cmdBuild = setupBlock(g4, b_cmdBuild);
        t_cmdBuild.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                if (canModify)
                    setBuildAttribute(BUILD_TARGET_INCREMENTAL, t_cmdBuild.getText());
            }
        });
        t_cmdBuild.getAccessible().addAccessibleListener(makeTargetLabelAccessibleListener);
        b_cmdClean = setupCheck(g4, Messages.BuilderSettingsTab_20, 1, GridData.BEGINNING);
        t_cmdClean = setupBlock(g4, b_cmdClean);
        t_cmdClean.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                if (canModify)
                    setBuildAttribute(BUILD_TARGET_CLEAN, t_cmdClean.getText());
            }
        });
        t_cmdClean.getAccessible().addAccessibleListener(makeTargetLabelAccessibleListener);
    }

    /**
     * Calculate enablements when multiple configurations selected on property page.
     *
     * @return: Mode 0: 0: bld.isManagedBuildOn() 1: N/A 2:
     *          bld.canKeepEnvironmentVariablesInBuildfile() 3:
     *          bld.keepEnvironmentVariablesInBuildfile() 4:
     *          bld.isDefaultBuildCmdOnly() 5: bld.isDefaultBuildArgsOnly() 6:
     *          !bld.isDefaultBuildArgsOnly() Mode 1: 0: stopOnFirstBuildError 1:
     *          supportsStopOnError(true) 2: bld.supportsStopOnError(false) 3: N/A
     *          4: N/A 5: bld.isDefaultBuildArgsOnly() 6:
     *          !bld.isDefaultBuildArgsOnly() Mode 2: 0: b.isAutoBuildEnable() 1:
     *          b.isIncrementalBuildEnabled() 2: b.isCleanBuildEnabled() 3: N/A 4:
     *          N/A 5: N/A 6: N/A
     */
    public int[] calc3states(ICPropertyProvider p, int mode) {
        if (p.isMultiCfg()) {
            boolean m0 = (mode == 0);
            boolean m1 = (mode == 1);

            //IAutoBuildConfigurationDescription bldr0 = AutoBuildProject.getAutoBuildConfig(p.getCfgsEditable()[0]);
            int[] res = new int[TRI_STATES_SIZE];
            boolean[] b = new boolean[TRI_STATES_SIZE];
            b[0] = m0 ? myAutoConfDesc.isManagedBuildOn()
                    : (m1 ? myAutoConfDesc.stopOnFirstBuildError() : myAutoConfDesc.isAutoBuildEnable());
            b[1] = m0 ? true
                    : (m1 ? myAutoConfDesc.supportsStopOnError(true) : myAutoConfDesc.isIncrementalBuildEnabled());
            b[2] = m0 ? myAutoConfDesc.canKeepEnvironmentVariablesInBuildfile()
                    : (m1 ? myAutoConfDesc.supportsStopOnError(false) : myAutoConfDesc.isCleanBuildEnabled());
            b[3] = m0 ? myAutoConfDesc.keepEnvironmentVariablesInBuildfile() : false;
            b[4] = m0 ? myAutoConfDesc.useDefaultBuildCommand() : false;
            b[5] = (m0 || m1) ? myAutoConfDesc.useStandardBuildArguments() : false;
            b[6] = (m0 || m1) ? !myAutoConfDesc.useStandardBuildArguments() : false;
            for (ICConfigurationDescription i : p.getCfgsEditable()) {
                //TOFIX JABA add support for multiple config
                IAutoBuildConfigurationDescription bldr = AutoBuildProject.getAutoBuildConfig(i);
                if (b[0] != (m0 ? bldr.isManagedBuildOn()
                        : (m1 ? bldr.stopOnFirstBuildError() : bldr.isAutoBuildEnable())))
                    res[0] = TRI_UNKNOWN;
                if (b[1] != (m0 ? true : (m1 ? bldr.supportsStopOnError(true) : bldr.isIncrementalBuildEnabled())))
                    res[1] = TRI_UNKNOWN;
                if (b[2] != (m0 ? bldr.canKeepEnvironmentVariablesInBuildfile()
                        : (m1 ? bldr.supportsStopOnError(false) : bldr.isCleanBuildEnabled())))
                    res[2] = TRI_UNKNOWN;
                if (b[3] != (m0 ? bldr.keepEnvironmentVariablesInBuildfile() : false)) {
                    res[3] = TRI_UNKNOWN;
                }
                if (b[4] != (m0 ? bldr.useDefaultBuildCommand() : false)) {
                    res[4] = TRI_UNKNOWN;
                }
                if (b[5] != ((m0 || m1) ? bldr.useStandardBuildArguments() : false)) {
                    res[5] = TRI_UNKNOWN;
                }
                if (b[6] != ((m0 || m1) ? !bldr.useStandardBuildArguments() : false)) {
                    res[6] = TRI_UNKNOWN;
                }
            }
            for (int i = 0; i < TRI_STATES_SIZE; i++) {
                if (res[i] != TRI_UNKNOWN)
                    res[i] = b[i] ? TRI_YES : TRI_NO;
            }
            return res;
        }

        return null;
    }

    /**
     * sets widgets states
     */
    @Override
    protected void updateButtons() {

        canModify = false;
        int[] extStates = calc3states(page, 1);
        boolean external = !myAutoConfDesc.isInternalBuilderEnabled();

        // use standard build args
        if (extStates == null) { // no extended states available
            setTriSelection(r_useStandardBuildArguments, myAutoConfDesc.useStandardBuildArguments());
            setTriSelection(r_useCustomBuildArguments, !myAutoConfDesc.useStandardBuildArguments());
        } else {
            int standardTri = extStates[5];
            int customTri = extStates[6];
            if (standardTri == TRI_UNKNOWN || customTri == TRI_UNKNOWN) {
                setTriSelection(r_useStandardBuildArguments, TRI_UNKNOWN);
                setTriSelection(r_useCustomBuildArguments, TRI_UNKNOWN);
            } else {
                setTriSelection(r_useStandardBuildArguments, standardTri);
                setTriSelection(r_useCustomBuildArguments, customTri);
            }
        }
        // t_buildArguments.setText(nonNull(icfg.getBuildArguments()));
        r_useStandardBuildArguments.setEnabled(external);
        r_useCustomBuildArguments.setEnabled(external);
        if (external) {
            checkPressed(r_useCustomBuildArguments, false); // do not update
        }

        // Stop on error
        boolean defaultBuildArguments = myAutoConfDesc.useStandardBuildArguments();
        if (defaultBuildArguments) {
            if (extStates != null) {
                setTriSelection(b_stopOnError, extStates[0]);
                b_stopOnError.setEnabled(extStates[1] == TRI_YES && extStates[2] == TRI_YES);
            } else {
                setTriSelection(b_stopOnError, myAutoConfDesc.stopOnFirstBuildError());
                b_stopOnError.setEnabled(myAutoConfDesc.supportsStopOnError(false));
            }
        } else {
            b_stopOnError.setEnabled(defaultBuildArguments);
        }

        updateParallelBlock(defaultBuildArguments);

        // Build commands
        extStates = calc3states(page, 2);
        if (extStates != null) {
            // multiple configurations selected
            setTriSelection(b_autoBuild, extStates[0]);
            setTriSelection(b_cmdBuild, extStates[1]);
            setTriSelection(b_cmdClean, extStates[2]);
        } else {
            setTriSelection(b_autoBuild, myAutoConfDesc.isAutoBuildEnable());
            setTriSelection(b_cmdBuild, myAutoConfDesc.isIncrementalBuildEnabled());
            setTriSelection(b_cmdClean, myAutoConfDesc.isCleanBuildEnabled());
        }

        //		if (page.isMultiCfg()) {
        //			MultiConfiguration mc = (MultiConfiguration) icfg;
        //			t_autoBuild.setText(mc.getBuildAttribute(IBuilder.BUILD_TARGET_AUTO, EMPTY_STR));
        //			t_cmdBuild.setText(mc.getBuildAttribute(IBuilder.BUILD_TARGET_INCREMENTAL, EMPTY_STR));
        //			t_cmdClean.setText(mc.getBuildAttribute(IBuilder.BUILD_TARGET_CLEAN, EMPTY_STR));
        //		} else {
        //			t_autoBuild.setText(bldr.getBuildAttribute(IBuilder.BUILD_TARGET_AUTO, EMPTY_STR));
        //			t_cmdBuild.setText(bldr.getBuildAttribute(IBuilder.BUILD_TARGET_INCREMENTAL, EMPTY_STR));
        //			t_cmdClean.setText(bldr.getBuildAttribute(IBuilder.BUILD_TARGET_CLEAN, EMPTY_STR));
        //		}

        title2.setVisible(external);
        t_autoBuild.setVisible(external);
        ((Control) t_autoBuild.getData()).setVisible(external);
        t_cmdBuild.setVisible(external);
        ((Control) t_cmdBuild.getData()).setVisible(external);
        t_cmdClean.setVisible(external);
        ((Control) t_cmdClean.getData()).setVisible(external);

        if (external) {
            checkPressed(b_autoBuild, false);
            checkPressed(b_cmdBuild, false);
            checkPressed(b_cmdClean, false);
        }
        canModify = true;
    }

    private void updateParallelBlock(boolean defaultBuildArguments) {
        // note: for multi-config selection bldr is from Active cfg

        boolean isParallelSupported = myAutoConfDesc.supportsParallelBuild();
        boolean isParallelOn = myAutoConfDesc.isParallelBuild();
        int triSelection = isParallelOn ? TRI_YES : TRI_NO;

        int parallelizationNumInternal = myAutoConfDesc.getParallelizationNum();
        int optimalParallelNumber = myAutoConfDesc.getOptimalParallelJobNum();
        int parallelNumber = myAutoConfDesc.getParallelizationNum();

        boolean isAnyParallelOn = isParallelOn;
        boolean isAnyParallelSupported = isParallelSupported;
        boolean isParallelDiffers = false;
        for (ICConfigurationDescription cfg : page.getCfgsEditable()) {
            //TOFIX JABA add support multiple configs
            IAutoBuildConfigurationDescription builder = AutoBuildProject.getAutoBuildConfig(cfg);
            isParallelDiffers = isParallelDiffers || builder.isParallelBuild() != isParallelOn
                    || builder.getParallelizationNum() != parallelizationNumInternal;

            isAnyParallelOn = isAnyParallelOn || builder.isParallelBuild();
            isAnyParallelSupported = isAnyParallelSupported || builder.supportsParallelBuild();
        }

        // reset initial display to "optimal" to enhance user experience:
        if ((!isParallelSupported && isAnyParallelSupported) // parallel is supported by other than Active cfg
                || (!isParallelOn && isAnyParallelOn) // prevent showing the 1 job as parallel in the spinner
        ) {
            isParallelSupported = true;
            parallelizationNumInternal = -optimalParallelNumber;
            parallelNumber = optimalParallelNumber;
        }
        if (isParallelSupported && isParallelDiffers) {
            triSelection = TRI_UNKNOWN;
        }

        b_parallel.setVisible(isParallelSupported);
        b_parallelOptimal.setVisible(isParallelSupported);
        b_parallelSpecific.setVisible(isParallelSupported);
        b_parallelUnlimited.setVisible(isParallelSupported);
        s_parallelNumber.setVisible(isParallelSupported);

        if (isParallelSupported) {
            if (defaultBuildArguments) {
                b_parallel.setEnabled(true);
                s_parallelNumber.setEnabled(true);

                setTriSelection(b_parallel, triSelection);
                boolean isParallelSelected = b_parallel.getSelection();

                b_parallelOptimal.setText(
                        MessageFormat.format(Messages.BuilderSettingsTab_UseOptimalJobs, optimalParallelNumber));
                b_parallelOptimal.setEnabled(isParallelSelected);
                b_parallelSpecific.setEnabled(isParallelSelected);
                b_parallelUnlimited.setEnabled(isParallelSelected);

                if (isParallelSelected) {
                    boolean isOptimal = parallelizationNumInternal <= 0;
                    boolean isUnlimited = parallelizationNumInternal == IAutoBuildConfigurationDescription.PARRALLEL_BUILD_UNLIMITED_JOBS;

                    b_parallelOptimal.setSelection(isOptimal);
                    b_parallelSpecific.setSelection(!isOptimal && !isUnlimited);
                    b_parallelUnlimited.setSelection(isUnlimited);
                    s_parallelNumber.setEnabled(b_parallelSpecific.getEnabled() && b_parallelSpecific.getSelection());
                    s_parallelNumber
                            .setSelection(s_parallelNumber.isEnabled() ? parallelNumber : optimalParallelNumber);
                } else {
                    b_parallelOptimal.setSelection(true);
                    b_parallelSpecific.setSelection(false);
                    b_parallelUnlimited.setSelection(false);
                    s_parallelNumber.setEnabled(false);
                    s_parallelNumber.setSelection(optimalParallelNumber);
                }
            } else {
                b_parallel.setEnabled(false);
                b_parallelOptimal.setEnabled(false);
                b_parallelSpecific.setEnabled(false);
                b_parallelUnlimited.setEnabled(false);
                s_parallelNumber.setEnabled(false);
            }
        }
    }

    /**
     * Sets up text + corresponding button Checkbox can be implemented either by
     * Button or by TriButton
     */
    private Text setupBlock(Composite c, Control check) {
        Text t = setupText(c, 1, GridData.FILL_HORIZONTAL);
        Button b = setupButton(c, VARIABLESBUTTON_NAME, 1, GridData.END);
        b.setData(t); // to get know which text is affected
        t.setData(b); // to get know which button to enable/disable
        b.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                buttonVarPressed(event);
            }
        });
        if (check != null)
            check.setData(t);
        return t;
    }

    /*
     * Unified handler for "Variables" buttons
     */
    private void buttonVarPressed(SelectionEvent e) {
        Widget b = e.widget;
        if (b == null || b.getData() == null)
            return;
        if (b.getData() instanceof Text) {
            String x = AbstractCPropertyTab.getVariableDialog(usercomp.getShell(), getResDesc().getConfiguration());
            if (x != null)
                ((Text) b.getData()).insert(x);
        }
    }

    @Override
    public void checkPressed(SelectionEvent e) {
        checkPressed((Control) e.widget, true);
        updateButtons();
    }

    private void checkPressed(Control b, boolean needsUpdate) {
        if (b == null)
            return;

        boolean val = false;
        if (b instanceof Button)
            val = ((Button) b).getSelection();

        if (b.getData() instanceof Text) {
            Text t = (Text) b.getData();
            t.setEnabled(val);
            if (t.getData() != null && t.getData() instanceof Control) {
                Control c = (Control) t.getData();
                c.setEnabled(val);
            }
        }
        if (needsUpdate) {
            //TOFIX JABA add support for multiple configs
            //            for (ICConfigurationDescription i : page.getCfgsEditable()) {
            //              IAutoBuildConfigurationDescription bld = AutoBuildProject.getAutoBuildConfig(i);
            if (b == r_useStandardBuildArguments) {
                myAutoConfDesc.setUseStandardBuildArguments(val);
            } else if (b == r_useCustomBuildArguments) {
                myAutoConfDesc.setUseStandardBuildArguments(!val);
            } else if (b == b_autoBuild) {
                myAutoConfDesc.setAutoBuildEnable(val);
            } else if (b == b_cmdBuild) {
                myAutoConfDesc.setIncrementalBuildEnable(val);
            } else if (b == b_cmdClean) {
                myAutoConfDesc.setCleanBuildEnable(val);
            } else if (b == b_stopOnError) {
                myAutoConfDesc.setStopOnFirstBuildError(val);
            } else if (b == b_parallel) {
                myAutoConfDesc.setIsParallelBuild(val);
            }
            //        }

        }
    }

    @Override
    public void performApply(ICResourceDescription src, ICResourceDescription dst) {
        apply(src, dst, page.isMultiCfg());
    }

    static void apply(ICResourceDescription src, ICResourceDescription dst, boolean multi) {
        if (multi) {
            ICMultiConfigDescription mcSrc = (ICMultiConfigDescription) src.getConfiguration();
            ICMultiConfigDescription mcDst = (ICMultiConfigDescription) dst.getConfiguration();
            ICConfigurationDescription[] cdsSrc = (ICConfigurationDescription[]) mcSrc.getItems();
            ICConfigurationDescription[] cdsDst = (ICConfigurationDescription[]) mcDst.getItems();
            for (int i = 0; i < cdsSrc.length; i++)
                applyToCfg(cdsSrc[i], cdsDst[i]);
        } else
            applyToCfg(src.getConfiguration(), dst.getConfiguration());
    }

    private static void applyToCfg(ICConfigurationDescription src, ICConfigurationDescription dst) {
        IAutoBuildConfigurationDescription srcCfg = AutoBuildProject.getAutoBuildConfig(src);
        IAutoBuildConfigurationDescription dstCfg = AutoBuildProject.getAutoBuildConfig(dst);
        dstCfg.enableInternalBuilder(srcCfg.isInternalBuilderEnabled());
        dstCfg.setUseStandardBuildArguments(srcCfg.useStandardBuildArguments());
        dstCfg.setUseCustomBuildArguments(srcCfg.useCustomBuildArguments());
        dstCfg.setStopOnFirstBuildError(srcCfg.stopOnFirstBuildError());
        dstCfg.setIsParallelBuild(srcCfg.isParallelBuild());
        dstCfg.setParallelizationNum(srcCfg.getParallelizationNum());
        dstCfg.setBuildFolder(srcCfg.getBuildFolder());

        dstCfg.setAutoBuildEnable((srcCfg.isAutoBuildEnable()));
        dstCfg.setCleanBuildEnable(srcCfg.isCleanBuildEnabled());
        dstCfg.setIncrementalBuildEnable(srcCfg.isIncrementalBuildEnabled());
    }

    // This page can be displayed for project only
    @Override
    public boolean canBeVisible() {
        return page.isForProject() || page.isForPrefs();
    }

    @Override
    public void setVisible(boolean b) {
        super.setVisible(b);
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

    private void setArguments(String makeArgs) {
        for (ICConfigurationDescription cfg : page.getCfgsEditable()) {
            //IAutoBuildConfigurationDescription b = AutoBuildProject.getAutoBuildConfig(cfg);
            //TOFIX JABA add support for multiple config select
            myAutoConfDesc.setMakeArguments(makeArgs);
        }
    }

    private void setParallelDef(boolean def) {
        for (ICConfigurationDescription cfg : page.getCfgsEditable()) {
            //IAutoBuildConfigurationDescription b = AutoBuildProject.getAutoBuildConfig(cfg);
            //TOFIX JABA add support for multiple config select
            myAutoConfDesc.setIsParallelBuild(def);
        }
    }

    private void setParallelNumber(int num) {
        for (ICConfigurationDescription cfg : page.getCfgsEditable()) {
            //IAutoBuildConfigurationDescription b = AutoBuildProject.getAutoBuildConfig(cfg);
            //TOFIX JABA add support for multiple config select
            myAutoConfDesc.setParallelizationNum(num);
        }
    }

    private boolean isInternalBuilderEnabled() {
        for (ICConfigurationDescription cfg : page.getCfgsEditable()) {
            //IAutoBuildConfigurationDescription b = AutoBuildProject.getAutoBuildConfig(cfg);
            //TOFIX JABA add support for multiple config select
            return myAutoConfDesc.isInternalBuilderEnabled();
        }
        return false;
    }

    private void setBuildAttribute(String name, String value) {
        //		try {
        //			if (icfg instanceof IMultiConfiguration) {
        //				IConfiguration[] cfs = (IConfiguration[]) ((IMultiConfiguration) icfg).getItems();
        //				for (int i = 0; i < cfs.length; i++) {
        //					IBuilder b = cfs[i].getEditableBuilder();
        //					b.setBuildAttribute(name, value);
        //				}
        //			} else {
        //				icfg.getEditableBuilder().setBuildAttribute(name, value);
        //			}
        //		} catch (CoreException e) {
        //			e.printStackTrace();
        ////			ManagedBuilderUIPlugin.log(e);
        //		}
    }

    /**
     * Return an empty string is parameter is null
     */
    private String nonNull(String maybeNullString) {
        if (maybeNullString == null) {
            return EMPTY_STR;
        }
        return maybeNullString;
    }
}
