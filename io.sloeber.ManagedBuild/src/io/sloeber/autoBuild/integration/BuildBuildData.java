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
 * Baltasar Belyavsky (Texas Instruments) - bug 340219: Project metadata files are saved unnecessarily
 *******************************************************************************/
package io.sloeber.autoBuild.integration;

import static io.sloeber.autoBuild.integration.AutoBuildConstants.*;

import org.eclipse.cdt.core.envvar.IEnvironmentContributor;
import org.eclipse.cdt.core.settings.model.COutputEntry;
import org.eclipse.cdt.core.settings.model.ICOutputEntry;
import org.eclipse.cdt.core.settings.model.ICSettingEntry;
import org.eclipse.cdt.core.settings.model.extension.CBuildData;
import org.eclipse.cdt.core.settings.model.util.CDataUtil;
import org.eclipse.core.resources.ICommand;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import io.sloeber.autoBuild.Internal.BuilderFactory;
import io.sloeber.autoBuild.core.Activator;
import io.sloeber.schema.api.IBuilder;

public class BuildBuildData extends CBuildData {
    private IBuilder fBuilder;
    private BuildEnvironmentContributor myBuildEnvironmentContributor;
    private ICOutputEntry[] myEntries = null;// new ICOutputEntry[0];
    private AutoBuildConfigurationDescription myAutoBuildConf;
    private String myID;

    public BuildBuildData(AutoBuildConfigurationDescription autoBuildConf) {
        this(autoBuildConf, (BuildBuildData) null, false);
        //        myAutoBuildConf = autoBuildConf;
        //        fBuilder = myAutoBuildConf.getConfiguration().getBuilder();
        //        myID = CDataUtil.genId(fBuilder.getId());
        //        myBuildEnvironmentContributor = new BuildEnvironmentContributor(myAutoBuildConf);
    }

    public BuildBuildData(AutoBuildConfigurationDescription autoBuildConf, BuildBuildData source, boolean clone) {
        myAutoBuildConf = autoBuildConf;
        fBuilder = myAutoBuildConf.getConfiguration().getBuilder();
        if (clone && source != null) {
            myID = source.myID;
        } else {
            myID = CDataUtil.genId(fBuilder.getId());
        }
        myBuildEnvironmentContributor = new BuildEnvironmentContributor(myAutoBuildConf);
    }

    @Override
    public IPath getBuilderCWD() {
        return myAutoBuildConf.getBuildFolder().getFullPath();
    }

    @Override
    public String[] getErrorParserIDs() {
        return myAutoBuildConf.getConfiguration().getErrorParserList();
    }

    @Override
    public ICOutputEntry[] getOutputDirectories() {
        if (myEntries == null) {
            myEntries = new ICOutputEntry[] { new COutputEntry(getBuilderCWD(), null,
                    ICSettingEntry.VALUE_WORKSPACE_PATH | ICSettingEntry.RESOLVED) };
        }
        return myEntries;
    }

    @Override
    public void setBuilderCWD(IPath path) {
        //Seems this is only used in a load data and tests
        //so I ignore (jaba said)
    }

    @Override
    public void setErrorParserIDs(String[] ids) {
        // fCfg.setErrorParserList(ids);
    }

    @Override
    public void setOutputDirectories(ICOutputEntry[] entries) {
        myEntries = entries;
    }

    @Override
    public String getId() {
        return myID;
    }

    @Override
    public String getName() {
        return fBuilder.getName();
    }

    @Override
    public boolean isValid() {
        return fBuilder != null;
    }

    @Override
    public IEnvironmentContributor getBuildEnvironmentContributor() {
        return myBuildEnvironmentContributor;
    }

    @Override
    public ICommand getBuildSpecCommand() {
        try {
            return BuilderFactory.createCommandFromBuilder(myAutoBuildConf.getProject(), fBuilder);
        } catch (CoreException cx) {
            Activator.log(cx);
            return null;
        }
    }

}
