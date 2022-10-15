package io.sloeber.managedBuild.Internal;

import static io.sloeber.managedBuild.Internal.ManagebBuildCommon.*;
import static io.sloeber.managedBuild.Internal.ManagedBuildConstants.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.cdt.core.settings.model.util.PathSettingsContainer;
import org.eclipse.cdt.managedbuilder.core.BuildException;
import org.eclipse.cdt.managedbuilder.core.IConfiguration;
import org.eclipse.cdt.managedbuilder.core.IInputType;
import org.eclipse.cdt.managedbuilder.core.IManagedCommandLineGenerator;
import org.eclipse.cdt.managedbuilder.core.IManagedCommandLineInfo;
import org.eclipse.cdt.managedbuilder.core.IOutputType;
import org.eclipse.cdt.managedbuilder.core.ITool;
import org.eclipse.cdt.managedbuilder.core.ManagedBuildManager;
import org.eclipse.cdt.managedbuilder.internal.core.ManagedMakeMessages;
import org.eclipse.cdt.managedbuilder.internal.macros.FileContextData;
import org.eclipse.cdt.managedbuilder.macros.BuildMacroException;
import org.eclipse.cdt.managedbuilder.macros.IBuildMacroProvider;
import org.eclipse.cdt.managedbuilder.makegen.gnu.IManagedBuildGnuToolInfo;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;

public class TopMakeFileGenerator {
    private ArduinoGnuMakefileGenerator caller = null;
    private Map<IOutputType, List<IFile>> myAllSourceTargets = null;
    private Set<String> myDependencyMacros = null;
    private Set<IFile> myDependencyFiles = null;

    private IConfiguration getConfig() {
        return caller.getConfig();
    }

    private IProject getProject() {
        return caller.getProject();
    }

    TopMakeFileGenerator(ArduinoGnuMakefileGenerator theCaller, Map<IOutputType, List<IFile>> allSourceTargets,
            Set<String> dependecyMacros, Set<IFile> dependencyFiles) {
        caller = theCaller;
        myAllSourceTargets = allSourceTargets;
        myDependencyMacros = dependecyMacros;
        myDependencyFiles = dependencyFiles;
    }

    /**
     * @return Collection of subdirectories (IContainers) contributing source code
     *         to the build
     */
    private Collection<IContainer> getSubdirList() {
        return caller.getSubdirList();
    }

    private Vector<String> getRuleList() {
        return caller.getRuleList();
    }

    public PathSettingsContainer getToolInfos() {
        return caller.getToolInfos();
    }

    /**
     * Create the entire contents of the makefile.
     *
     * @param fileHandle
     *            The file to place the contents in.
     * @param rebuild
     *            FLag signaling that the user is doing a full rebuild
     */
    public void populateTopMakefile(IFile fileHandle, boolean rebuild) throws CoreException {
        StringBuffer buffer = new StringBuffer();
        // Add the header
        buffer.append(addDefaultHeader());
        // Add the macro definitions
        buffer.append(addMacros());
        // List to collect needed build output variables
        List<String> outputVarsAdditionsList = new ArrayList<String>();
        // Determine target rules
        StringBuffer targetRules = addTargets(outputVarsAdditionsList, rebuild);
        // Add outputMacros that were added to by the target rules
        //TOFIX reenable line below
        //buffer.append(writeTopAdditionMacros(outputVarsAdditionsList, getTopBuildOutputVars()));
        // Add target rules
        buffer.append(targetRules);
        // Save the file
        save(buffer, fileHandle);
    }

