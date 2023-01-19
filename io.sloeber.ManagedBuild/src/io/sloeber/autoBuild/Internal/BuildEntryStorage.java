/*******************************************************************************
 * Copyright (c) 2007, 2016 Intel Corporation and others.
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
 * James Blackburn (Broadcom Corp.)
 *******************************************************************************/
package io.sloeber.autoBuild.Internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.eclipse.cdt.core.cdtvariables.CdtVariableException;
import org.eclipse.cdt.core.settings.model.ICLanguageSettingEntry;
import org.eclipse.cdt.core.settings.model.ICLanguageSettingPathEntry;
import org.eclipse.cdt.core.settings.model.ICLibraryFileEntry;
import org.eclipse.cdt.core.settings.model.ICSettingEntry;
import org.eclipse.cdt.core.settings.model.util.AbstractEntryStorage;
import org.eclipse.cdt.core.settings.model.util.CDataUtil;
import org.eclipse.cdt.core.settings.model.util.SettingsSet;
import org.eclipse.cdt.core.settings.model.util.SettingsSet.EntryInfo;
import org.eclipse.cdt.core.settings.model.util.SettingsSet.SettingLevel;
import org.eclipse.cdt.make.core.scannerconfig.PathInfo;
//import org.eclipse.cdt.managedbuilder.core.BuildException;
//import org.eclipse.cdt.managedbuilder.core.IConfiguration;
//import org.eclipse.cdt.managedbuilder.core.IEnvVarBuildPath;
//import org.eclipse.cdt.managedbuilder.core.IOption;
//import org.eclipse.cdt.managedbuilder.core.IOptionPathConverter;
//import org.eclipse.cdt.managedbuilder.core.IResourceInfo;
//import org.eclipse.cdt.managedbuilder.core.IReverseOptionPathConverter;
//import org.eclipse.cdt.managedbuilder.core.ITool;
//import org.eclipse.cdt.managedbuilder.core.ManagedBuildManager;
//import org.eclipse.cdt.managedbuilder.core.ManagedBuilderCorePlugin;
//import org.eclipse.cdt.managedbuilder.core.OptionStringValue;
//import org.eclipse.cdt.managedbuilder.envvar.IEnvironmentVariableProvider;
//import org.eclipse.cdt.managedbuilder.internal.core.Option;
//import org.eclipse.cdt.managedbuilder.internal.macros.DefaultMacroContextInfo;
//import org.eclipse.cdt.managedbuilder.internal.macros.OptionContextData;
//import org.eclipse.cdt.managedbuilder.macros.IBuildMacroProvider;
import org.eclipse.cdt.utils.cdtvariables.CdtVariableResolver;
import org.eclipse.cdt.utils.cdtvariables.SupplierBasedCdtVariableSubstitutor;
import org.eclipse.core.runtime.IPath;

import io.sloeber.autoBuild.api.BuildException;
import io.sloeber.autoBuild.api.IBuildMacroProvider;
import io.sloeber.autoBuild.api.IEnvVarBuildPath;
import io.sloeber.autoBuild.api.IEnvironmentVariableProvider;
import io.sloeber.autoBuild.api.OptionStringValue;
import io.sloeber.autoBuild.core.Activator;
import io.sloeber.schema.api.IOption;
import io.sloeber.schema.api.IResourceInfo;
import io.sloeber.schema.api.ITool;
import io.sloeber.schema.internal.Option;

/**
 * BuildEntryStorage has a handle back to the BuildLanguageData
 * to allow checking on which language entries are actually defined.
 */
public class BuildEntryStorage extends AbstractEntryStorage {
    private static final int USER_ENTRIES_LEVEL = 0;
    private static final int ENV_ENTRIES_LEVEL = 1;
    private static final int DISCOVERY_ENTRIES_LEVEL = 2;

    private BuildLanguageData fLangData;
    private String fBuildDirName;

    private static class UserEntryInfo {
        private ICLanguageSettingEntry fEntry;
        private OptionStringValue fOriginalValue;
        private OptionStringValue fBsResolvedValue;
        private List<UserEntryInfo> fSequense;

