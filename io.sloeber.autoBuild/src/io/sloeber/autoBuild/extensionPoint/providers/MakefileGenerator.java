package io.sloeber.autoBuild.extensionPoint.providers;

import static io.sloeber.autoBuild.core.Messages.*;
import static io.sloeber.autoBuild.helpers.api.AutoBuildConstants.*;
import static io.sloeber.autoBuild.internal.AutoBuildCommon.*;

import java.text.MessageFormat;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;

import io.sloeber.autoBuild.api.IAutoBuildMakeRule;
import io.sloeber.autoBuild.api.IAutoBuildMakeRules;
import io.sloeber.autoBuild.core.Activator;
import io.sloeber.autoBuild.integration.AutoBuildConfigurationDescription;
import io.sloeber.autoBuild.schema.api.IConfiguration;
import io.sloeber.autoBuild.schema.api.IOutputType;
import io.sloeber.autoBuild.schema.api.ITool;
import io.sloeber.autoBuild.schema.api.IToolChain;

/**
 * This is the default makefile generator Feel free to extend to add the flavors
 * you need
 *
 * @noinstantiate This class is not intended to be instantiated by clients.
 */
public class MakefileGenerator  {
	static private boolean VERBOSE = false;
	 public static final int COLS_PER_LINE = 80;

    // Generation error codes
    public static final int SPACES_IN_PATH = 0;
    public static final int NO_SOURCE_FOLDERS = 1;

	// Local variables needed by generator
	IConfiguration myConfig;
	IProject myProject;
	ICConfigurationDescription myCConfigurationDescription;
	IFolder myBuildRoot;
	AutoBuildMakeRules myMakeRules = null;
	Set<IContainer> myContainersToBuild = null;
	AutoBuildConfigurationDescription myAutoBuildConfData;

	/****************************************************************************************
	 * CONSTRUCTOR / INITIALIZING code / overrides
	 *****************************************************************************************/

	public MakefileGenerator() {
		super();
	}

	public void initialize(AutoBuildConfigurationDescription autoData) {
		myProject = autoData.getProject();
		myAutoBuildConfData = autoData;
		myCConfigurationDescription = myAutoBuildConfData.getCdtConfigurationDescription();
		myConfig = myAutoBuildConfData.getConfiguration();
		myBuildRoot = myAutoBuildConfData.getBuildFolder();

		// Get the target info
		String buildTargetName;
		String buildTargetExt;
		buildTargetName = myConfig.getArtifactName();
		// Get its extension
		buildTargetExt = myConfig.getArtifactExtension();
		// try to resolve the build macros in the target extension
		buildTargetExt = resolve(buildTargetExt, EMPTY_STRING, BLANK, autoData);
		// try to resolve the build macros in the target name
		String resolved = resolve(buildTargetName, EMPTY_STRING, BLANK, autoData);
		if (resolved != null) {
			resolved = resolved.trim();
			if (resolved.length() > 0)
				buildTargetName = resolved;
		}
		if (buildTargetExt == null) {
			buildTargetExt = EMPTY_STRING;
		}

	}


	/****************************************************************************************
	 * Make rule generation code
	 *****************************************************************************************/

	/****************************************************************************************
	 * MakeFile generation code
	 *****************************************************************************************/

