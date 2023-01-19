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
 *******************************************************************************/
package io.sloeber.autoBuild.integration;

import java.util.LinkedList;
import java.util.List;

import org.eclipse.cdt.core.cdtvariables.ICdtVariablesContributor;
import org.eclipse.cdt.core.settings.model.CConfigurationStatus;
import org.eclipse.cdt.core.settings.model.ICSettingBase;
import org.eclipse.cdt.core.settings.model.ICSourceEntry;
import org.eclipse.cdt.core.settings.model.extension.CBuildData;
import org.eclipse.cdt.core.settings.model.extension.CConfigurationData;
import org.eclipse.cdt.core.settings.model.extension.CFileData;
import org.eclipse.cdt.core.settings.model.extension.CFolderData;
import org.eclipse.cdt.core.settings.model.extension.CLanguageData;
import org.eclipse.cdt.core.settings.model.extension.CResourceData;
import org.eclipse.cdt.core.settings.model.extension.CTargetPlatformData;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;

import io.sloeber.autoBuild.Internal.BuildFileData;
import io.sloeber.autoBuild.Internal.BuildFolderData;
import io.sloeber.autoBuild.Internal.BuildLanguageData;
import io.sloeber.autoBuild.Internal.BuildTargetPlatformData;
import io.sloeber.autoBuild.Internal.ICfgScannerConfigBuilderInfo2Set;
import io.sloeber.autoBuild.Internal.ManagedBuildManager;
import io.sloeber.autoBuild.core.Activator;
import io.sloeber.schema.api.IBuilder;
import io.sloeber.schema.api.IConfiguration;
import io.sloeber.schema.api.IFileInfo;
import io.sloeber.schema.api.IFolderInfo;
import io.sloeber.schema.api.IResourceInfo;
import io.sloeber.schema.api.ITargetPlatform;
import io.sloeber.schema.api.ITool;
import io.sloeber.schema.api.IToolChain;
import io.sloeber.schema.internal.Configuration;
import io.sloeber.schema.internal.FolderInfo;
import io.sloeber.schema.internal.TargetPlatform;

public class BuildConfigurationData extends CConfigurationData {
    private Configuration fCfg;
    private IProject myProject;
    private ICfgScannerConfigBuilderInfo2Set cfgScannerInfo;
    private CFolderData myFolderData;


    //	private BuildVariablesContributor fCdtVars;
    public BuildConfigurationData(Configuration cfg, IProject project) {
        fCfg =  cfg;
        myProject=project;
        myFolderData=new BuildFolderData(myProject.getFolder("test"));
    }

    public IConfiguration getConfiguration() {
        return fCfg;
    }

    @Override
    public String getDescription() {
        return fCfg.getDescription();
    }

    @Override
    public CResourceData[] getResourceDatas() {
    	//TOFIX configuration storage must be added here
        List<IResourceInfo> infos = new LinkedList<>();
        return infos.toArray( new CResourceData[infos.size()]);
    }

    @Override
    public CFolderData getRootFolderData() {
        return myFolderData;
    }



    @Override
    public String getId() {
        return fCfg.getId();
    }

    @Override
    public String getName() {
        return fCfg.getName();
    }



    @Override
    public boolean isValid() {
        return (fCfg != null)&&(myProject!=null);
    }

    @Override
    public CTargetPlatformData getTargetPlatformData() {
    	IToolChain toolchain =fCfg.getToolChain();
    	TargetPlatform targetPlatform=(TargetPlatform)toolchain.getTargetPlatform();
    	return new BuildTargetPlatformData(targetPlatform);
  //      return fCfg.getToolChain().getTargetPlatformData();
    }



    @Override
    public CBuildData getBuildData() {

    	return new BuildBuildData(fCfg,myProject);
    }

    @Override
    public ICdtVariablesContributor getBuildVariablesContributor() {
        return null;//TOFIX new BuildVariablesContributor(this);
    }

    void clearCachedData() {
        for (CResourceData data:  getResourceDatas()) {
            if (data.getType() == ICSettingBase.SETTING_FOLDER) {
                ((BuildFolderData) data).clearCachedData();
            } else {
                ((BuildFileData) data).clearCachedData();
            }
        }
    }