    private StringBuffer addMacros() {
        IConfiguration config = getConfig();
        StringBuffer buffer = new StringBuffer();
        buffer.append("-include " + ROOT + FILE_SEPARATOR + MAKEFILE_INIT).append(NEWLINE);
        buffer.append(NEWLINE);
        // Get the clean command from the build model
        buffer.append("RM := ");
        // support macros in the clean command
        String cleanCommand = config.getCleanCommand();
        try {
            cleanCommand = ManagedBuildManager.getBuildMacroProvider().resolveValueToMakefileFormat(
                    config.getCleanCommand(), EMPTY_STRING, WHITESPACE, IBuildMacroProvider.CONTEXT_CONFIGURATION,
                    config);
        } catch (BuildMacroException e) {
            // jaba is not going to write this code
        }

        buffer.append(cleanCommand).append(NEWLINE);
        buffer.append(NEWLINE);
        // Now add the source providers
        buffer.append(COMMENT_SYMBOL).append(WHITESPACE).append(ManagedMakeMessages.getResourceString(SRC_LISTS))
                .append(NEWLINE);
        buffer.append("-include sources.mk").append(NEWLINE);
        // JABA Change the include of the "root" (our sketch) folder to be
        // before
        // libraries and other files
        buffer.append("-include subdir.mk").append(NEWLINE);
        // Add includes for each subdir in child-subdir-first order (required
        // for makefile rule matching to work).
        List<String> subDirList = new ArrayList<>();
        for (IContainer subDir : getSubdirList()) {
            String projectRelativePath = subDir.getProjectRelativePath().toOSString();
            if (!projectRelativePath.isEmpty())
                subDirList.add(0, projectRelativePath);
        }
        Collections.sort(subDirList, Collections.reverseOrder());
        for (String dir : subDirList) {
            buffer.append("-include ").append(escapeWhitespaces(dir)).append(FILE_SEPARATOR).append("subdir.mk")
                    .append(NEWLINE);
        }
        // Change the include of the "root" (our sketch) folder to be before
        // libraries and other files
        // buffer.append("-include subdir.mk" + NEWLINE); //
        buffer.append("-include objects.mk").append(NEWLINE).append(NEWLINE);
        if (!myDependencyMacros.isEmpty()) {
            buffer.append("ifneq ($(MAKECMDGOALS),clean)").append(NEWLINE);
            for (String depsMacro : myDependencyMacros) {
                buffer.append("ifneq ($(strip $(").append(depsMacro).append(")),)").append(NEWLINE);
                buffer.append("-include $(").append(depsMacro).append(')').append(NEWLINE);
                buffer.append("endif").append(NEWLINE);
            }
            buffer.append("endif").append(NEWLINE).append(NEWLINE);
        }
        // Include makefile.defs supplemental makefile
        buffer.append("-include ").append(ROOT).append(FILE_SEPARATOR).append(MAKEFILE_DEFS).append(NEWLINE);
        return (buffer.append(NEWLINE));
    }