	protected MultiStatus localgenerateMakefiles(IProgressMonitor monitor) throws CoreException {
		if (VERBOSE) {
			System.out.println("Start MakeFile Generation for " + myProject.getName() + " for config " //$NON-NLS-1$ //$NON-NLS-2$
					+ myCConfigurationDescription.getName());
		}
		MultiStatus status;
		myMakeRules = new AutoBuildMakeRules(myAutoBuildConfData);
		myContainersToBuild =myMakeRules.getFoldersThatContainSourceFiles();

		if (myMakeRules.size() == 0) {
			// Throw an error if no source file make rules have been created
			String info = MessageFormat.format(MakefileGenerator_warning_no_source, myProject.getName());
			updateMonitor(info, monitor);
			status = new MultiStatus(Activator.getId(), IStatus.INFO, EMPTY_STRING, null);
			status.add(new Status(IStatus.ERROR, Activator.getId(), NO_SOURCE_FOLDERS, info, null));
			return status;
		}
		// We have all the rules. Time to make the make files
		Set<String> srcMacroNames = myMakeRules.getPrerequisiteMacros();
		Set<String> objMacroNames = myMakeRules.getTargetMacros();
		objMacroNames = myMakeRules.getTargetMacros();
		srcMacroNames = myMakeRules.getPrerequisiteMacros();
		Set<String> dependencyMacroNames = myMakeRules.getDependencyMacros();
		generateSrcMakefiles();
		generateSourceMakefile(srcMacroNames);
		generateObjectsMakefile(objMacroNames);
		topMakeGeneratefile(dependencyMacroNames);

		checkCancel(monitor);

		if (VERBOSE) {
			System.out.println("MakeFile Generation done for " + myProject.getName() + " for config " //$NON-NLS-1$ //$NON-NLS-2$
					+ myCConfigurationDescription.getName());
		}

		return new MultiStatus(Activator.getId(), IStatus.OK, EMPTY_STRING, null);
	}

	/*************************************************************************
	 * M A K E F I L E G E N E R A T I O N C O M M O N M E T H O D S
	 ************************************************************************/

	protected void generateSrcMakefiles() throws CoreException {
		for (IContainer curContainer : myContainersToBuild) {
			// generate the file content
			StringBuffer makeBuf = addDefaultHeader();
			AutoBuildMakeRules applicableMakeRules = myMakeRules.getRulesForContainer(curContainer);
			makeBuf.append(generateMacroSection(myBuildRoot, applicableMakeRules));
			makeBuf.append(generateRules(applicableMakeRules));

			// Save the files
			IFolder targetFolder = myBuildRoot.getFolder(curContainer.getProjectRelativePath());
			save(makeBuf, targetFolder.getFile(MODFILE_NAME));
		}
	}

	protected StringBuffer generateRules(IAutoBuildMakeRules makeRules) {
		StringBuffer buffer = new StringBuffer();
		buffer.append(NEWLINE);
		buffer.append(COMMENT_SYMBOL).append(WHITESPACE).append(MakefileGenerator_comment_build_rule).append(NEWLINE);

		for (IAutoBuildMakeRule makeRule : makeRules) {
			buffer.append(getRecipesInMakeFileStyle(makeRule));
		}

		return buffer;
	}

	protected static StringBuffer generateMacroSection(IFolder buildRoot, AutoBuildMakeRules makeRules) {
		StringBuffer buffer = new StringBuffer();
		buffer.append(NEWLINE);
		buffer.append(COMMENT_SYMBOL).append(WHITESPACE).append(MakefileGenerator_comment_module_variables)
				.append(NEWLINE);
		HashSet<String> macroNames = new HashSet<>();
		for (IAutoBuildMakeRule makeRule : makeRules) {
			macroNames.addAll(((AutoBuildMakeRule)makeRule).getAllMacros());
		}
		macroNames.remove(EMPTY_STRING);
		for (String macroName : macroNames) {
			HashSet<IFile> files = new HashSet<>();
			files.addAll(makeRules.getMacroElements(macroName));
			if (files.size() > 0) {
				buffer.append(macroName).append(MAKE_ADDITION);
				for (IFile file : files) {
					buffer.append(LINEBREAK);
					buffer.append(getMakeSafeNiceFileName(buildRoot, file)).append(WHITESPACE);
				}
				buffer.append(NEWLINE);
				buffer.append(NEWLINE);
			}
		}
		return buffer;
	}


	/****************************************************************************************
	 * Sources/objects MakeFile generation code
	 ****************************************************************************************/

