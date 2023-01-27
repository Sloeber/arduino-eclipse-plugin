package io.sloeber.autoBuild.extensionPoint.providers;

import static io.sloeber.autoBuild.Internal.ManagedBuildConstants.MAKE_ADDITION;
import static io.sloeber.autoBuild.core.Messages.*;
import static io.sloeber.autoBuild.extensionPoint.providers.ManagebBuildCommon.*;
import static io.sloeber.autoBuild.integration.Const.*;

import java.text.MessageFormat;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.eclipse.cdt.core.settings.model.CSourceEntry;
import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.cdt.core.settings.model.ICSettingEntry;
import org.eclipse.cdt.core.settings.model.ICSourceEntry;
import org.eclipse.cdt.core.settings.model.util.CDataUtil;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceProxy;
import org.eclipse.core.resources.IResourceProxyVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;

import io.sloeber.autoBuild.api.IBuildMacroProvider;
import io.sloeber.autoBuild.core.Activator;
import io.sloeber.autoBuild.extensionPoint.IMakefileGenerator;
import io.sloeber.autoBuild.integration.AutoBuildConfigurationData;
import io.sloeber.schema.api.IBuilder;
import io.sloeber.schema.api.IConfiguration;
import io.sloeber.schema.api.IInputType;
import io.sloeber.schema.api.IOutputType;
import io.sloeber.schema.api.ITool;

/**
 * This is the default makefile generator 
 * Feel free to extend to add the flavors you need
 *
 * @noinstantiate This class is not intended to be instantiated by clients.
 */
public class MakefileGenerator implements IMakefileGenerator {
	// Local variables needed by generator
	IConfiguration myConfig;
	IProject myProject;
	ICConfigurationDescription myCConfigurationDescription;
	IFolder myTopBuildDir;
	ICSourceEntry[] mySrcEntries;
	MakeRules myMakeRules =null;
	Set<IFolder> myFoldersToBuild = null;

	/****************************************************************************************
	 * CONSTRUCTOR / INITIALIZING code / overrides
	 *****************************************************************************************/

	public MakefileGenerator() {
		super();
	}

	@Override
	public void initialize(int buildKind, IProject project, ICConfigurationDescription cfg, IBuilder builder) {
		myProject = project;
		myCConfigurationDescription = cfg;
		AutoBuildConfigurationData autoBuildData = (AutoBuildConfigurationData) cfg.getConfigurationData();
		myConfig = autoBuildData.getConfiguration();
		myTopBuildDir = myConfig.getBuildFolder(cfg);

		// Get the target info
		String buildTargetName;
		String buildTargetExt;
		buildTargetName = myConfig.getArtifactName();
		// Get its extension
		buildTargetExt = myConfig.getArtifactExtension();
		// try to resolve the build macros in the target extension
		buildTargetExt = resolveValueToMakefileFormat(buildTargetExt, EMPTY_STRING, BLANK,
				IBuildMacroProvider.CONTEXT_CONFIGURATION, myCConfigurationDescription);
		// try to resolve the build macros in the target name
		String resolved = resolveValueToMakefileFormat(buildTargetName, EMPTY_STRING, BLANK,
				IBuildMacroProvider.CONTEXT_CONFIGURATION, myCConfigurationDescription);
		if (resolved != null) {
			resolved = resolved.trim();
			if (resolved.length() > 0)
				buildTargetName = resolved;
		}
		if (buildTargetExt == null) {
			buildTargetExt = EMPTY_STRING;
		}

		// TOFIX JABA currently the source entries are always null
		// need to revisit this after storing the data to activate the exclude from
		// build functionality
		// get the source entries
		List<ICSourceEntry> srcEntries = myConfig.getSourceEntries();
		if (srcEntries.size() == 0) {
			// srcEntries = new LinkedList<ICSourceEntry>();
			srcEntries.add(
					new CSourceEntry(Path.EMPTY, null, ICSettingEntry.RESOLVED | ICSettingEntry.VALUE_WORKSPACE_PATH));
		} else {

			ICSourceEntry[] resolvedEntries = CDataUtil.resolveEntries(srcEntries.toArray(new ICSourceEntry[0]),
					myCConfigurationDescription);
			for (ICSourceEntry curEntry : resolvedEntries) {
				srcEntries.add(curEntry);
			}
		}
		mySrcEntries = srcEntries.toArray(new ICSourceEntry[srcEntries.size()]);
	}
	