    private StringBuffer addTargets(List<String> outputVarsAdditionsList, boolean rebuild) {
        IConfiguration config = getConfig();
        StringBuffer buffer = new StringBuffer();
        // IConfiguration config = info.getDefaultConfiguration();
        // Assemble the information needed to generate the targets
        String prebuildStep = config.getPrebuildStep();
        // JABA issue927 adding recipe.hooks.sketch.prebuild.NUMBER.pattern as cdt
        // prebuild command if needed
        ICConfigurationDescription confDesc = ManagedBuildManager.getDescriptionForConfiguration(config);
        String sketchPrebuild = io.sloeber.core.common.Common.getBuildEnvironmentVariable(confDesc, "sloeber.prebuild",
                new String(), false);
        if (!sketchPrebuild.isEmpty()) {
            String separator = new String();
            if (!prebuildStep.isEmpty()) {
                prebuildStep = prebuildStep + "\n\t" + sketchPrebuild;
            } else {
                prebuildStep = sketchPrebuild;
            }
        }
        // end off JABA issue927
        try {
            // try to resolve the build macros in the prebuild step
            prebuildStep = ManagedBuildManager.getBuildMacroProvider().resolveValueToMakefileFormat(prebuildStep,
                    EMPTY_STRING, WHITESPACE, IBuildMacroProvider.CONTEXT_CONFIGURATION, config);
        } catch (BuildMacroException e) {
            /* JABA is not going to write this code */
        }
        prebuildStep = prebuildStep.trim();
        String postbuildStep = config.getPostbuildStep();
        try {
            // try to resolve the build macros in the postbuild step
            postbuildStep = ManagedBuildManager.getBuildMacroProvider().resolveValueToMakefileFormat(postbuildStep,
                    EMPTY_STRING, WHITESPACE, IBuildMacroProvider.CONTEXT_CONFIGURATION, config);
        } catch (BuildMacroException e) {
            /* JABA is not going to write this code */
        }
        postbuildStep = postbuildStep.trim();
        String preannouncebuildStep = config.getPreannouncebuildStep();
        String postannouncebuildStep = config.getPostannouncebuildStep();
        String targets = rebuild ? "clean all" : "all";
        ITool targetTool = config.calculateTargetTool();
        // if (targetTool == null) {
        // targetTool = info.getToolFromOutputExtension(buildTargetExt);
        // }
        // Get all the projects the build target depends on
        // If this configuration produces a static archive, building the archive
        // doesn't depend on the output
        // from any of the referenced configurations
        IConfiguration[] refConfigs = new IConfiguration[0];
        if (config.getBuildArtefactType() == null || !ManagedBuildManager.BUILD_ARTEFACT_TYPE_PROPERTY_STATICLIB
                .equals(config.getBuildArtefactType().getId()))
            refConfigs = ManagedBuildManager.getReferencedConfigurations(config);
        /*
         * try { refdProjects = project.getReferencedProjects(); } catch (CoreException
         * e) { // There are 2 exceptions; the project does not exist or it is not open
         * // and neither conditions apply if we are building for it .... }
         */
        // If a prebuild step exists, redefine the all target to be
        // all: {pre-build} main-build
        // and then reset the "traditional" all target to main-build
        // This will allow something meaningful to happen if the generated
        // makefile is
        // extracted and run standalone via "make all"
        //

        // JABA add the arduino upload/program targets
        buffer.append("\n#bootloaderTest\n" + "BurnBootLoader: \n"
                + "\t@echo trying to burn bootloader ${bootloader.tool}\n"
                + "\t${tools.${bootloader.tool}.erase.pattern}\n" + "\t${tools.${bootloader.tool}.bootloader.pattern}\n"
                + "\n" + "uploadWithBuild: all\n"
                + "\t@echo trying to build and upload with upload tool ${upload.tool}\n"
                + "\t${tools.${upload.tool}.upload.pattern}\n" + "\n" + "uploadWithoutBuild: \n"
                + "\t@echo trying to upload without build with upload tool ${upload.tool}\n"
                + "\t${tools.${upload.tool}.upload.pattern}\n" + "    \n" + "uploadWithProgrammerWithBuild: all\n"
                + "\t@echo trying to build and upload with programmer ${program.tool}\n"
                + "\t${tools.${program.tool}.program.pattern}\n" + "\n" + "uploadWithProgrammerWithoutBuild: \n"
                + "\t@echo trying to upload with programmer ${program.tool} without build\n"
                + "\t${tools.${program.tool}.program.pattern}\n\n");
        String defaultTarget = "all:";
        if (prebuildStep.length() > 0) {
            // Add the comment for the "All" target
            buffer.append(COMMENT_SYMBOL).append(WHITESPACE).append(ManagedMakeMessages.getResourceString(ALL_TARGET))
                    .append(NEWLINE);
            buffer.append(defaultTarget).append(NEWLINE);
            buffer.append(TAB).append(MAKE).append(WHITESPACE).append(NO_PRINT_DIR).append(WHITESPACE).append(PREBUILD)
                    .append(NEWLINE);
            buffer.append(TAB).append(MAKE).append(WHITESPACE).append(NO_PRINT_DIR).append(WHITESPACE).append(MAINBUILD)
                    .append(NEWLINE);
            buffer.append(NEWLINE);
            defaultTarget = MAINBUILD.concat(COLON);
            buffer.append(COMMENT_SYMBOL).append(WHITESPACE)
                    .append(ManagedMakeMessages.getResourceString(MAINBUILD_TARGET)).append(NEWLINE);
        } else
            // Add the comment for the "All" target
            buffer.append(COMMENT_SYMBOL).append(WHITESPACE).append(ManagedMakeMessages.getResourceString(ALL_TARGET))
                    .append(NEWLINE);
        // Write out the all target first in case someone just runs make
        // all: <target_name> or mainbuild: <target_name>
        String outputPrefix = EMPTY_STRING;
        if (targetTool != null) {
            outputPrefix = targetTool.getOutputPrefix();
        }
        buffer.append(defaultTarget).append(WHITESPACE).append(outputPrefix)
                .append(ensurePathIsGNUMakeTargetRuleCompatibleSyntax(caller.buildTargetName));
        if (caller.buildTargetExt.length() > 0) {
            buffer.append(DOT).append(caller.buildTargetExt);
        }
        // Add the Secondary Outputs to the all target, if any
        IOutputType[] secondaryOutputs = config.getToolChain().getSecondaryOutputs();
        if (secondaryOutputs.length > 0) {
            buffer.append(WHITESPACE).append(SECONDARY_OUTPUTS);
        }
        buffer.append(NEWLINE).append(NEWLINE);
        /*
         * The build target may depend on other projects in the workspace. These are
         * captured in the deps target: deps: <cd <Proj_Dep_1/build_dir>; $(MAKE) [clean
         * all | all]>
         */
        // Vector managedProjectOutputs = new Vector(refdProjects.length);
        // if (refdProjects.length > 0) {
        Vector<String> managedProjectOutputs = new Vector<String>(refConfigs.length);
        if (refConfigs.length > 0) {
            boolean addDeps = true;
            // if (refdProjects != null) {
            for (IConfiguration depCfg : refConfigs) {
                // IProject dep = refdProjects[i];
                if (!depCfg.isManagedBuildOn())
                    continue;
                // if (!dep.exists()) continue;
                if (addDeps) {
                    buffer.append("dependents:").append(NEWLINE);
                    addDeps = false;
                }
                String buildDir = depCfg.getOwner().getLocation().toOSString();
                String depTargets = targets;
                // if (ManagedBuildManager.manages(dep)) {
                // Add the current configuration to the makefile path
                // IManagedBuildInfo depInfo =
                // ManagedBuildManager.getBuildInfo(dep);
                buildDir += FILE_SEPARATOR + depCfg.getName();
                // Extract the build artifact to add to the dependency list
                String depTarget = depCfg.getArtifactName();
                String depExt = depCfg.getArtifactExtension();
                try {
                    // try to resolve the build macros in the artifact extension
                    depExt = ManagedBuildManager.getBuildMacroProvider().resolveValueToMakefileFormat(depExt, "", " ",
                            IBuildMacroProvider.CONTEXT_CONFIGURATION, depCfg);
                } catch (BuildMacroException e) {
                    /* JABA is not going to write this code */
                }
                try {
                    // try to resolve the build macros in the artifact name
                    String resolved = ManagedBuildManager.getBuildMacroProvider().resolveValueToMakefileFormat(
                            depTarget, "", " ", IBuildMacroProvider.CONTEXT_CONFIGURATION, depCfg);
                    if ((resolved = resolved.trim()).length() > 0)
                        depTarget = resolved;
                } catch (BuildMacroException e) {
                    /* JABA is not going to write this code */
                }
                String depPrefix = depCfg.getOutputPrefix(depExt);
                if (depCfg.needsRebuild()) {
                    depTargets = "clean all";
                }
                String dependency = buildDir + FILE_SEPARATOR + depPrefix + depTarget;
                if (depExt.length() > 0) {
                    dependency += DOT + depExt;
                }
                dependency = escapeWhitespaces(dependency);
                managedProjectOutputs.add(dependency);
                // }
                buffer.append(TAB).append("-cd").append(WHITESPACE).append(escapeWhitespaces(buildDir))
                        .append(WHITESPACE).append(LOGICAL_AND).append(WHITESPACE).append("$(MAKE) ").append(depTargets)
                        .append(NEWLINE);
            }
            // }
            buffer.append(NEWLINE);
        }
        // Add the targets tool rules
        buffer.append(addTargetsRules(targetTool, outputVarsAdditionsList, managedProjectOutputs,
                (postbuildStep.length() > 0)));
        // Add the prebuild step target, if specified
        if (prebuildStep.length() > 0) {
            buffer.append(PREBUILD).append(COLON).append(NEWLINE);
            if (preannouncebuildStep.length() > 0) {
                buffer.append(TAB).append(DASH).append(AT).append(escapedEcho(preannouncebuildStep));
            }
            buffer.append(TAB).append(DASH).append(prebuildStep).append(NEWLINE);
            buffer.append(TAB).append(DASH).append(AT).append(ECHO_BLANK_LINE).append(NEWLINE);
        }
        // Add the postbuild step, if specified
        if (postbuildStep.length() > 0) {
            buffer.append(POSTBUILD).append(COLON).append(NEWLINE);
            if (postannouncebuildStep.length() > 0) {
                buffer.append(TAB).append(DASH).append(AT).append(escapedEcho(postannouncebuildStep));
            }
            buffer.append(TAB).append(DASH).append(postbuildStep).append(NEWLINE);
            buffer.append(TAB).append(DASH).append(AT).append(ECHO_BLANK_LINE).append(NEWLINE);
        }
        // Add the Secondary Outputs target, if needed
        if (secondaryOutputs.length > 0) {
            buffer.append(SECONDARY_OUTPUTS).append(COLON);
            Vector<String> outs2 = calculateSecondaryOutputs(secondaryOutputs);
            for (int i = 0; i < outs2.size(); i++) {
                buffer.append(WHITESPACE).append("$(").append(outs2.get(i)).append(')');
            }
            buffer.append(NEWLINE).append(NEWLINE);
        }
        // Add all the needed dummy and phony targets
        buffer.append(".PHONY: all clean dependents");
        if (prebuildStep.length() > 0) {
            buffer.append(WHITESPACE).append(MAINBUILD).append(WHITESPACE).append(PREBUILD);
        }
        if (postbuildStep.length() > 0) {
            buffer.append(WHITESPACE).append(POSTBUILD);
        }
        buffer.append(NEWLINE);
        for (String output : managedProjectOutputs) {
            buffer.append(output).append(COLON).append(NEWLINE);
        }
        buffer.append(NEWLINE);
        // Include makefile.targets supplemental makefile
        buffer.append("-include ").append(ROOT).append(FILE_SEPARATOR).append(MAKEFILE_TARGETS).append(NEWLINE);
        return buffer;
    }