	protected void generateSourceMakefile(Set<String> macroNames) throws CoreException {
		StringBuffer buffer = addDefaultHeader();
		for (String macroName : macroNames) {
			if (!macroName.isBlank()) {
				buffer.append(macroName).append(WHITESPACE).append(":=").append(WHITESPACE).append(NEWLINE); //$NON-NLS-1$
			}
		}
		// Add a list of subdirectories to the makefile
		buffer.append(NEWLINE);
		// Add the comment
		buffer.append(COMMENT_SYMBOL).append(WHITESPACE).append(MakefileGenerator_comment_module_list).append(NEWLINE);
		buffer.append("SUBDIRS := ").append(LINEBREAK); //$NON-NLS-1$

		for (IContainer container : myContainersToBuild) {
			if (container.getFullPath() == myProject.getFullPath()) {
				buffer.append(DOT).append(WHITESPACE).append(LINEBREAK);
			} else {
				IPath path = container.getProjectRelativePath();
				buffer.append(escapeWhitespaces(path.toOSString())).append(WHITESPACE).append(LINEBREAK);
			}
		}
		buffer.append(NEWLINE);
		// Save the file
		IFile fileHandle = myBuildRoot.getFile(SRCSFILE_NAME);
		save(buffer, fileHandle);
	}

	/**
	 * The makefile generator generates a Macro for each type of output, other than
	 * final artifact, created by the build.
	 *
	 * @param fileHandle The file that should be populated with the output
	 */
	protected void generateObjectsMakefile(Set<String> outputMacros) throws CoreException {
		StringBuffer macroBuffer = new StringBuffer();
		macroBuffer.append(addDefaultHeader());

//		for (String macroName : outputMacros) {
//			if (!macroName.isBlank()) {
//				macroBuffer.append(macroName).append(MAKE_EQUAL);
//				macroBuffer.append(NEWLINE);
//				macroBuffer.append(NEWLINE);
//			}
//		}
		IFile fileHandle = myBuildRoot.getFile(OBJECTS_MAKFILE);
		save(macroBuffer, fileHandle);
	}

	/****************************************************************************************
	 * End of Sources/objects MakeFile generation code
	 ****************************************************************************************/

	/****************************************************************************************
	 * Top MakeFile generation code
	 ****************************************************************************************/

	protected void topMakeGeneratefile(Set<String> objMacroNames) throws CoreException {

		StringBuffer buffer = new StringBuffer();
		buffer.append(addDefaultHeader());
		buffer.append(topMakeGetIncludeSubDirs());
		buffer.append(topMakeGetIncludeDependencies());
		buffer.append(topMakeGetRMCommand(objMacroNames));
		buffer.append(topMakeGetTargets());// this is the include dependencies
		buffer.append(topMakeGetMacros());
		buffer.append(topMakeGetMakeRules());
		buffer.append(topMakeGetFinalTargets());

		IFile fileHandle = myBuildRoot.getFile(MAKEFILE_NAME);
		save(buffer, fileHandle);
	}


	protected StringBuffer topMakeGetIncludeSubDirs() {
		StringBuffer buffer = new StringBuffer();
		buffer.append("-include ").append(OBJECTS_MAKFILE).append(NEWLINE).append(NEWLINE); //$NON-NLS-1$
		buffer.append("-include ").append(SRCSFILE_NAME).append(NEWLINE); //$NON-NLS-1$

		for (IContainer subDir : myContainersToBuild) {
			String includeFile = subDir.getProjectRelativePath().append(MODFILE_NAME).toOSString();
			buffer.append("-include " + makeMakeFileSafe(includeFile)).append(NEWLINE); //$NON-NLS-1$
		}
		return buffer;
	}

