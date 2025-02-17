package io.sloeber.core.internal;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import static io.sloeber.core.api.Common.*;
import static io.sloeber.core.api.Const.*;

import org.eclipse.cdt.core.settings.model.CSourceEntry;
import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.cdt.core.settings.model.ICSourceEntry;
import org.eclipse.cdt.core.settings.model.extension.CConfigurationData;
import org.eclipse.cdt.core.settings.model.util.CDataUtil;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

import io.sloeber.arduinoFramework.api.BoardDescription;
import io.sloeber.arduinoFramework.api.IArduinoLibraryVersion;
import io.sloeber.arduinoFramework.api.LibraryManager;
import io.sloeber.autoBuild.api.AutoBuildConfigurationExtensionDescription;
import io.sloeber.autoBuild.api.AutoBuildProject;
import io.sloeber.autoBuild.api.IAutoBuildConfigurationDescription;
import io.sloeber.autoBuild.helpers.api.KeyValueTree;
import io.sloeber.autoBuild.integration.AutoBuildConfigurationDescription;
import io.sloeber.core.Activator;
import io.sloeber.core.Messages;
import io.sloeber.core.api.CompileDescription;
import io.sloeber.core.api.ConfigurationPreferences;
import io.sloeber.core.api.ISloeberConfiguration;
import io.sloeber.core.api.OtherDescription;
import io.sloeber.core.tools.Helpers;
import io.sloeber.core.tools.uploaders.UploadSketchWrapper;

public class SloeberConfiguration extends AutoBuildConfigurationExtensionDescription implements ISloeberConfiguration {
	// configuration data
	private BoardDescription myBoardDescription;
	private OtherDescription myOtherDesc;
	private CompileDescription myCompileDescription;
	// a map of foldername library
	private Map<IPath, IArduinoLibraryVersion> myLibraries = new HashMap<>();

	// operational data
	private boolean myMemoryIsDirty = true;

	// derived data
	private Map<String, String> myEnvironmentVariables = new HashMap<>();

	public static SloeberConfiguration getFromAutoBuildConfDesc(IAutoBuildConfigurationDescription autoBuildConfData) {
		return (SloeberConfiguration) autoBuildConfData.getAutoBuildConfigurationExtensionDescription();
	}

	public static SloeberConfiguration getConfig(ICConfigurationDescription cConfigDesc) {
		CConfigurationData confData = cConfigDesc.getConfigurationData();
		if (confData instanceof IAutoBuildConfigurationDescription) {
		return (SloeberConfiguration) ((IAutoBuildConfigurationDescription)confData).getAutoBuildConfigurationExtensionDescription();
		}
		return null;
	}

	/**
	 * copy constructor This constructor must be implemented for each derived class
	 * of AutoBuildConfigurationExtensionDescription or you will get run time errors
	 *
	 * @param owner
	 * @param source
	 * @throws Exception
	 */
	public SloeberConfiguration(AutoBuildConfigurationDescription owner,
			AutoBuildConfigurationExtensionDescription source) {
		// the code below will throw an error in case source is not an instance of
		// SloeberConfiguration
		// This may sound strange for you but this is exactly what I want JABA

		SloeberConfiguration src = (SloeberConfiguration) source;
		setAutoBuildDescription(owner);
		setBoardDescription(src.getBoardDescription());
		setOtherDescription(src.getOtherDescription());
		setCompileDescription(src.getCompileDescription());
		myLibraries = src.getLibrariesFromLinks();
	}

	public SloeberConfiguration(BoardDescription boardDesc, OtherDescription otherDesc,
			CompileDescription compileDescriptor) {
		setBoardDescription(boardDesc);
		setOtherDescription(otherDesc);
		setCompileDescription(compileDescriptor);
	}

	@Override
	public BoardDescription getBoardDescription() {
		return new BoardDescription(myBoardDescription);
	}

	@Override
	public OtherDescription getOtherDescription() {
		return new OtherDescription(myOtherDesc);
	}

