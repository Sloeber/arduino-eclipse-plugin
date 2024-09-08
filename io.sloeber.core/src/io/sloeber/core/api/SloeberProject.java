package io.sloeber.core.api;

import static io.sloeber.core.api.Const.*;

import java.io.File;
import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.eclipse.cdt.core.CCProjectNature;
import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.settings.model.CSourceEntry;
import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.cdt.core.settings.model.ICProjectDescription;
import org.eclipse.cdt.core.settings.model.ICSettingEntry;
import org.eclipse.cdt.core.settings.model.ICSourceEntry;
import org.eclipse.cdt.core.settings.model.util.CDataUtil;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.ICoreRunnable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;

import io.sloeber.arduinoFramework.api.BoardDescription;
import io.sloeber.arduinoFramework.api.IArduinoLibraryVersion;
import io.sloeber.autoBuild.api.AutoBuildProject;
import io.sloeber.autoBuild.api.IAutoBuildConfigurationDescription;
import io.sloeber.autoBuild.buildTools.api.IBuildTools;
import io.sloeber.autoBuild.buildTools.api.IBuildToolsManager;
import io.sloeber.autoBuild.helpers.api.KeyValueTree;
import io.sloeber.autoBuild.integration.AutoBuildConfigurationDescription;
import io.sloeber.autoBuild.integration.AutoBuildManager;
import io.sloeber.autoBuild.schema.api.IProjectType;
import io.sloeber.core.Activator;
import io.sloeber.core.internal.SloeberConfiguration;
import io.sloeber.core.natures.SloeberNature;
import io.sloeber.core.txt.TxtFile;

public class SloeberProject extends Common {
	public static String LATEST_EXTENSION_POINT_ID = "io.sloeber.autoBuild.buildDefinitions"; //$NON-NLS-1$
	public static String LATEST_EXTENSION_ID = "io.sloeber.builddef"; //$NON-NLS-1$
	public static String PROJECT_ID = "io.sloeber.core.sketch"; //$NON-NLS-1$
	private static String SLOEBER_BUILD_TOOL_PROVIDER_ID = "io.sloeber.core.arduino.ToolProvider"; //$NON-NLS-1$
	private static String SOURCE_ENTRY_FILTER_ALL = "/**"; //$NON-NLS-1$