	protected StringBuffer topMakeGetRMCommand(Set<String> myDependencyMacros) {
		StringBuffer buffer = new StringBuffer();
		IFile makeInitFile=myProject.getFile(MAKEFILE_INIT);
		buffer.append("-include " +GetNiceFileName(myBuildRoot,makeInitFile)).append(NEWLINE); //$NON-NLS-1$
		IFile makeExtendile=myBuildRoot.getFile(MAKE_FILE_EXTENSION);
		buffer.append("-include " +GetNiceFileName(myBuildRoot,makeExtendile)).append(NEWLINE); //$NON-NLS-1$
		buffer.append(NEWLINE);
		// Get the clean command from the build model
		buffer.append("RM := "); //$NON-NLS-1$
		buffer.append("rm -rf").append(NEWLINE); //$NON-NLS-1$
		buffer.append(NEWLINE);

		if (!myDependencyMacros.isEmpty()) {
			buffer.append("ifneq ($(MAKECMDGOALS),clean)").append(NEWLINE); //$NON-NLS-1$
			for (String depsMacro : myDependencyMacros) {
				buffer.append("ifneq ($(strip $(").append(depsMacro).append(")),)").append(NEWLINE); //$NON-NLS-1$ //$NON-NLS-2$
				buffer.append("-include $(").append(depsMacro).append(')').append(NEWLINE); //$NON-NLS-1$
				buffer.append("endif").append(NEWLINE); //$NON-NLS-1$
			}
			buffer.append("endif").append(NEWLINE).append(NEWLINE); //$NON-NLS-1$
		}
		// Include makefile.defs supplemental makefile
		IFile makeDefFile=myProject.getFile(MAKEFILE_DEFS);
		buffer.append("-include ").append(GetNiceFileName(myBuildRoot,makeDefFile)).append(NEWLINE); //$NON-NLS-1$
		return (buffer.append(NEWLINE));
	}

	protected String topMakeGetPreBuildStep() {
		String prebuildStep = myAutoBuildConfData.getPrebuildStep();
		// JABA issue927 adding recipe.hooks.sketch.prebuild.NUMBER.pattern as cdt
		// prebuild command if needed
		// ICConfigurationDescription confDesc =
		// ManagedBuildManager.getDescriptionForConfiguration(config);
		// String sketchPrebuild =
		// io.sloeber.core.common.Common.getBuildEnvironmentVariable(confDesc,
		// "sloeber.prebuild",
		// new String(), false);
		String sketchPrebuild = getVariableValue("sloeber.prebuild", EMPTY_STRING, true, myAutoBuildConfData); //$NON-NLS-1$
		if (!sketchPrebuild.isEmpty()) {
			if (!prebuildStep.isEmpty()) {
				prebuildStep = prebuildStep + NEWLINE+TAB + sketchPrebuild;
			} else {
				prebuildStep = sketchPrebuild;
			}
		}
		// end off JABA issue927
		// try to resolve the build macros in the prebuild step
		prebuildStep = resolve(prebuildStep, EMPTY_STRING, WHITESPACE, myAutoBuildConfData);
		return prebuildStep.trim();
	}

	protected StringBuffer topMakeGetIncludeDependencies() {
		StringBuffer buffer = new StringBuffer();

		// Add the comment for the "All" target
		buffer.append(COMMENT_START).append(MakefileGenerator_comment_build_alltarget).append(NEWLINE);

		String prebuildStep = topMakeGetPreBuildStep();
		String postbuildStep = resolve(myAutoBuildConfData.getPostbuildStep(), EMPTY_STRING, WHITESPACE,
				myAutoBuildConfData);
		if (prebuildStep.isBlank() && postbuildStep.isBlank()) {
			buffer.append(TARGET_ALL).append(COLON).append(WHITESPACE).append(MAINBUILD).append(NEWLINE);
		} else {
			buffer.append(TARGET_ALL).append(COLON).append(NEWLINE);
			if (!prebuildStep.isBlank()) {
				buffer.append(TAB).append(MAKE).append(WHITESPACE).append(NO_PRINT_DIR).append(WHITESPACE)
						.append(PREBUILD).append(NEWLINE);
			}
			buffer.append(TAB).append(MAKE).append(WHITESPACE).append(NO_PRINT_DIR).append(WHITESPACE).append(MAINBUILD)
					.append(NEWLINE);
			if (!postbuildStep.isBlank()) {
				buffer.append(TAB).append(MAKE).append(WHITESPACE).append(NO_PRINT_DIR).append(WHITESPACE)
						.append(POSTBUILD).append(NEWLINE);
			}

		}
		buffer.append(NEWLINE);
		buffer.append(TARGET_OBJECTS + COLON + WHITESPACE + "$(OBJS)"); //$NON-NLS-1$
		buffer.append(NEWLINE);
		buffer.append(NEWLINE);
		return buffer;
	}

