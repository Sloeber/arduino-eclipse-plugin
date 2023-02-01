package io.sloeber.autoBuild.extensionPoint.providers;

import static io.sloeber.autoBuild.Internal.ManagedBuildConstants.*;
import static io.sloeber.autoBuild.core.Messages.*;
import static io.sloeber.autoBuild.extensionPoint.providers.ManagebBuildCommon.*;
import static io.sloeber.autoBuild.integration.Const.*;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.cdt.core.model.CoreModel;
import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.cdt.core.settings.model.ICProjectDescription;
//import org.eclipse.cdt.managedbuilder.core.IConfiguration;
//import org.eclipse.cdt.managedbuilder.core.IInputType;
//import org.eclipse.cdt.managedbuilder.core.IOutputType;
//import org.eclipse.cdt.managedbuilder.core.ITool;
//import org.eclipse.cdt.managedbuilder.core.ManagedBuildManager;
//import org.eclipse.cdt.managedbuilder.macros.BuildMacroException;
//import org.eclipse.cdt.managedbuilder.macros.IBuildMacroProvider;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;

import io.sloeber.autoBuild.api.IBuildMacroProvider;
import io.sloeber.schema.api.IConfiguration;
import io.sloeber.schema.api.IInputType;
import io.sloeber.schema.api.IOutputType;
import io.sloeber.schema.api.ITool;

public class TopMakeFileGenerator {
//    private MakefileGenerator caller = null;
//    private Set<MakeRule> mySubDirMakeRules = new LinkedHashSet<>();
    private MakeRules myMakeRules = new MakeRules();
    private Collection<IContainer> myFoldersToBuild;
//    private Map<IOutputType, Set<IFile>> myAllSourceTargets = new HashMap<>();
    private Set<String> myDependencyMacros = new HashSet<>();
//
//    private IConfiguration getConfig() {
//        return caller.getConfig();
//    }
//
//    private ICConfigurationDescription getCConfigurationDescription() {
//        return caller.getCConfigurationDescription();
//    }
//
//    private IProject getProject() {
//        return caller.getProject();
//    }
//
//    private IFolder getBuildFolder() {
//        return caller.getBuildFolder();
//    }

//    public TopMakeFileGenerator(MakefileGenerator theCaller, Set<MakeRule> subDirMakeRules,
//            Collection<IContainer> foldersToBuild) {
    public TopMakeFileGenerator() {
//        caller = theCaller;
//        mySubDirMakeRules = subDirMakeRules;
//        myFoldersToBuild = foldersToBuild;
//        for (MakeRule curMakeRule : mySubDirMakeRules) {
//            myAllSourceTargets.putAll(curMakeRule.getTargets());
//            myDependencyMacros.addAll(curMakeRule.getDependecyMacros());
//        }
//        MakeRules makeRules = new MakeRules();
//        Map<IOutputType, Set<IFile>> generatedFiles = new HashMap<>();
//        for (MakeRule makeRule : subDirMakeRules) {
//            Map<IOutputType, Set<IFile>> targets = makeRule.getTargets();
//            for (Entry<IOutputType, Set<IFile>> curTarget : targets.entrySet()) {
//                Set<IFile> esxistingTarget = generatedFiles.get(curTarget.getKey());
//                if (esxistingTarget != null) {
//                    esxistingTarget.addAll(curTarget.getValue());
//                } else {
//                    Set<IFile> copySet = new HashSet<>();
//                    copySet.addAll(curTarget.getValue());
//                    generatedFiles.put(curTarget.getKey(), copySet);
//                }
//
//            }
//        }
//        int depth = 10;
//        while (depth > 0) {
//            makeRules = getMakeRulesFromGeneratedFiles(generatedFiles);
//            generatedFiles.clear();
//            if (makeRules.size() > 0) {
//                depth--;
//                myMakeRules.addRules(makeRules);
//                generatedFiles.putAll(makeRules.getTargets());
//            } else {
//                depth = 0;
//            }
//        }

    }

    public static void generateMakefile(IFolder buildFolder,ICConfigurationDescription cfg,IConfiguration config, Collection<IFolder> myFoldersToBuild,MakeRules myMakeRules,Set<String> myDependencyMacros) throws CoreException {
    	IProject project=cfg.getProjectDescription().getProject();

        StringBuffer buffer = new StringBuffer();
        buffer.append(addDefaultHeader());

        buffer.append(getMakeIncludeSubDirs(myFoldersToBuild));
        buffer.append(getMakeIncludeDependencies(config, cfg));
        buffer.append(getMakeRMCommand( config, cfg, myDependencyMacros));
        buffer.append(getMakeTopTargets(config, buildFolder, myMakeRules, cfg));// this is the include dependencies
        // TOFIX the content from the append below should come from a registered method
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
        buffer.append(getMakeMacros(buildFolder, myMakeRules));
        buffer.append(getMakeRules(project, buildFolder, myMakeRules, cfg));
        buffer.append(getMakeFinalTargets("", ""));

        IFile fileHandle = buildFolder.getFile( MAKEFILE_NAME);
        save(buffer, fileHandle);
    }

