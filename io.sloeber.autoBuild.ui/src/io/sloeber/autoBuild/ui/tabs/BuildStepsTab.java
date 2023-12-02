/*******************************************************************************
 * Copyright (c) 2007, 2016 Intel Corporation and others.
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
 *******************************************************************************/
package io.sloeber.autoBuild.ui.tabs;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.cdt.core.settings.model.ICResourceDescription;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;

import io.sloeber.autoBuild.api.IAutoBuildConfigurationDescription;
import io.sloeber.autoBuild.ui.internal.Messages;

/**
 * @noextend This class is not intended to be subclassed by clients.
 * @noinstantiate This class is not intended to be instantiated by clients.
 */
public class BuildStepsTab extends AbstractAutoBuildPropertyTab {
    private Combo preCmd;
    private Combo preDes;
    private Combo postCmd;
    private Combo postDes;
    private boolean myAreWeUpdating = true;
    //    private ITool tool;

    private static final String label1 = Messages.BuildStepsTab_0;
    private static final String label2 = Messages.BuildStepsTab_1;
    //    private static final String PATH_SEPERATOR = ";"; //$NON-NLS-1$
    //    private static final String rcbsToolId = "org.eclipse.cdt.managedbuilder.ui.rcbs"; //$NON-NLS-1$
    //    private static final String rcbsToolName = "Resource Custom Build Step"; //$NON-NLS-1$
    //    private static final String rcbsToolInputTypeId = "org.eclipse.cdt.managedbuilder.ui.rcbs.inputtype"; //$NON-NLS-1$
    //    private static final String rcbsToolInputTypeName = "Resource Custom Build Step Input Type"; //$NON-NLS-1$
    //    private static final String rcbsToolOutputTypeId = "org.eclipse.cdt.managedbuilder.ui.rcbs.outputtype"; //$NON-NLS-1$
    //    private static final String rcbsToolOutputTypeName = "Resource Custom Build Step Output Type"; //$NON-NLS-1$

    //    private enum FIELD {
    //        PRECMD, PREANN, PSTCMD, PSTANN
    //    }
    //
    //    private Set<String> set1 = new TreeSet<>();
    //    private Set<String> set2 = new TreeSet<>();
    //    private Set<String> set3 = new TreeSet<>();
    //    private Set<String> set4 = new TreeSet<>();
    private static final String EMPTY_STRING = new String();

    //    private static final String[] rcbsApplicabilityRules = {
    //            Messages.ResourceCustomBuildStepBlock_label_applicability_rule_override,
    //            //		ManagedBuilderMessages_getResourceString("ResourceCustomBuildStepBlock_label_applicability_rule_before"),
    //            //		ManagedBuilderMessages_getResourceString("ResourceCustomBuildStepBlock_label_applicability_rule_after"),
    //            Messages.ResourceCustomBuildStepBlock_label_applicability_rule_disable, };

    @Override
    public void createControls(Composite parent) {
        super.createControls(parent);
        usercomp.setLayout(new GridLayout(1, false));

        if (page.isForProject())
            createForProject();
        else
            createForFile();
    }

