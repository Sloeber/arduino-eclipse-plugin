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

import org.eclipse.cdt.core.settings.model.extension.CFileData;
import org.eclipse.cdt.core.settings.model.extension.CLanguageData;
//import org.eclipse.cdt.managedbuilder.core.IFileInfo;
//import org.eclipse.cdt.managedbuilder.internal.core.ResourceConfiguration;
import org.eclipse.core.runtime.IPath;

import io.sloeber.autoBuild.api.IFileInfo;

public class BuildFileData extends CFileData {
    private ResourceConfiguration fFileInfo;

    public BuildFileData(IFileInfo fileInfo) {
        fFileInfo = (ResourceConfiguration) fileInfo;
    }

    public IFileInfo getFileInfo() {
        return fFileInfo;
    }

    @Override
    public IPath getPath() {
        return fFileInfo.getPath();
    }

    //	public boolean isExcluded() {
    //		return fFileInfo.isExcluded();
    //	}
    //
    //	public void setExcluded(boolean excluded) {
    //		fFileInfo.setExclude(excluded);
    //	}

    @Override
    public void setPath(IPath path) {
        fFileInfo.setPath(path);
    }

    @Override
    public String getId() {
        return fFileInfo.getId();
    }

    @Override
    public String getName() {
        return fFileInfo.getName();
    }

    public void setName(String name) {
        //		fFileInfo.setN
    }

    @Override
    public boolean isValid() {
        return fFileInfo.isValid();
    }

    @Override
    public CLanguageData getLanguageData() {
        CLanguageData datas[] = fFileInfo.getCLanguageDatas();
        if (datas.length > 0)
            return datas[0];
        return null;
    }

    @Override
    public boolean hasCustomSettings() {
        return fFileInfo.hasCustomSettings();
    }

    public void clearCachedData() {
        BuildLanguageData lData = (BuildLanguageData) getLanguageData();
        if (lData != null)
            lData.clearCachedData();
    }

}
