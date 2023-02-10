package io.sloeber.autoBuild.extensionPoint.providers;

import static io.sloeber.autoBuild.core.Messages.*;
import static io.sloeber.autoBuild.extensionPoint.providers.AutoBuildCommon.*;
import static io.sloeber.autoBuild.integration.AutoBuildConstants.*;

import java.text.MessageFormat;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.eclipse.cdt.core.settings.model.CSourceEntry;
import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.cdt.core.settings.model.ICSettingEntry;
import org.eclipse.cdt.core.settings.model.ICSourceEntry;
import org.eclipse.cdt.core.settings.model.util.CDataUtil;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;


import io.sloeber.autoBuild.core.Activator;
import io.sloeber.autoBuild.extensionPoint.IMakefileGenerator;
import io.sloeber.autoBuild.integration.AutoBuildConfigurationData;
import io.sloeber.schema.api.IBuilder;
import io.sloeber.schema.api.IConfiguration;
import io.sloeber.schema.api.IOutputType;
import io.sloeber.schema.api.ITool;
import io.sloeber.autoBuild.api.IBuildMacroProvider;

/**
 * This is the default makefile generator
 * Feel free to extend to add the flavors you need
 *
 * @noinstantiate This class is not intended to be instantiated by clients.
 */
public class MakefileGenerator implements IMakefileGenerator {
    static private boolean VERBOSE = true;

    // Local variables needed by generator
    IConfiguration myConfig;
    IProject myProject;
    ICConfigurationDescription myCConfigurationDescription;
    IFolder myTopBuildDir;
    ICSourceEntry[] mySrcEntries;
    MakeRules myMakeRules = null;
    Set<IFolder> myFoldersToBuild = null;
    AutoBuildConfigurationData myAutoBuildConfData;

    /****************************************************************************************
     * CONSTRUCTOR / INITIALIZING code / overrides
     *****************************************************************************************/

    public MakefileGenerator() {
        super();
    }

    @Override
    public void initialize(int buildKind, IProject project, AutoBuildConfigurationData autoBuildConfData,
            IBuilder builder) {
        myProject = project;
        myAutoBuildConfData = autoBuildConfData;
        myCConfigurationDescription = myAutoBuildConfData.getCdtConfigurationDescription();
        myConfig = myAutoBuildConfData.getConfiguration();
        myTopBuildDir = myAutoBuildConfData.getBuildFolder();

        // Get the target info
        String buildTargetName;
        String buildTargetExt;
        buildTargetName = myConfig.getArtifactName();
        // Get its extension
        buildTargetExt = myConfig.getArtifactExtension();
        // try to resolve the build macros in the target extension
        buildTargetExt = resolveValueToMakefileFormat(buildTargetExt, EMPTY_STRING, BLANK,
                IBuildMacroProvider.CONTEXT_CONFIGURATION, autoBuildConfData);
        // try to resolve the build macros in the target name
        String resolved = resolveValueToMakefileFormat(buildTargetName, EMPTY_STRING, BLANK,
                IBuildMacroProvider.CONTEXT_CONFIGURATION, autoBuildConfData);
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
        //Nothing to do here
    }