    /**
     * Create the rule
     *
     * @param buffer
     *            Buffer to add makefile rules to
     * @param bTargetTool
     *            True if this is the target tool
     * @param targetName
     *            If this is the "targetTool", the target file name, else
     *            <code>null</code>
     * @param targetExt
     *            If this is the "targetTool", the target file extension, else
     *            <code>null</code>
     * @param outputVarsAdditionsList
     *            list to add needed build output variables to
     * @param managedProjectOutputs
     *            Other projects in the workspace that this project depends upon
     * @param bEmitPostBuildStepCall
     *            Emit post-build step invocation
     */
    protected boolean addRuleForTool(ITool tool, StringBuffer buffer, boolean bTargetTool, String targetName,
            String targetExt, List<String> outputVarsAdditionsList, Vector<String> managedProjectOutputs,
            boolean bEmitPostBuildStepCall) {
        IConfiguration config = getConfig();
        Vector<String> inputs = new Vector<String>();
        Vector<String> dependencies = new Vector<String>();
        Vector<String> outputs = new Vector<String>();
        Vector<String> enumeratedPrimaryOutputs = new Vector<String>();
        Vector<String> enumeratedSecondaryOutputs = new Vector<String>();
        Vector<String> outputVariables = new Vector<String>();
        Vector<String> additionalTargets = new Vector<String>();
        String outputPrefix = EMPTY_STRING;
        if (!getToolInputsOutputs(tool, inputs, dependencies, outputs, enumeratedPrimaryOutputs,
                enumeratedSecondaryOutputs, outputVariables, additionalTargets, bTargetTool, managedProjectOutputs)) {
            return false;
        }
        // If we have no primary output, make all of the secondary outputs the
        // primary output
        if (enumeratedPrimaryOutputs.size() == 0) {
            enumeratedPrimaryOutputs = enumeratedSecondaryOutputs;
            enumeratedSecondaryOutputs.clear();
        }
        // Add the output variables for this tool to our list
        outputVarsAdditionsList.addAll(outputVariables);
        // Create the build rule
        String buildRule = EMPTY_STRING;
        String outflag = tool.getOutputFlag();
        String primaryOutputs = EMPTY_STRING;
        String primaryOutputsQuoted = EMPTY_STRING;
        boolean first = true;
        for (int i = 0; i < enumeratedPrimaryOutputs.size(); i++) {
            String output = enumeratedPrimaryOutputs.get(i);
            if (!first) {
                primaryOutputs += WHITESPACE;
                primaryOutputsQuoted += WHITESPACE;
            }
            first = false;
            primaryOutputs += new Path(output).toOSString();
            primaryOutputsQuoted += ensurePathIsGNUMakeTargetRuleCompatibleSyntax(new Path(output).toOSString());
        }
        buildRule += (primaryOutputsQuoted + COLON + WHITESPACE);
        first = true;
        String calculatedDependencies = EMPTY_STRING;
        for (int i = 0; i < dependencies.size(); i++) {
            String input = dependencies.get(i);
            if (!first)
                calculatedDependencies += WHITESPACE;
            first = false;
            calculatedDependencies += input;
        }
        buildRule += calculatedDependencies;
        // We can't have duplicates in a makefile
        if (getRuleList().contains(buildRule)) {
            /* JABA is not going to write this code */
        } else {
            getRuleList().add(buildRule);
            buffer.append(buildRule).append(NEWLINE);
            if (bTargetTool) {
                buffer.append(TAB).append(AT).append(escapedEcho(MESSAGE_START_BUILD + WHITESPACE + OUT_MACRO));
            }
            buffer.append(TAB).append(AT).append(escapedEcho(tool.getAnnouncement()));
            // Get the command line for this tool invocation
            String[] flags;
            try {
                flags = tool.getToolCommandFlags(null, null);
            } catch (BuildException ex) {
                // TODO report error
                flags = EMPTY_STRING_ARRAY;
            }
            String command = tool.getToolCommand();
            try {
                // try to resolve the build macros in the tool command
                String resolvedCommand = ManagedBuildManager.getBuildMacroProvider().resolveValueToMakefileFormat(
                        command, EMPTY_STRING, WHITESPACE, IBuildMacroProvider.CONTEXT_FILE,
                        new FileContextData(null, null, null, tool));
                if ((resolvedCommand = resolvedCommand.trim()).length() > 0) {
                    command = resolvedCommand.replace(" \"\" ", " ");
                }
            } catch (BuildMacroException e) {
                /* JABA is not going to write this code */
            }
            String[] cmdInputs = inputs.toArray(new String[inputs.size()]);
            IManagedCommandLineGenerator gen = tool.getCommandLineGenerator();
            IManagedCommandLineInfo cmdLInfo = gen.generateCommandLineInfo(tool, command, flags, outflag, outputPrefix,
                    primaryOutputs, cmdInputs, getToolCommandLinePattern(config, tool));
            // The command to build
            String buildCmd = null;
            if (cmdLInfo == null) {
                String toolFlags;
                try {
                    toolFlags = tool.getToolCommandFlagsString(null, null);
                } catch (BuildException ex) {
                    // TODO report error
                    toolFlags = EMPTY_STRING;
                }
                buildCmd = command + WHITESPACE + toolFlags + WHITESPACE + outflag + WHITESPACE + outputPrefix
                        + primaryOutputs + WHITESPACE + IN_MACRO;
            } else
                buildCmd = cmdLInfo.getCommandLine();
            // resolve any remaining macros in the command after it has been
            // generated
            try {
                //TOFIX JABA heavy hack to get the combiner to work properly
                //if the command contains ${ARCHIVES}
                //remove the ${AR}
                //replace ${ARCHIVES} with ${AR}
                String ARCHIVES = " ${ARCHIVES} ";
                String AR = " $(AR) ";
                if (buildCmd.contains(ARCHIVES)) {
                    buildCmd = buildCmd.replace(AR, " ");
                    buildCmd = buildCmd.replace(ARCHIVES, AR);
                }
                //end JABA heavy hack
                String resolvedCommand = ManagedBuildManager.getBuildMacroProvider().resolveValueToMakefileFormat(
                        buildCmd, EMPTY_STRING, WHITESPACE, IBuildMacroProvider.CONTEXT_FILE,
                        new FileContextData(null, null, null, tool));
                if ((resolvedCommand = resolvedCommand.trim()).length() > 0) {
                    buildCmd = resolvedCommand.replace(" \"\" ", " ");
                }
            } catch (BuildMacroException e) {
                /* JABA is not going to write this code */
            }
            // buffer.append(TAB).append(AT).append(escapedEcho(buildCmd));
            // buffer.append(TAB).append(AT).append(buildCmd);
            buffer.append(TAB).append(buildCmd);
            // TODO
            // NOTE WELL: Dependency file generation is not handled for this
            // type of Tool
            // Echo finished message
            buffer.append(NEWLINE);
            buffer.append(TAB).append(AT).append(
                    escapedEcho((bTargetTool ? MESSAGE_FINISH_BUILD : MESSAGE_FINISH_FILE) + WHITESPACE + OUT_MACRO));
            buffer.append(TAB).append(AT).append(ECHO_BLANK_LINE);
            // If there is a post build step, then add a recursive invocation of
            // MAKE to invoke it after the main build
            // Note that $(MAKE) will instantiate in the recusive invocation to
            // the make command that was used to invoke
            // the makefile originally
            if (bEmitPostBuildStepCall) {
                buffer.append(TAB).append(MAKE).append(WHITESPACE).append(NO_PRINT_DIR).append(WHITESPACE)
                        .append(POSTBUILD).append(NEWLINE).append(NEWLINE);
            } else {
                // Just emit a blank line
                buffer.append(NEWLINE);
            }
        }
        // If we have secondary outputs, output dependency rules without
        // commands
        if (enumeratedSecondaryOutputs.size() > 0 || additionalTargets.size() > 0) {
            String primaryOutput = enumeratedPrimaryOutputs.get(0);
            Vector<String> addlOutputs = new Vector<String>();
            addlOutputs.addAll(enumeratedSecondaryOutputs);
            addlOutputs.addAll(additionalTargets);
            for (int i = 0; i < addlOutputs.size(); i++) {
                String output = addlOutputs.get(i);
                String depLine = output + COLON + WHITESPACE + primaryOutput + WHITESPACE + calculatedDependencies
                        + NEWLINE;
                buffer.append(depLine);
            }
            buffer.append(NEWLINE);
        }
        return true;
    }