	@Override
	public void setOtherDescription(OtherDescription newOtherDesc) {
		if (myOtherDesc != null && myOtherDesc.needsRebuild(newOtherDesc)) {
			getAutoBuildDescription().forceCleanBeforeBuild();
		}
		myOtherDesc = new OtherDescription(newOtherDesc);
		setIsDirty();
	}

	@Override
	public CompileDescription getCompileDescription() {
		return new CompileDescription(myCompileDescription);
	}

	@Override
	public void setCompileDescription(CompileDescription newCompDesc) {
		if (myCompileDescription != null && myCompileDescription.needsRebuild(newCompDesc)) {
			getAutoBuildDescription().forceCleanBeforeBuild();
		}
		myCompileDescription = new CompileDescription(newCompDesc);
		setIsDirty();
	}

	@Override
	public void copyData(AutoBuildConfigurationExtensionDescription from) {
		// TODO Auto-generated method stub

	}

	@Override
	public void serialize(KeyValueTree serialize) {
		myBoardDescription.serialize(serialize);
		myOtherDesc.serialize(serialize);
		myCompileDescription.serialize(serialize);
		configureWhenDirty();
	}

	public SloeberConfiguration(IAutoBuildConfigurationDescription autoCfgDescription, KeyValueTree keyValues) {
		setAutoBuildDescription(autoCfgDescription);
		myBoardDescription = new BoardDescription(keyValues);
		myOtherDesc = new OtherDescription(keyValues);
		myCompileDescription = new CompileDescription(keyValues);
		myMemoryIsDirty = true;
		// configure(); Seems I can not do the config here
	}

	@Override
	public IProject getProject() {
		return getAutoBuildDescription().getProject();
	}

	@Override
	public IFolder getArduinoRootFolder() {
		IProject project = getProject();
		return project.getFolder(SLOEBER_ARDUINO_FOLDER_NAME);
	}

	@Override
	public IFolder getArduinoConfigurationFolder() {
		String cdtConfDescName = getCDTConfName();
		return getArduinoRootFolder().getFolder(cdtConfDescName);
	}

	@Override
	public IFolder getArduinoCoreFolder() {
		return getArduinoConfigurationFolder().getFolder(SLOEBER_CODE_FOLDER_NAME);
	}

	@Override
	public IFolder getArduinoVariantFolder() {
		return getArduinoConfigurationFolder().getFolder(SLOEBER_VARIANT_FOLDER_NAME);
	}

	@Override
	public IFolder getArduinoLibraryFolder() {
		return getArduinoConfigurationFolder().getFolder(SLOEBER_LIBRARY_FOLDER_NAME);
	}

	@Override
	public Map<String, String> getEnvironmentVariables() {
		configureWhenDirty();

		return myEnvironmentVariables;
	}

	private void configureWhenDirty() {
		if (myMemoryIsDirty) {
			myMemoryIsDirty = getEnvVarsNonExpanding();
		}

	}

	/* Update the links to the Arduino stuff
	 * Return true if the project needs a refresh
	 */
	private boolean LinkToCore() {
		if (!ResourcesPlugin.getWorkspace().isTreeLocked()) {
			if (projectNeedsUpdate()) {
				updateArduinoCodeLinks();
				return true;
			}
		}
		return false;
	}

	private boolean projectNeedsUpdate() {
		IPath corePath = myBoardDescription.getActualCoreCodePath();
		IFolder coreFolder = getArduinoCoreFolder();
		if (!corePath.equals(coreFolder.getLocation())) {
//        	System.out.println("projectNeedsUpdate core Folder mismatch");
//        	System.out.println("corefolder "+coreFolder.getLocation());
//        	System.out.println("corePath   "+corePath);
			return true;
		}
		IFolder arduinoVariantFolder = getArduinoVariantFolder();
		IPath variantPath = myBoardDescription.getActualVariantPath();
		if (variantPath == null) {
			return arduinoVariantFolder.exists();
		}
		if ((!variantPath.toFile().exists()) && (arduinoVariantFolder.exists())) {
//        	System.out.println("projectNeedsUpdate variant Folder exists but sdhould not");
			return true;
		}
		if ((variantPath.toFile().exists()) && !arduinoVariantFolder.getLocation().equals(variantPath)) {
//        	System.out.println("projectNeedsUpdate variant Folder mismatch");
//        	System.out.println("folder   "+arduinoVariantFolder.getLocation());
//        	System.out.println("location "+variantPath);
			return true;
		}

		return false;
	}