	@Override
	public void regenerateDependencies(boolean force, IProgressMonitor monitor) throws CoreException {
		localgenerateMakefiles(monitor);
	}

	@Override
	public void generateDependencies(IProgressMonitor monitor) throws CoreException {
		localgenerateMakefiles(monitor);
	}

	@Override
	public MultiStatus generateMakefiles(IResourceDelta delta, IProgressMonitor monitor) throws CoreException {
		return localgenerateMakefiles(monitor);
	}
	
	@Override
	public MultiStatus regenerateMakefiles(IProgressMonitor monitor) throws CoreException {
		return localgenerateMakefiles(monitor);
	}

	/****************************************************************************************
	 * Make rule generation code
	 *****************************************************************************************/

	/**
	 * This class is used to recursively walk the project and create make rules for
	 * all appropriate files found
	 */
	class SourceLevelMakeRuleGenerator implements IResourceProxyVisitor {

		@Override
		public boolean visit(IResourceProxy proxy) throws CoreException {
			IResource resource = proxy.requestResource();
			if (resource.isDerived()) {
				return false;
			}
			boolean isExcluded = !CDataUtil.isExcluded(resource.getLocation(), mySrcEntries);
			if (isExcluded) {
				return false;
			}
			if (proxy.getType() == IResource.FILE) {
				if (getMakeRulesFromSourceFile((IFile) resource)) {
					myFoldersToBuild.add((IFolder) ((IFile) resource).getParent());
				}
				return false;
			}
			return true;
		}

		/**
		 * For the found file gives the makerules that need to be executed
		 * 
		 * @param inputFile
		 * @return true if a makerule has been created
		 */
		private boolean getMakeRulesFromSourceFile(IFile inputFile) {
			boolean ret = false;

			String ext = inputFile.getFileExtension();
			if (ext == null || ext.isBlank()) {
				return ret;
			}

			for (ITool tool : myConfig.getToolChain().getTools()) {
				for (IInputType inputType : tool.getInputTypes()) {
					if (inputType.isAssociatedWith(inputFile)) {
						for (IOutputType outputType : tool.getOutputTypes()) {
							IFile outputFile = outputType.getOutputName(myTopBuildDir, inputFile,
									myCConfigurationDescription, inputType);
							if (outputFile == null) {
								continue;
							}
							// We found a tool that provides a outputfile for our source file
							MakeRule newMakeRule = new MakeRule(tool, inputType, inputFile, outputType, outputFile,0);
							// newMakeRule.addDependencies(caller);

							myMakeRules.addRule(newMakeRule);
							ret = true;
						}
					}
				}
			}
			return ret;
		}
	}

	/**
	 * This method generates the rules for the files generated from the source files
	 * The results are added to the field makerules which already contain the source
	 * make rules.
	 * 
	 * The generated Make rules will have a MakeRuleSequenceID >0
	 */
	protected void generateHigherLevelMakeRules() {
		int makeRuleSequenceID=1;
		Map<IOutputType, Set<IFile>> generatedFiles = myMakeRules.getTargets();
		MakeRules newMakeRules = getMakeRulesFromGeneratedFiles(generatedFiles,makeRuleSequenceID);
		while (makeRuleSequenceID < 20 && newMakeRules.size() > 0) {
			myMakeRules.addRules(newMakeRules);
			generatedFiles.clear();
			generatedFiles.putAll(newMakeRules.getTargets());
			makeRuleSequenceID++;
			newMakeRules = getMakeRulesFromGeneratedFiles(generatedFiles,makeRuleSequenceID);
		}

	}

