/*******************************************************************************
 * Copyright (c) 2007, 2020 Intel Corporation and others.
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.eclipse.cdt.core.settings.model.ICLanguageSettingEntry;
import org.eclipse.cdt.core.settings.model.extension.CLanguageData;
import org.eclipse.cdt.core.settings.model.util.CDataUtil;
import org.eclipse.cdt.core.settings.model.util.IKindBasedInfo;
import org.eclipse.cdt.core.settings.model.util.KindBasedStore;

import io.sloeber.autoBuild.api.BuildException;
import io.sloeber.autoBuild.api.IBuildObject;
import io.sloeber.autoBuild.api.IConfiguration;
import io.sloeber.autoBuild.api.IInputType;
import io.sloeber.autoBuild.api.IOption;
import io.sloeber.autoBuild.api.IResourceInfo;
import io.sloeber.autoBuild.api.ITool;
import io.sloeber.autoBuild.api.IToolChain;

/**
 * This class holds the language data for managed build tool
 *
 * It current holds both the main kind => BuildEntryStorage
 * mapping as well as mappings on the currently undef'd kinds
 * (e.g. a language setting entry defined by scanner discovery
 * but later re-defined by a build system setting )
 */
public class BuildLanguageData extends CLanguageData {
    private static final IOption[] EMPTY_OPTION_ARRAY = new IOption[0];

    private final String fId;
    private ITool fTool;
    private IInputType fInputType;

    /**
     * The main kind => BuildEntryStorage store
     * The BuildEntryStorage calls back to this BuildLanguageData
     * to work out which entries are actually (un)defined.
     */
    private KindBasedStore<BuildEntryStorage> fKindToEntryStore = new KindBasedStore<>();

    /** Indicates that the option array stores have been inited */
    private volatile boolean fOptionStoreInited;
    private KindBasedStore<IOption[]> fKindToOptionArrayStore = new KindBasedStore<>();
    private KindBasedStore<IOption[]> fKindToUndefOptionArrayStore = new KindBasedStore<>();

    //	private Map fKindToEntryArrayMap = new HashMap();
    //	private ProfileInfoProvider fDiscoveredInfo;

    public BuildLanguageData(ITool tool, IInputType inType) {
        fTool = tool;
        if (inType != null) {
            //			inType = tool.getEdtableInputType(inType);
            fInputType = inType;
            if (inType.getParent() != tool)
                throw new IllegalArgumentException();
            //			IInputType extType = inType;
            //			for(;extType != null && !extType.isExtensionElement(); extType = extType.getSuperClass());
            //			String typeId;
            //			if(extType != null)
            //				typeId = extType.getId();
            //			else
            //				typeId = inType.getId();
            fId = inType.getId();//new StringBuilder(fTool.getId()).append(".").append(typeId).toString();
        } else {
            fInputType = null;
            fId = new StringBuilder(fTool.getId()).append(".").append("languagedata").toString(); //$NON-NLS-1$ //$NON-NLS-2$
        }

        //		fDiscoveredInfo = new ProfileInfoProvider(this);
    }

    private void obtainEditableInputType() {
        if (fInputType != null) {
            //			IInputType old = fInputType;
            fInputType = fTool.getEditableInputType(fInputType);
            //			if(old != fInputType){
            //				fDiscoveredInfo.checkUpdateInputType(fInputType);
            //			}
        }
    }

    @Override
    public void setEntries(int kind, ICLanguageSettingEntry entries[]) {
        BuildEntryStorage storage = getEntryStorage(kind);
        if (storage != null)
            storage.setEntries(entries);
    }

    private BuildEntryStorage getEntryStorage(int kind) {
        if (getOptionsForKind(kind).length == 0 && isToolChainDiscoveryProfile())
            return null;

        BuildEntryStorage storage = fKindToEntryStore.get(kind);
        if (storage == null) {
            storage = new BuildEntryStorage(kind, this);
            fKindToEntryStore.put(kind, storage);
        }
        return storage;
    }

    private void notifyOptionsChangeForKind(int kind) {
        fOptionStoreInited = false;
        BuildEntryStorage storage = getEntryStorage(kind);
        if (storage != null)
            storage.optionsChanged();
    }

    public void optionsChanged(int type) {
        int kind = ManagedBuildManager.optionTypeToEntryKind(type);
        if (kind == 0)
            kind = ManagedBuildManager.optionUndefTypeToEntryKind(type);

        if (kind != 0)
            notifyOptionsChangeForKind(kind);
    }

    //	private ProfileInfoProvider getDiscoveredInfoProvider(){
    //		return fDiscoveredInfo;
    //	}
    /*
    	private String getOptionValueFromEntry(ICLanguageSettingEntry entry){
    		String optValue = entry.getName();
    		if(entry.getKind() == ICLanguageSettingEntry.MACRO){
    			String macroValue = entry.getValue();
    			StringBuilder buf = new StringBuilder(optValue).append('=').append(macroValue);
    			optValue = buf.toString();
    		}
    		return optValue;
    	}
    */
    @Override
    public String getLanguageId() {
        return fInputType != null ? fInputType.getLanguageId(fTool) : null;
    }