	/**
	 * get the environment variables that do not reliy on variable expansion to get the value.
	 * @return true when data was missing
	 */
	private boolean getEnvVarsNonExpanding() {
		myEnvironmentVariables.clear();

		myEnvironmentVariables.put(ENV_KEY_BUILD_PATH,
				getProject().getFolder(getAutoBuildDescription().getBuildFolderString()).getLocation().toOSString());

		myEnvironmentVariables.put(ENV_KEY_BUILD_SOURCE_PATH, getCodeLocation().toOSString());
//			myEnvironmentVariables.put(ENV_KEY_BUILD_PATH,
//					getAutoBuildDescription().getBuildFolder().getLocation().toOSString());

		if (myBoardDescription != null) {
			myEnvironmentVariables.putAll(myBoardDescription.getEnvVars());
		}
		if (myCompileDescription != null) {
			myEnvironmentVariables.putAll(myCompileDescription.getEnvVars());
		}
		if (myOtherDesc != null) {
			myEnvironmentVariables.putAll(myOtherDesc.getEnvVars());
		}
		// set the paths
		String pathDelimiter = makeEnvironmentVar("PathDelimiter"); //$NON-NLS-1$
		if (isWindows) {
			myEnvironmentVariables.put(SLOEBER_MAKE_LOCATION,
					ConfigurationPreferences.getMakePath().addTrailingSeparator().toOSString());
			myEnvironmentVariables.put(SLOEBER_AWK_LOCATION,
					ConfigurationPreferences.getAwkPath().addTrailingSeparator().toOSString());

			String systemroot = makeEnvironmentVar("SystemRoot"); //$NON-NLS-1$
			myEnvironmentVariables.put("PATH", //$NON-NLS-1$
					makeEnvironmentVar(ENV_KEY_COMPILER_PATH) + pathDelimiter
								+ makeEnvironmentVar(ENV_KEY_BUILD_GENERIC_PATH) + pathDelimiter + systemroot
								+ "\\system32" //$NON-NLS-1$
							+ pathDelimiter + systemroot + pathDelimiter + systemroot + "\\system32\\Wbem" //$NON-NLS-1$
							+ pathDelimiter + makeEnvironmentVar("sloeber_path_extension")); //$NON-NLS-1$
		} else {
			myEnvironmentVariables.put("PATH", makeEnvironmentVar(ENV_KEY_COMPILER_PATH) + pathDelimiter //$NON-NLS-1$
					+ makeEnvironmentVar(ENV_KEY_BUILD_GENERIC_PATH) + pathDelimiter + makeEnvironmentVar("PATH")); //$NON-NLS-1$
		}
		return (myBoardDescription == null) || (myCompileDescription == null) || (myOtherDesc == null);
	}

	private IPath getCodeLocation() {
		IProject project = getProject();
		IPath arduinoPath = getArduinoRootFolder().getFullPath();
		ICSourceEntry[] sourceEntries = getAutoBuildDesc().getCdtConfigurationDescription().getSourceEntries();
		for (ICSourceEntry curEntry : sourceEntries) {
			IPath entryPath = curEntry.getFullPath();
			if (arduinoPath.isPrefixOf(entryPath)) {
				// this is the arduino code folder: ignore
				continue;
			}
			// Just pick the first none arduino one as there should only be one
			return getLocation(curEntry);
		}
		return project.getLocation();
	}

	/**
	 * workaround code because ICSourceEntry.getLocation returns null if the
	 * resolved flag is not set moreover the resolved flag is not set when adding a
	 * source location via the gui Therefore one can not convert from root to src
	 * based projects because the manually added src is not marked as resolved This
	 * code is the same as ICSourceEntry.getLocation except for it assumes the
	 * resolved flag is set
	 *
	 * @param entry
	 * @return
	 */
	private static IPath getLocation(ICSourceEntry entry) {
		if (!entry.isValueWorkspacePath())
			return new Path(entry.getValue());
		IPath path = new Path(entry.getValue());
		IResource rc = ResourcesPlugin.getWorkspace().getRoot().findMember(path);
		if (rc != null)
			return rc.getLocation();
		return null;
	}