        UserEntryInfo(ICLanguageSettingEntry entry, OptionStringValue originalValue, OptionStringValue bsResolvedValue,
                List<UserEntryInfo> sequense) {
            fEntry = entry;
            fOriginalValue = originalValue;
            fBsResolvedValue = bsResolvedValue;
            fSequense = sequense;
            if (sequense != null)
                sequense.add(this);
        }
    }

    private static class EmptyEntryInfo {
        private OptionStringValue fOriginalValue;
        private int fPosition;

        EmptyEntryInfo(OptionStringValue value, int position) {
            fOriginalValue = value;
            fPosition = position;
        }
    }

    public BuildEntryStorage(int kind, BuildLanguageData lData) {
        super(kind);
        fLangData = lData;
    }

    @Override
    protected SettingsSet createEmptySettings() {
        SettingsSet settings = new SettingsSet(3);
        SettingLevel levels[] = settings.getLevels();

        boolean override = isDiscoveredEntriesOverridable();
        int readOnlyFlag = override ? 0 : ICSettingEntry.READONLY;
        levels[USER_ENTRIES_LEVEL].setFlagsToClear(ICSettingEntry.READONLY | ICSettingEntry.BUILTIN);
        levels[USER_ENTRIES_LEVEL].setFlagsToSet(0);
        levels[USER_ENTRIES_LEVEL].setReadOnly(false);
        levels[USER_ENTRIES_LEVEL].setOverrideSupported(override);

        levels[ENV_ENTRIES_LEVEL].setFlagsToClear(ICSettingEntry.BUILTIN);
        levels[ENV_ENTRIES_LEVEL].setFlagsToSet(readOnlyFlag | ICSettingEntry.RESOLVED);
        levels[ENV_ENTRIES_LEVEL].setReadOnly(true);
        levels[ENV_ENTRIES_LEVEL].setOverrideSupported(false);

        levels[DISCOVERY_ENTRIES_LEVEL].setFlagsToClear(0);
        levels[DISCOVERY_ENTRIES_LEVEL].setFlagsToSet(readOnlyFlag | ICSettingEntry.BUILTIN | ICSettingEntry.RESOLVED);
        levels[DISCOVERY_ENTRIES_LEVEL].setReadOnly(true);
        levels[DISCOVERY_ENTRIES_LEVEL].setOverrideSupported(false);

        return settings;
    }

    private boolean isDiscoveredEntriesOverridable() {
        return fLangData.getUndefOptionsForKind(getKind()).length != 0;
    }

    private String getBuildDitName() {
        if (fBuildDirName == null) {
            fBuildDirName = fLangData.getConfiguration().getName();
        }
        return fBuildDirName;
    }

    @Override
    protected void obtainEntriesFromLevel(int levelNum, SettingLevel level) {
        switch (levelNum) {
        case USER_ENTRIES_LEVEL:
            if (level == null) {
                restoreDefaults();
            } else {
                EntryInfo infos[] = level.getInfos();
                UserEntryInfo[] userInfos = new UserEntryInfo[infos.length];
                for (int i = 0; i < infos.length; i++) {
                    UserEntryInfo uei = (UserEntryInfo) infos[i].getCustomInfo();
                    if (uei == null)
                        uei = new UserEntryInfo(infos[i].getEntry(), null, null, null);
                    userInfos[i] = uei;
                }
                @SuppressWarnings("unchecked")
                List<EmptyEntryInfo> context = (List<EmptyEntryInfo>) level.getContext();
                setUserEntries(userInfos, context);
                setUserUndefinedStringSet(level.containsOverrideInfo() ? level.getOverrideSet() : null);
            }
            break;
        }
    }

    private void restoreDefaults() {
    	//TOFIX JABA
//        ITool tool = fLangData.getTool();
//        for (IOption option : fLangData.getOptionsForKind(getKind())) {
//            if (option.getParent() == tool) {
//                tool.removeOption(option);
//            }
//        }
//
//        for (IOption option : fLangData.getUndefOptionsForKind(getKind())) {
//            if (option.getParent() == tool) {
//                tool.removeOption(option);
//            }
//        }
    }