    /**
     *
     */
    private void createForProject() {
        Group g1 = setupGroup(usercomp, Messages.BuildStepsTab_2, 1, GridData.FILL_HORIZONTAL);
        setupLabel(g1, label1, 1, GridData.BEGINNING);
        preCmd = setCombo(g1);

        setupLabel(g1, label2, 1, GridData.BEGINNING);
        preDes = setCombo(g1);

        Group g2 = setupGroup(usercomp, Messages.BuildStepsTab_3, 1, GridData.FILL_HORIZONTAL);
        setupLabel(g2, label1, 1, GridData.BEGINNING);
        postCmd = setCombo(g2);

        setupLabel(g2, label2, 1, GridData.BEGINNING);
        postDes = setCombo(g2);

        updateComboItems();

        //Add the listeners
        preCmd.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                if (myAreWeUpdating) {
                    return;
                }
                myAutoConfDesc.setPrebuildStep(preCmd.getText());
            }
        });

        preDes.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                if (myAreWeUpdating) {
                    return;
                }
                myAutoConfDesc.setPreBuildAnouncement(preDes.getText());
            }
        });

        postCmd.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                if (myAreWeUpdating) {
                    return;
                }
                myAutoConfDesc.setPostbuildStep(postCmd.getText());
            }
        });

        postCmd.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                if (myAreWeUpdating) {
                    return;
                }
                myAutoConfDesc.setPostbuildStep(postCmd.getText());
            }
        });

        postDes.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                if (myAreWeUpdating) {
                    return;
                }
                myAutoConfDesc.setPostBuildAnouncement(postDes.getText());
            }
        });
    }

    /**
     *
     */
    private void createForFile() {
        //        Group g1 = setupGroup(usercomp, Messages.BuildStepsTab_4, 1, GridData.FILL_HORIZONTAL);
        //        setupLabel(g1, Messages.ResourceCustomBuildStepBlock_label_applicability, 1, GridData.BEGINNING);
        //
        //        combo = new Combo(g1, SWT.READ_ONLY | SWT.DROP_DOWN | SWT.BORDER);
        //        combo.setItems(rcbsApplicabilityRules);
        //        combo.addSelectionListener(new SelectionAdapter() {
        //            @Override
        //            public void widgetSelected(SelectionEvent e) {
        //                rcfg.setRcbsApplicability(sel2app(combo.getSelectionIndex()));
        //            }
        //        });
        //
        //        setupLabel(g1, Messages.BuildStepsTab_5, 1, GridData.BEGINNING);
        //        preCmd = setCombo(g1, FIELD.PRECMD, set1);
        //        preCmd.addModifyListener(new ModifyListener() {
        //            @Override
        //            public void modifyText(ModifyEvent e) {
        //                if (canModify && tool != null) {
        //                    IInputType[] ein = tool.getInputTypes();
        //                    if (ein != null && ein.length > 0) {
        //                        IAdditionalInput[] add = ein[0].getAdditionalInputs();
        //                        if (add != null && add.length > 0) {
        //                            add[0].setPaths(preCmd.getText());
        //                        }
        //                    }
        //                }
        //            }
        //        });
        //
        //        setupLabel(g1, Messages.BuildStepsTab_6, 1, GridData.BEGINNING);
        //        preDes = setCombo(g1, FIELD.PREANN, set2);
        //        preDes.addModifyListener(new ModifyListener() {
        //            @Override
        //            public void modifyText(ModifyEvent e) {
        //                if (canModify && tool != null) {
        //                    IOutputType[] out = tool.getOutputTypes();
        //                    if (valid(out))
        //                        out[0].setOutputNames(preDes.getText());
        //                }
        //            }
        //        });
        //
        //        setupLabel(g1, label1, 1, GridData.BEGINNING);
        //        postCmd = setCombo(g1, FIELD.PSTCMD, set3);
        //        postCmd.addModifyListener(new ModifyListener() {
        //            @Override
        //            public void modifyText(ModifyEvent e) {
        //                if (canModify && tool != null)
        //                    tool.setToolCommand(postCmd.getText());
        //            }
        //        });
        //
        //        setupLabel(g1, label2, 1, GridData.BEGINNING);
        //        postDes = setCombo(g1, FIELD.PSTANN, set4);
        //        postDes.addModifyListener(new ModifyListener() {
        //            @Override
        //            public void modifyText(ModifyEvent e) {
        //                if (canModify && tool != null)
        //                    tool.setAnnouncement(postDes.getText());
        //            }
        //        });
    }

    @Override
    public void updateData(ICResourceDescription cfgd) {
        super.updateData(cfgd);
        myAreWeUpdating = true;
        update();
        myAreWeUpdating = false;

    }

    private void update() {

        updateComboItems();

        if (page.isForProject()) {
            preCmd.setText(myAutoConfDesc.getPrebuildStep());
            preDes.setText(myAutoConfDesc.getPreBuildAnouncement());
            postCmd.setText(myAutoConfDesc.getPostbuildStep());
            postDes.setText(myAutoConfDesc.getPostBuildAnouncement());
        } else {
            //            rcfg = (IFileInfo) getResCfg(cfgdescr);
            //            combo.select(app2sel(rcfg.getRcbsApplicability()));
            //            tool = getRcbsTool(rcfg);
            //
            //            if (tool != null) {
            //                preCmd.setText(getInputTypes(tool));
            //                preDes.setText(getOutputNames(tool));
            //                postCmd.setText(tool.getToolCommand());
            //                postDes.setText(tool.getAnnouncement());
            //            } else {
            //                preCmd.setText(EMPTY_STR);
            //                preDes.setText(EMPTY_STR);
            //                postCmd.setText(EMPTY_STR);
            //                postDes.setText(EMPTY_STR);
            //            }
        }
    }

    //    private String getInputTypes(ITool t) {
    //        String s = EMPTY_STR;
    //        IInputType[] tmp = t.getInputTypes();
    //        if (tmp != null && tmp.length > 0) {
    //            IAdditionalInput[] add = tmp[0].getAdditionalInputs();
    //            if (add != null && add.length > 0)
    //                s = createList(add[0].getPaths());
    //        }
    //        return s;
    //    }

    //    private String getOutputNames(ITool t) {
    //        String s = EMPTY_STR;
    //        IOutputType[] tmp2 = t.getOutputTypes();
    //        if (tmp2 != null && tmp2.length > 0)
    //            s = createList(tmp2[0].getOutputNames());
    //        return s;
    //    }

    //    private ITool getRcbsTool(IFileInfo rcConfig) {
    //        ITool rcbsTools[] = getRcbsTools(rcConfig);
    //        ITool rcbsTool = null;
    //
    //        if (rcbsTools != null)
    //            rcbsTool = rcbsTools[0];
    //        else {
    //            rcbsTool = rcConfig.createTool(null, rcbsToolId + "." + ManagedBuildManager.getRandomNumber(), rcbsToolName, //$NON-NLS-1$
    //                    false);
    //            rcbsTool.setCustomBuildStep(true);
    //            IInputType rcbsToolInputType = rcbsTool.createInputType(null,
    //                    rcbsToolInputTypeId + "." + ManagedBuildManager.getRandomNumber(), rcbsToolInputTypeName, false); //$NON-NLS-1$
    //            IAdditionalInput rcbsToolInputTypeAdditionalInput = rcbsToolInputType.createAdditionalInput(""); //$NON-NLS-1$
    //            rcbsToolInputTypeAdditionalInput.setKind(IAdditionalInput.KIND_ADDITIONAL_INPUT_DEPENDENCY);
    //            rcbsTool.createOutputType(null, rcbsToolOutputTypeId + "." + ManagedBuildManager.getRandomNumber(), //$NON-NLS-1$
    //                    rcbsToolOutputTypeName, false);
    //        }
    //        return rcbsTool;
    //    }

    //    private ITool[] getRcbsTools(IResourceInfo rcConfig) {
    //        List<ITool> list = new ArrayList<>();
    //        ITool tools[] = rcConfig.getTools();
    //
    //        for (int i = 0; i < tools.length; i++) {
    //            ITool tool = tools[i];
    //            if (tool.getCustomBuildStep() && !tool.isExtensionElement()) {
    //                list.add(tool);
    //            }
    //        }
    //        if (list.size() != 0) {
    //            return list.toArray(new ITool[list.size()]);
    //        }
    //        return null;
    //    }

    //    private String createList(String[] items) {
    //        if (items == null)
    //            return ""; //$NON-NLS-1$
    //
    //        StringBuilder path = new StringBuilder(EMPTY_STR);
    //
    //        for (int i = 0; i < items.length; i++) {
    //            path.append(items[i]);
    //            if (i < (items.length - 1)) {
    //                path.append(PATH_SEPERATOR);
    //            }
    //        }
    //        return path.toString();
    //    }

    //    @Override
    //    public void performApply(ICResourceDescription src, ICResourceDescription dst) {
    //        if (page.isForProject()) {
    //            IConfiguration cfg1 = getCfg(src.getConfiguration());
    //            IConfiguration cfg2 = getCfg(dst.getConfiguration());
    //            cfg2.setPrebuildStep(cfg1.getPrebuildStep());
    //            cfg2.setPreannouncebuildStep(cfg1.getPreannouncebuildStep());
    //            cfg2.setPostbuildStep(cfg1.getPostbuildStep());
    //            cfg2.setPostannouncebuildStep(cfg1.getPostannouncebuildStep());
    //        } else {
    //            if (page.isMultiCfg()) {
    //                ICResourceDescription[] ris1 = (ICResourceDescription[]) ((ICMultiResourceDescription) src).getItems();
    //                ICResourceDescription[] ris2 = (ICResourceDescription[]) ((ICMultiResourceDescription) dst).getItems();
    //                for (int i = 0; i < ris1.length; i++)
    //                    applyToFile(ris1[i], ris2[i]);
    //            } else
    //                applyToFile(src, dst);
    //        }
    //    }

    //    private void applyToFile(ICResourceDescription src, ICResourceDescription dst) {
    //        IFileInfo rcfg1 = (IFileInfo) getResCfg(src);
    //        IFileInfo rcfg2 = (IFileInfo) getResCfg(dst);
    //        rcfg2.setRcbsApplicability(rcfg1.getRcbsApplicability());
    //        ITool tool1 = getRcbsTool(rcfg1);
    //        ITool tool2 = getRcbsTool(rcfg2);
    //        IInputType[] ein1 = tool1.getInputTypes();
    //        IInputType[] ein2 = tool2.getInputTypes();
    //        if (valid(ein1) && valid(ein2)) {
    //            IAdditionalInput[] add1 = ein1[0].getAdditionalInputs();
    //            IAdditionalInput[] add2 = ein2[0].getAdditionalInputs();
    //            if (valid(add1) && valid(add2)) {
    //                //			if (add1 != null && add2 != null && add1.length > 0 && add2.length > 0) {
    //                add2[0].setPaths(createList(add1[0].getPaths()));
    //            }
    //        }
    //        IOutputType[] tmp1 = tool1.getOutputTypes();
    //        IOutputType[] tmp2 = tool2.getOutputTypes();
    //        //		if (tmp1 != null && tmp2 != null && tmp1.length > 0 && tmp2.length > 0) {
    //        if (valid(tmp1) && valid(tmp2)) {
    //            tmp2[0].setOutputNames(createList(tmp1[0].getOutputNames()));
    //        }
    //        tool2.setToolCommand(tool1.getToolCommand());
    //        tool2.setAnnouncement(tool1.getAnnouncement());
    //    }

    //    private int sel2app(int index) {
    //        String sel = combo.getItem(index);
    //        if (Messages.ResourceCustomBuildStepBlock_label_applicability_rule_override.equals(sel)) {
    //            return IResourceConfiguration.KIND_APPLY_RCBS_TOOL_AS_OVERRIDE;
    //        } else if (Messages.ResourceCustomBuildStepBlock_label_applicability_rule_after.equals(sel)) {
    //            return IResourceConfiguration.KIND_APPLY_RCBS_TOOL_AFTER;
    //        } else if (Messages.ResourceCustomBuildStepBlock_label_applicability_rule_before.equals(sel)) {
    //            return IResourceConfiguration.KIND_APPLY_RCBS_TOOL_BEFORE;
    //        }
    //        return IResourceConfiguration.KIND_DISABLE_RCBS_TOOL;
    //    }

    //    private boolean valid(Object[] arr) {
    //        return (arr != null && arr.length > 0);
    //    }

    //    private int app2sel(int val) {
    //        switch (val) {
    //        case IResourceConfiguration.KIND_APPLY_RCBS_TOOL_AFTER:
    //            return combo.indexOf(Messages.ResourceCustomBuildStepBlock_label_applicability_rule_after);
    //        case IResourceConfiguration.KIND_APPLY_RCBS_TOOL_BEFORE:
    //            return combo.indexOf(Messages.ResourceCustomBuildStepBlock_label_applicability_rule_before);
    //        case IResourceConfiguration.KIND_DISABLE_RCBS_TOOL:
    //            return combo.indexOf(Messages.ResourceCustomBuildStepBlock_label_applicability_rule_disable);
    //        case IResourceConfiguration.KIND_APPLY_RCBS_TOOL_AS_OVERRIDE:
    //        default:
    //            return combo.indexOf(Messages.ResourceCustomBuildStepBlock_label_applicability_rule_override);
    //        }
    //    }

    // This page can be displayed for managed project only
    //    @Override
    //    public boolean canBeVisible() {
    //        if (page.isForProject() || page.isForFile()) {
    //            if (page.isMultiCfg()) {
    //                ICMultiItemsHolder mih = (ICMultiItemsHolder) getCfg();
    //                IConfiguration[] cfs = (IConfiguration[]) mih.getItems();
    //                for (int i = 0; i < cfs.length; i++) {
    //                    if (cfs[i].getBuilder().isManagedBuildOn())
    //                        return true;
    //                }
    //                return false;
    //            } else
    //                return getCfg().getBuilder().isManagedBuildOn();
    //        } else
    //            return false;
    //    }

    @Override
    protected void performDefaults() {
        if (page.isForProject()) {
            myAutoConfDesc.setPrebuildStep(EMPTY_STRING);
            myAutoConfDesc.setPreBuildAnouncement(EMPTY_STRING);
            myAutoConfDesc.setPostbuildStep(EMPTY_STRING);
            myAutoConfDesc.setPostBuildAnouncement(EMPTY_STRING);
        } else {
            //            rcfg.setRcbsApplicability(IResourceConfiguration.KIND_DISABLE_RCBS_TOOL);
            //            ITool tool = getRcbsTool(rcfg);
            //            IInputType[] ein = tool.getInputTypes();
            //            if (valid(ein)) {
            //                IAdditionalInput[] add = ein[0].getAdditionalInputs();
            //                if (valid(add))
            //                    add[0].setPaths(null);
            //            }
            //            IOutputType[] tmp = tool.getOutputTypes();
            //            if (valid(tmp))
            //                tmp[0].setOutputNames(null);
            //            tool.setToolCommand(null);
            //            tool.setAnnouncement(null);
        }
        update();
    }

    @Override
    protected void updateButtons() {
        // Do nothing. No buttons to update.
    }

    private Combo setCombo(Composite c) {
        Combo combo = new Combo(c, SWT.BORDER);
        setupControl(combo, 1, GridData.FILL_HORIZONTAL);

        combo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        return combo;
    }

    private void updateComboItems() {
        if (myAutoConfDesc == null) {
            return;
        }
        Set<String> getPrebuildStepSet = new HashSet<>();
        Set<String> getPreannouncebuildStepSet = new HashSet<>();
        Set<String> getPostbuildStepSet = new HashSet<>();
        Set<String> getPostannouncebuildStepSet = new HashSet<>();

        if (page.isForProject()) {
            //           for (IAutoBuildConfigurationDescription cf : myAutoConfDescMap.values()) {
            getPrebuildStepSet.add(myAutoConfDesc.getPrebuildStep());
            getPreannouncebuildStepSet.add(myAutoConfDesc.getPreBuildAnouncement());
            getPostbuildStepSet.add(myAutoConfDesc.getPostbuildStep());
            getPostannouncebuildStepSet.add(myAutoConfDesc.getPostBuildAnouncement());
            //         }
        }

        if (getPrebuildStepSet.size() > 0) {
            preCmd.removeAll();
            preCmd.setItems(getPrebuildStepSet.toArray(new String[getPrebuildStepSet.size()]));
        }
        if (getPreannouncebuildStepSet.size() > 0) {
            preDes.removeAll();
            preDes.setItems(getPreannouncebuildStepSet.toArray(new String[getPreannouncebuildStepSet.size()]));
        }
        if (getPostbuildStepSet.size() > 0) {
            postCmd.removeAll();
            postCmd.setItems(getPostbuildStepSet.toArray(new String[getPostbuildStepSet.size()]));
        }
        if (getPostannouncebuildStepSet.size() > 0) {
            postDes.removeAll();
            postDes.setItems(getPostannouncebuildStepSet.toArray(new String[getPostannouncebuildStepSet.size()]));
        }
    }
}