	/**
	 * get the text for the decorator
	 *
	 * @param text
	 * @return
	 */
	@Override
	public String getDecoratedText(String text) {
		String boardName = myBoardDescription.getBoardName();
		String portName = myBoardDescription.getActualUploadPort();
		if (portName.isEmpty()) {
			portName = Messages.decorator_no_port;
		}
		if (boardName.isEmpty()) {
			boardName = Messages.decorator_no_platform;
		}

		return text + ' ' + boardName + ' ' + ':' + portName;
	}

	/**
	 * Synchronous upload of the sketch to the board returning the status.
	 *
	 * @param project
	 * @return the status of the upload. Status.OK means upload is OK
	 */
	@Override
	public IStatus upload() {

		Job upLoadJob = UploadSketchWrapper.upload(this);

		if (upLoadJob == null)
			return new Status(IStatus.ERROR, CORE_PLUGIN_ID, Messages.Upload_failed, null);
		try {
			upLoadJob.join();
			return upLoadJob.getResult();
		} catch (InterruptedException e) {
			return new Status(IStatus.ERROR, CORE_PLUGIN_ID, Messages.Upload_failed, e);
		}
	}

	@Override
	public IStatus upLoadUsingProgrammer() {
		return BuildTarget("uploadWithProgrammerWithoutBuild"); //$NON-NLS-1$
	}

	@Override
	public IStatus burnBootloader() {
		return BuildTarget("BurnBootLoader"); //$NON-NLS-1$
	}

	private IStatus BuildTarget(String targetName) {

		try {
			Map<String, String> args = new HashMap<>();
			args.put(AutoBuildProject.ARGS_BUILDER_KEY, AutoBuildProject.MAKE_BUILDER_ID);
			// args.put(AutoBuildProject.ARGS_CONFIGS_KEY, getAutoBuildDesc() );
			args.put(AutoBuildProject.ARGS_TARGET_KEY, targetName);

			getProject().build(IncrementalProjectBuilder.FULL_BUILD, AutoBuildProject.COMMON_BUILDER_ID, args,
					new NullProgressMonitor());
		} catch (CoreException e) {
			return new Status(IStatus.ERROR, CORE_PLUGIN_ID, e.getMessage(), e);
		}

//
//        try {
//            IMakeTargetManager targetManager = MakeCorePlugin.getDefault().getTargetManager();
//            IContainer targetResource = getAutoBuildDesc().getBuildFolder();
//            IMakeTarget itarget = targetManager.findTarget(targetResource, targetName);
//            if (itarget == null) {
//                itarget = targetManager.createTarget(getProject(), targetName,
//                        "org.eclipse.cdt.build.MakeTargetBuilder"); //$NON-NLS-1$
//                //if (itarget instanceof MakeTarget) {
//                //    itarget.setBuildTarget(targetName);
//                    targetManager.addTarget(targetResource, itarget);
//                //}
//            }
//            if (itarget != null) {
//                itarget.build(new NullProgressMonitor());
//            }
//        } catch (CoreException e) {
//            return new Status(IStatus.ERROR, CORE_PLUGIN_ID, e.getMessage(), e);
//        }
		return Status.OK_STATUS;
	}

	@Override
	public boolean canBeIndexed() {
		return true;
	}

	@Override
	public void setBoardDescription(BoardDescription boardDescription) {
		if (myBoardDescription != null && myBoardDescription.needsRebuild(boardDescription)) {
			getAutoBuildDescription().forceCleanBeforeBuild();
		}
		myBoardDescription = new BoardDescription(boardDescription);
		setIsDirty();
	}

	private void setIsDirty() {
		myMemoryIsDirty = true;
	}

