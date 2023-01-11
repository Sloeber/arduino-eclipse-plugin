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
package io.sloeber.autoBuild.Internal;

import org.eclipse.cdt.core.envvar.IEnvironmentContributor;
import org.eclipse.cdt.core.settings.model.ICOutputEntry;
import org.eclipse.cdt.core.settings.model.extension.CBuildData;
import org.eclipse.core.resources.ICommand;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import io.sloeber.autoBuild.api.IBuilder;
import io.sloeber.autoBuild.core.Activator;

public class BuildBuildData extends CBuildData {
    private Builder fBuilder;
    private Configuration fCfg;

    public BuildBuildData(IBuilder builder) {
        fBuilder = (Builder) builder;
        fCfg = (Configuration) fBuilder.getParent().getParent();
    }

    @Override
    public IPath getBuilderCWD() {
        return ManagedBuildManager.getBuildFolder(fCfg, fBuilder).getLocation();
    }

    //	private IPath createAbsolutePathFromWorkspacePath(IPath path){
    //		IStringVariableManager mngr = VariablesPlugin.getDefault().getStringVariableManager();
    //		String locationString = mngr.generateVariableExpression("workspace_loc", path.toString()); //$NON-NLS-1$
    //		return new Path(locationString);
    //	}

    @Override
    public String[] getErrorParserIDs() {
        return fCfg.getErrorParserList().toArray(new String[0]);
    }

    @Override
    public ICOutputEntry[] getOutputDirectories() {
        return fBuilder.getOutputEntries();
    }

    @Override
    public void setBuilderCWD(IPath path) {
    	//JABA not sure what to do here TOFIX
        //fBuilder.setBuildPath(path.toString());
    	return;
    }

    @Override
    public void setErrorParserIDs(String[] ids) {
       // fCfg.setErrorParserList(ids);
    }

    @Override
    public void setOutputDirectories(ICOutputEntry[] entries) {
        fBuilder.setOutputEntries(entries);
    }

    @Override
    public String getId() {
        return fBuilder.getId();
    }

    @Override
    public String getName() {
        return fBuilder.getName();
    }

    @Override
    public boolean isValid() {
        return fBuilder != null;
    }

    public void setName(String name) {
        //TODO
    }

    @Override
    public IEnvironmentContributor getBuildEnvironmentContributor() {
        return new BuildEnvironmentContributor(this);
    }

    @Override
    public ICommand getBuildSpecCommand() {
        try {
            return BuilderFactory.createCommandFromBuilder(this.fBuilder);
        } catch (CoreException cx) {
            Activator.log(cx);
            return null;
        }
    }

    public IBuilder getBuilder() {
        return fBuilder;
    }

}
