/*******************************************************************************
 * Copyright (c) 2007, 2013 Intel Corporation and others.
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
package io.sloeber.autoBuild.integration;

import static io.sloeber.autoBuild.helpers.api.AutoBuildConstants.*;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Set;
import org.apache.commons.io.FileUtils;
import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.cdt.core.settings.model.ICProjectDescription;
import org.eclipse.cdt.core.settings.model.IModificationContext;
import org.eclipse.cdt.core.settings.model.extension.CConfigurationData;
import org.eclipse.cdt.core.settings.model.extension.CConfigurationDataProvider;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import io.sloeber.autoBuild.core.Activator;
import io.sloeber.autoBuild.helpers.api.KeyValueTree;

/**
 * The main hook ManagedBuild uses to connect to cdt.core's project model.
 * Provides & Persists Build configuration data in the project model storage.
 */
public class AutoBuildConfigurationDescriptionProvider extends CConfigurationDataProvider {// implements
																							// ISettingsChangeListener {
	public static final String CFG_DATA_PROVIDER_ID = Activator.PLUGIN_ID + ".ConfigurationDataProvider"; //$NON-NLS-1$
	private static final String AUTO_BUILD_PROJECT_FILE = ".autoBuildProject"; //$NON-NLS-1$
	private static final String AUTO_BUILD_TEAM_FILE = "autoBuildProject.cfg"; //$NON-NLS-1$

//	public class KeyValuePairs{
//		private String myLinePrefix;
//		private String myLineEnd;
//		private Map<String,String> myKeys =new TreeMap<>();
//		private Set<KeyValuePairs> myChildren =new HashSet<>();
//		KeyValuePairs(String linePrefix, String lineEnd){
//			myLinePrefix= linePrefix;
//			myLineEnd=lineEnd;
//		}
//		public void append(String key,String value) {
//			if(value.isBlank()) {
//				return;
//			}
//			myKeys.put( key , value );
//		}
//		public StringBuffer getBuffer() {
//			return getBuffer(null);
//		}
//		public void append(KeyValuePairs stringBuffer) {
//			myKeys.putAll(stringBuffer.myKeys);
//
//		}
//		public KeyValuePairs getChild(String childKey) {
//			KeyValuePairs ret=new KeyValuePairs(childKey,myLineEnd);
//			myChildren.add(ret);
//			return ret;
//		}
//		public StringBuffer getBuffer(Set<String> excludedKeys) {
//			StringBuffer ret=new StringBuffer();
//			for(Entry<String, String> curPair:myKeys.entrySet()) {
//				if(excludedKeys!=null && excludedKeys.contains(curPair.getKey()) ) {
//					continue;
//				}
//				ret.append(myLinePrefix);
//				ret.append(DOT);
//				ret.append(curPair.getKey());
//				ret.append(EQUAL);
//				ret.append(curPair.getValue());
//				ret.append(myLineEnd);
//			}
//			for(KeyValuePairs child:myChildren) {
//				ret.append(child.getBuffer(myLinePrefix,excludedKeys));
//			}
//			return ret;
//		}
//
//		public StringBuffer getBuffer(String linePrefix,Set<String> excludedKeys) {
//			StringBuffer ret=new StringBuffer();
//			for(Entry<String, String> curPair:myKeys.entrySet()) {
//				if(excludedKeys!=null && excludedKeys.contains(curPair.getKey()) ) {
//					continue;
//				}
//				ret.append(linePrefix );
//				ret.append(DOT );
//				ret.append(myLinePrefix);
//				ret.append(DOT);
//				ret.append(curPair.getKey());
//				ret.append(EQUAL);
//				ret.append(curPair.getValue());
//				ret.append(myLineEnd);
//			}
//			for(KeyValuePairs child:myChildren) {
//				ret.append(child.getBuffer(linePrefix +myLinePrefix,excludedKeys));
//			}
//			return ret;
//		}
//
//
//	}

	public AutoBuildConfigurationDescriptionProvider() {
	}