	@Override
	public IFile getTargetFile() {
		// I assume the extension is .hex as the Arduino Framework does not provide the
		// extension nor a key for the uploadable sketch (=build target)
		// as currently this method is only used for network upload via yun this is ok
		// for now
		IProject project = getProject();
		return getAutoBuildDescription().getBuildFolder().getFile(project.getName() + ".hex"); //$NON-NLS-1$
	}

	@Override
	public String getBundelName() {
		return Activator.getId();
	}

	@Override
	public IAutoBuildConfigurationDescription getAutoBuildDesc() {
		return getAutoBuildDescription();
	}

	/**
	 * This method adds or updates the Arduino code links in a subfolder named
	 * Arduino/[cfg name]. 2 linked subfolders named core and variant link to the
	 * real Arduino code note
	 *
	 *
	 */
	private void updateArduinoCodeLinks() {

		IFolder arduinoVariantFolder = getArduinoVariantFolder();
		IFolder arduinoCodeFolder = getArduinoCoreFolder();
		NullProgressMonitor monitor = new NullProgressMonitor();
		try {
			arduinoVariantFolder.delete(true, monitor);
			arduinoCodeFolder.delete(true, monitor);
		} catch (CoreException e) {
			// ignore exception
			e.printStackTrace();
		}
		IPath corePath = myBoardDescription.getActualCoreCodePath();
		if (corePath != null) {
			Helpers.LinkFolderToFolder(corePath, arduinoCodeFolder);
		}
		IPath variantPath = myBoardDescription.getActualVariantPath();
		if (variantPath != null) {
			Helpers.LinkFolderToFolder(variantPath, arduinoVariantFolder);
		}
	}

	@Override
	public Set<IFolder> getIncludeFolders() {
		Set<IFolder> ret = new HashSet<>();
		ret.add(getArduinoCoreFolder());
		if (myBoardDescription.getActualVariantPath() != null) {
			ret.add(getArduinoVariantFolder());
		}
		try {
			if (getArduinoLibraryFolder().exists()) {
				for (IResource curMember : getArduinoLibraryFolder().members()) {
					if (curMember instanceof IFolder) {
						IFolder curFolder = (IFolder) curMember;
						IFolder srcFolder = curFolder.getFolder(SRC_FODER);
						if (srcFolder.exists()) {
							ret.add(srcFolder);
						} else {
							ret.add(curFolder);
						}
					}
				}
			}
		} catch (CoreException e) {
			e.printStackTrace();
		}
		return ret;
	}

	/**
	 * Get all the libraries that are linked to on disk
	 *
	 * @return
	 * @throws CoreException
	 */
	private Map<IPath, IArduinoLibraryVersion> getLibrariesFromLinks() {
		Map<IPath, IArduinoLibraryVersion> ret = new HashMap<>();
		IFolder libFolder = getArduinoLibraryFolder();
		if (!libFolder.exists()) {
			return ret;
		}
		try {
			for (IResource curResource : libFolder.members()) {
				if (curResource instanceof IFolder) {
					IFolder curFolder = (IFolder) curResource;
					IArduinoLibraryVersion curLib=null;
					for(IArduinoLibraryVersion curknowLib: myLibraries.values()) {
						if(curknowLib.getName().equals(curFolder.getName())){
							curLib=curknowLib;
							continue;
						}
					}
					if (curLib != null) {
						// We know the lib so it is ok
						ret.put(curLib.getFQN(), curLib);
						continue;
					}

					curLib = LibraryManager.getLibraryVersionFromLocation(curFolder, getBoardDescription());
					if (curLib != null) {
						ret.put(curLib.getFQN(), curLib);
					}
				}
			}
		} catch (CoreException e) {
			e.printStackTrace();
			ret.putAll(myLibraries);
		}
		return ret;
	}

	/**
	 * remove the links from the libraries on disk
	 *
	 */
	private void removeLibraryLinks() {
		IProgressMonitor monitor = new NullProgressMonitor();
		IFolder libFolder = getArduinoLibraryFolder();
		for (IArduinoLibraryVersion curLib : myLibraries.values()) {
			IFolder curLibFolder = libFolder.getFolder(curLib.getName());
			if (curLibFolder.exists()) {
				try {
					curLibFolder.delete(true, monitor);
				} catch (CoreException e) {
					// ignore exception
					e.printStackTrace();
				}
			}
		}
	}