	/**
	 * Helper method to generateHigherLevelMakeRules Generate the makerules for the
	 * generated files
	 * 
	 * @param generatedFiles The files generated by a rule that may generate make
	 *                       rules
	 * @param makeRuleSequenceID The makeRuleSequenceID to assign to the created MakeRules
	 * 
	 * @return The MakeRules that have been created
	 */
	protected MakeRules getMakeRulesFromGeneratedFiles(Map<IOutputType, Set<IFile>> generatedFiles, int makeRuleSequenceID) {
		MakeRules newMakeRules = new MakeRules();

		for (Entry<IOutputType, Set<IFile>> entry : generatedFiles.entrySet()) {
			IOutputType outputTypeIn = entry.getKey();
			Set<IFile> files = entry.getValue();
			for (ITool tool : myConfig.getToolChain().getTools()) {
				for (IFile file : files) {
					for (IInputType inputType : tool.getInputTypes()) {
						if (inputType.isAssociatedWith(file, outputTypeIn)) {
							for (IOutputType outputType : tool.getOutputTypes()) {
								IFile outputFile = outputType.getOutputName(myTopBuildDir, file,
										myCConfigurationDescription, inputType);

								if (outputFile != null) {
									newMakeRules.addRule(tool, inputType,  file, outputType, outputFile,
											makeRuleSequenceID);
									continue;
								}
							}
						}
					}
				}
			}
		}
		return newMakeRules;
	}

	
	/****************************************************************************************
	 * MakeFile generation code
	 *****************************************************************************************/
	