	public static void convertToArduinoProject(IProject project, IProgressMonitor monitor) {
		if (project == null) {
			Activator.log(new Status(IStatus.ERROR, Activator.getId(),
					"The provided project is null. Sloeber can not upgrade."));
			return;
		}
		final IWorkspace workspace = ResourcesPlugin.getWorkspace();
		IWorkspaceRoot root = workspace.getRoot();
		ICoreRunnable runnable = new ICoreRunnable() {
			@Override
			public void run(IProgressMonitor internalMonitor) throws CoreException {
				//remove managed build nature if it exists
				String managedBuildNature="org.eclipse.cdt.managedbuilder.core.managedBuildNature"; //$NON-NLS-1$
				String ScannerConfigNature="org.eclipse.cdt.managedbuilder.core.ScannerConfigNature"; //$NON-NLS-1$
				if(project.hasNature(managedBuildNature) || project.hasNature(ScannerConfigNature)) {

			        IProjectDescription description = project.getDescription();
			        Set<String> curNatures = new HashSet<>(Arrays.asList(description.getNatureIds()));
			        curNatures.remove(managedBuildNature);
			        curNatures.remove(ScannerConfigNature);
			        description.setNatureIds(curNatures.toArray(new String[curNatures.size()]));
			        project.setDescription(description, monitor);
				}
				IFolder OldCoreFolder =project.getFolder("core"); //$NON-NLS-1$
				if(OldCoreFolder.exists()) {
					OldCoreFolder.delete(true, monitor);
				}

//				IFile CdtDotFile = project.getFile(".cproject"); //$NON-NLS-1$
//				if(CdtDotFile.exists()) {
//					CdtDotFile.delete(true, monitor);
//				}

				File sloeberDotFile = project.getFile(".sproject").getLocation().toFile(); //$NON-NLS-1$
				File sloeberCfgFile = project.getFile(SLOEBER_CFG).getLocation().toFile();
				TxtFile oldSloeberInfo = null;
				if (sloeberDotFile.exists() && sloeberCfgFile.exists()) {
					oldSloeberInfo = new TxtFile(sloeberDotFile);
					oldSloeberInfo.mergeFile(sloeberCfgFile);
				} else {
					if (sloeberDotFile.exists()) {
						oldSloeberInfo = new TxtFile(sloeberDotFile);
					}
					if (sloeberCfgFile.exists()) {
						oldSloeberInfo = new TxtFile(sloeberCfgFile);
					}
				}

				String builderName = AutoBuildProject.INTERNAL_BUILDER_ID;
				IBuildTools buildTools = IBuildToolsManager.getDefault().getBuildTools(SLOEBER_BUILD_TOOL_PROVIDER_ID,
						null);
				IProjectType projectType = AutoBuildManager.getProjectType(LATEST_EXTENSION_POINT_ID,
						LATEST_EXTENSION_ID, PROJECT_ID, true);
				CodeDescription codeProvider =  CodeDescription.createNone();
				AutoBuildProject.createProject(project.getName(), null, projectType, builderName,
						CCProjectNature.CC_NATURE_ID, codeProvider, buildTools, true, internalMonitor);

				SloeberNature.addNature(project, internalMonitor);

				CCorePlugin cCorePlugin = CCorePlugin.getDefault();
				ICProjectDescription prjCDesc = cCorePlugin.getProjectDescription(project, true);

				Set <String>cfgNames=new HashSet<>();
				for (ICConfigurationDescription curConfig : prjCDesc.getConfigurations()) {
					cfgNames.add( curConfig.getName());
				}
				KeyValueTree oldConfigs=null;
				if (oldSloeberInfo != null) {
					oldConfigs = oldSloeberInfo.getData().getChild("Config"); //$NON-NLS-1$
					for (String curTree : oldConfigs.getChildren().keySet()) {
						cfgNames.add(curTree);
					}
				}
				for(String cfgName:cfgNames) {
					KeyValueTree oldConfig=null;
					if (oldConfigs != null) {
						oldConfig = oldConfigs.getChild(cfgName);
					}
					//get the CDT config and AutoBuildConfigurationDescription (if it does not exist create it)
					ICConfigurationDescription curConfig = prjCDesc.getConfigurationByName(cfgName);
					if(curConfig==null) {
						//config does not exists so create it
						String id = CDataUtil.genId(Activator.getId());
						curConfig = prjCDesc.createConfiguration(id,cfgName,prjCDesc.getActiveConfiguration());
					}
					IAutoBuildConfigurationDescription autoConf = IAutoBuildConfigurationDescription
							.getConfig(curConfig);
					if (!(autoConf instanceof AutoBuildConfigurationDescription)) {
						// this should not happen as we just created a autoBuild project
						Activator.log(new Status(SLOEBER_STATUS_DEBUG, Activator.getId(),
								"\"Auto build created a project that does not seem to be a autobuild project :-s : " //$NON-NLS-1$
										+ project.getName()));
						continue;
					}




					ICSourceEntry newSourceEntries[] = new ICSourceEntry[2];
					// old Sloeber project so the code is in the root of the project for sure
					// as we are at the root
					// exclude bin folder and arduino folder
					IPath excludes[] = new IPath[2];
					excludes[0] = autoConf.getBuildFolder().getProjectRelativePath().removeLastSegments(1)
							.append(SOURCE_ENTRY_FILTER_ALL);
					excludes[1] = project.getFolder(SLOEBER_ARDUINO_FOLDER_NAME).getProjectRelativePath()
							.append(SOURCE_ENTRY_FILTER_ALL);
					newSourceEntries[0] = new CSourceEntry(project.getFullPath(), excludes, ICSettingEntry.RESOLVED);
					IPath excludes2[] = new IPath[8];
					excludes2[0] = IPath.fromOSString("**/*.ino"); //$NON-NLS-1$
					excludes2[1] = IPath.fromOSString("libraries/?*/**/doc*/**"); //$NON-NLS-1$
					excludes2[2] = IPath.fromOSString("libraries/?*/**/?xamples/**"); //$NON-NLS-1$
					excludes2[3] = IPath.fromOSString("libraries/?*/**/?xtras/**"); //$NON-NLS-1$
					excludes2[4] = IPath.fromOSString("libraries/?*/**/test*/**"); //$NON-NLS-1$
					excludes2[5] = IPath.fromOSString("libraries/?*/**/third-party/**"); //$NON-NLS-1$
					excludes2[6] = IPath.fromOSString("libraries/**/._*"); //$NON-NLS-1$
					excludes2[7] = IPath.fromOSString("libraries/?*/utility/*/*"); //$NON-NLS-1$

					/*
					 * CDT currently causes issues with ${ConfigName]
					 * https://github.com/eclipse-cdt/cdt/issues/870 IPath arduinoRoot =
					 * newProjectHandle.getFolder(SLOEBER_ARDUINO_FOLDER_NAME).getFullPath().append(
					 * CONFIG_NAME_VARIABLE);
					 */
					IPath arduinoRoot = project.getFolder(SLOEBER_ARDUINO_FOLDER_NAME).getFullPath()
							.append(cfgName);
					newSourceEntries[1] = new CSourceEntry(arduinoRoot, excludes2, ICSettingEntry.NONE);
					curConfig.setSourceEntries(newSourceEntries);

					//Get the Sloeber information from the original project (and if nothing found use defaults)
					BoardDescription boardDescriptor = getBoardDescription(oldConfig);
					CompileDescription compileDescriptor = getCompileDescription(oldConfig);
					OtherDescription otherDesc = getOtherDescription(oldConfig);

					//Set the Sloeber configuration based on to the old project
					autoConf.setIsParallelBuild(compileDescriptor.isParallelBuildEnabled());
					SloeberConfiguration sloeberConfiguration = new SloeberConfiguration(boardDescriptor, otherDesc,
							compileDescriptor);
					//Save the sloeber configuration in the autoBuild configuration
					autoConf.setAutoBuildConfigurationExtensionDescription(sloeberConfiguration);
				}

				SubMonitor refreshMonitor = SubMonitor.convert(internalMonitor, 3);
				project.open(refreshMonitor);
				project.refreshLocal(IResource.DEPTH_INFINITE, refreshMonitor);
				prjCDesc.setCdtProjectCreated();
				cCorePlugin.setProjectDescription(project, prjCDesc, true, SubMonitor.convert(internalMonitor, 1));
				project.close(monitor);
				project.open(monitor);

				Activator.log(new Status(SLOEBER_STATUS_DEBUG, Activator.getId(),
						"internal creation of project is done: " + project.getName())); //$NON-NLS-1$
				// IndexerController.index(newProjectHandle);
			}
		};
		try

		{
			workspace.run(runnable, root, IWorkspace.AVOID_UPDATE, monitor);
		} catch (Exception e) {
			Activator.log(new Status(IStatus.INFO, io.sloeber.core.Activator.getId(),
					"Project creation failed: " + project.getName(), e)); //$NON-NLS-1$
		}
		monitor.done();
	}