	/**
	 * remove the links from the libraries on disk
	 *
	 */
	private void linkLibrariesToFolder() {
		IFolder libFolder = getArduinoLibraryFolder();
		for (IArduinoLibraryVersion curLib : myLibraries.values()) {
			IFolder curLibFolder = libFolder.getFolder(curLib.getName());
			Helpers.LinkFolderToFolder(curLib.getInstallPath(), curLibFolder);
		}
	}

	/**
	 * For libraries of with FQN Libraries/hardware/X make sure that the location
	 * point to a valid location of the given boardDescriptor
	 */
	private void upDateHardwareLibraries() {
		// make sure the libraries that link to hardware are the correct ones
		BoardDescription boardDesc = getBoardDescription();
		IPath referenceLibPath = boardDesc.getReferencedCoreLibraryPath();
		IPath referencingLibPath = boardDesc.getReferencingLibraryPath();
		if (referencingLibPath == null) {
			referencingLibPath = referenceLibPath;
		}
		Set<IPath> hardwareLibsFQN = new HashSet<>();
		for (IArduinoLibraryVersion curLib : myLibraries.values()) {
			if (curLib.isHardwareLib()) {
				IPath libPath = curLib.getInstallPath();
				if (referencingLibPath.isPrefixOf(libPath) || referenceLibPath.isPrefixOf(libPath)) {
					// the hardware lib is ok
					continue;
				}
				// The hardware lib is for a different hardware.
				// add it to the lists to reattach
				hardwareLibsFQN.add(curLib.getFQN());
			}
		}
		if (!hardwareLibsFQN.isEmpty()) {
			Map<String, IArduinoLibraryVersion> boardLibs = LibraryManager.getLibrariesHarware(boardDesc);
			for (IPath curReplaceLibFQN : hardwareLibsFQN) {
				IArduinoLibraryVersion newLib = boardLibs.get(curReplaceLibFQN.toPortableString());
				if (newLib != null) {
					// a library with the same name was found so use this one
					myLibraries.put(newLib.getFQN(), newLib);
				} else {
					// no new library was found remove the old lib
					myLibraries.remove(curReplaceLibFQN);
				}
			}
		}
	}

	@Override
	public void reAttachLibraries() {
		upDateHardwareLibraries();

		IProgressMonitor monitor = new NullProgressMonitor();
		IFolder libFolder = getArduinoLibraryFolder();
		// Remove all existing lib folders that are not known or are linking to the
		// wrong lib
		try {
			if (!libFolder.exists()) {
				libFolder.create(true, true, new NullProgressMonitor());
			}
			for (IResource curResource : libFolder.members()) {
				if (curResource instanceof IFolder) {
					IFolder curFolder = (IFolder) curResource;

					IArduinoLibraryVersion curLib =null;
					for(IArduinoLibraryVersion curknowLib: myLibraries.values()) {
						if(curknowLib.getName().equals(curFolder.getName())){
							curLib=curknowLib;
						}
					}

					if ((curLib == null) || (!curLib.getInstallPath().equals(curFolder.getLocation()))) {
						try {
							curFolder.delete(true, monitor);
						} catch (CoreException e) {
							e.printStackTrace();
						}
					}
				}
			}
		} catch (CoreException e) {
			e.printStackTrace();
		}

		// Add all remaining needed libs
		for (IArduinoLibraryVersion curLib : myLibraries.values()) {
			IFolder curLibFolder = libFolder.getFolder(curLib.getName());
			if (!curLibFolder.exists()) {
				Helpers.LinkFolderToFolder(curLib.getInstallPath(), curLibFolder);
			}
		}
	}

	@Override
	public Map<IPath, IArduinoLibraryVersion> getUsedLibraries() {
		myLibraries = getLibrariesFromLinks();
		return new HashMap<>(myLibraries);
	}