	@Override
	public CConfigurationData applyConfiguration(ICConfigurationDescription cfgDescription,
			ICConfigurationDescription baseCfgDescription, CConfigurationData baseData, IModificationContext context,
			IProgressMonitor monitor) throws CoreException {

		ICProjectDescription projDesc = cfgDescription.getProjectDescription();
		IProject iProject = projDesc.getProject();

		// Get the tree value pairs
		KeyValueTree keyValuePairs = KeyValueTree.createRoot();
		for (ICConfigurationDescription curConfDesc : projDesc.getConfigurations()) {
			AutoBuildConfigurationDescription autoBuildConfigBase = (AutoBuildConfigurationDescription) curConfDesc
					.getConfigurationData();

			KeyValueTree cfgkeyValuePairs = keyValuePairs.addChild(curConfDesc.getName());

			autoBuildConfigBase.serialize(cfgkeyValuePairs);
		}

		// Save the autobuild project file and the team file (if needed)
		try {
			File projectFile = getStorageFile(iProject).getLocation().toFile();
			IFile teamFile = getTeamFile(iProject);

			// save the project file if needed
			boolean needsWriting = true;
			String configText = keyValuePairs.dump();
			if (projectFile.exists()) {
				String curConfigsText = FileUtils.readFileToString(projectFile, AUTOBUILD_CONFIG_FILE_CHARSET);
				needsWriting = !curConfigsText.equals(configText);
			}
			if (needsWriting) {
				FileUtils.write(projectFile, configText, Charset.defaultCharset());
			}

			// Remove the keys that the user does not want in the team file
			for (ICConfigurationDescription curConfDesc : projDesc.getConfigurations()) {
				AutoBuildConfigurationDescription autoBuildConfigBase = (AutoBuildConfigurationDescription) curConfDesc
						.getConfigurationData();

				Set<String> excludedKeys = autoBuildConfigBase.getTeamExclusionKeys();
				for (String curKey : excludedKeys) {
					keyValuePairs.removeKey(curKey);
				}
			}

			// save the team file if needed
			needsWriting = true;
			String teamText = keyValuePairs.dump();
			if (teamText.length() < 2) {
				teamFile.delete(true, monitor);
				needsWriting = false;
			}
			File teamFile2 = teamFile.getLocation().toFile();
			if (teamFile.exists()) {
				String curTeamText = FileUtils.readFileToString(teamFile2, Charset.defaultCharset());
				needsWriting = !curTeamText.equals(teamText.toString());

			}
			if (needsWriting) {
				FileUtils.write(teamFile2, teamText, Charset.defaultCharset());
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		return baseData;
	}

	@Override
	public CConfigurationData createConfiguration(ICConfigurationDescription cfgDescription,
			ICConfigurationDescription baseCfgDescription, CConfigurationData base, boolean clone,
			IProgressMonitor monitor) throws CoreException {
		AutoBuildConfigurationDescription autoBuildConfigBase = (AutoBuildConfigurationDescription) base;
		AutoBuildConfigurationDescription ret = new AutoBuildConfigurationDescription(cfgDescription,
				autoBuildConfigBase, clone);
		return ret;
	}

	@Override
	public CConfigurationData loadConfiguration(ICConfigurationDescription cfgDescription, IProgressMonitor monitor)
			throws CoreException {

		IProject iProject = cfgDescription.getProjectDescription().getProject();
		File projectFile = getStorageFile(iProject).getLocation().toFile();
		IFile teamFile = getTeamFile(iProject);
		try {
			if (projectFile.exists()) {
				KeyValueTree keyValues = KeyValueTree.createRoot();
				keyValues.mergeFile(projectFile);
				if (teamFile.exists()) {
					keyValues.mergeFile(teamFile.getLocation().toFile());
				}
				return new AutoBuildConfigurationDescription(cfgDescription,
						keyValues.getChild(cfgDescription.getName()));
			}
			// This Should not happen
			throw new CoreException(null);
		} catch (IOException e) {
			e.printStackTrace();
		}

		return null;
	}

	@Override
	public void removeConfiguration(ICConfigurationDescription cfgDescription, CConfigurationData data,
			IProgressMonitor monitor) {
		return;
		// String configname = cfgDescription.getName();
		// if (cfgDescription.getProjectDescription().getConfigurationByName(configname)
		// != null) {
		// //no need to remove the configuration from disk
		// return;
		// }
		// String lineStart = getLinePrefix(cfgDescription);
		// String lineEnd = getLineEnd();
		// File projectFile = getStorageFile(cfgDescription);
		// try {
		// if (projectFile.exists()) {
		// String curConfigsText = FileUtils.readFileToString(projectFile,
		// Charset.defaultCharset());
		// String clean = curConfigsText.replaceAll("(?m)^" + lineStart + ".+$" +
		// lineEnd, EMPTY_STRING); //$NON-NLS-1$ //$NON-NLS-2$
		// FileUtils.write(projectFile, clean, Charset.defaultCharset());
		// }
		// } catch (IOException e) {
		// // TODO Auto-generated catch block
		// e.printStackTrace();
		// }
		// return;
	}

	@Override
	public void dataCached(ICConfigurationDescription cfgDescription, CConfigurationData data,
			IProgressMonitor monitor) {
		// doc says: default implementation is empty :-)
		return;
	}

	public static IFile getStorageFile(IProject iProject) {
		return iProject.getFile(AUTO_BUILD_PROJECT_FILE);
	}

	public static IFile getTeamFile(IProject iProject) {
		return iProject.getFile(AUTO_BUILD_TEAM_FILE);
	}
}