	private static OtherDescription getOtherDescription(KeyValueTree config) {
		OtherDescription ret =new OtherDescription();
		if (config == null) {
			return ret;
		}
		ret.setVersionControlled(Boolean.valueOf(config.getValue("other.IS_VERSION_CONTROLLED")).booleanValue()); //$NON-NLS-1$
		return ret;
	}

	private static CompileDescription getCompileDescription(KeyValueTree oldConfig) {
		CompileDescription ret = new CompileDescription();
		KeyValueTree compileConfig=oldConfig.getChild("compile").getChild("sloeber"); //$NON-NLS-1$ //$NON-NLS-2$
		if (compileConfig == null) {
			return ret;
		}
		KeyValueTree extraConfig=compileConfig.getChild("extra"); //$NON-NLS-1$
	 	ret.set_All_CompileOptions(extraConfig.getValue("all")); //$NON-NLS-1$
	 	ret.set_Archive_CompileOptions(extraConfig.getValue("archive")); //$NON-NLS-1$
	 	ret.set_Assembly_CompileOptions(extraConfig.getValue("assembly")); //$NON-NLS-1$
	 	ret.set_C_CompileOptions(extraConfig.getValue("c.compile")); //$NON-NLS-1$
	 	ret.set_CPP_CompileOptions(extraConfig.getValue("cpp.compile")); //$NON-NLS-1$
	 	ret.set_Link_CompileOptions(extraConfig.getValue("link")); //$NON-NLS-1$
	 	ret.set_C_andCPP_CompileOptions(extraConfig.getValue("compile")); //$NON-NLS-1$


//	 	ret.setSizeCommand(extraConfig.getValue("compile")); //$NON-NLS-1$
//
//
//				Config.Release.compile.sloeber.size.custom=
//				Config.Release.compile.sloeber.size.type=RAW_RESULT
//				Config.Release.compile.sloeber.warning_level=NONE
//				Config.Release.compile.sloeber.warning_level.custom=

		return ret;
	}