	protected MultiStatus localgenerateMakefiles(IProgressMonitor monitor) throws CoreException {
		MultiStatus status;
		//This code is called several times so we need to reset the field values
		myMakeRules = new MakeRules();
		myFoldersToBuild = new HashSet<>();
		/*
		 * generate the makeRules for the source files The makeRules are stored in the
		 * field myMakeRules This method also calculates the list of folders that
		 * contain source code leading to make rules Those are stored in myFoldersToBuild
		 * The generated Make rules will have a MakeRuleSequenceID of 0
		 */
		SourceLevelMakeRuleGenerator subDirVisitor = new SourceLevelMakeRuleGenerator();
		myProject.accept(subDirVisitor, IResource.NONE);

		if (myMakeRules.size() == 0) {
			// Throw an error if no source file make rules have been created
			// String info =
			// ManagedMakeMessages.getFormattedString("MakefileGenerator.warning.no.source",
			// project.getName());
			String info = MessageFormat.format(MakefileGenerator_warning_no_source, myProject.getName());
			updateMonitor(info, monitor);
			status = new MultiStatus(Activator.getId(), IStatus.INFO, EMPTY_STRING, null);
			status.add(new Status(IStatus.INFO, Activator.getId(), NO_SOURCE_FOLDERS, info, null));
			return status;
		}
		checkCancel(monitor);

		// Now we have the makeRules for the source files generate the Makerules for the
		// created files
		generateHigherLevelMakeRules();
		checkCancel(monitor);

//		List<SubDirMakeGenerator> subDirMakeGenerators = new LinkedList<>();
//		// Generate the make rules from the source files
//		Set<MakeRule> sourceMakeRules = new HashSet<>();
//		Collection<IContainer> foldersToBuild = new LinkedHashSet<>();
//
//		for (IContainer res : foldersToInvestigate) {
//			// For all the folders get the make rules for this folder
//			SubDirMakeGenerator subDirMakeGenerator = new SubDirMakeGenerator(this, res);
//			if (!subDirMakeGenerator.isEmpty()) {
//				foldersToBuild.add(res);
//				subDirMakeGenerators.add(subDirMakeGenerator);
//				sourceMakeRules.addAll(subDirMakeGenerator.getMakeRules());
//			}
//			checkCancel(monitor);
//		}



		//We have all the rules. Time to make the make files
		Set<String> srcMacroNames = new LinkedHashSet<>();
		Set<String> objMacroNames = new LinkedHashSet<>();
//		for (SubDirMakeGenerator curSubDirMake : subDirMakeGenerators) {
//			curSubDirMake.generateMakefile();
//			srcMacroNames.addAll(curSubDirMake.getPrerequisiteMacros());
//			srcMacroNames.addAll(curSubDirMake.getDependecyMacros());
//			objMacroNames.addAll(curSubDirMake.getTargetMacros());
//		}
		// TOFIX also need to add macro's from main makefile
		objMacroNames = myMakeRules.getTargetMacros();
		srcMacroNames = myMakeRules.getPrerequisiteMacros();
		// srcMacroNames.addAll(myMakeRules.getDependecyMacros());
		generateSrcMakefiles();
		SrcMakeGenerator.generateSourceMakefile(myTopBuildDir, myProject, srcMacroNames, myFoldersToBuild);
		SrcMakeGenerator.generateObjectsMakefile(myTopBuildDir, myProject, objMacroNames);
		TopMakeFileGenerator.generateMakefile(myTopBuildDir, myCConfigurationDescription, myConfig, myFoldersToBuild,
				myMakeRules, objMacroNames);

		checkCancel(monitor);
		// How did we do
		status = new MultiStatus(Activator.getId(), IStatus.OK, EMPTY_STRING, null);

		// TOFIX this should be done differently
		// JABA SLOEBER create the size.awk file
		// ICConfigurationDescription confDesc =
		// ManagedBuildManager.getDescriptionForConfiguration(config);
		// IWorkspaceRoot root = CCorePlugin.getWorkspace().getRoot();
		// IFile sizeAwkFile1 =
		// root.getFile(topBuildDir.getFullPath().append("size.awk"));
		// File sizeAwkFile = sizeAwkFile1.getLocation().toFile();
		// String regex = Common.getBuildEnvironmentVariable(confDesc,
		// "recipe.size.regex", EMPTY);
		// String awkContent = "/" + regex + "/ {arduino_size += $2 }\n";
		// regex = Common.getBuildEnvironmentVariable(confDesc,
		// "recipe.size.regex.data", EMPTY);
		// awkContent += "/" + regex + "/ {arduino_data += $2 }\n";
		// regex = Common.getBuildEnvironmentVariable(confDesc,
		// "recipe.size.regex.eeprom", EMPTY);
		// awkContent += "/" + regex + "/ {arduino_eeprom += $2 }\n";
		// awkContent += "END { print \"\\n";
		// String max = Common.getBuildEnvironmentVariable(confDesc,
		// "upload.maximum_size", "10000");
		// awkContent += Messages.sizeReportSketch.replace("maximum_size", max);
		// awkContent += "\\n";
		// max = Common.getBuildEnvironmentVariable(confDesc,
		// "upload.maximum_data_size", "10000");
		// awkContent += Messages.sizeReportData.replace("maximum_data_size", max);
		// awkContent += "\\n";
		// awkContent += "\"}";
		//
		// try {
		// FileUtils.write(sizeAwkFile, awkContent, Charset.defaultCharset());
		// } catch (IOException e) {
		// // TODO Auto-generated catch block
		// e.printStackTrace();
		// }
		// END JABA SLOEBER create the size.awk file

		return status;
	}

	/*************************************************************************
	 * M A K E F I L E G E N E R A T I O N C O M M O N M E T H O D S
	 ************************************************************************/

	protected void generateSrcMakefiles() throws CoreException {
		for (IFolder curFolder : myFoldersToBuild) {
			// generate the file content
			StringBuffer makeBuf = addDefaultHeader();
			MakeRules applicableMakeRules = myMakeRules.getRulesForFolder(curFolder);
			makeBuf.append(GenerateMacroSection(myTopBuildDir,applicableMakeRules));
			makeBuf.append(GenerateRules(applicableMakeRules));

			// Save the files
			IFolder srcFile=myTopBuildDir.getFolder(curFolder.getProjectRelativePath());
			save(makeBuf, srcFile.getFile(MODFILE_NAME));
		}
	}
	
	/***
	 * Method that asks the rule from the makerule
	 * Override this if you want to modify the rule of all/some targets
	 * 
	 * @param makeRule
	 * @return
	 */
	protected StringBuffer getRule(MakeRule makeRule) {
		return makeRule.getRule(myProject, myTopBuildDir, myCConfigurationDescription);
	}
	
