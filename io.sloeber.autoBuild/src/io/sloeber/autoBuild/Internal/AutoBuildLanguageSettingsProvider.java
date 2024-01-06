/*******************************************************************************
 * Copyright (c) 2009, 2013 Andrew Gvozdev and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Andrew Gvozdev - initial API and implementation
 *******************************************************************************/

package io.sloeber.autoBuild.Internal;

import static io.sloeber.autoBuild.integration.AutoBuildConstants.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.eclipse.cdt.core.AbstractExecutableExtensionBase;
import org.eclipse.cdt.core.language.settings.providers.ILanguageSettingsBroadcastingProvider;
import org.eclipse.cdt.core.language.settings.providers.LanguageSettingsStorage;
import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.cdt.core.settings.model.ICLanguageSettingEntry;
import org.eclipse.cdt.core.settings.model.ICSettingEntry;
import org.eclipse.cdt.core.settings.model.util.CDataUtil;
import org.eclipse.core.resources.IResource;
import io.sloeber.autoBuild.api.IAutoBuildConfigurationDescription;
import io.sloeber.schema.api.IOption;
import io.sloeber.schema.api.ITool;

/**
 * Implementation of language settings provider for autoBuild.
 */
public class AutoBuildLanguageSettingsProvider extends AbstractExecutableExtensionBase
		implements ILanguageSettingsBroadcastingProvider {
	@SuppressWarnings("incomplete-switch")
	@Override
	public List<ICLanguageSettingEntry> getSettingEntries(ICConfigurationDescription cfgDescription, IResource rc,
			String languageId) {
		if (cfgDescription == null || rc == null || languageId == null) {
			return null;
		}
		IAutoBuildConfigurationDescription autoConf = IAutoBuildConfigurationDescription.getConfig(cfgDescription);
		if (autoConf == null) {
			return null;
		}

		// this list is allowed to contain duplicate entries, cannot be LinkedHashSet
		List<ICLanguageSettingEntry> list = new ArrayList<>();
		int flags = ICSettingEntry.VALUE_WORKSPACE_PATH|ICSettingEntry.BUILTIN|ICSettingEntry.READONLY;

		TreeMap<IOption, String> options = autoConf.getSelectedOptions(rc);
		for (Entry<IOption, String> curOption : options.entrySet()) {
			IOption option = curOption.getKey();
			if(!option.isForLanguage(languageId)) {
				continue;
			}
			String optionValue = curOption.getValue();
			String optionValues[] = optionValue.split(SEMICOLON);
			int valueType = option.getValueType();
			switch (valueType) {
			case IOption.STRING:
			case IOption.STRING_LIST:
			case IOption.BOOLEAN:
			case IOption.ENUMERATED:
			case IOption.OBJECTS:
			case IOption.TREE:
				break;
			case IOption.INCLUDE_FILES:{
				for (String curName : optionValues) {
					list.add(CDataUtil.createCIncludeFileEntry(curName, flags));
				}
				break;
			}
			case IOption.INCLUDE_PATH:{
				for (String curName : optionValues) {
					list.add(CDataUtil.createCIncludePathEntry(curName, flags));
				}
				break;
			}
			case IOption.LIBRARIES:{
				for (String curName : optionValues) {
					list.add(CDataUtil.createCLibraryFileEntry(curName, flags));
				}
				break;
			}

			case IOption.LIBRARY_PATHS:
			{
				for (String curName : optionValues) {
					list.add(CDataUtil.createCLibraryPathEntry(curName, flags));
				}
				break;
			}
			case IOption.LIBRARY_FILES:
			{
				for (String curName : optionValues) {
					list.add(CDataUtil.createCLibraryFileEntry(curName, flags));
				}
				break;
			}
				
			case IOption.PREPROCESSOR_SYMBOLS:
			{
				for (String curName : optionValues) {
					String parts[]=curName.split(EQUAL,2);
					if(parts.length==2) {
					list.add(CDataUtil.createCMacroEntry(parts[0],parts[1], flags));
					}
				}
				break;
			}
			case IOption.MACRO_FILES:
			{
				for (String curName : optionValues) {
					list.add(CDataUtil.createCMacroFileEntry(curName, flags));
				}
				break;
			}
				
			case IOption.UNDEF_INCLUDE_PATH:
			case IOption.UNDEF_PREPROCESSOR_SYMBOLS:
			case IOption.UNDEF_INCLUDE_FILES:
			case IOption.UNDEF_MACRO_FILES:
			case IOption.UNDEF_LIBRARY_PATHS:
			case IOption.UNDEF_LIBRARY_FILES:

			}
		}
		return LanguageSettingsStorage.getPooledList(list);
	}



	@Override
	public LanguageSettingsStorage copyStorage() {
		class PretendStorage extends LanguageSettingsStorage {
			@Override
			public boolean isEmpty() {
				return false;
			}

			@Override
			public LanguageSettingsStorage clone() throws CloneNotSupportedException {
				return this;
			}

			@Override
			public boolean equals(Object obj) {
				// Note that this always triggers change event even if nothing changed in MBS
				return false;
			}
		}
		return new PretendStorage();
	}

}
