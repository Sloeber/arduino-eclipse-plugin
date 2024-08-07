package io.sloeber.autoBuild.integration;

import static io.sloeber.autoBuild.helpers.api.AutoBuildConstants.*;

import java.util.Arrays;
/**
 * A abstract class to satisfy the resource handling required from a CConfigurationData implementation
 * The idea is that this class is kind of stand alone so it can be reused
 * the main reasons to put this code in a separate class are:
 * 1) The AutoBuildConfiguration class was becoming to big as to my likings
 * 2) The code is very tricky (change something and stuff stops working) and there is no documentation
 * 3) I think CDT should provide a default which could be this
 */
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import org.eclipse.cdt.core.settings.model.CSourceEntry;
import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.cdt.core.settings.model.ICSettingEntry;
import org.eclipse.cdt.core.settings.model.ICSourceEntry;
import org.eclipse.cdt.core.settings.model.extension.CConfigurationData;
import org.eclipse.cdt.core.settings.model.extension.CFileData;
import org.eclipse.cdt.core.settings.model.extension.CFolderData;
import org.eclipse.cdt.core.settings.model.extension.CLanguageData;
import org.eclipse.cdt.core.settings.model.extension.CResourceData;
import org.eclipse.cdt.core.settings.model.extension.impl.CDataFactory;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import io.sloeber.autoBuild.helpers.api.KeyValueTree;

public abstract class AutoBuildResourceData extends CConfigurationData {
	protected static final String KEY_SOURCE_ENTRY = "SourceEntry"; //$NON-NLS-1$
	// protected IProject myProject;
	private Map<String, CResourceData> myResourceDatas = new HashMap<>();
	private ICSourceEntry mySourceEntries[] = null;
	private CFolderData myRootFolderData;

	/**
	 * Copy constructor
	 *
	 * @param cfgDescription
	 */
	public void clone(AutoBuildConfigurationDescription parent, AutoBuildConfigurationDescription autoBuildResourceBase,
			boolean clone) {
		myRootFolderData = new FolderData(parent, autoBuildResourceBase.getRootFolderData(), clone);
		// myProject=autoBuildResourceBase.getProject();
		cloneSourceEntries(autoBuildResourceBase.getSourceEntries());

	}

	private void cloneSourceEntries(ICSourceEntry entries[]) {
		if (!Arrays.equals(entries, mySourceEntries)) {
			mySourceEntries = entries != null ? (ICSourceEntry[]) entries.clone() : null;
		}
	}

	/**
	 * persistency constructor based on text
	 *
	 * @param cfgDescription the configuration that owns the reosurceDatas
	 * @param curConfigsText The text that needs parsing
	 * @param lineStart      the beginning of each line that needs to be ignored
	 * @param lineEnd        The ending of each line that needs to be ignored
	 */
	public AutoBuildResourceData(ICConfigurationDescription cfgDescription, KeyValueTree keyValues) {
		// TODO read Map<String, CResourceData> myResourceDatas = new HashMap<>();
		// TODO read CFolderData myRootFolderData;
		Set<CSourceEntry> sourceEntries = new HashSet<>();
		for (KeyValueTree cursourceEntykeyValue : keyValues.getChildren().values()) {
			String name = cursourceEntykeyValue.getKey();
			String value = cursourceEntykeyValue.getValue();
			if(ROOT.equals(name)) {
				name=EMPTY_STRING;
			}
			String values[] = value.split(Pattern.quote(COLON));
			if (values.length < 2) {
				// no exclusion patterns
				int flags = Integer.valueOf(value).intValue();
				sourceEntries.add(new CSourceEntry(name, null, flags));
			} else {
				int flags = Integer.valueOf(values[0]).intValue();
				Set<IPath> exclusionPatterns = new HashSet<>();
				for (int curEx = 1; curEx < values.length; curEx++) {
					exclusionPatterns.add(new Path(values[curEx]));
				}
				sourceEntries.add(new CSourceEntry(name, exclusionPatterns.toArray(new IPath[exclusionPatterns.size()]), flags));
			}
		}
		mySourceEntries = sourceEntries.toArray(new ICSourceEntry[sourceEntries.size()]);
	}

	public AutoBuildResourceData() {
	}

