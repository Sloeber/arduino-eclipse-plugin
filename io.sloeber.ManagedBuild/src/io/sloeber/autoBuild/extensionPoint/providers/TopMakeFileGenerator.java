package io.sloeber.autoBuild.extensionPoint.providers;

import static io.sloeber.autoBuild.Internal.ManagedBuildConstants.*;
import static io.sloeber.autoBuild.core.Messages.*;
import static io.sloeber.autoBuild.extensionPoint.providers.AutoBuildCommon.*;
import static io.sloeber.autoBuild.integration.Const.*;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import io.sloeber.autoBuild.api.IBuildMacroProvider;
import io.sloeber.autoBuild.integration.AutoBuildConfigurationData;
import io.sloeber.schema.api.IConfiguration;
import io.sloeber.schema.api.IOutputType;
import io.sloeber.schema.api.ITool;

public class TopMakeFileGenerator {

    public TopMakeFileGenerator() {
    }

    public static void generateMakefile(IFolder buildFolder, AutoBuildConfigurationData autoBuildConfData,
            Collection<IFolder> myFoldersToBuild, MakeRules myMakeRules, Set<String> myDependencyMacros)
            throws CoreException {
        IProject project = autoBuildConfData.getProject();

        StringBuffer buffer = new StringBuffer();
        buffer.append(addDefaultHeader());

        buffer.append(getMakeIncludeSubDirs(myFoldersToBuild));
        buffer.append(getMakeIncludeDependencies(autoBuildConfData));
        buffer.append(getMakeRMCommand(autoBuildConfData, myDependencyMacros));
        buffer.append(getMakeTopTargets(buildFolder, myMakeRules, autoBuildConfData));// this is the include dependencies
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
        buffer.append(getMakeMacros(buildFolder, myMakeRules));
        buffer.append(getMakeRules(project, buildFolder, myMakeRules, autoBuildConfData));
        buffer.append(getMakeFinalTargets("", "")); //$NON-NLS-1$ //$NON-NLS-2$

        IFile fileHandle = buildFolder.getFile(MAKEFILE_NAME);
        save(buffer, fileHandle);
    }

    private static StringBuffer getMakeIncludeSubDirs(Collection<IFolder> myFoldersToBuild) {
        StringBuffer buffer = new StringBuffer();

        for (IContainer subDir : myFoldersToBuild) {
            String includeFile = subDir.getProjectRelativePath().append(MODFILE_NAME).toOSString();
            buffer.append("-include " + includeFile).append(NEWLINE); //$NON-NLS-1$
        }
        buffer.append("-include sources.mk").append(NEWLINE); //$NON-NLS-1$
        buffer.append("-include objects.mk").append(NEWLINE).append(NEWLINE); //$NON-NLS-1$
        return buffer;
    }

    private static StringBuffer getMakeRMCommand(AutoBuildConfigurationData autoBuildConfData,
            Set<String> myDependencyMacros) {
        IConfiguration config = autoBuildConfData.getConfiguration();
        StringBuffer buffer = new StringBuffer();
        buffer.append("-include " + ROOT + FILE_SEPARATOR + MAKEFILE_INIT).append(NEWLINE); //$NON-NLS-1$
        buffer.append(NEWLINE);
        // Get the clean command from the build model
        buffer.append("RM := "); //$NON-NLS-1$
        // support macros in the clean command
        String cleanCommand = resolveValueToMakefileFormat(config.getCleanCommand(), EMPTY_STRING, WHITESPACE,
                IBuildMacroProvider.CONTEXT_CONFIGURATION, autoBuildConfData);
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

    private static String getPreBuildStep(AutoBuildConfigurationData autoBuildConfData) {
        String prebuildStep = autoBuildConfData.getConfiguration().getPrebuildStep();
        // JABA issue927 adding recipe.hooks.sketch.prebuild.NUMBER.pattern as cdt
        // prebuild command if needed
        // ICConfigurationDescription confDesc =
        // ManagedBuildManager.getDescriptionForConfiguration(config);
        // String sketchPrebuild =
        // io.sloeber.core.common.Common.getBuildEnvironmentVariable(confDesc,
        // "sloeber.prebuild",
        // new String(), false);
        String sketchPrebuild = resolveValueToMakefileFormat("sloeber.prebuild", EMPTY_STRING, WHITESPACE, //$NON-NLS-1$
                IBuildMacroProvider.CONTEXT_CONFIGURATION, autoBuildConfData);
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
                IBuildMacroProvider.CONTEXT_CONFIGURATION, autoBuildConfData);
        return prebuildStep.trim();
    }

    private static StringBuffer getMakeIncludeDependencies(AutoBuildConfigurationData autoBuildConfData) {

        // JABA add the arduino upload/program targets
        StringBuffer buffer = new StringBuffer();

        String defaultTarget = "all:"; //$NON-NLS-1$
        String prebuildStep = getPreBuildStep(autoBuildConfData);
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

    private static StringBuffer getMakeTopTargets(IFolder buildFolder, MakeRules myMakeRules,
            AutoBuildConfigurationData autoBuildConfData) {
        IConfiguration config = autoBuildConfData.getConfiguration();
        StringBuffer buffer = new StringBuffer();

        Set<ITool> targetTools = config.getToolChain().getTargetTools();
        buffer.append("all:").append(WHITESPACE); //$NON-NLS-1$
        for (ITool curTargetTool : targetTools) {
            Set<IFile> allTargets = myMakeRules.getTargetsForTool(curTargetTool);
            for (IFile curTarget : allTargets) {
                String targetString = GetNiceFileName(buildFolder, curTarget);
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

        String prebuildStep = getPreBuildStep(autoBuildConfData);
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
                IBuildMacroProvider.CONTEXT_CONFIGURATION, autoBuildConfData);
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

    private static StringBuffer getMakeFinalTargets(String prebuildStep, String postbuildStep) {
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

    private static StringBuffer getMakeMacros(IFolder buildRoot, MakeRules myMakeRules) {
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
                    buffer.append(GetNiceFileName(buildRoot, file)).append(WHITESPACE);
                }
                buffer.append(NEWLINE);
                buffer.append(NEWLINE);
            }
        }
        return buffer;
    }

    private static StringBuffer getMakeRules(IProject project, IFolder topBuildDir, MakeRules myMakeRules,
            AutoBuildConfigurationData autoBuildConfData) {
        StringBuffer buffer = new StringBuffer();
        buffer.append(NEWLINE);
        buffer.append(COMMENT_START).append(MakefileGenerator_comment_build_rule).append(NEWLINE);

        for (MakeRule makeRule : myMakeRules.getMakeRules()) {
            if (makeRule.getSequenceGroupID() != 0) {
                buffer.append(makeRule.getRule(project, topBuildDir, autoBuildConfData));
            }
        }
        return buffer;
    }

}