	protected StringBuffer GenerateRules( MakeRules makeRules) {
        StringBuffer buffer = new StringBuffer();
        buffer.append(NEWLINE);
        buffer.append(COMMENT_SYMBOL).append(WHITESPACE).append(MakefileGenerator_comment_build_rule).append(NEWLINE);

        for (MakeRule makeRule : makeRules) {
            buffer.append(getRule(makeRule));
        }

        return buffer;
    }
	
    protected static StringBuffer GenerateMacroSection(IFolder buildRoot,MakeRules makeRules ) {
        StringBuffer buffer = new StringBuffer();
        buffer.append(NEWLINE);
        buffer.append(COMMENT_SYMBOL).append(WHITESPACE).append(MakefileGenerator_comment_module_variables)
                .append(NEWLINE);
        HashSet<String> macroNames = new HashSet<>();
        for (MakeRule makeRule : makeRules) {
            macroNames.addAll(makeRule.getAllMacros());
        }
        macroNames.remove(EMPTY_STRING);
        for (String macroName : macroNames) {
            HashSet<IFile> files = new HashSet<>();
            for (MakeRule makeRule : makeRules) {
                files.addAll(makeRule.getMacroElements(macroName));
            }
            if (files.size() > 0) {
                buffer.append(macroName).append(MAKE_ADDITION);
                for (IFile file : files) {
                    buffer.append(LINEBREAK);
                    buffer.append(GetNiceFileName(buildRoot, file)).append(WHITESPACE);
                }
                buffer.append(NEWLINE);
                buffer.append(NEWLINE);
            }
        }
        return buffer;
    }

	/****************************************************************************************
	 * Some Static house keeping methods
	 *****************************************************************************************/
	
	/**
	 * Check whether the build has been cancelled. Cancellation requests propagated
	 * to the caller by throwing <code>OperationCanceledException</code>.
	 *
	 * @see org.eclipse.core.runtime.OperationCanceledException#OperationCanceledException()
	 */
	private static void checkCancel(IProgressMonitor monitor) {
		if (monitor != null && monitor.isCanceled()) {
			throw new OperationCanceledException();
		}
	}

	private static void updateMonitor(String msg, IProgressMonitor monitor) {
		if (monitor != null && !monitor.isCanceled()) {
			monitor.subTask(msg);
			monitor.worked(1);
		}
	}
}
//	/*
//	 * (non-Javadoc)
//	 *
//	 * @see org.eclipse.cdt.managedbuilder.makegen.IManagedBuilderMakefileGenerator
//	 * #initialize(IProject, IManagedBuildInfo, IProgressMonitor)
//	 */
//	@Override
//	public void initialize(IProject project, IManagedBuildInfo info, IProgressMonitor monitor) {
//
//		this.project = project;
//
//		// Save the monitor reference for reporting back to the user
//		this.monitor = monitor;
//		// Get the name of the build target
//		buildTargetName = info.getBuildArtifactName();
//		// Get its extension
//		buildTargetExt = info.getBuildArtifactExtension();
//		// try to resolve the build macros in the target extension
//		buildTargetExt = resolveValueToMakefileFormat(buildTargetExt, "", " ",
//				IBuildMacroProvider.CONTEXT_CONFIGURATION, info.getDefaultConfiguration());
//		// try to resolve the build macros in the target name
//		String resolved = resolveValueToMakefileFormat(buildTargetName, "", " ",
//				IBuildMacroProvider.CONTEXT_CONFIGURATION, info.getDefaultConfiguration());
//		if (resolved != null && (resolved = resolved.trim()).length() > 0)
//			buildTargetName = resolved;
//		if (buildTargetExt == null) {
//			buildTargetExt = "";
//		}
//		// Cache the build tools
//		config = info.getDefaultConfiguration();
//		// initToolInfos();
//		// set the top build dir path
//		topBuildDir = project.getFile(info.getConfigurationName());
//	}