	@Override
	public void removeResourceData(CResourceData data) throws CoreException {
		checkIfWeCanWrite();
		myResourceDatas.remove(data.getId());
		return;
	}

	@Override
	public CFolderData createFolderData(IPath path, CFolderData base) throws CoreException {
		checkIfWeCanWrite();
		CDataFactory factory = CDataFactory.getDefault();
		CFolderData folderData = factory.createFolderData(this, base, null, false, path);
		myResourceDatas.put(folderData.getId(), folderData);
		return folderData;
	}

	@Override
	public CFileData createFileData(IPath path, CFileData base) throws CoreException {
		checkIfWeCanWrite();
		CDataFactory factory = CDataFactory.getDefault();
		CFileData fileData = factory.createFileData(this, base, null, null, false, path);
		myResourceDatas.put(fileData.getId(), fileData);
		return fileData;
	}

	@Override
	public CFileData createFileData(IPath path, CFolderData base, CLanguageData langData) throws CoreException {
		checkIfWeCanWrite();
		CDataFactory factory = CDataFactory.getDefault();
		CFileData fileData = factory.createFileData(this, base, langData, null, false, path);
		myResourceDatas.put(fileData.getId(), fileData);
		return fileData;
	}

	@Override
	public ICSourceEntry[] getSourceEntries() {
		return mySourceEntries.clone();
	}

	@Override
	public void setSourceEntries(ICSourceEntry[] entries) {
		checkIfWeCanWrite();
		cloneSourceEntries(entries);
	}

	@Override
	public CFolderData getRootFolderData() {
		if (myRootFolderData == null) {
			// CDataFactory factory = CDataFactory.getDefault();
			// myRootFolderData = factory.createFolderData(this, null, myRootFolderID,
			// false, Path.ROOT);
			AutoBuildConfigurationDescription autoBuildConfDesc = ((AutoBuildConfigurationDescription) this);
			myRootFolderData = new FolderData(autoBuildConfDesc.getProject(), autoBuildConfDesc);
		}
		return myRootFolderData;
	}

	@Override
	public CResourceData[] getResourceDatas() {
		// if (!myResourceDataContainsRootFolder) {
		// //myResourceDataContainsRootFolder = true;
		// AutoBuildConfigurationDescription autoBuildConfDesc =
		// ((AutoBuildConfigurationDescription) this);
		// FolderData rootFolderData = new FolderData(myProject, autoBuildConfDesc);
		//
		// myResourceDatas.clear();
		// myResourceDatas.put(rootFolderData.getId(), rootFolderData);
		// }
		Set<CResourceData> ret = new HashSet<>();
		ret.addAll(myResourceDatas.values());
		ret.add(getRootFolderData());

		return ret.toArray(new CResourceData[ret.size()]);

	}

	abstract protected void checkIfWeCanWrite();

	protected void serialize(KeyValueTree keyValuePairs) {
		if (mySourceEntries != null) {
			for (ICSourceEntry curSourceEntry : mySourceEntries) {
				String key = curSourceEntry.getName();
				if (key.isBlank()) {
					key = ROOT;
				}
				String value = Integer.toString(curSourceEntry.getFlags());//&~(ICSettingEntry.VALUE_WORKSPACE_PATH));
				for (IPath curExclusion : curSourceEntry.getExclusionPatterns()) {
					value = value + COLON + curExclusion.toString();
				}
				keyValuePairs.addValue(key, value);
			}
		}
	}

	protected void initializeResourceData(IProject project, String rootCodeFolder, String BuildFolderString) {
		mySourceEntries = new ICSourceEntry[1];
		if (rootCodeFolder == null || rootCodeFolder.isBlank()) {
			// exclude the bin folder
			String buildRootFolder = new Path(BuildFolderString).segment(0);
			IPath excludes[] = new IPath[1];
			excludes[0] = project.getFolder(buildRootFolder).getProjectRelativePath();
			mySourceEntries[0] = new CSourceEntry(Path.ROOT.toString(), excludes, ICSettingEntry.RESOLVED);
		} else {
			// no need to exclude the bin folder
			mySourceEntries[0] = new CSourceEntry(rootCodeFolder, null, ICSettingEntry.RESOLVED);
		}
	}
}