	protected StringBuffer topMakeGetTargets() {
		IToolChain toolChain=myAutoBuildConfData.getProjectType().getToolChain();
		StringBuffer buffer = new StringBuffer();

		buffer.append(MAINBUILD).append(COLON).append(WHITESPACE);
		Set<ITool> targetTools = toolChain.getTargetTools();
		if (targetTools.size() > 0) {
			for (ITool curTargetTool : targetTools) {
				Set<IFile> allTargets = myMakeRules.getTargetsForTool(curTargetTool);
				for (IFile curTarget : allTargets) {
					String targetString = getMakeSafeNiceFileName(myBuildRoot, curTarget);
					buffer.append(ensurePathIsGNUMakeTargetRuleCompatibleSyntax(targetString));
					buffer.append(WHITESPACE);
				}
			}
		} else {
			Set<IFile> allTargets = myMakeRules.getFinalTargets();
			for (IFile curTarget : allTargets) {
				String targetString = getMakeSafeNiceFileName(myBuildRoot, curTarget);
				buffer.append(ensurePathIsGNUMakeTargetRuleCompatibleSyntax(targetString));
				buffer.append(WHITESPACE);
			}
		}

		// Add the Secondary Outputs to the all target, if any
		List<IOutputType> secondaryOutputs = toolChain.getSecondaryOutputs();
		if (secondaryOutputs.size() > 0) {
			buffer.append(WHITESPACE).append(SECONDARY_OUTPUTS);
		}
		buffer.append(NEWLINE).append(NEWLINE);

		String prebuildStep = topMakeGetPreBuildStep();
		if (prebuildStep.length() > 0) {

			String preannouncebuildStep = myAutoBuildConfData.getPreBuildAnouncement();
			buffer.append(PREBUILD).append(COLON).append(NEWLINE);
			if (preannouncebuildStep.length() > 0) {
				buffer.append(TAB).append(DASH).append(AT_SYMBOL).append(escapedEcho(preannouncebuildStep));
			}
			buffer.append(TAB).append(DASH).append(prebuildStep).append(NEWLINE);
			buffer.append(TAB).append(DASH).append(AT_SYMBOL).append(ECHO_BLANK_LINE).append(NEWLINE);
		}

		String postbuildStep = myAutoBuildConfData.getPostbuildStep();
		postbuildStep = resolve(postbuildStep, EMPTY_STRING, WHITESPACE, myAutoBuildConfData);
		postbuildStep = postbuildStep.trim();
		// Add the postbuild step, if specified
		if (postbuildStep.length() > 0) {
			String postannouncebuildStep = myAutoBuildConfData.getPostBuildAnouncement();
			buffer.append(POSTBUILD).append(COLON).append(NEWLINE);
			if (postannouncebuildStep.length() > 0) {
				buffer.append(TAB).append(DASH).append(AT_SYMBOL).append(escapedEcho(postannouncebuildStep));
			}
			buffer.append(TAB).append(DASH).append(postbuildStep).append(NEWLINE);
			buffer.append(TAB).append(DASH).append(AT_SYMBOL).append(ECHO_BLANK_LINE).append(NEWLINE);
		}

		return buffer;
	}