    @Override
    public CConfigurationStatus getStatus() {
        int flags = 0;
        String msg = null;
        if (!fCfg.isSupported()) {
            flags |= CConfigurationStatus.TOOLCHAIN_NOT_SUPPORTED;
            IToolChain toolChain = fCfg.getToolChain();
            String tname = toolChain != null ? toolChain.getName() : ""; //$NON-NLS-1$
            //TOFIX msg = NLS.bind(DataProviderMessages.getString("BuildConfigurationData.NoToolchainSupport"), tname); //$NON-NLS-1$
            msg = "BuildConfigurationData.NoToolchainSupport"; //$NON-NLS-1$
        }
        //        else if (ManagedBuildManager.getExtensionConfiguration(fCfg) == null) {
        //            flags |= CConfigurationStatus.SETTINGS_INVALID;
        //            //TOFIX msg = NLS.bind(DataProviderMessages.getString("BuildConfigurationData.OrphanedConfiguration"), //$NON-NLS-1$
        //            //fCfg.getId());
        //            msg = "BuildConfigurationData.OrphanedConfiguration"; //$NON-NLS-1$
        //        }

        if (flags != 0)
            return new CConfigurationStatus(Activator.getId(), flags, msg, null);

        return CConfigurationStatus.CFG_STATUS_OK;
    }

	@Override
	public void setDescription(String description) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void removeResourceData(CResourceData data) throws CoreException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public CFolderData createFolderData(IPath path, CFolderData base) throws CoreException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public CFileData createFileData(IPath path, CFileData base) throws CoreException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public CFileData createFileData(IPath path, CFolderData base, CLanguageData langData) throws CoreException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ICSourceEntry[] getSourceEntries() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setSourceEntries(ICSourceEntry[] entries) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setName(String name) {
		// TODO Auto-generated method stub
		
	}
	
	public ICfgScannerConfigBuilderInfo2Set getCfgScannerConfigInfo() {
		return cfgScannerInfo;
	}

	public void setCfgScannerConfigInfo(ICfgScannerConfigBuilderInfo2Set info) {
		cfgScannerInfo = info;
	}

	
//  @Override
//  public CFileData createFileData(IPath path, CFileData base) throws CoreException {
//      String id = ManagedBuildManager.calculateChildId(fCfg.getId(), null);
//      IFileInfo info = fCfg.createFileInfo(path, ((BuildFileData) base).getFileInfo(), id, path.lastSegment());
//      return info.getFileData();
//  }
//
//  @Override
//  public CFileData createFileData(IPath path, CFolderData base, CLanguageData baseLangData) throws CoreException {
//      String id = ManagedBuildManager.calculateChildId(fCfg.getId(), null);
//      ITool baseTool;
//      if (baseLangData instanceof BuildLanguageData) {
//          baseTool = ((BuildLanguageData) baseLangData).getTool();
//      } else {
//          baseTool = null;
//      }
//      IFileInfo info = fCfg.createFileInfo(path, ((BuildFolderData) base).getFolderInfo(), baseTool, id,
//              path.lastSegment());
//      return info.getFileData();
//  }
//
//  @Override
//  public CFolderData createFolderData(IPath path, CFolderData base) throws CoreException {
//      String id = ManagedBuildManager.calculateChildId(fCfg.getId(), null);
//      IFolderInfo folderInfo = fCfg.createFolderInfo(path, ((BuildFolderData) base).getFolderInfo(), id,
//              base.getName());
//      return folderInfo.getFolderData();
//  }
	
//  @Override
//  public ICSourceEntry[] getSourceEntries() {
//      return fCfg.getSourceEntries();
//  }
//
//  @Override
//  public void setSourceEntries(ICSourceEntry[] entries) {
//      fCfg.setSourceEntries(entries);
//  }
	
//  @Override
//  public void removeResourceData(CResourceData data) throws CoreException {
//      fCfg.removeResourceInfo(data.getPath());
//  }
//
//  @Override
//  public void setDescription(String description) {
//      fCfg.setDescription(description);
//  }
	
//  @Override
//  public void setName(String name) {
//      fCfg.setName(name);
//  }
}