    @Override
    public void generateDependencies(IProgressMonitor monitor) throws CoreException {
        //Nothing to do here
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

    /****************************************************************************************
     * MakeFile generation code
     *****************************************************************************************/

    protected MultiStatus localgenerateMakefiles(IProgressMonitor monitor) throws CoreException {
        if (VERBOSE) {
            System.out.println("Start MakeFile Generation for " + myProject.getName() + " for config " //$NON-NLS-1$ //$NON-NLS-2$
                    + myCConfigurationDescription.getName());
        }
        beforeRuleGeneration();
        MultiStatus status;
        //This object remains alive between builds; therefore we need to reset the field values
        myFoldersToBuild = new HashSet<>();
        myMakeRules = new MakeRules(myProject, myAutoBuildConfData, myTopBuildDir, myConfig, mySrcEntries,
                myFoldersToBuild);

        if (myMakeRules.size() == 0) {
            // Throw an error if no source file make rules have been created
            String info = MessageFormat.format(MakefileGenerator_warning_no_source, myProject.getName());
            updateMonitor(info, monitor);
            status = new MultiStatus(Activator.getId(), IStatus.INFO, EMPTY_STRING, null);
            status.add(new Status(IStatus.ERROR, Activator.getId(), NO_SOURCE_FOLDERS, info, null));
            return status;
        }
        beforeMakefileGeneration();
        //We have all the rules. Time to make the make files
        Set<String> srcMacroNames = new LinkedHashSet<>();
        Set<String> objMacroNames = new LinkedHashSet<>();
        objMacroNames = myMakeRules.getTargetMacros();
        srcMacroNames = myMakeRules.getPrerequisiteMacros();
        // srcMacroNames.addAll(myMakeRules.getDependecyMacros());
        generateSrcMakefiles();
        generateSourceMakefile( srcMacroNames);
        generateObjectsMakefile( objMacroNames);
        topMakeGeneratefile( objMacroNames);

        checkCancel(monitor);
        afterMakefileGeneration();

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
        for (IFolder curFolder : myFoldersToBuild) {
            // generate the file content
            StringBuffer makeBuf = addDefaultHeader();
            MakeRules applicableMakeRules = myMakeRules.getRulesForFolder(curFolder);
            makeBuf.append(GenerateMacroSection(myTopBuildDir, applicableMakeRules));
            makeBuf.append(GenerateRules(applicableMakeRules));

            // Save the files
            IFolder srcFile = myTopBuildDir.getFolder(curFolder.getProjectRelativePath());
            save(makeBuf, srcFile.getFile(MODFILE_NAME));
        }
    }

    protected StringBuffer GenerateRules(MakeRules makeRules) {
        StringBuffer buffer = new StringBuffer();
        buffer.append(NEWLINE);
        buffer.append(COMMENT_SYMBOL).append(WHITESPACE).append(MakefileGenerator_comment_build_rule).append(NEWLINE);

        for (MakeRule makeRule : makeRules) {
            buffer.append(getRule(makeRule));
        }

        return buffer;
    }

    protected static StringBuffer GenerateMacroSection(IFolder buildRoot, MakeRules makeRules) {
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
     * Convenience methods to interfere in the makefile generation
     *****************************************************************************************/
    /***
     * Method that asks the rule from the makerule
     * Override this if you want to modify the rule of all/some targets
     * 
     * @param makeRule
     * @return
     */
    protected StringBuffer getRule(MakeRule makeRule) {
        return makeRule.getRule(myProject, myTopBuildDir, myAutoBuildConfData);
    }

    protected void afterMakefileGeneration() {
        // nothing to do. 
    }

    protected void beforeMakefileGeneration() {
        //nothing to do. 
    }

    protected void beforeRuleGeneration() {
        // nothing to do. 
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
    }

    /****************************************************************************************
     * End of Convenience methods to interfere in the makefile generation
     *****************************************************************************************/
    
    /****************************************************************************************
     * Sources/objects MakeFile generation code
     ****************************************************************************************/

    
	protected void generateSourceMakefile( Set<String> macroNames) throws CoreException {
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

		for (IFolder container : myFoldersToBuild) {
			if (container.getFullPath() == myProject.getFullPath()) {
				buffer.append(DOT).append(WHITESPACE).append(LINEBREAK);
			} else {
				IPath path = container.getProjectRelativePath();
				buffer.append(escapeWhitespaces(path.toOSString())).append(WHITESPACE).append(LINEBREAK);
			}
		}
		buffer.append(NEWLINE);
		// Save the file
		IFile fileHandle = myTopBuildDir.getFile(SRCSFILE_NAME);
		save(buffer, fileHandle);
	}

	/**
	 * The makefile generator generates a Macro for each type of output, other than
	 * final artifact, created by the build.
	 *
	 * @param fileHandle The file that should be populated with the output
	 */
	protected  void generateObjectsMakefile(  Set<String> outputMacros)
			throws CoreException {
		StringBuffer macroBuffer = new StringBuffer();
		macroBuffer.append(addDefaultHeader());

		for (String macroName : outputMacros) {
			if (!macroName.isBlank()) {
				macroBuffer.append(macroName).append(MAKE_EQUAL);
				macroBuffer.append(NEWLINE);
				macroBuffer.append(NEWLINE);
			}
		}
		IFile fileHandle = myTopBuildDir.getFile( OBJECTS_MAKFILE);
		save(macroBuffer, fileHandle);
	}
    
    
    /****************************************************************************************
     * End of Sources/objects MakeFile generation code
     ****************************************************************************************/
    
    
    
    
    /****************************************************************************************
     * Top MakeFile generation code
     ****************************************************************************************/
    
     protected void topMakeGeneratefile( Set<String> objMacroNames)
            throws CoreException {

        StringBuffer buffer = new StringBuffer();
        buffer.append(addDefaultHeader());

        buffer.append(topMakeGetIncludeSubDirs());
        buffer.append(topMakeGetIncludeDependencies());
        buffer.append(topMakeGetRMCommand( objMacroNames));
        buffer.append(topMakeGetTargets());// this is the include dependencies
        // TOFIX the content from the append below should come from a registered method
        buffer.append("\n#bootloaderTest\n" + "BurnBootLoader: \n" //$NON-NLS-1$ //$NON-NLS-2$
                + "\t@echo trying to burn bootloader ${bootloader.tool}\n" //$NON-NLS-1$
                + "\t${tools.${bootloader.tool}.erase.pattern}\n" + "\t${tools.${bootloader.tool}.bootloader.pattern}\n" //$NON-NLS-1$ //$NON-NLS-2$
                + "\n" + "uploadWithBuild: all\n" //$NON-NLS-1$ //$NON-NLS-2$
                + "\t@echo trying to build and upload with upload tool ${upload.tool}\n" //$NON-NLS-1$
                + "\t${tools.${upload.tool}.upload.pattern}\n" + "\n" + "uploadWithoutBuild: \n" //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                + "\t@echo trying to upload without build with upload tool ${upload.tool}\n" //$NON-NLS-1$
                + "\t${tools.${upload.tool}.upload.pattern}\n" + "    \n" + "uploadWithProgrammerWithBuild: all\n" //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                + "\t@echo trying to build and upload with programmer ${program.tool}\n" //$NON-NLS-1$
                + "\t${tools.${program.tool}.program.pattern}\n" + "\n" + "uploadWithProgrammerWithoutBuild: \n" //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                + "\t@echo trying to upload with programmer ${program.tool} without build\n" //$NON-NLS-1$
                + "\t${tools.${program.tool}.program.pattern}\n\n"); //$NON-NLS-1$
        buffer.append(topMakeGetMacros());
        buffer.append(topMakeGetMakeRules());
        buffer.append(topMakeGetFinalTargets("", "")); //$NON-NLS-1$ //$NON-NLS-2$

        IFile fileHandle = myTopBuildDir.getFile(MAKEFILE_NAME);
        save(buffer, fileHandle);
    }

     protected  StringBuffer topMakeGetIncludeSubDirs() {
        StringBuffer buffer = new StringBuffer();

        for (IContainer subDir : myFoldersToBuild) {
            String includeFile = subDir.getProjectRelativePath().append(MODFILE_NAME).toOSString();
            buffer.append("-include " + includeFile).append(NEWLINE); //$NON-NLS-1$
        }
        buffer.append("-include sources.mk").append(NEWLINE); //$NON-NLS-1$
        buffer.append("-include objects.mk").append(NEWLINE).append(NEWLINE); //$NON-NLS-1$
        return buffer;
    }

     protected  StringBuffer topMakeGetRMCommand(   Set<String> myDependencyMacros) {
        IConfiguration config = myAutoBuildConfData.getConfiguration();
        StringBuffer buffer = new StringBuffer();
        buffer.append("-include " + ROOT + FILE_SEPARATOR + MAKEFILE_INIT).append(NEWLINE); //$NON-NLS-1$
        buffer.append(NEWLINE);
        // Get the clean command from the build model
        buffer.append("RM := "); //$NON-NLS-1$
        // support macros in the clean command
        String cleanCommand = resolveValueToMakefileFormat(config.getCleanCommand(), EMPTY_STRING, WHITESPACE,
                IBuildMacroProvider.CONTEXT_CONFIGURATION, myAutoBuildConfData);
        buffer.append(cleanCommand).append(NEWLINE);
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
        buffer.append("-include ").append(ROOT).append(FILE_SEPARATOR).append(MAKEFILE_DEFS).append(NEWLINE); //$NON-NLS-1$
        return (buffer.append(NEWLINE));
    }

     protected  String topMakeGetPreBuildStep() {
        String prebuildStep = myAutoBuildConfData.getConfiguration().getPrebuildStep();
        // JABA issue927 adding recipe.hooks.sketch.prebuild.NUMBER.pattern as cdt
        // prebuild command if needed
        // ICConfigurationDescription confDesc =
        // ManagedBuildManager.getDescriptionForConfiguration(config);
        // String sketchPrebuild =
        // io.sloeber.core.common.Common.getBuildEnvironmentVariable(confDesc,
        // "sloeber.prebuild",
        // new String(), false);
        String sketchPrebuild = resolveValueToMakefileFormat("sloeber.prebuild", EMPTY_STRING, WHITESPACE, //$NON-NLS-1$
                IBuildMacroProvider.CONTEXT_CONFIGURATION, myAutoBuildConfData);
        if (!sketchPrebuild.isEmpty()) {
            if (!prebuildStep.isEmpty()) {
                prebuildStep = prebuildStep + "\n\t" + sketchPrebuild; //$NON-NLS-1$
            } else {
                prebuildStep = sketchPrebuild;
            }
        }
        // end off JABA issue927
        // try to resolve the build macros in the prebuild step
        prebuildStep = resolveValueToMakefileFormat(prebuildStep, EMPTY_STRING, WHITESPACE,
                IBuildMacroProvider.CONTEXT_CONFIGURATION, myAutoBuildConfData);
        return prebuildStep.trim();
    }

     protected StringBuffer topMakeGetIncludeDependencies() {

        // JABA add the arduino upload/program targets
        StringBuffer buffer = new StringBuffer();

        String defaultTarget = "all:"; //$NON-NLS-1$
        String prebuildStep = topMakeGetPreBuildStep();
        if (prebuildStep.length() > 0) {
            // Add the comment for the "All" target
            buffer.append(COMMENT_START).append(MakefileGenerator_comment_build_alltarget).append(NEWLINE);
            buffer.append(defaultTarget).append(NEWLINE);
            buffer.append(TAB).append(MAKE).append(WHITESPACE).append(NO_PRINT_DIR).append(WHITESPACE).append(PREBUILD)
                    .append(NEWLINE);
            buffer.append(TAB).append(MAKE).append(WHITESPACE).append(NO_PRINT_DIR).append(WHITESPACE).append(MAINBUILD)
                    .append(NEWLINE);
            buffer.append(NEWLINE);
            // defaultTarget = MAINBUILD.concat(COLON);
            buffer.append(COMMENT_SYMBOL).append(WHITESPACE).append(MakefileGenerator_comment_build_mainbuildtarget)
                    .append(NEWLINE);

        } else {
            // Add the comment for the "All" target
            buffer.append(COMMENT_START).append(MakefileGenerator_comment_build_alltarget).append(NEWLINE);
        }
        return buffer;
    }

     protected StringBuffer topMakeGetTargets() {
        IConfiguration config = myAutoBuildConfData.getConfiguration();
        StringBuffer buffer = new StringBuffer();

        Set<ITool> targetTools = config.getToolChain().getTargetTools();
        buffer.append("all:").append(WHITESPACE); //$NON-NLS-1$
        for (ITool curTargetTool : targetTools) {
            Set<IFile> allTargets = myMakeRules.getTargetsForTool(curTargetTool);
            for (IFile curTarget : allTargets) {
                String targetString = GetNiceFileName(myTopBuildDir, curTarget);
                buffer.append(ensurePathIsGNUMakeTargetRuleCompatibleSyntax(targetString));
                buffer.append(WHITESPACE);
            }
        }

        // Add the Secondary Outputs to the all target, if any
        List<IOutputType> secondaryOutputs = config.getToolChain().getSecondaryOutputs();
        if (secondaryOutputs.size() > 0) {
            buffer.append(WHITESPACE).append(SECONDARY_OUTPUTS);
        }
        buffer.append(NEWLINE).append(NEWLINE);

        String prebuildStep = topMakeGetPreBuildStep();
        if (prebuildStep.length() > 0) {

            String preannouncebuildStep = config.getPreannouncebuildStep();
            buffer.append(PREBUILD).append(COLON).append(NEWLINE);
            if (preannouncebuildStep.length() > 0) {
                buffer.append(TAB).append(DASH).append(AT).append(escapedEcho(preannouncebuildStep));
            }
            buffer.append(TAB).append(DASH).append(prebuildStep).append(NEWLINE);
            buffer.append(TAB).append(DASH).append(AT).append(ECHO_BLANK_LINE).append(NEWLINE);
        }

        String postbuildStep = config.getPostbuildStep();
        postbuildStep = resolveValueToMakefileFormat(postbuildStep, EMPTY_STRING, WHITESPACE,
                IBuildMacroProvider.CONTEXT_CONFIGURATION, myAutoBuildConfData);
        postbuildStep = postbuildStep.trim();
        // Add the postbuild step, if specified
        if (postbuildStep.length() > 0) {
            String postannouncebuildStep = config.getPostannouncebuildStep();
            buffer.append(POSTBUILD).append(COLON).append(NEWLINE);
            if (postannouncebuildStep.length() > 0) {
                buffer.append(TAB).append(DASH).append(AT).append(escapedEcho(postannouncebuildStep));
            }
            buffer.append(TAB).append(DASH).append(postbuildStep).append(NEWLINE);
            buffer.append(TAB).append(DASH).append(AT).append(ECHO_BLANK_LINE).append(NEWLINE);
        }

        return buffer;
    }

    private static StringBuffer topMakeGetFinalTargets(String prebuildStep, String postbuildStep) {
        StringBuffer buffer = new StringBuffer();
        buffer.append(NEWLINE).append(NEWLINE);

        // Add all the needed dummy and phony targets
        buffer.append(".PHONY: all clean dependents"); //$NON-NLS-1$
        if (prebuildStep.length() > 0) {
            buffer.append(WHITESPACE).append(MAINBUILD).append(WHITESPACE).append(PREBUILD);
        }
        if (postbuildStep.length() > 0) {
            buffer.append(WHITESPACE).append(POSTBUILD);
        }

        buffer.append(NEWLINE);
        // Include makefile.targets supplemental makefile
        buffer.append("-include ").append(ROOT).append(FILE_SEPARATOR).append(MAKEFILE_TARGETS).append(NEWLINE); //$NON-NLS-1$
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
                    buffer.append(GetNiceFileName(myTopBuildDir, file)).append(WHITESPACE);
                }
                buffer.append(NEWLINE);
                buffer.append(NEWLINE);
            }
        }
        return buffer;
    }

    protected  StringBuffer topMakeGetMakeRules( ) {
        StringBuffer buffer = new StringBuffer();
        buffer.append(NEWLINE);
        buffer.append(COMMENT_START).append(MakefileGenerator_comment_build_rule).append(NEWLINE);

        for (MakeRule makeRule : myMakeRules.getMakeRules()) {
            if (makeRule.getSequenceGroupID() != 0) {
                buffer.append(makeRule.getRule(myProject, myTopBuildDir, myAutoBuildConfData));
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
    
    
    
    
    
}