    private static StringBuffer getMakeIncludeSubDirs( Collection<IFolder> myFoldersToBuild) {
        StringBuffer buffer = new StringBuffer();

        for (IContainer subDir : myFoldersToBuild) {
            String includeFile = subDir.getProjectRelativePath().append(MODFILE_NAME).toOSString();
            buffer.append("-include " + includeFile).append(NEWLINE);
        }
        buffer.append("-include sources.mk").append(NEWLINE);
        buffer.append("-include objects.mk").append(NEWLINE).append(NEWLINE);
        return buffer;
    }

    private static StringBuffer getMakeRMCommand(IConfiguration config,ICConfigurationDescription cfg, Set<String> myDependencyMacros ) {
        StringBuffer buffer = new StringBuffer();
        buffer.append("-include " + ROOT + FILE_SEPARATOR + MAKEFILE_INIT).append(NEWLINE);
        buffer.append(NEWLINE);
        // Get the clean command from the build model
        buffer.append("RM := ");
        // support macros in the clean command
        String cleanCommand = resolveValueToMakefileFormat(config.getCleanCommand(), EMPTY_STRING, WHITESPACE,
                IBuildMacroProvider.CONTEXT_CONFIGURATION, cfg);
        buffer.append(cleanCommand).append(NEWLINE);
        buffer.append(NEWLINE);

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

    private static String getPreBuildStep(IConfiguration config,ICConfigurationDescription cfg) {
        String prebuildStep = config.getPrebuildStep();
        // JABA issue927 adding recipe.hooks.sketch.prebuild.NUMBER.pattern as cdt
        // prebuild command if needed
        // ICConfigurationDescription confDesc =
        // ManagedBuildManager.getDescriptionForConfiguration(config);
        // String sketchPrebuild =
        // io.sloeber.core.common.Common.getBuildEnvironmentVariable(confDesc,
        // "sloeber.prebuild",
        // new String(), false);
        String sketchPrebuild = resolveValueToMakefileFormat("sloeber.prebuild", EMPTY_STRING, WHITESPACE,
                IBuildMacroProvider.CONTEXT_CONFIGURATION,cfg);
        if (!sketchPrebuild.isEmpty()) {
            if (!prebuildStep.isEmpty()) {
                prebuildStep = prebuildStep + "\n\t" + sketchPrebuild;
            } else {
                prebuildStep = sketchPrebuild;
            }
        }
        // end off JABA issue927
        // try to resolve the build macros in the prebuild step
        prebuildStep = resolveValueToMakefileFormat(prebuildStep, EMPTY_STRING, WHITESPACE,
                IBuildMacroProvider.CONTEXT_CONFIGURATION,cfg);
        return prebuildStep.trim();
    }

    private static StringBuffer getMakeIncludeDependencies(IConfiguration config,ICConfigurationDescription cfg) {

        // JABA add the arduino upload/program targets
        StringBuffer buffer = new StringBuffer();

        String defaultTarget = "all:";
        String prebuildStep = getPreBuildStep(config,cfg);
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

    private static StringBuffer getMakeTopTargets(IConfiguration config,IFolder buildFolder,MakeRules myMakeRules,ICConfigurationDescription cfg) {


        StringBuffer buffer = new StringBuffer();

        Set<ITool> targetTools = config.getToolChain().getTargetTools();
        buffer.append("all:").append(WHITESPACE);
        for(ITool curTargetTool:targetTools) {
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

        String prebuildStep = getPreBuildStep(config, cfg);
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
                IBuildMacroProvider.CONTEXT_CONFIGURATION, cfg);
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
        buffer.append(".PHONY: all clean dependents");
        if (prebuildStep.length() > 0) {
            buffer.append(WHITESPACE).append(MAINBUILD).append(WHITESPACE).append(PREBUILD);
        }
        if (postbuildStep.length() > 0) {
            buffer.append(WHITESPACE).append(POSTBUILD);
        }

        buffer.append(NEWLINE);
        // Include makefile.targets supplemental makefile
        buffer.append("-include ").append(ROOT).append(FILE_SEPARATOR).append(MAKEFILE_TARGETS).append(NEWLINE);
        return buffer;
    }



    private static StringBuffer getMakeMacros(IFolder buildRoot,MakeRules myMakeRules) {
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

    private static StringBuffer getMakeRules(IProject project,IFolder topBuildDir,MakeRules myMakeRules,ICConfigurationDescription cfg) {
        StringBuffer buffer = new StringBuffer();
        buffer.append(NEWLINE);
        buffer.append(COMMENT_START).append(MakefileGenerator_comment_build_rule).append(NEWLINE);

        for (MakeRule makeRule : myMakeRules.getMakeRules()) {
        	if(makeRule.getSequenceGroupID()!=0) {
        		buffer.append(makeRule.getRule(project, topBuildDir, cfg));
        	}
        }
        return buffer;
    }

}