	protected StringBuffer topMakeGetFinalTargets() {

		String cleanCommand = TAB+"-$(RM)"; //$NON-NLS-1$
		StringBuffer buffer = new StringBuffer();
		buffer.append(NEWLINE).append(NEWLINE);
		buffer.append(CLEAN).append(COLON).append(NEWLINE);
		StringBuffer cleanFiles = new StringBuffer();
		for (IFile curFile : myMakeRules.getBuildFiles()) {
			cleanFiles.append(BLANK).append(DOUBLE_QUOTE).append(GetNiceFileName(myBuildRoot, curFile))
					.append(DOUBLE_QUOTE);
			if (cleanFiles.length() > 1000) {
				buffer.append(cleanCommand).append(cleanFiles);
				buffer.append(NEWLINE);
				cleanFiles.setLength(0);
			}
		}
		if (cleanFiles.length() > 0) {
			buffer.append(cleanCommand).append(cleanFiles);
			buffer.append(NEWLINE);
		}
		buffer.append(NEWLINE);

		// Add all the needed dummy and phony targets
		buffer.append(PHONY).append(COLON).append(ALL).append(WHITESPACE).append(MAINBUILD).append(WHITESPACE)
				.append(CLEAN).append(WHITESPACE).append(POSTBUILD).append(WHITESPACE).append(TARGET_OBJECTS)
				.append(NEWLINE);
		// Include makefile.targets supplemental makefile
		IFile makeTargetFile=myProject.getFile(MAKEFILE_TARGETS);
		buffer.append("-include ").append(GetNiceFileName(myBuildRoot,makeTargetFile)).append(NEWLINE); //$NON-NLS-1$
		return buffer;
	}

	protected StringBuffer topMakeGetMacros() {
		StringBuffer buffer = new StringBuffer();
		buffer.append(NEWLINE);
		buffer.append(COMMENT_START).append(MakefileGenerator_comment_module_variables).append(NEWLINE);

		for (String macroName : myMakeRules.getPrerequisiteMacros()) {
			Set<IFile> files = myMakeRules.getMacroElements(macroName);
			if (files.size() > 0) {
				buffer.append(macroName).append(MAKE_ADDITION);
				for (IFile file : files) {
					if (files.size() != 1) {
						buffer.append(LINEBREAK);
					}
					buffer.append(getMakeSafeNiceFileName(myBuildRoot, file)).append(WHITESPACE);
				}
				buffer.append(NEWLINE);
				buffer.append(NEWLINE);
			}
		}
		return buffer;
	}

	protected StringBuffer topMakeGetMakeRules() {
		StringBuffer buffer = new StringBuffer();
		buffer.append(NEWLINE);
		buffer.append(COMMENT_START).append(MakefileGenerator_comment_build_rule).append(NEWLINE);

		for (IAutoBuildMakeRule makeRule : myMakeRules.getMakeRules()) {
			if (makeRule.getSequenceGroupID() != 0) {
				buffer.append(getRecipesInMakeFileStyle(makeRule));
			}
		}
		return buffer;
	}

	/****************************************************************************************
	 * End of Top MakeFile generation code
	 ****************************************************************************************/

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

	private static String makeMakeFileSafe(String fileName) {
		if (fileName.contains(BLANK)) {
			return fileName.replace(BLANK, BACKSLACH + BLANK);

		}
		return fileName;
	}

	private static String getMakeSafeNiceFileName(IFolder folder, IFile file) {
		String ret = GetNiceFileName(folder, file);
		if (ret.contains(BLANK)) {
			return ret.replace(BLANK, BACKSLACH + BLANK);

		}
		return ret;
	}

	private static String enumTargets(IAutoBuildMakeRule makeRule, IFolder buildFolder) {
		String ret = new String();
		for (IFile curFile : makeRule.getTargetFiles()) {
			ret = ret + getMakeSafeNiceFileName(buildFolder, curFile) + WHITESPACE;
		}
		return ret;
	}