	private static BoardDescription getBoardDescription(KeyValueTree oldConfig) {
		if (oldConfig == null) {
			return new BoardDescription();
		}
		String boardsFileString=oldConfig.getValue("board.BOARD.TXT"); //$NON-NLS-1$
		String boardID=oldConfig.getValue("board.BOARD.ID"); //$NON-NLS-1$

		if(boardsFileString.isBlank() || boardID.isBlank()) {
			return new BoardDescription();
		}

		KeyValueTree optionsHolder=oldConfig.getChild("board.BOARD.MENU"); //$NON-NLS-1$

		Map<String, String> options=new HashMap<>();
		for(KeyValueTree curOption:optionsHolder.getChildren().values()) {
			options.put(curOption.getKey(), curOption.getValue());
		}
		File boardsFile=new File(boardsFileString);

		BoardDescription ret= new BoardDescription( boardsFile,  boardID, options);
		String uploadPort=oldConfig.getValue("board.UPLOAD.PORT"); //$NON-NLS-1$
		ret.setUploadPort(uploadPort);

		return ret;
	}

	/**
	 * convenient method to create project
	 *
	 * @param proj1Name
	 * @param object
	 * @param proj1BoardDesc
	 * @param codeDesc
	 * @param proj1CompileDesc
	 * @param otherDesc
	 * @param nullProgressMonitor
	 * @return
	 */
	public static IProject createArduinoProject(String projectName, URI projectURI, BoardDescription boardDescriptor,
			CodeDescription codeDesc, CompileDescription compileDescriptor, IProgressMonitor monitor) {
		return createArduinoProject(projectName, projectURI, boardDescriptor, codeDesc, compileDescriptor, null, null,
				monitor);
	}

	public static IProject createArduinoProject(String projectName, URI projectURI, BoardDescription boardDescriptor,
			CodeDescription codeDesc, CompileDescription compileDescriptor, String builderName,
			IProgressMonitor monitor) {
		return createArduinoProject(projectName, projectURI, boardDescriptor, codeDesc, compileDescriptor, null,
				builderName, monitor);
	}

	public static IProject createArduinoProject(String projectName, URI projectURI, BoardDescription boardDescriptor,
			CodeDescription codeDesc, CompileDescription compileDescriptor, OtherDescription otherDesc,
			IProgressMonitor monitor) {
		return createArduinoProject(projectName, projectURI, boardDescriptor, codeDesc, compileDescriptor, otherDesc,
				null, monitor);
	}

