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
import io.sloeber.autoBuild.api.IManagedBuildInfo;
import io.sloeber.autoBuild.api.OptionStringValue;
import io.sloeber.autoBuild.core.Activator;
import io.sloeber.schema.api.IConfiguration;
import io.sloeber.schema.api.IManagedProject;
import io.sloeber.schema.api.IOption;
import io.sloeber.schema.api.IResourceInfo;
import io.sloeber.schema.api.ITool;

public class BuildSettingsUtil {
    private static final int[] COMMON_SETTINGS_IDS = new int[] { IOption.INCLUDE_PATH, IOption.PREPROCESSOR_SYMBOLS,
            IOption.LIBRARIES, IOption.OBJECTS, IOption.INCLUDE_FILES, IOption.LIBRARY_PATHS, IOption.LIBRARY_FILES,
            IOption.MACRO_FILES, };










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

 }
