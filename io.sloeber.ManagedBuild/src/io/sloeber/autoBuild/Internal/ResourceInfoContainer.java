/*******************************************************************************
 * Copyright (c) 2007, 2011 Intel Corporation and others.
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

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.cdt.core.settings.model.ICSettingBase;
import org.eclipse.cdt.core.settings.model.util.IPathSettingsContainerVisitor;
import org.eclipse.cdt.core.settings.model.util.PathSettingsContainer;
//import org.eclipse.cdt.managedbuilder.core.IFileInfo;
//import org.eclipse.cdt.managedbuilder.core.IFolderInfo;
//import org.eclipse.cdt.managedbuilder.core.IResourceInfo;
import org.eclipse.core.runtime.IPath;

import io.sloeber.autoBuild.api.IFileInfo;
import io.sloeber.autoBuild.api.IFolderInfo;
import io.sloeber.autoBuild.api.IResourceInfo;

public class ResourceInfoContainer {
    private PathSettingsContainer fRcDataContainer;
    private boolean fIncludeCurrent;

    public ResourceInfoContainer(PathSettingsContainer pathSettings, boolean includeCurrent) {
        fRcDataContainer = pathSettings;
        fIncludeCurrent = includeCurrent;
    }

    public void changeCurrentPath(IPath path, boolean moveChildren) {
        fRcDataContainer.setPath(path, moveChildren);
    }

    public IPath getCurrentPath() {
        return fRcDataContainer.getPath();
    }

    public IResourceInfo getCurrentResourceInfo() {
        return (IResourceInfo) fRcDataContainer.getValue();
    }

    public IResourceInfo getResourceInfo(IPath path, boolean exactPath) {
        PathSettingsContainer cr = fRcDataContainer.getChildContainer(path, false, exactPath);
        if (cr != null)
            return (IResourceInfo) cr.getValue();
        return null;
    }

    public IResourceInfo[] getResourceInfos(Class<? extends IResourceInfo> clazz) {
        return getResourceInfos(ICSettingBase.SETTING_FILE | ICSettingBase.SETTING_FOLDER, clazz);
    }

    public IResourceInfo[] getResourceInfos() {
        return getResourceInfos(ICSettingBase.SETTING_FILE | ICSettingBase.SETTING_FOLDER);
    }

    public IResourceInfo[] getResourceInfos(final int kind) {
        return getResourceInfos(kind, IResourceInfo.class);
    }

    public IResourceInfo[] getResourceInfos(int kind, Class<? extends IResourceInfo> clazz) {
        List<IResourceInfo> list = getRcInfoList(kind);

        IResourceInfo datas[] = (IResourceInfo[]) Array.newInstance(clazz, list.size());

        return list.toArray(datas);
    }

    public IResourceInfo[] getDirectChildResourceInfos() {
        PathSettingsContainer[] children = fRcDataContainer.getDirectChildren();

        IResourceInfo datas[] = new IResourceInfo[children.length];

        for (int i = 0; i < datas.length; i++) {
            datas[i] = (IResourceInfo) children[i].getValue();
        }

        return datas;
    }

    public List<IResourceInfo> getRcInfoList(final int kind) {
        return getRcInfoList(kind, fIncludeCurrent);
    }

    public List<IResourceInfo> getRcInfoList(final int kind, final boolean includeCurrent) {
        final List<IResourceInfo> list = new ArrayList<>();
        fRcDataContainer.accept(new IPathSettingsContainerVisitor() {

            @Override
            public boolean visit(PathSettingsContainer container) {
                if (includeCurrent || container != fRcDataContainer) {
                    IResourceInfo data = (IResourceInfo) container.getValue();
                    if ((data.getKind() & kind) == data.getKind())
                        list.add(data);
                }
                return true;
            }
        });

        return list;
    }

    public IResourceInfo getResourceInfo(IPath path, boolean exactPath, int kind) {
        IResourceInfo data = getResourceInfo(path, exactPath);
        if (data != null && (data.getKind() & kind) == data.getKind())
            return data;
        return null;
    }

    public void removeResourceInfo(IPath path) {
        fRcDataContainer.removeChildContainer(path);
    }

    public void addResourceInfo(IResourceInfo data) {
        PathSettingsContainer cr = fRcDataContainer.getChildContainer(data.getPath(), true, true);
        cr.setValue(data);
    }

    public IFileInfo getFileInfo(IPath path) {
        return (IFileInfo) getResourceInfo(path, true, ICSettingBase.SETTING_FILE);
    }

    public IFolderInfo getFolderInfo(IPath path) {
        return (IFolderInfo) getResourceInfo(path, true, ICSettingBase.SETTING_FOLDER);
    }
}
