/*******************************************************************************
 * Copyright (c) 2007, 2010 Intel Corporation and others.
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
package io.sloeber.autoBuild.Internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.cdt.core.model.CoreModel;
import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.cdt.core.settings.model.ICProjectDescription;
import org.eclipse.cdt.core.settings.model.ICProjectDescriptionManager;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;

import io.sloeber.autoBuild.api.BuildException;
import io.sloeber.autoBuild.api.IConfiguration;
import io.sloeber.autoBuild.api.IManagedBuildInfo;
import io.sloeber.autoBuild.api.IManagedProject;
import io.sloeber.autoBuild.api.IOption;
import io.sloeber.autoBuild.api.IResourceInfo;
import io.sloeber.autoBuild.api.ITool;
import io.sloeber.autoBuild.api.OptionStringValue;
import io.sloeber.autoBuild.core.Activator;

public class BuildSettingsUtil {
    private static final int[] COMMON_SETTINGS_IDS = new int[] { IOption.INCLUDE_PATH, IOption.PREPROCESSOR_SYMBOLS,
            IOption.LIBRARIES, IOption.OBJECTS, IOption.INCLUDE_FILES, IOption.LIBRARY_PATHS, IOption.LIBRARY_FILES,
            IOption.MACRO_FILES, };

    public static void disconnectDepentents(IConfiguration cfg, List<ITool> tools) {
        for (ITool tool: tools) {
            disconnectDepentents(cfg, tool);
        }
    }

    public static void disconnectDepentents(IConfiguration cfg, ITool tool) {
        ITool deps[] = getDependentTools(cfg, tool);
        for (int i = 0; i < deps.length; i++) {
            disconnect(deps[i], tool);
        }
    }

    private static void disconnect(ITool child, ITool superClass) {
        ITool directChild = child;
        for (; directChild != null; directChild = directChild.getSuperClass()) {
            if (superClass.equals(directChild.getSuperClass()))
                break;
        }

        if (directChild == null)
            return;

        //		TOFFIX JABA Why do a copy on a disconnect?
        //		((Tool) directChild).copyNonoverriddenSettings((Tool) superClass);
        //		((Tool) directChild).setSuperClass(superClass.getSuperClass());
    }

    public static ITool[] getDependentTools(IConfiguration cfg, ITool tool) {
        List<IResourceInfo> rcInfos= cfg.getResourceInfos();
        List<ITool> list = new ArrayList<>();
        for (IResourceInfo rcInfo: rcInfos) {
            calcDependentTools(rcInfo, tool, list);
        }
        return list.toArray(new Tool[list.size()]);
    }

    private static List<ITool> calcDependentTools(IResourceInfo info, ITool tool, List<ITool> list) {
        return calcDependentTools(info.getTools(), tool, list);
    }

    public static List<ITool> calcDependentTools(List<ITool> tools, ITool tool, List<ITool> list) {
        if (list == null)
            list = new ArrayList<>();

        for (ITool superTool: tools) {
            for (; superTool != null; superTool = superTool.getSuperClass()) {
                if (superTool.equals(tool)) {
                    list.add(superTool);
                }
            }
        }

        return list;
    }



    public static ICProjectDescription checkSynchBuildInfo(IProject project) throws CoreException {
        IManagedBuildInfo info = ManagedBuildManager.getBuildInfo(project, false);
        if (info == null)
            return null;

        ICProjectDescription projDes = CoreModel.getDefault().getProjectDescription(project);
        projDes = synchBuildInfo(info, projDes, false);

        return projDes.isModified() ? projDes : null;
    }

    public static ICProjectDescription synchBuildInfo(IManagedBuildInfo info, ICProjectDescription projDes,
            boolean force) throws CoreException {
        IManagedProject mProj = info.getManagedProject();

        ICConfigurationDescription cfgDess[] = projDes.getConfigurations();

        for (int i = 0; i < cfgDess.length; i++) {
            ICConfigurationDescription cfgDes = cfgDess[i];
            IConfiguration cfg = mProj.getConfiguration(cfgDes.getId());
            if (cfg == null) {
                projDes.removeConfiguration(cfgDes);
            }
        }

        return projDes;
    }

    public static void checkApplyDescription(IProject project, ICProjectDescription des) throws CoreException {
        checkApplyDescription(project, des, false);
    }

    public static void checkApplyDescription(IProject project, ICProjectDescription des, boolean avoidSerialization)
            throws CoreException {
        ICConfigurationDescription[] cfgs = des.getConfigurations();
        for (int i = 0; i < cfgs.length; i++) {
            if (!ManagedBuildManager.CFG_DATA_PROVIDER_ID.equals(cfgs[i].getBuildSystemId()))
                des.removeConfiguration(cfgs[i]);
        }

        int flags = 0;
        if (avoidSerialization)
            flags |= ICProjectDescriptionManager.SET_NO_SERIALIZE;
        CoreModel.getDefault().getProjectDescriptionManager().setProjectDescription(project, des, flags, null);
    }

    public static ITool[] getToolsBySuperClassId(ITool[] tools, String id) {
        List<ITool> retTools = new ArrayList<>();
        if (id != null) {
            for (int i = 0; i < tools.length; i++) {
                ITool targetTool = tools[i];
                ITool tool = targetTool;
                do {
                    if (id.equals(tool.getId())) {
                        retTools.add(targetTool);
                        break;
                    }
                    tool = tool.getSuperClass();
                } while (tool != null);
            }
        }
        return retTools.toArray(new ITool[retTools.size()]);
    }
}
