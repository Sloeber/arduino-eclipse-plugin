package io.sloeber.core.internal;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import static io.sloeber.core.api.Common.*;
import static io.sloeber.core.api.Const.*;

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
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

import io.sloeber.autoBuild.api.AutoBuildConfigurationExtensionDescription;
import io.sloeber.autoBuild.api.AutoBuildProject;
import io.sloeber.autoBuild.api.IAutoBuildConfigurationDescription;
import io.sloeber.autoBuild.helpers.api.KeyValueTree;
import io.sloeber.autoBuild.integration.AutoBuildConfigurationDescription;
import io.sloeber.core.Activator;
import io.sloeber.core.Messages;
import io.sloeber.core.api.BoardDescription;
import io.sloeber.core.api.CompileDescription;
import io.sloeber.core.api.ConfigurationPreferences;
import io.sloeber.core.api.IArduinoLibraryVersion;
import io.sloeber.core.api.ISloeberConfiguration;
import io.sloeber.core.api.OtherDescription;
import io.sloeber.core.tools.Helpers;
import io.sloeber.core.tools.uploaders.UploadSketchWrapper;

public class SloeberConfiguration extends AutoBuildConfigurationExtensionDescription implements ISloeberConfiguration {
	// configuration data
	private BoardDescription myBoardDescription;
	private OtherDescription myOtherDesc;
	private CompileDescription myCompileDescription;
	private Map<String, IArduinoLibraryVersion> myLibraries = new HashMap<>();

	// operational data
	private boolean myMemoryIsDirty = true;
	private boolean myCalculatingEnvVars = false;

	// derived data
	private Map<String, String> myEnvironmentVariables = new HashMap<>();
	private final static String KEY="key"; //$NON-NLS-1$
	private final static String VALUE="value"; //$NON-NLS-1$

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
		myLibraries = src.myLibraries;
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
	public void serialize(KeyValueTree keyValuePairs) {
		Map<String, String> envVars = myBoardDescription.getEnvVarsConfig();
		envVars.putAll(myOtherDesc.getEnvVarsConfig());
		envVars.putAll(myCompileDescription.getEnvVarsConfig());
		int counter=0;
		for (Entry<String, String> curEnvVar : envVars.entrySet()) {
			KeyValueTree curKeyValue=keyValuePairs.addChild(String.valueOf(counter));
			curKeyValue.addChild( KEY, curEnvVar.getKey() );
			curKeyValue.addChild(VALUE , curEnvVar.getValue() );
			counter++;
		}
		configureIfDirty();
	}

	public SloeberConfiguration(IAutoBuildConfigurationDescription autoCfgDescription, KeyValueTree keyValues) {
		setAutoBuildDescription(autoCfgDescription);
		Map<String, String> envVars = new HashMap<>();
		for (KeyValueTree curChild : keyValues.getChildren().values()) {
			String key=curChild.getValue(KEY);
			String value=curChild.getValue(VALUE);
				envVars.put(key, value);
		}
		myBoardDescription = new BoardDescription(envVars);
		myOtherDesc = new OtherDescription(envVars);
		myCompileDescription = new CompileDescription(envVars);
		myMemoryIsDirty = true;
		// configure(); Seems I can not dpo the config here
	}

	@Override
	public IProject getProject() {
		return getAutoBuildDescription().getProject();
	}

	@Override
	public IFolder getArduinoCodeFolder() {
		String cdtConfDescName = getAutoBuildDescription().getCdtConfigurationDescription().getName();
		IProject project = getProject();
		return project.getFolder(SLOEBER_ARDUINO_FOLDER_NAME).getFolder(cdtConfDescName);
	}

	@Override
	public IFolder getArduinoCoreFolder() {
		return getArduinoCodeFolder().getFolder(SLOEBER_CODE_FOLDER_NAME);
	}

	@Override
	public IFolder getArduinoVariantFolder() {
		return getArduinoCodeFolder().getFolder(SLOEBER_VARIANT_FOLDER_NAME);
	}

	@Override
	public IFolder getArduinoLibraryFolder() {
		return getArduinoCodeFolder().getFolder(SLOEBER_LIBRARY_FOLDER_NAME);
	}

	@Override
	public Map<String, String> getEnvironmentVariables() {
		configureIfDirty();

		return myEnvironmentVariables;
	}

