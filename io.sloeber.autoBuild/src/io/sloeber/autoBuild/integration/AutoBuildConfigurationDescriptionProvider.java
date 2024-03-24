/*******************************************************************************
 * Copyright (c) 2007, 2013 Intel Corporation and others.
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
 * Baltasar Belyavsky (Texas Instruments) - bug 340219: Project metadata files are saved unnecessarily
 *******************************************************************************/
package io.sloeber.autoBuild.integration;

import static io.sloeber.autoBuild.api.AutoBuildConstants.*;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;

import org.apache.commons.io.FileUtils;
import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.cdt.core.settings.model.ICProjectDescription;
import org.eclipse.cdt.core.settings.model.IModificationContext;
import org.eclipse.cdt.core.settings.model.extension.CConfigurationData;
import org.eclipse.cdt.core.settings.model.extension.CConfigurationDataProvider;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import io.sloeber.autoBuild.core.Activator;

/**
 * The main hook ManagedBuild uses to connect to cdt.core's project model.
 * Provides & Persists Build configuration data in the project model storage.
 */
public class AutoBuildConfigurationDescriptionProvider extends CConfigurationDataProvider {// implements ISettingsChangeListener {
    public static final String CFG_DATA_PROVIDER_ID = Activator.PLUGIN_ID + ".ConfigurationDataProvider"; //$NON-NLS-1$
    private static final String AUTO_BUILD_PROJECT_FILE = ".autoBuildProject"; //$NON-NLS-1$

    public AutoBuildConfigurationDescriptionProvider() {
    }

    @Override
    public CConfigurationData applyConfiguration(ICConfigurationDescription cfgDescription,
            ICConfigurationDescription baseCfgDescription, CConfigurationData baseData, IModificationContext context,
            IProgressMonitor monitor) throws CoreException {

        ICProjectDescription projDesc = cfgDescription.getProjectDescription();
        String lineEnd = getLineEnd();
        StringBuffer configText = new StringBuffer();
        for (ICConfigurationDescription curConfDesc : projDesc.getConfigurations()) {
            AutoBuildConfigurationDescription autoBuildConfigBase = (AutoBuildConfigurationDescription) curConfDesc
                    .getConfigurationData();

            String lineStart = getLinePrefix(curConfDesc);

            configText.append(autoBuildConfigBase.serialize(lineStart, lineEnd));
        }

        File projectFile = getStorageFile(cfgDescription);
        try {
            if (projectFile.exists()) {
                //                String curConfigsText = FileUtils.readFileToString(projectFile, Charset.defaultCharset());
                //                String clean = curConfigsText.replaceAll("(?m)^" + lineStart + ".+$" + lineEnd, EMPTY_STRING); //$NON-NLS-1$ //$NON-NLS-2$
                FileUtils.write(projectFile, configText, Charset.defaultCharset());
            } else {
                FileUtils.write(projectFile, configText, Charset.defaultCharset());
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return baseData;
    }

    @Override
    public CConfigurationData createConfiguration(ICConfigurationDescription cfgDescription,
            ICConfigurationDescription baseCfgDescription, CConfigurationData base, boolean clone,
            IProgressMonitor monitor) throws CoreException {
        AutoBuildConfigurationDescription autoBuildConfigBase = (AutoBuildConfigurationDescription) base;
        AutoBuildConfigurationDescription ret = new AutoBuildConfigurationDescription(cfgDescription,
                autoBuildConfigBase, clone);
        return ret;
    }

    @Override
    public CConfigurationData loadConfiguration(ICConfigurationDescription cfgDescription, IProgressMonitor monitor)
            throws CoreException {

        String lineStart = getLinePrefix(cfgDescription);
        String lineEnd = getLineEnd();
        File projectFile = getStorageFile(cfgDescription);
        try {
            if (projectFile.exists()) {
                String curConfigsText = FileUtils.readFileToString(projectFile, Charset.defaultCharset());
                return new AutoBuildConfigurationDescription(cfgDescription, curConfigsText, lineStart, lineEnd);
            }
            //This Should not happen
            throw new CoreException(null);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public void removeConfiguration(ICConfigurationDescription cfgDescription, CConfigurationData data,
            IProgressMonitor monitor) {
        return;
        //        String configname = cfgDescription.getName();
        //        if (cfgDescription.getProjectDescription().getConfigurationByName(configname) != null) {
        //            //no need to remove the configuration from disk
        //            return;
        //        }
        //        String lineStart = getLinePrefix(cfgDescription);
        //        String lineEnd = getLineEnd();
        //        File projectFile = getStorageFile(cfgDescription);
        //        try {
        //            if (projectFile.exists()) {
        //                String curConfigsText = FileUtils.readFileToString(projectFile, Charset.defaultCharset());
        //                String clean = curConfigsText.replaceAll("(?m)^" + lineStart + ".+$" + lineEnd, EMPTY_STRING); //$NON-NLS-1$ //$NON-NLS-2$
        //                FileUtils.write(projectFile, clean, Charset.defaultCharset());
        //            }
        //        } catch (IOException e) {
        //            // TODO Auto-generated catch block
        //            e.printStackTrace();
        //        }
        //        return;
    }

    @Override
    public void dataCached(ICConfigurationDescription cfgDescription, CConfigurationData data,
            IProgressMonitor monitor) {
        //doc says: default implementation is empty :-)
        return;
    }

    private static String getLinePrefix(ICConfigurationDescription cfgDescription) {
        return cfgDescription.getName() + DOT;
    }

    private static String getLineEnd() {
        return NEWLINE;
    }

    private static File getStorageFile(ICConfigurationDescription cfgDescription) {
        IFile project = cfgDescription.getProjectDescription().getProject().getFile(AUTO_BUILD_PROJECT_FILE);
        return project.getLocation().toFile();
    }
}