    @Override
    protected void putEntriesToLevel(int levelNum, SettingLevel level) {
        switch (levelNum) {
        case USER_ENTRIES_LEVEL:
            List<EmptyEntryInfo> emptyEntryInfos = new ArrayList<>();
            for (UserEntryInfo userEntry : getUserEntries(level.getFlags(0), true, emptyEntryInfos)) {
                level.addEntry(userEntry.fEntry, userEntry);
            }
            level.addOverrideNameSet(getUserUndefinedStringSet());
            if (emptyEntryInfos.size() != 0)
                level.setContext(emptyEntryInfos);
            break;
        case ENV_ENTRIES_LEVEL:
            ICLanguageSettingEntry[] envEntries = getEnvEntries(level.getFlags(0));
            level.addEntries(envEntries);
            break;
        case DISCOVERY_ENTRIES_LEVEL:
            ICLanguageSettingEntry[] discoveredEntries = getDiscoveredEntries(level.getFlags(0));
            level.addEntries(discoveredEntries);
            break;
        }
    }

    /**
     * @return scanner discovered entries (level 2)
     */
    private ICLanguageSettingEntry[] getDiscoveredEntries(int flags) {
        ICLanguageSettingEntry[] entries = ProfileInfoProvider.getInstance().getEntryValues(fLangData, getKind(),
                flags);
        if (entries == null || entries.length == 0) {
            UserEntryInfo[] infos = getUserEntries(flags, false, null);
            if (infos.length != 0) {
                entries = new ICLanguageSettingEntry[infos.length];
                for (int i = 0; i < entries.length; i++) {
                    entries[i] = infos[i].fEntry;
                }
            }
        }
        return entries;
    }

    private SupplierBasedCdtVariableSubstitutor createSubstitutor(IOption option, boolean bsVarsOnly) {
        OptionContextData ocd = new OptionContextData(option, fLangData.getTool());
        DefaultMacroContextInfo ci = new DefaultMacroContextInfo(IBuildMacroProvider.CONTEXT_OPTION, ocd);

        //        return bsVarsOnly ? new BuildSystemSpecificVariableSubstitutor(ci)
        //                : new SupplierBasedCdtVariableSubstitutor(ci, "", " "); //$NON-NLS-1$ //$NON-NLS-2$
        return new SupplierBasedCdtVariableSubstitutor(ci, "", " ");

    }