	private void configureIfDirty() {
		if (myMemoryIsDirty) {
			getEnvVars();
			myMemoryIsDirty = false;
		}
		if (!ResourcesPlugin.getWorkspace().isTreeLocked()) {
			if (projectNeedsUpdate()) {
				updateArduinoCodeLinks();
			}
		}
	}

	private boolean projectNeedsUpdate() {
		IPath corePath = myBoardDescription.getActualCoreCodePath();
		IFolder coreFolder = getArduinoCoreFolder();
		if (!coreFolder.getLocation().equals(corePath)) {
//        	System.out.println("projectNeedsUpdate core Folder mismatch");
//        	System.out.println("corefolder "+coreFolder.getLocation());
//        	System.out.println("corePath   "+corePath);
			return true;
		}
		IFolder arduinoVariantFolder = getArduinoVariantFolder();
		IPath variantPath = myBoardDescription.getActualVariantPath();
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

	private void getEnvVars() {
		if (myCalculatingEnvVars) {
			return;
		}
		try {
			myCalculatingEnvVars = true;
			IProject project = getProject();

			myEnvironmentVariables.clear();

			myEnvironmentVariables.put(ENV_KEY_BUILD_SOURCE_PATH, project.getLocation().toOSString());
			myEnvironmentVariables.put(ENV_KEY_BUILD_PATH,
					getAutoBuildDescription().getBuildFolder().getLocation().toOSString());

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
		} finally {
			myCalculatingEnvVars = false;
		}
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
			// not sure if this is needed
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
		IPath corePath = myBoardDescription.getActualCoreCodePath();
		IFolder arduinoVariantFolder = getArduinoVariantFolder();
		if (corePath != null) {
			Helpers.LinkFolderToFolder(corePath, getArduinoCoreFolder());
			IPath variantPath = myBoardDescription.getActualVariantPath();
			if ((variantPath == null) || (!variantPath.toFile().exists())) {
				// remove the existing link
				Helpers.removeCodeFolder(arduinoVariantFolder);
			} else {
				Helpers.LinkFolderToFolder(variantPath, arduinoVariantFolder);
			}
		}
	}

	@Override
	public Set<IFolder> getIncludeFolders() {
		Set<IFolder> ret = new HashSet<>();
		ret.add(getArduinoCoreFolder());
		ret.add(getArduinoVariantFolder());
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
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return ret;
	}

	@Override
	public void reAttachLibraries() {
		IProgressMonitor monitor = new NullProgressMonitor();
		IFolder libFolder = getArduinoLibraryFolder();
		// Remove all existing lib folders that are not known or are linking to the
		// wrong lib
		try {
			for (IResource curResource : libFolder.members()) {
				if (curResource instanceof IFolder) {
					IFolder curFolder = (IFolder) curResource;
					IArduinoLibraryVersion curLib = myLibraries.get(curFolder.getName());
					if ((curLib == null) || (!curLib.getInstallPath().equals(curFolder.getLocation()))) {
						try {
							curFolder.delete(true, monitor);
						} catch (CoreException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				}
			}
		} catch (CoreException e) {
			// TODO Auto-generated catch block
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
	public Map<String, IArduinoLibraryVersion> getUsedLibraries() {
		return new HashMap<>(myLibraries);
	}

	@Override
	public boolean addLibraries(Collection<IArduinoLibraryVersion> librartiesToAdd) {
		boolean ret = false;
		IFolder libFolder = getArduinoLibraryFolder();
		for (IArduinoLibraryVersion curLib : librartiesToAdd) {
			if (curLib == null) {
				continue;
			}
			Helpers.LinkFolderToFolder(curLib.getInstallPath(), libFolder.getFolder(curLib.getName()));
			myLibraries.put(curLib.getName(), curLib);
			ret = true;
		}
		return ret;
	}

	@Override
	public boolean removeLibraries(Collection<IArduinoLibraryVersion> librariesToRemove) {
		boolean ret = false;
		IProgressMonitor monitor = new NullProgressMonitor();
		IFolder libFolder = getArduinoLibraryFolder();
		for (IArduinoLibraryVersion curLib : librariesToRemove) {
			if (myLibraries.containsKey(curLib.getName())) {
				ret = true;
				myLibraries.remove(curLib.getName());
				try {
					libFolder.getFolder(curLib.getName()).delete(true, monitor);
				} catch (CoreException e) {
					// TODO Auto-generated catch block
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

}