    @Override
    public ICLanguageSettingEntry[] getEntries(int kinds) {
        List<ICLanguageSettingEntry> list = new ArrayList<>();

        if ((kinds & ICLanguageSettingEntry.INCLUDE_PATH) != 0) {
            BuildEntryStorage storage = getEntryStorage(ICLanguageSettingEntry.INCLUDE_PATH);
            if (storage != null)
                storage.getEntries(list);
        } else if ((kinds & ICLanguageSettingEntry.INCLUDE_FILE) != 0) {
            BuildEntryStorage storage = getEntryStorage(ICLanguageSettingEntry.INCLUDE_FILE);
            if (storage != null)
                storage.getEntries(list);
        } else if ((kinds & ICLanguageSettingEntry.MACRO) != 0) {
            BuildEntryStorage storage = getEntryStorage(ICLanguageSettingEntry.MACRO);
            if (storage != null)
                storage.getEntries(list);
        } else if ((kinds & ICLanguageSettingEntry.MACRO_FILE) != 0) {
            BuildEntryStorage storage = getEntryStorage(ICLanguageSettingEntry.MACRO_FILE);
            if (storage != null)
                storage.getEntries(list);
        } else if ((kinds & ICLanguageSettingEntry.LIBRARY_PATH) != 0) {
            BuildEntryStorage storage = getEntryStorage(ICLanguageSettingEntry.LIBRARY_PATH);
            if (storage != null)
                storage.getEntries(list);
        } else if ((kinds & ICLanguageSettingEntry.LIBRARY_FILE) != 0) {
            BuildEntryStorage storage = getEntryStorage(ICLanguageSettingEntry.LIBRARY_FILE);
            if (storage != null)
                storage.getEntries(list);
        }

        return list.toArray(new ICLanguageSettingEntry[list.size()]);
    }

    public void updateInputType(IInputType type) {
        fInputType = type;
    }

    @Override
    public String[] getSourceContentTypeIds() {
        if (fInputType != null) {
            return fInputType.getSourceContentTypeIds();
        }
        return null;
    }

    @Override
    public String[] getSourceExtensions() {
        return fInputType != null ? fInputType.getSourceExtensions(fTool) : fTool.getPrimaryInputExtensions();
    }

    @Override
    public int getSupportedEntryKinds() {
        KindBasedStore<IOption[]> store = getKindToOptionArrayStore();
        IKindBasedInfo<IOption[]>[] infos = store.getContents();
        int kinds = 0;
        for (int i = 0; i < infos.length; i++) {
            if (infos[i].getInfo().length > 0)
                kinds |= infos[i].getKind();
        }
        return kinds;
    }

    private KindBasedStore<IOption[]> getKindToOptionArrayStore() {
        initOptionStores();
        return fKindToOptionArrayStore;
    }

    private void initOptionStores() {
        if (!fOptionStoreInited) {
            synchronized (this) {
                if (!fOptionStoreInited) {
                    calculateKindToOptionArrayStore();
                    calculateKindToUndefOptionArrayStore();
                    fOptionStoreInited = true;
                }
            }
        }
    }

    private KindBasedStore<IOption[]> getKindToUndefOptionArrayStore() {
        initOptionStores();
        return fKindToUndefOptionArrayStore;
    }

    private void calculateKindToOptionArrayStore() {
        fKindToOptionArrayStore.clear();
        Map<Integer, List<IOption>> kindToOptionList = new HashMap<>();
        IOption options[] = fTool.getOptions();
        for (final IOption option : options) {
            try {
                Integer entryKind = ManagedBuildManager.optionTypeToEntryKind(option.getValueType());
                if (entryKind != 0) {
                    if (!kindToOptionList.containsKey(entryKind))
                        kindToOptionList.put(entryKind, new ArrayList<IOption>(3) {
                            {
                                add(option);
                            }
                        });
                    else
                        kindToOptionList.get(entryKind).add(option);
                }
            } catch (BuildException e) {
            }
        }

        IKindBasedInfo<IOption[]>[] infos = fKindToOptionArrayStore.getContents();
        for (IKindBasedInfo<IOption[]> info : infos) {
            List<IOption> list = kindToOptionList.get(info.getKind());
            if (list != null) {
                IOption[] opts = list.toArray(new IOption[list.size()]);
                info.setInfo(opts);
            } else {
                info.setInfo(EMPTY_OPTION_ARRAY);
            }
        }
    }