    /**
     * Return user entries (level 0)
     *
     * The UserEntryInfo[] is an array of all user entries from all the options
     * applicable to the language setting entry kind.
     * 
     * @param emptyValuesInfos
     *            list to which unresolved entries are added
     * @return UserEntryInfo[] (never null)
     */
    private UserEntryInfo[] getUserEntries(int flags, boolean usr, List<EmptyEntryInfo> emptyValuesInfos) {
        IOption options[] = fLangData.getOptionsForKind(getKind());
        if (options.length > 0) {
            List<UserEntryInfo> entryList = new ArrayList<>();
            for (IOption opt : options) {
                Option option = (Option) opt;
                try {
                    @SuppressWarnings("unchecked")
                    List<OptionStringValue> list = usr ? (List<OptionStringValue>) option.getExactValue()
                            : (List<OptionStringValue>) option.getExactBuiltinsList();
                    if (list != null) {
                        SupplierBasedCdtVariableSubstitutor subst = createSubstitutor(option, false);
                        SupplierBasedCdtVariableSubstitutor bSVarsSubst = createSubstitutor(option, true);
                        for (int j = 0; j < list.size(); j++) {
                            OptionStringValue ve = list.get(j);
                            OptionStringValue[] rVes = resolve(ve, option, bSVarsSubst);
                            if (rVes.length == 0) {
                                // If not resolved, add EmptyEntryInfo based off the value entry
                                if (emptyValuesInfos != null) {
                                    emptyValuesInfos.add(new EmptyEntryInfo(ve, j));
                                }
                            } else {
                                // If resolved, add each resolved entry as a separate UserEntryInfo
                                boolean isMultiple = rVes.length > 1;
                                List<UserEntryInfo> sequense = isMultiple ? new ArrayList<>(rVes.length) : null;
                                for (OptionStringValue rVe : rVes) {
                                    ICLanguageSettingEntry entry = createUserEntry(option, rVe, flags, subst);
                                    entryList.add(new UserEntryInfo(entry, ve, rVe, sequense));
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    Activator.log(e);
                }
            }

            return entryList.toArray(new UserEntryInfo[entryList.size()]);
        }
        return new UserEntryInfo[0];
    }

    //	private static OptionStringValue stripQuotes(OptionStringValue ov) {
    //		String value = ov.getValue();
    //		value = stripQuotes(value, true);
    //		if (value != null) {
    //			value = value.substring(1, value.length() - 1);
    //			ov = substituteValue(ov, value);
    //		}
    //		return ov;
    //	}

    private static String stripQuotes(String value, boolean nullIfNone) {
        if (value.indexOf('"') == 0 && value.lastIndexOf('"') == value.length() - 1 && value.length() != 1) {
            return value.substring(1, value.length() - 1);
        }
        return nullIfNone ? null : value;

    }

    private static OptionStringValue substituteValue(OptionStringValue ov, String value) {
        return new OptionStringValue(value, ov.isBuiltIn(), ov.getSourceAttachmentPath(),
                ov.getSourceAttachmentRootPath(), ov.getSourceAttachmentPrefixMapping());
    }

    private HashSet<String> getUserUndefinedStringSet() {
        HashSet<String> set = null;
        for (IOption option : fLangData.getUndefOptionsForKind(getKind())) {
            @SuppressWarnings("unchecked")
            List<String> list = (List<String>) option.getValue();
            if (list.size() != 0) {
                if (set == null)
                    set = new HashSet<>();
                set.addAll(list);
            }
        }
        return set;
    }

    /**
     * Makes non-absolute paths relative to the build directory
     */
    private PathInfo fromBuildToProj(PathInfo info) {
        return info;
        //        if (info.isAbsolute())
        //            return info;
        //
        //        Path path = new Path(info.getUnresolvedPath());
        //        String projPath;
        //        if (path.segmentCount() != 0 && "..".equals(path.segment(0))) { //$NON-NLS-1$
        //            projPath = path.removeFirstSegments(1).toString();
        //        } else {
        //            StringBuilder buf = new StringBuilder();
        //            buf.append(getBuildDitName()).append('/').append(info.getUnresolvedPath());
        //            projPath = buf.toString();
        //        }
        //        return new PathInfo(projPath, info.isWorkspacePath(), info.getSubstitutor());
    }

    private PathInfo fromProjToBuild(PathInfo info) {
        return info;
        //        if (info.isAbsolute())
        //            return info;
        //
        //        Path path = new Path(info.getUnresolvedPath());
        //        String projPath;
        //        if (path.segmentCount() != 0 && getBuildDitName().equals(path.segment(0))) {
        //            projPath = path.removeFirstSegments(1).toString();
        //        } else {
        //            StringBuilder buf = new StringBuilder();
        //            buf.append("../").append(info.getUnresolvedPath()); //$NON-NLS-1$
        //            projPath = buf.toString();
        //        }
        //        return new PathInfo(projPath, info.isWorkspacePath(), info.getSubstitutor());
    }

    //	private String[] resolve(String v, IOption option, IPath[] buildLocation) {
    //	}

    private String[] resolve(String v, IOption option, SupplierBasedCdtVariableSubstitutor sub) {
        try {
            return CdtVariableResolver.resolveToStringList(v, sub);
        } catch (CdtVariableException e) {
            Activator.log(e);
        }
        return new String[0];
    }

    private OptionStringValue[] resolve(OptionStringValue ov, IOption option, SupplierBasedCdtVariableSubstitutor sub) {
        String value = ov.getValue();
        value = stripQuotes(value, false);
        String[] rValues = resolve(value, option, sub);
        OptionStringValue[] result = new OptionStringValue[rValues.length];
        for (int i = 0; i < result.length; i++) {
            result[i] = substituteValue(ov, stripQuotes(rValues[i], false));
        }
        return result;
    }

    /**
     * @return env entries (level 1)
     */
    private ICLanguageSettingEntry[] getEnvEntries(int flags) {
        String paths[] = null;
        int kind = getKind();
        switch (kind) {
        case ICSettingEntry.INCLUDE_PATH: {
            IEnvironmentVariableProvider provider = ManagedBuildManager.getEnvironmentVariableProvider();
            paths = provider.getBuildPaths(fLangData.getConfiguration(), IEnvVarBuildPath.BUILDPATH_INCLUDE);
        }
            break;
        case ICSettingEntry.LIBRARY_PATH: {
            IEnvironmentVariableProvider provider = ManagedBuildManager.getEnvironmentVariableProvider();
            paths = provider.getBuildPaths(fLangData.getConfiguration(), IEnvVarBuildPath.BUILDPATH_LIBRARY);
        }
            break;
        }

        if (paths != null && paths.length != 0) {
            ICLanguageSettingEntry entries[] = new ICLanguageSettingEntry[paths.length];
            for (int i = 0; i < paths.length; i++) {
                entries[i] = (ICLanguageSettingEntry) CDataUtil.createEntry(kind, paths[i].toString(), null, null,
                        flags);
            }

            return entries;
        }
        return new ICLanguageSettingEntry[0];
    }

    /**
     * Create an ICLanguageSettingEntry based on the passed in Option
     */
    private ICLanguageSettingEntry createUserEntry(Option option, OptionStringValue optionValue, int flags,
            SupplierBasedCdtVariableSubstitutor subst) {
        return null;
        //        final int kind = getKind();
        //
        //        if (kind == ICSettingEntry.MACRO) {
        //            String nv[] = macroNameValueFromValue(optionValue.getValue());
        //            return CDataUtil.createCMacroEntry(nv[0], nv[1], flags);
        //        }
        //
        //        IPath srcPath = null, srcRootPath = null, srcPrefixMapping = null;
        //
        //        IOptionPathConverter optionPathConverter = fLangData.getTool().getOptionPathConverter();
        //        // Create a PathInfo entry representing the optionValue
        //        PathInfo pInfo = optionPathValueToEntry(optionValue.getValue(), subst);
        //
        //        if (pInfo.isWorkspacePath()) {
        //            flags |= ICSettingEntry.VALUE_WORKSPACE_PATH;
        //        } else if (optionPathConverter != null) {
        //            IPath path = optionPathConverter.convertToPlatformLocation(pInfo.getUnresolvedPath(), option,
        //                    fLangData.getTool());
        //            if (path != null) {
        //                pInfo = new PathInfo(path.toString(), false, subst);
        //            }
        //        }
        //
        //        // make non absolute paths relative to the build directory
        //        if (getOptionType(option) != IOption.LIBRARIES) {
        //            pInfo = fromBuildToProj(pInfo);
        //        } else {
        //            // The IOption.LIBRARIES type is morphed to => ICSettingEntyr#LIBRARY_FILE
        //            // It *isn't* a workspace path!
        //            flags &= ~ICSettingEntry.VALUE_WORKSPACE_PATH;
        //            pInfo = new PathInfo(optionValue.getValue(), false, subst);
        //        }
        //
        //        // Library files are special, they potentially know about their source Prefix & Root
        //        // The build system has two different types for storing Library Files: IOption.LIBRARIES
        //        //  && IOption.LIBRARY_FILES.  We map both of these to a CLibraryFileEntry, and mark the
        //        //  difference with a flag isRawEntry Type.
        //        if (kind == ICSettingEntry.LIBRARY_FILE) {
        //            // Handle source types
        //            String tmp = optionValue.getSourceAttachmentPath();
        //            if (tmp != null)
        //                srcPath = new Path(tmp);
        //            tmp = optionValue.getSourceAttachmentRootPath();
        //            if (tmp != null)
        //                srcRootPath = new Path(tmp);
        //            tmp = optionValue.getSourceAttachmentPrefixMapping();
        //            if (tmp != null)
        //                srcPrefixMapping = new Path(tmp);
        //        }
        //
        //        return (ICLanguageSettingEntry) CDataUtil.createEntry(kind, pInfo.getUnresolvedPath(), null, null, flags,
        //                srcPath, srcRootPath, srcPrefixMapping);
    }

    private OptionStringValue createOptionValue(IOption option, UserEntryInfo info,
            SupplierBasedCdtVariableSubstitutor subst) {
        if (info.fOriginalValue != null)
            return info.fOriginalValue;

        return entryValueToOption(option, info.fEntry, subst);
    }

    private OptionStringValue entryValueToOption(IOption option, ICLanguageSettingEntry entry,
            SupplierBasedCdtVariableSubstitutor subst) {
        String optionValue = entryValueToOptionStringValue(option, entry, subst);
        if (entry.getKind() == ICSettingEntry.LIBRARY_FILE) {
            ICLibraryFileEntry libFile = (ICLibraryFileEntry) entry;
            return new OptionStringValue(optionValue, false, pathToString(libFile.getSourceAttachmentPath()),
                    pathToString(libFile.getSourceAttachmentRootPath()),
                    pathToString(libFile.getSourceAttachmentPrefixMapping()));
        }
        return new OptionStringValue(optionValue);
    }

    private static String pathToString(IPath path) {
        return path != null ? path.toString() : null;
    }

    private String entryValueToOptionStringValue(IOption option, ICLanguageSettingEntry entry,
            SupplierBasedCdtVariableSubstitutor subst) {
        return null;
        //        String result;
        //        boolean checkQuote = true;
        //        if (entry.getKind() == ICSettingEntry.MACRO && entry.getValue().length() > 0) {
        //            result = new StringBuilder(entry.getName()).append('=').append(entry.getValue()).toString();
        //        } else if (entry instanceof ICLanguageSettingPathEntry) {
        //            IOptionPathConverter converter = fLangData.getTool().getOptionPathConverter();
        //            if (converter instanceof IReverseOptionPathConverter) {
        //                result = ((IReverseOptionPathConverter) converter).convertToOptionValue(entry, option,
        //                        fLangData.getTool());
        //                checkQuote = false;
        //            } else {
        //                ICLanguageSettingPathEntry pathEntry = (ICLanguageSettingPathEntry) entry;
        //                result = doConvertToOptionValue(option, pathEntry, subst);
        //            }
        //        } else {
        //            result = entry.getName();
        //        }
        //
        //        if (checkQuote) {
        //            result = doubleQuotePath(result, false);
        //        }
        //        return result;
    }

    private String doConvertToOptionValue(IOption option, ICLanguageSettingPathEntry pathEntry,
            SupplierBasedCdtVariableSubstitutor subst) {
        return null;
        //        boolean isWsp = pathEntry.isValueWorkspacePath();
        //        PathInfo pInfo = new PathInfo(pathEntry.getName(), isWsp, subst);
        //        String result;
        //        if (isWsp) {
        //            if (!pInfo.isAbsolute()) {
        //                IConfiguration cfg = fLangData.getConfiguration();
        //                IResource rc = cfg.getOwner();
        //                if (rc != null) {
        //                    IProject proj = rc.getProject();
        //                    String path = pInfo.getUnresolvedPath();
        //                    IPath p = proj.getFullPath().append(path);
        //                    result = p.toString();
        //                } else {
        //                    result = pathEntry.getName();
        //                }
        //            } else {
        //                result = pathEntry.getName();
        //            }
        //
        //            result = ManagedBuildManager.fullPathToLocation(result);
        //        } else {
        //            // Persisting to the LIBRARIES option type doens't require path translation
        //            // as this is just the name of a library, not the path to it...
        //            if (getOptionType(option) != IOption.LIBRARIES)
        //                pInfo = fromProjToBuild(pInfo);
        //            result = pInfo.getUnresolvedPath();
        //        }
        //
        //        return result;
    }

    /**
     * @return the option type for the option
     */
    private static int getOptionType(IOption option) {
        try {
            return option.getValueType();
        } catch (BuildException e) {
            Activator.log(e);
        }
        return IOption.STRING;
    }

    private static String doubleQuotePath(String pathName, boolean nullIfNone) {
        /* Trim */
        pathName = pathName.trim();

        /* Check if path is already double-quoted */
        boolean bStartsWithQuote = pathName.indexOf('"') == 0;
        boolean bEndsWithQuote = pathName.lastIndexOf('"') == pathName.length() - 1;

        boolean quoted = false;

        /* Check for spaces, backslashes or macros */
        int i = pathName.indexOf(' ') + pathName.indexOf('\\') + pathName.indexOf("${"); //$NON-NLS-1$

        /* If indexOf didn't fail all three times, double-quote path */
        if (i != -3) {
            if (!bStartsWithQuote) {
                pathName = "\"" + pathName; //$NON-NLS-1$
                quoted = true;
            }
            if (!bEndsWithQuote) {
                pathName = pathName + "\""; //$NON-NLS-1$
                quoted = true;
            }
        }

        if (quoted)
            return pathName;
        return nullIfNone ? null : pathName;
    }

    public static String[] macroNameValueFromValue(String value) {
        String nv[] = new String[2];
        int index = value.indexOf('=');
        if (index > 0) {
            nv[0] = value.substring(0, index);
            nv[1] = value.substring(index + 1);
        } else {
            nv[0] = value;
            nv[1] = ""; //$NON-NLS-1$
        }
        return nv;
    }

    //	private static Object[] optionPathValueToEntry(String value) {
    //		String wspPath = ManagedBuildManager.locationToFullPath(value);
    //		if (wspPath != null)
    //			return new Object[] { wspPath, Boolean.valueOf(true) };
    //		return new Object[] { value, Boolean.valueOf(false) };
    //	}

    /**
     * Returns a string representing the workspace relative path with
     * ${workspace_loc: stripped
     * or null if the String path doesn't contain workspace_loc
     * 
     * @param path
     *            String path to have workspace_loc removed
     * @return workspace path or null
     */
    private static String locationToFullPath(String path) {
        path = path.trim();
        // A path starting from "${workspace_loc}" is always a filesystem path.
        // Only ${workspace_loc:/thing} is treated here as workspace path
        if (!path.startsWith("${workspace_loc:")) //$NON-NLS-1$
            return null;

        int index = -1;
        int depth = 1;
        for (int i = "${workspace_loc:".length(); i < path.length(); i++) { //$NON-NLS-1$
            char c = path.charAt(i);
            if (c == '}') {
                depth--;
                if (depth < 1) {
                    index = i;
                    break;
                }
            } else if (c == '{' && path.charAt(i - 1) == '$') {
                depth++;
            }
        }

        if (index == -1)
            return null;

        String varName = "workspace_loc"; //$NON-NLS-1$
        String str1 = path.substring(2, index);
        String result = null;
        if (str1.startsWith(varName)) {
            String str2 = str1.substring(varName.length());
            if (str2.length() != 0) {
                if (str2.startsWith(":")) { //$NON-NLS-1$
                    result = str2.substring(1);
                }
            } else {
                result = "/"; //$NON-NLS-1$
            }
            // If the user has a path like ${workspace_loc:/thing}/other/thing
            // ensure we return /thing/other/thing
            if (index < path.length() - 1)
                result += path.substring(index + 1);
        }

        return result;
    }

    private static PathInfo optionPathValueToEntry(String str, SupplierBasedCdtVariableSubstitutor subst) {
        return null;
        //        String unresolvedStr = locationToFullPath(str);
        //        boolean isWorkspacePath = false;
        //        if (unresolvedStr == null) {
        //            unresolvedStr = str;
        //        } else {
        //            // A path starting from "${workspace_loc}" is always a filesystem path.
        //            if (!unresolvedStr.startsWith("${workspace_loc}")) { //$NON-NLS-1$
        //                isWorkspacePath = true;
        //            }
        //        }
        //        return new PathInfo(unresolvedStr, isWorkspacePath, subst);
    }

    private void setUserEntries(UserEntryInfo[] entries, List<EmptyEntryInfo> emptyEntryInfos) {
        int kind = getKind();
        IOption options[] = fLangData.getOptionsForKind(kind);
        // We don't expect more than one option to manage a particular entry kind, though it
        // is theoretically possible... Add a trace for Toolchain developers
        if (options.length > 1)
            Activator.error("Unexpected error: Warning more than one options found for kind " + getKind()); //$NON-NLS-1$
        if (options.length != 0) {
            IOption option = options[0];
            OptionStringValue[] optValue;
            if (entries.length != 0) {
                entries = combineSequenses(entries);

                entries = addEmptyEntries(entries, emptyEntryInfos);

                optValue = new OptionStringValue[entries.length];
                SupplierBasedCdtVariableSubstitutor subst = createSubstitutor(option, false);

                for (int i = 0; i < entries.length; i++) {
                    optValue[i] = createOptionValue(option, entries[i], subst);
                }
            } else {
                optValue = Option.EMPTY_LV_ARRAY;
            }

            ITool tool = fLangData.getTool();
            IResourceInfo rcInfo = tool.getParentResourceInfo();
            IOption newOption = ManagedBuildManager.setOption(rcInfo, tool, option, optValue);
            for (IOption opt : fLangData.getOptionsForKind(kind)) {
                if (opt != newOption)
                    ManagedBuildManager.setOption(rcInfo, tool, opt, new String[0]);
            }
        }
    }

    private UserEntryInfo[] addEmptyEntries(UserEntryInfo infos[], List<EmptyEntryInfo> emptyEntryInfos) {
        if (emptyEntryInfos == null || emptyEntryInfos.size() == 0)
            return infos;

        LinkedList<UserEntryInfo> list = new LinkedList<>();
        list.addAll(Arrays.asList(infos));
        for (int i = 0; i < emptyEntryInfos.size(); i++) {
            EmptyEntryInfo ei = emptyEntryInfos.get(i);
            int index = ei.fPosition;
            if (index > list.size())
                index = list.size();

            list.add(index, new UserEntryInfo(null, ei.fOriginalValue, ei.fOriginalValue, null));
        }

        return list.toArray(new UserEntryInfo[list.size()]);
    }

    /**
     * This method iterates over the passed in UserEntryInfo[]. The point of it is
     * to collapse entries which are contained in a given UserEntry's sequence into
     * a single
     * UserEntryInfo.
     *
     * FIXME: As far as I can see info.fSequense only ever has a single entry in
     * it...
     * see UserEntryInfo constructor => this method doesn't accomplish anything
     * useful
     */
    private UserEntryInfo[] combineSequenses(UserEntryInfo infos[]) {
        if (infos.length == 0)
            return infos;

        List<UserEntryInfo> list = new ArrayList<>(infos.length);

        for (int i = 0; i < infos.length; i++) {
            UserEntryInfo info = infos[i];
            if (info.fSequense != null) {
                boolean match = true;
                int seqSize = info.fSequense.size();
                if (seqSize > infos.length - i) {
                    match = false;
                } else {
                    for (int k = 0; k < seqSize; k++) {
                        if (info.fSequense.get(k) != infos[i + k]) {
                            match = false;
                            break;
                        }
                    }
                }

                if (match) {
                    i = i + seqSize - 1;
                } else {
                    infos[i] = createDesequencedEntry(info);
                    for (int k = i + 1; k < infos.length; k++) {
                        if (infos[k].fSequense == info.fSequense)
                            infos[k] = createDesequencedEntry(infos[k]);
                    }
                    info = infos[i];
                }
            }
            list.add(info);
        }

        return list.toArray(new UserEntryInfo[list.size()]);
    }

    private static UserEntryInfo createDesequencedEntry(UserEntryInfo info) {
        OptionStringValue resolvedValue = info.fBsResolvedValue;
        if (resolvedValue != null) {
            String v = doubleQuotePath(resolvedValue.getValue(), true);
            if (v != null)
                resolvedValue = substituteValue(resolvedValue, v);
        }
        return new UserEntryInfo(info.fEntry, resolvedValue, resolvedValue, null);
    }

    private void setUserUndefinedStringSet(Set<String> set) {
        int kind = getKind();
        IOption[] options = fLangData.getUndefOptionsForKind(kind);
        if (options.length != 0) {
            if (set != null && set.size() == 0)
                set = null;

            String[] optValue = set != null ? (String[]) set.toArray(new String[set.size()]) : new String[0];
            IOption option = options[0];
            ITool tool = fLangData.getTool();
            IResourceInfo rcInfo = tool.getParentResourceInfo();
            IOption newOption = ManagedBuildManager.setOption(rcInfo, tool, option, optValue);
            options = fLangData.getUndefOptionsForKind(kind);
            for (IOption opt : options) {
                if (opt != newOption)
                    ManagedBuildManager.setOption(rcInfo, tool, option, new String[0]);
            }
        }
    }

    void optionsChanged() {
    }
}