	@Override
	public boolean addLibraries(Collection<IArduinoLibraryVersion> librartiesToAdd) {
		if (librartiesToAdd == null || librartiesToAdd.isEmpty()) {
			return false;
		}
		List<IResource> foldersToRemoveFromBuildPath = new LinkedList<>();
		IFolder libFolder = getArduinoLibraryFolder();
		ICConfigurationDescription confdesc = getAutoBuildDesc().getCdtConfigurationDescription();
		ICSourceEntry[] sourceEntries = confdesc.getSourceEntries();
		for (IArduinoLibraryVersion curLib : librartiesToAdd) {
			if (curLib == null) {
				continue;
			}
			IFolder newLibFolder = libFolder.getFolder(curLib.getName());
			Helpers.LinkFolderToFolder(curLib.getInstallPath(), newLibFolder);
			myLibraries.put(curLib.getFQN(), curLib);

			// exclude bad folders
			File[] subFolders;
			subFolders = curLib.getInstallPath().toFile().listFiles();

			for (File subFolder : subFolders) {
				if (subFolder.isFile()) {
					continue;
				}
				String curName = subFolder.getName();
				if ("src".equals(curName)) { //$NON-NLS-1$
					continue;
				}
				IFolder curFolder = newLibFolder.getFolder(curName);
				if (CDataUtil.isExcluded(curFolder.getFullPath(), sourceEntries)) {
					continue;
				}
				foldersToRemoveFromBuildPath.add(curFolder);
			}
		}
		if (foldersToRemoveFromBuildPath.isEmpty()) {
			return false;
		}
		try {
			for (IResource subFolder : foldersToRemoveFromBuildPath) {
				sourceEntries = CDataUtil.setExcluded(subFolder.getFullPath(), true, true, sourceEntries);
			}
			confdesc.setSourceEntries(sourceEntries);
		} catch (CoreException e) {
			// ignore error
			e.printStackTrace();
		}
		return true;
	}


	@Override
	public boolean removeLibraries(Collection<IArduinoLibraryVersion> librariesToRemove) {
		boolean ret = false;
		IProgressMonitor monitor = new NullProgressMonitor();
		IFolder libFolder = getArduinoLibraryFolder();
		for (IArduinoLibraryVersion curLib : librariesToRemove) {
			if (myLibraries.containsKey(curLib.getFQN())) {
				ret = true;
				myLibraries.remove(curLib.getFQN());
				try {
					libFolder.getFolder(curLib.getName()).delete(true, monitor);
				} catch (CoreException e) {
					e.printStackTrace();
				}
			}
		}
		return ret;
	}

	@Override
	public void setLibraries(Set<IArduinoLibraryVersion> selectedLibraries) {
		myLibraries.clear();
		addLibraries(selectedLibraries);
		reAttachLibraries();
	}

	@Override
	public Set<String> getTeamDefaultExclusionKeys(String name) {
		Set<String> ret = new HashSet<>();
		ret.add(name + DOT + KEY_SLOEBER_UPLOAD_PORT);
		return ret;
	}

	@Override
	public boolean equals(AutoBuildConfigurationExtensionDescription base) {
		if (!(base instanceof SloeberConfiguration)) {
			return false;
		}
		SloeberConfiguration other = (SloeberConfiguration) base;
		if( myBoardDescription.equals(other.myBoardDescription) &&
				myOtherDesc.equals(other.myOtherDesc) &&
				myCompileDescription.equals(other.myCompileDescription) &&
				myLibraries.size()==other.myLibraries.size()) {
			for ( IArduinoLibraryVersion localValue : myLibraries.values()) {
				IArduinoLibraryVersion otherValue = other.myLibraries.get(localValue.getFQN());
				if (!localValue.equals(otherValue)) {
					return false;
				}
			}
			return true;
		}
		return false;
	}

	@Override
	public LinkedHashMap<String, String> getPrebuildSteps() {
		LinkedHashMap<String, String> ret = new LinkedHashMap<>();
		LinkedHashSet<String> hookNamess = new LinkedHashSet<>();
		hookNamess.add("prebuild"); //$NON-NLS-1$
		ret.putAll(myBoardDescription.getHookSteps(hookNamess, getAutoBuildDescription()));
		return ret;
	}