    private void calculateKindToUndefOptionArrayStore() {
        fKindToUndefOptionArrayStore.clear();
        Map<Integer, List<IOption>> kindToOptionList = new HashMap<>();
        IOption options[] = fTool.getOptions();
        for (final IOption option : options) {
            try {
                Integer entryKind = ManagedBuildManager.optionUndefTypeToEntryKind(option.getValueType());
                if (entryKind != 0) {
                    if (!kindToOptionList.containsKey(entryKind))
                        kindToOptionList.put(entryKind, new ArrayList<IOption>(3) {
                            {
                                add(option);
                            }
                        });
                    else
                        kindToOptionList.get(entryKind).add(option);
                }
            } catch (BuildException e) {
            }
        }

        IKindBasedInfo<IOption[]>[] infos = fKindToUndefOptionArrayStore.getContents();
        for (IKindBasedInfo<IOption[]> info : infos) {
            List<IOption> list = kindToOptionList.get(info.getKind());
            if (list != null) {
                IOption[] opts = list.toArray(new IOption[list.size()]);
                info.setInfo(opts);
            } else {
                info.setInfo(EMPTY_OPTION_ARRAY);
            }
        }
    }

    IOption[] getUndefOptionsForKind(int entryKind) {
        KindBasedStore<IOption[]> store = getKindToUndefOptionArrayStore();
        return store.get(entryKind);
    }

    IOption[] getOptionsForKind(int entryKind) {
        KindBasedStore<IOption[]> store = getKindToOptionArrayStore();
        return store.get(entryKind);
    }

    /*	private IOption[] getOptionsForType(int type){
    		Map map = getTypeToOptionArrayMap();
    		return (IOption[])map.get(Integer.valueOf(type));
    	}
    */

    @Override
    public void setLanguageId(String id) {
    	//TODO JABA Should be dead code
//        if (Objects.equals(id, fInputType.getLanguageId(fTool))) {
//            //			fInputType = fTool.getEdtableInputType(fInputType);
//            obtainEditableInputType();
//            fInputType.setLanguageIdAttribute(id);
//        }
    }

    @Override
    public String getId() {
        return fId;
    }

    @Override
    public String getName() {
        String name;
        if (fInputType == null) {
            name = fTool.getName();
            if (name == null) {
                String[] exts = getSourceExtensions();
                if (exts.length != 0) {
                    name = CDataUtil.arrayToString(exts, ","); //$NON-NLS-1$
                } else {
                    name = fTool.getId();
                }
            }
        } else {
            name = fInputType.getLanguageName(fTool);
        }
        return name;
    }

    @Override
    public boolean isValid() {
        // TODO Auto-generated method stub
        return true;
    }

    public void setName(String name) {
        // TODO Auto-generated method stub
    }

    public ITool getTool() {
        return fTool;
    }

    public IInputType getInputType() {
        return fInputType;
    }

    boolean isToolChainDiscoveryProfile() {
        return fInputType != null ? ((InputType) fInputType).getDiscoveryProfileIdAttribute() == null : true;
    }

    String getDiscoveryProfileId() {
        if (fInputType != null)
            return fInputType.getDiscoveryProfileId(fTool);
        IBuildObject bo = fTool.getParent();
        if (bo instanceof IToolChain)
            return ((IToolChain) bo).getScannerConfigDiscoveryProfileId();
        else if (bo instanceof IResourceInfo) {
            IToolChain tCh = ((ResourceConfiguration) bo).getBaseToolChain();
            if (tCh != null)
                return tCh.getScannerConfigDiscoveryProfileId();
        }
        return null;
    }

    public IConfiguration getConfiguration() {
        return fTool.getParentResourceInfo().getParent();
    }

    @Override
    public void setSourceContentTypeIds(String[] ids) {
    	//TODO JABA should be dead code
//        String[] headerIds = fInputType.getHeaderContentTypeIds();
//
//        List<String> newSrc = new ArrayList<>(ids.length);
//        List<String> newHeaders = new ArrayList<>(ids.length);
//        for (int i = 0; i < ids.length; i++) {
//            String id = ids[i];
//            int j = 0;
//            for (; j < headerIds.length; j++) {
//                if (id.equals(headerIds[j])) {
//                    newHeaders.add(id);
//                    break;
//                }
//            }
//            if (j == headerIds.length) {
//                newSrc.add(id);
//            }
//        }
//
//        String newSrcIds[] = newSrc.toArray(new String[newSrc.size()]);
//        String newHeaderIds[] = newHeaders.toArray(new String[newHeaders.size()]);
//
//        if (!Arrays.equals(newSrcIds, fInputType.getSourceContentTypeIds())) {
//            //			fInputType = fTool.getEdtableInputType(fInputType);
//            obtainEditableInputType();
//            fInputType.setSourceContentTypeIds(newSrcIds);
//        }
//
//        if (!Arrays.equals(newHeaderIds, fInputType.getHeaderContentTypeIds())) {
//            //			fInputType = fTool.getEdtableInputType(fInputType);
//            obtainEditableInputType();
//            fInputType.setHeaderContentTypeIds(newHeaderIds);
//        }

    }

    @Override
    public void setSourceExtensions(String[] exts) {
        // TODO Auto-generated method stub

    }

    void clearCachedData() {
        fKindToEntryStore.clear();
    }

    @Override
    public boolean containsDiscoveredScannerInfo() {
        //        IResourceInfo rcInfo = fTool.getParentResourceInfo();
        //        if (rcInfo instanceof FolderInfo) {
        //            return ((FolderInfo) rcInfo).containsDiscoveredScannerInfo();
        //        }
        //        return true;
        return false;
    }
}