	/*
	 * Method to create a project based on the board
	 */
	public static IProject createArduinoProject(String projectName, URI projectURI, BoardDescription boardDescriptor,
			CodeDescription codeDesc, CompileDescription compileDescriptor, OtherDescription inOtherDesc,
			String inBuilderName, IProgressMonitor monitor) {
		OtherDescription otherDesc = inOtherDesc == null ? new OtherDescription() : inOtherDesc;
		String builderName = inBuilderName == null ? AutoBuildProject.INTERNAL_BUILDER_ID : inBuilderName;

		String realProjectName = makeNameCompileSafe(projectName);

		final IWorkspace workspace = ResourcesPlugin.getWorkspace();
		IWorkspaceRoot root = workspace.getRoot();
		ICoreRunnable runnable = new ICoreRunnable() {
			@Override
			public void run(IProgressMonitor internalMonitor) throws CoreException {
				IProject newProjectHandle = root.getProject(realProjectName);
				// IndexerController.doNotIndex(newProjectHandle);

				IBuildTools buildTools = IBuildToolsManager.getDefault().getBuildTools(SLOEBER_BUILD_TOOL_PROVIDER_ID,
						null);
				IProjectType projectType = AutoBuildManager.getProjectType(LATEST_EXTENSION_POINT_ID,
						LATEST_EXTENSION_ID, PROJECT_ID, true);
				newProjectHandle = AutoBuildProject.createProject(realProjectName, projectURI, projectType, builderName,
						CCProjectNature.CC_NATURE_ID, codeDesc, buildTools, true, internalMonitor);

				String rootCodeFolder = codeDesc.getCodeFolder();
				// Add the sketch code
				Set<IArduinoLibraryVersion> librariesToAdd = codeDesc.getNeededLibraries();

				SloeberNature.addNature(newProjectHandle, internalMonitor);

				CCorePlugin cCorePlugin = CCorePlugin.getDefault();
				ICProjectDescription prjCDesc = cCorePlugin.getProjectDescription(newProjectHandle, true);

				for (ICConfigurationDescription curConfig : prjCDesc.getConfigurations()) {

					IAutoBuildConfigurationDescription autoConf = IAutoBuildConfigurationDescription
							.getConfig(curConfig);
					ICSourceEntry newSourceEntries[] = new ICSourceEntry[2];
					if (rootCodeFolder == null || rootCodeFolder.isBlank()) {
						// as we are at the root
						// exclude bin folder and arduino folder
						IPath excludes[] = new IPath[2];
						excludes[0] = autoConf.getBuildFolder().getProjectRelativePath().removeLastSegments(1)
								.append(SOURCE_ENTRY_FILTER_ALL);
						excludes[1] = newProjectHandle.getFolder(SLOEBER_ARDUINO_FOLDER_NAME).getProjectRelativePath()
								.append(SOURCE_ENTRY_FILTER_ALL);
						newSourceEntries[0] = new CSourceEntry(newProjectHandle.getFullPath(), excludes,
								ICSettingEntry.RESOLVED);
					} else {
						// no need to exclude any folder as we have a dedicated code folder
						IPath path = newProjectHandle.getFolder(rootCodeFolder).getFullPath();
						newSourceEntries[0] = new CSourceEntry(path, null, ICSettingEntry.RESOLVED);
					}
					IPath excludes[] = new IPath[8];
					excludes[0] = IPath.fromOSString("**/*.ino"); //$NON-NLS-1$
					excludes[1] = IPath.fromOSString("libraries/?*/**/doc*/**"); //$NON-NLS-1$
					excludes[2] = IPath.fromOSString("libraries/?*/**/?xamples/**"); //$NON-NLS-1$
					excludes[3] = IPath.fromOSString("libraries/?*/**/?xtras/**"); //$NON-NLS-1$
					excludes[4] = IPath.fromOSString("libraries/?*/**/test*/**"); //$NON-NLS-1$
					excludes[5] = IPath.fromOSString("libraries/?*/**/third-party/**"); //$NON-NLS-1$
					excludes[6] = IPath.fromOSString("libraries/**/._*"); //$NON-NLS-1$
					excludes[7] = IPath.fromOSString("libraries/?*/utility/*/*"); //$NON-NLS-1$

					/*
					 * CDT currently causes issues with ${ConfigName]
					 * https://github.com/eclipse-cdt/cdt/issues/870 IPath arduinoRoot =
					 * newProjectHandle.getFolder(SLOEBER_ARDUINO_FOLDER_NAME).getFullPath().append(
					 * CONFIG_NAME_VARIABLE);
					 */
					IPath arduinoRoot = newProjectHandle.getFolder(SLOEBER_ARDUINO_FOLDER_NAME).getFullPath()
							.append(curConfig.getName());
					newSourceEntries[1] = new CSourceEntry(arduinoRoot, excludes, ICSettingEntry.NONE);
					curConfig.setSourceEntries(newSourceEntries);
					IAutoBuildConfigurationDescription iAutoBuildConfig = IAutoBuildConfigurationDescription
							.getConfig(curConfig);
					if (!(iAutoBuildConfig instanceof AutoBuildConfigurationDescription)) {
						// this should not happen as we just created a autoBuild project
						Activator.log(new Status(SLOEBER_STATUS_DEBUG, Activator.getId(),
								"\"Auto build created a project that does not seem to be a autobuild project :-s : " //$NON-NLS-1$
										+ realProjectName));
						continue;
					}
					AutoBuildConfigurationDescription autoBuildConfig = (AutoBuildConfigurationDescription) iAutoBuildConfig;
					autoBuildConfig.setIsParallelBuild(compileDescriptor.isParallelBuildEnabled());
					SloeberConfiguration sloeberConfiguration = new SloeberConfiguration(boardDescriptor, otherDesc,
							compileDescriptor);
					autoBuildConfig.setAutoBuildConfigurationExtensionDescription(sloeberConfiguration);
					sloeberConfiguration.addLibraries(librariesToAdd);
				}

				SubMonitor refreshMonitor = SubMonitor.convert(internalMonitor, 3);
				newProjectHandle.open(refreshMonitor);
				newProjectHandle.refreshLocal(IResource.DEPTH_INFINITE, refreshMonitor);
				prjCDesc.setCdtProjectCreated();
				cCorePlugin.setProjectDescription(newProjectHandle, prjCDesc, true,
						SubMonitor.convert(internalMonitor, 1));

				Activator.log(new Status(SLOEBER_STATUS_DEBUG, Activator.getId(),
						"internal creation of project is done: " + realProjectName)); //$NON-NLS-1$
				// IndexerController.index(newProjectHandle);
			}
		};

		try {
			workspace.run(runnable, root, IWorkspace.AVOID_UPDATE, monitor);
		} catch (Exception e) {
			Activator.log(new Status(IStatus.INFO, io.sloeber.core.Activator.getId(),
					"Project creation failed: " + realProjectName, e)); //$NON-NLS-1$
		}
		monitor.done();
		return root.getProject(realProjectName);
	}

}
