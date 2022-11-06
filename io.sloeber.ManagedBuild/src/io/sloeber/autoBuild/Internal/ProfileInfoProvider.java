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

import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

//import org.eclipse.cdt.build.core.scannerconfig.CfgInfoContext;
//import org.eclipse.cdt.build.internal.core.scannerconfig.CfgDiscoveredPathManager;
import org.eclipse.cdt.core.settings.model.ICLanguageSettingEntry;
import org.eclipse.cdt.core.settings.model.util.CDataUtil;
import org.eclipse.cdt.make.core.scannerconfig.PathInfo;
import org.eclipse.core.runtime.IPath;

public class ProfileInfoProvider {
    private static ProfileInfoProvider fInstance;
    //	private BuildLanguageData fLlanguageData;
    //	private IProject fProject;
    //    private CfgInfoContext fContext;
    // private CfgDiscoveredPathManager fMngr;

    private ProfileInfoProvider() {
        //		fLlanguageData = lData;
        //		IResourceInfo rcInfo = lData.getTool().getParentResourceInfo();
        //		fContext = new CfgInfoContext(rcInfo, lData.getTool(), lData.getInputType());
        //   fMngr = CfgDiscoveredPathManager.getInstance();
        //		IResource rc = rcInfo.getParent().getOwner();
        //		fProject = rc != null ? rc.getProject() : null;
    }

    public static ProfileInfoProvider getInstance() {
        if (fInstance == null)
            fInstance = new ProfileInfoProvider();
        return fInstance;
    }

    //	void checkUpdateInputType(IInputType inType){
    //		if(inType != fContext.getInputType()){
    //			fContext = new CfgInfoContext(fContext.getResourceInfo(), fContext.getTool(), inType);
    //		}
    //	}

    public ICLanguageSettingEntry[] getEntryValues(BuildLanguageData lData, int kind, int flags) {
        //        IResourceInfo rcInfo = lData.getTool().getParentResourceInfo();
        //        IResource rc = rcInfo.getParent().getOwner();
        //        IProject project = rc != null ? rc.getProject() : null;
        //
        //        if (project != null) {
        //            try {
        //                CfgInfoContext context = new CfgInfoContext(rcInfo, lData.getTool(), lData.getInputType());
        //                PathInfo info = fMngr.getDiscoveredInfo(project, context);
        //                if (info != null) {
        //                    return entriesForKind(kind, flags, info);
        //                }
        //            } catch (CoreException e) {
        //                Activator.log(e);
        //            }
        //        }
        return new ICLanguageSettingEntry[0];
    }

    private ICLanguageSettingEntry[] entriesForKind(int kind, int flags, PathInfo info) {
        switch (kind) {
        case ICLanguageSettingEntry.INCLUDE_PATH:
            ICLanguageSettingEntry[] incPaths = calculateEntries(kind, flags, info.getIncludePaths());
            IPath[] quotedPaths = info.getQuoteIncludePaths();
            if (quotedPaths.length != 0) {
                if (incPaths.length != 0) {
                    ICLanguageSettingEntry quotedEntries[] = calculateEntries(kind, flags, quotedPaths);
                    ICLanguageSettingEntry[] tmp = new ICLanguageSettingEntry[incPaths.length + quotedEntries.length];
                    System.arraycopy(incPaths, 0, tmp, 0, incPaths.length);
                    System.arraycopy(quotedEntries, 0, tmp, incPaths.length, quotedEntries.length);
                    incPaths = tmp;
                } else {
                    incPaths = calculateEntries(kind, flags, quotedPaths);
                }
            }
            return incPaths;
        case ICLanguageSettingEntry.MACRO:
            return calculateEntries(kind, flags, info.getSymbols());
        case ICLanguageSettingEntry.MACRO_FILE:
            return calculateEntries(kind, flags, info.getMacroFiles());
        case ICLanguageSettingEntry.INCLUDE_FILE:
            return calculateEntries(kind, flags, info.getIncludeFiles());
        }
        return new ICLanguageSettingEntry[0];
    }

    private ICLanguageSettingEntry[] calculateEntries(int kind, int flags, Map<String, String> map) {
        ICLanguageSettingEntry entries[] = new ICLanguageSettingEntry[map.size()];
        int num = 0;
        Set<Entry<String, String>> entrySet = map.entrySet();
        for (Entry<String, String> entry : entrySet) {
            String name = entry.getKey();
            String value = entry.getValue();
            entries[num++] = (ICLanguageSettingEntry) CDataUtil.createEntry(kind, name, value, null, flags);
        }
        return entries;
    }

    //	private ICLanguageSettingEntry[] calculateEntries(int kind, int flags, String[] values){
    //		ICLanguageSettingEntry entries[] = new ICLanguageSettingEntry[values.length];
    //		for(int i = 0; i < values.length; i++){
    //			String name = values[i];
    //			entries[i] = (ICLanguageSettingEntry)CDataUtil.createEntry(kind, name, null, null, flags);
    //		}
    //		return entries;
    //	}

    private ICLanguageSettingEntry[] calculateEntries(int kind, int flags, IPath[] values) {
        ICLanguageSettingEntry entries[] = new ICLanguageSettingEntry[values.length];
        for (int i = 0; i < values.length; i++) {
            String name = values[i].toString();
            entries[i] = (ICLanguageSettingEntry) CDataUtil.createEntry(kind, name, null, null, flags);
        }
        return entries;
    }

    //	private ICLanguageSettingEntry[] calculateEntries(int kind, int flags, List list){
    //		ICLanguageSettingEntry entries[] = new ICLanguageSettingEntry[list.size()];
    //		int num = 0;
    //		for(Iterator iter = list.iterator(); iter.hasNext();){
    //			String name = (String)iter.next();
    //			entries[num++] = (ICLanguageSettingEntry)CDataUtil.createEntry(kind, name, null, null, flags);
    //		}
    //		return entries;
    //	}
}