	private static String enumPrerequisites(IAutoBuildMakeRule makeRule, IFolder buildFolder) {
		String ret = new String();
		for (IFile curFile : makeRule.getPrerequisiteFiles()) {
			ret = ret + getMakeSafeNiceFileName(buildFolder, curFile) + WHITESPACE;
		}
		return ret;
	}

	public StringBuffer getRecipesInMakeFileStyle(IAutoBuildMakeRule makeRule) {

		ITool tool = makeRule.getTool();
		StringBuffer buffer = new StringBuffer();
		buffer.append(enumTargets(makeRule, myBuildRoot)).append(COLON).append(WHITESPACE);
		buffer.append(enumPrerequisites(makeRule, myBuildRoot)).append(NEWLINE);
		buffer.append(TAB).append(AT_SYMBOL)
				.append(escapedEcho(MakefileGenerator_message_start_file + WHITESPACE + OUT_MACRO));
		buffer.append(TAB).append(AT_SYMBOL).append(escapedEcho(tool.getAnnouncement()));

		// // JABA add sketch.prebuild and postbuild if needed
		// //TOFIX this should not be here
		// if ("sloeber.ino".equals(fileName)) { //$NON-NLS-1$
		//
		// // String sketchPrebuild =
		// io.sloeber.core.common.Common.getBuildEnvironmentVariable(confDesc,
		// // "sloeber.sketch.prebuild", new String(), true); //$NON-NLS-1$
		// // String sketchPostBuild =
		// io.sloeber.core.common.Common.getBuildEnvironmentVariable(confDesc,
		// // "sloeber.sketch.postbuild", new String(), true); //$NON-NLS-1$
		// String sketchPrebuild = resolve("sloeber.sketch.prebuild", EMPTY_STRING,
		// WHITESPACE, autoBuildConfData);
		// String sketchPostBuild = resolve("sloeber.sketch.postbuild", EMPTY_STRING,
		// WHITESPACE,autoBuildConfData);
		// if (!sketchPrebuild.isEmpty()) {
		// buffer.append(TAB).append(sketchPrebuild);
		// }
		// buffer.append(TAB).append(buildCmd).append(NEWLINE);
		// if (!sketchPostBuild.isEmpty()) {
		// buffer.append(TAB).append(sketchPostBuild);
		// }
		// } else {
		for (String resolvedCommand : makeRule.getRecipes(myBuildRoot, myAutoBuildConfData)) {
			buffer.append(TAB).append(resolvedCommand);
			buffer.append(NEWLINE);
		}
		// }
		// // end JABA add sketch.prebuild and postbuild if needed

		buffer.append(NEWLINE);
		buffer.append(TAB).append(AT_SYMBOL)
				.append(escapedEcho(MakefileGenerator_message_finish_file + WHITESPACE + OUT_MACRO));
		buffer.append(TAB).append(AT_SYMBOL).append(ECHO_BLANK_LINE).append(NEWLINE);
		return buffer;
	}


    /**
     * Outputs a comment formatted as follows: ##### ....... ##### # <Comment
     * message> ##### ....... #####
     */
    static private  StringBuffer addDefaultHeader() {
        StringBuffer buffer = new StringBuffer();
        outputCommentLine(buffer);
        buffer.append(COMMENT_SYMBOL).append(WHITESPACE).append(MakefileGenerator_comment_header).append(NEWLINE);
        outputCommentLine(buffer);
        buffer.append(NEWLINE);
        return buffer;
    }


    /**
     * Put COLS_PER_LINE comment charaters in the argument.
     */
    static private void outputCommentLine(StringBuffer buffer) {
        for (int i = 0; i < COLS_PER_LINE; i++) {
            buffer.append(COMMENT_SYMBOL);
        }
        buffer.append(NEWLINE);
    }
}