	@Override
	public LinkedHashMap<String, String> getPostbuildSteps() {
		LinkedHashMap<String, String> ret = new LinkedHashMap<>();
		LinkedHashSet<String> hookNamess = new LinkedHashSet<>();
		hookNamess.add("postbuild"); //$NON-NLS-1$
		ret.putAll(myBoardDescription.getHookSteps(hookNamess, getAutoBuildDescription()));
		return ret;
	}

	/**
	 * Because SloeberConfiguration are copied and can be changed at all times
	 * but there is only 1 disk representation of each config
	 *  we can not update the disk
	 * at the time the SloeberConfiguration is changed
	 *
	 * When a configuration becomes "the active" configuration this method will
	 *  create/update the necessary resources on disk.
	 *  This apply is when this configuration is new.
	 */
	public void aboutToApplyConfigChange() {
//		try {
//			myLibraries =getLibrariesFromLinks();
//		} catch (CoreException e) {
//			// ignore exception
//			e.printStackTrace();
//		}
		if (updateSourceEntries()) {
			// the config has been renamed;
			// remove the library links
			removeLibraryLinks();
		}
		configureWhenDirty();

	}

	public void appliedConfigChange() {
		LinkToCore();
		upDateHardwareLibraries();
		linkLibrariesToFolder();
	}

	/**
	 * Look at the source entries to see whether this is a rename
	 * If it is a rename update the source entries and update the arduino/[configName]
	 *
	 * return true if the config has been renamed otherwise false
	 */
	private boolean updateSourceEntries() {

		IFolder curArduinoConfigurationfolder = getArduinoConfigurationFolder();
		IFolder oldArduinoConfigurationfolder = null;

		// update the source entries

		ICSourceEntry[] orgSourceEntries = getAutoBuildDesc().getCdtConfigurationDescription().getSourceEntries();
		ICSourceEntry[] newSourceEntries = new ICSourceEntry[orgSourceEntries.length];
		for (int curItem = 0; curItem < orgSourceEntries.length; curItem++) {
			ICSourceEntry curEntry = orgSourceEntries[curItem];
			if (curArduinoConfigurationfolder.getFullPath().equals(curEntry.getFullPath())) {
				newSourceEntries[curItem] = orgSourceEntries[curItem];
				continue;
			}
			if (getArduinoRootFolder().getFullPath().isPrefixOf(curEntry.getFullPath())) {
				oldArduinoConfigurationfolder = getProject().getFolder(curEntry.getFullPath().removeFirstSegments(1));
					newSourceEntries[curItem]=new CSourceEntry(curArduinoConfigurationfolder, curEntry.getExclusionPatterns(), curEntry.getFlags());
			} else {
				newSourceEntries[curItem] = orgSourceEntries[curItem];
			}
		}
		if (oldArduinoConfigurationfolder == null) {
			return false;
		}
		try {
			if (oldArduinoConfigurationfolder.exists()) {
				NullProgressMonitor monitor = new NullProgressMonitor();
				IFolder deleteFolder = oldArduinoConfigurationfolder.getFolder(SLOEBER_VARIANT_FOLDER_NAME);
				if (deleteFolder.exists()) {
					deleteFolder.delete(true, monitor);
				}
				deleteFolder = oldArduinoConfigurationfolder.getFolder(SLOEBER_CODE_FOLDER_NAME);
				if (deleteFolder.exists()) {
					deleteFolder.delete(true, monitor);
				}
				if (oldArduinoConfigurationfolder.exists()) {
					oldArduinoConfigurationfolder.delete(true, monitor);
				}
			}
			getAutoBuildDesc().getCdtConfigurationDescription().setSourceEntries(newSourceEntries);

		} catch (Exception e) {
				Activator.log(new Status(IStatus.ERROR, CORE_PLUGIN_ID, Messages.SloeberConfiguration_Failed_Modify_config_rename, e));
		}
		return true;
	}

	private String getCDTConfName() {
		return getAutoBuildDescription().getCdtConfigurationDescription().getName();
	}

}