    /**
     * @param outputVarsAdditionsList
     *            list to add needed build output variables to
     * @param buffer
     *            buffer to add rules to
     */
    private void generateRulesForConsumers(ITool generatingTool, List<String> outputVarsAdditionsList,
            StringBuffer buffer) {
        // Generate a build rule for any tool that consumes the output of this
        // tool
        PathSettingsContainer toolInfos = getToolInfos();
        ToolInfoHolder h = (ToolInfoHolder) toolInfos.getValue();
        ITool[] buildTools = h.buildTools;
        boolean[] buildToolsUsed = h.buildToolsUsed;
        IOutputType[] outTypes = generatingTool.getOutputTypes();
        for (IOutputType outType : outTypes) {
            String[] outExts = outType.getOutputExtensions(generatingTool);
            String outVariable = outType.getBuildVariable();
            if (outExts != null) {
                for (String outExt : outExts) {
                    for (int k = 0; k < buildTools.length; k++) {
                        ITool tool = buildTools[k];
                        if (!buildToolsUsed[k]) {
                            // Also has to match build variables if specified
                            IInputType inType = tool.getInputType(outExt);
                            if (inType != null) {
                                String inVariable = inType.getBuildVariable();
                                if ((outVariable == null && inVariable == null) || (outVariable != null
                                        && inVariable != null && outVariable.equals(inVariable))) {
                                    if (addRuleForTool(buildTools[k], buffer, false, null, null,
                                            outputVarsAdditionsList, null, false)) {
                                        buildToolsUsed[k] = true;
                                        // Look for tools that consume the
                                        // output
                                        generateRulesForConsumers(buildTools[k], outputVarsAdditionsList, buffer);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Returns the targets rules. The targets make file (top makefile) contains: 1
     * the rule for the final target tool 2 the rules for all of the tools that use
     * multipleOfType in their primary input type 3 the rules for all tools that use
     * the output of #2 tools
     *
     * @param outputVarsAdditionsList
     *            list to add needed build output variables to
     * @param managedProjectOutputs
     *            Other projects in the workspace that this project depends upon
     * @return StringBuffer
     */
    private StringBuffer addTargetsRules(ITool targetTool, List<String> outputVarsAdditionsList,
            Vector<String> managedProjectOutputs, boolean postbuildStep) {
        StringBuffer buffer = new StringBuffer();
        // Add the comment
        buffer.append(COMMENT_SYMBOL).append(WHITESPACE).append(ManagedMakeMessages.getResourceString(BUILD_TOP))
                .append(NEWLINE);
        PathSettingsContainer toolInfos = getToolInfos();
        ToolInfoHolder h = (ToolInfoHolder) toolInfos.getValue();
        ITool[] buildTools = h.buildTools;
        boolean[] buildToolsUsed = h.buildToolsUsed;
        // Get the target tool and generate the rule
        if (targetTool != null) {
            // Note that the name of the target we pass to addRuleForTool does
            // not
            // appear to be used there (and tool outputs are consulted
            // directly), but
            // we quote it anyway just in case it starts to use it in future.
            if (addRuleForTool(targetTool, buffer, true,
                    ensurePathIsGNUMakeTargetRuleCompatibleSyntax(caller.buildTargetName), caller.buildTargetExt,
                    outputVarsAdditionsList, managedProjectOutputs, postbuildStep)) {
                // Mark the target tool as processed
                for (int i = 0; i < buildTools.length; i++) {
                    if (targetTool == buildTools[i]) {
                        buildToolsUsed[i] = true;
                    }
                }
            }
        } else {
            buffer.append(TAB).append(AT).append(escapedEcho(MESSAGE_NO_TARGET_TOOL + WHITESPACE + OUT_MACRO));
        }
        // Generate the rules for all Tools that specify
        // InputType.multipleOfType, and any Tools that
        // consume the output of those tools. This does not apply to pre-3.0
        // integrations, since
        // the only "multipleOfType" tool is the "target" tool
        for (int i = 0; i < buildTools.length; i++) {
            ITool tool = buildTools[i];
            IInputType type = tool.getPrimaryInputType();
            if (type != null && type.getMultipleOfType()) {
                if (!buildToolsUsed[i]) {
                    addRuleForTool(tool, buffer, false, null, null, outputVarsAdditionsList, null, false);
                    // Mark the target tool as processed
                    buildToolsUsed[i] = true;
                    // Look for tools that consume the output
                    generateRulesForConsumers(tool, outputVarsAdditionsList, buffer);
                }
            }
        }
        // Add the comment
        buffer.append(COMMENT_SYMBOL).append(WHITESPACE).append(ManagedMakeMessages.getResourceString(BUILD_TARGETS))
                .append(NEWLINE);
        // Always add a clean target
        buffer.append("clean:").append(NEWLINE);
        buffer.append(TAB).append("-$(RM)").append(WHITESPACE);
        for (IOutputType entry : myAllSourceTargets.keySet()) {
            String macroName = entry.getBuildVariable();
            buffer.append("$(").append(macroName).append(')');
        }
        String outputPrefix = EMPTY_STRING;
        if (targetTool != null) {
            outputPrefix = targetTool.getOutputPrefix();
        }
        String completeBuildTargetName = outputPrefix + caller.buildTargetName;
        if (caller.buildTargetExt.length() > 0) {
            completeBuildTargetName = completeBuildTargetName + DOT + caller.buildTargetExt;
        }
        // if (completeBuildTargetName.contains(" ")) {
        // buffer.append(WHITESPACE + "\"" + completeBuildTargetName + "\"");
        // } else {
        // buffer.append(WHITESPACE + completeBuildTargetName);
        // }
        buffer.append(NEWLINE);
        buffer.append(TAB).append(DASH).append(AT).append(ECHO_BLANK_LINE).append(NEWLINE);
        return buffer;
    }

    /**
     * Write all macro addition entries in a map to the buffer
     */
    private StringBuffer writeTopAdditionMacros(List<String> varList, HashMap<String, String> varMap) {
        StringBuffer buffer = new StringBuffer();
        // Add the comment
        buffer.append(COMMENT_SYMBOL).append(WHITESPACE).append(ManagedMakeMessages.getResourceString(MOD_VARS))
                .append(NEWLINE);
        for (String curVar : varList) {
            buffer.append(varMap.get(curVar));
            buffer.append(NEWLINE);
        }
        return buffer.append(NEWLINE);
    }

    private Vector<String> calculateSecondaryOutputs(IOutputType[] secondaryOutputs) {
        PathSettingsContainer toolInfos = getToolInfos();
        ToolInfoHolder h = (ToolInfoHolder) toolInfos.getValue();
        ITool[] buildTools = h.buildTools;
        Vector<String> buildVars = new Vector<String>();
        for (int i = 0; i < buildTools.length; i++) {
            // Add the specified output build variables
            IOutputType[] outTypes = buildTools[i].getOutputTypes();
            if (outTypes != null && outTypes.length > 0) {
                for (int j = 0; j < outTypes.length; j++) {
                    IOutputType outType = outTypes[j];
                    // Is this one of the secondary outputs?
                    // Look for an outputType with this ID, or one with a
                    // superclass with this id
                    thisType: for (int k = 0; k < secondaryOutputs.length; k++) {
                        IOutputType matchType = outType;
                        do {
                            if (matchType.getId().equals(secondaryOutputs[k].getId())) {
                                buildVars.add(outType.getBuildVariable());
                                break thisType;
                            }
                            matchType = matchType.getSuperClass();
                        } while (matchType != null);
                    }
                }
            }
        }
        return buildVars;
    }

    private boolean getToolInputsOutputs(ITool tool, Vector<String> inputs, Vector<String> dependencies,
            Vector<String> outputs, Vector<String> enumeratedPrimaryOutputs, Vector<String> enumeratedSecondaryOutputs,
            Vector<String> outputVariables, Vector<String> additionalTargets, boolean bTargetTool,
            Vector<String> managedProjectOutputs) {
        PathSettingsContainer toolInfos = getToolInfos();
        ToolInfoHolder h = (ToolInfoHolder) toolInfos.getValue();
        ITool[] buildTools = h.buildTools;
        ArduinoManagedBuildGnuToolInfo[] gnuToolInfos = h.gnuToolInfos;
        // Get the information regarding the tool's inputs and outputs from the
        // objects
        // created by calculateToolInputsOutputs
        IManagedBuildGnuToolInfo toolInfo = null;
        for (int i = 0; i < buildTools.length; i++) {
            if (tool == buildTools[i]) {
                toolInfo = gnuToolInfos[i];
                break;
            }
        }
        if (toolInfo == null)
            return false;
        // Populate the output Vectors
        inputs.addAll(toolInfo.getCommandInputs());
        outputs.addAll(toolInfo.getCommandOutputs());
        enumeratedPrimaryOutputs.addAll(toolInfo.getEnumeratedPrimaryOutputs());
        enumeratedSecondaryOutputs.addAll(toolInfo.getEnumeratedSecondaryOutputs());
        outputVariables.addAll(toolInfo.getOutputVariables());
        Vector<String> unprocessedDependencies = toolInfo.getCommandDependencies();
        for (String path : unprocessedDependencies) {
            dependencies.add(ensurePathIsGNUMakeTargetRuleCompatibleSyntax(path));
        }
        additionalTargets.addAll(toolInfo.getAdditionalTargets());
        if (bTargetTool && managedProjectOutputs != null) {
            for (String output : managedProjectOutputs) {
                dependencies.add(output);
            }
        }
        return true;
    }

}
