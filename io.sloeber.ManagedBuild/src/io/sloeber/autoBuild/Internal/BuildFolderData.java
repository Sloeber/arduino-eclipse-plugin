/*******************************************************************************
 * Copyright (c) 2007, 2009 Intel Corporation and others.
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

import org.eclipse.cdt.core.settings.model.extension.CFolderData;
import org.eclipse.cdt.core.settings.model.extension.CLanguageData;
//import org.eclipse.cdt.managedbuilder.core.IFolderInfo;
//import org.eclipse.cdt.managedbuilder.internal.core.FolderInfo;
import org.eclipse.core.runtime.IPath;

import io.sloeber.autoBuild.api.IFolderInfo;

public class BuildFolderData extends CFolderData {
    private FolderInfo fFolderInfo;

    public BuildFolderData(IFolderInfo folderInfo) {
        fFolderInfo = (FolderInfo) folderInfo;
    }

    public IFolderInfo getFolderInfo() {
        return fFolderInfo;
    }

    @Override
    public CLanguageData[] getLanguageDatas() {
    	return new CLanguageData[0];
      // return fFolderInfo.getCLanguageDatas();
    }

    @Override
    public IPath getPath() {
        return fFolderInfo.getPath();
    }


    @Override
    public String getId() {
        return fFolderInfo.getId();
    }

    @Override
    public String getName() {
        return fFolderInfo.getName();
    }

    public void setName(String name) {
        // TODO Auto-generated method stub
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public CLanguageData createLanguageDataForContentTypes(String languageId, String[] typesIds) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public CLanguageData createLanguageDataForExtensions(String languageId, String[] extensions) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean hasCustomSettings() {
        return true;
    }

    public void clearCachedData() {
        CLanguageData[] lDatas = getLanguageDatas();
        for (int i = 0; i < lDatas.length; i++) {
            ((BuildLanguageData) lDatas[i]).clearCachedData();
        }
    }

    private boolean myContainsScannerInfo=true;
    public boolean containsScannerInfo() {
        return myContainsScannerInfo;
    }

    public void setContainsDiscoveredScannerInfo(boolean contains) {
    	myContainsScannerInfo=contains;
    }

	@Override
	public void setPath(IPath path) {
		// TODO Auto-generated method stub
		
	}

}
