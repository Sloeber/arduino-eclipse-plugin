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
import org.eclipse.core.resources.IFolder;
//import org.eclipse.cdt.managedbuilder.core.IFolderInfo;
//import org.eclipse.cdt.managedbuilder.internal.core.FolderInfo;
import org.eclipse.core.runtime.IPath;

import io.sloeber.schema.api.IFolderInfo;

public class BuildFolderData extends CFolderData {
    private IFolder myFolder;
    private String myID;

    public BuildFolderData(IFolder folder) {
    	myFolder=folder;
    	myID = Integer.toString(ManagedBuildManager.getRandomNumber());
    }


    @Override
    public CLanguageData[] getLanguageDatas() {
    	return new CLanguageData[0];
      // return fFolderInfo.getCLanguageDatas();
    }

    @Override
    public IPath getPath() {
        return myFolder.getFullPath();
    }


    @Override
    public String getId() {
        return myID;
    }

    @Override
    public String getName() {
        return myFolder.toString();
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