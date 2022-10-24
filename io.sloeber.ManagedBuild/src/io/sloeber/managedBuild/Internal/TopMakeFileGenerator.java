package io.sloeber.managedBuild.Internal;

import static io.sloeber.managedBuild.Internal.ManagebBuildCommon.*;
import static io.sloeber.managedBuild.Internal.ManagedBuildConstants.*;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.cdt.managedbuilder.core.IConfiguration;
import org.eclipse.cdt.managedbuilder.core.IInputType;
import org.eclipse.cdt.managedbuilder.core.IOutputType;
import org.eclipse.cdt.managedbuilder.core.ITool;
import org.eclipse.cdt.managedbuilder.core.ManagedBuildManager;
import org.eclipse.cdt.managedbuilder.macros.BuildMacroException;
import org.eclipse.cdt.managedbuilder.macros.IBuildMacroProvider;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import io.sloeber.managedBuild.api.INewManagedOutputNameProvider;

public class TopMakeFileGenerator {
	private ArduinoGnuMakefileGenerator caller = null;
	private Set<MakeRule> mySubDirMakeRules = new LinkedHashSet<>();
	private MakeRules myMakeRules = new MakeRules();
	private Collection<IContainer> myFoldersToBuild;
	private Map<IOutputType, Set<IFile>> myAllSourceTargets = new HashMap<>();
	private Set<String> myDependencyMacros = new HashSet<>();

	private IConfiguration getConfig() {
		return caller.getConfig();
	}

	private IProject getProject() {
		return caller.getProject();
	}

	private IPath getBuildFolder() {
		return caller.getBuildFolder();
	}

	TopMakeFileGenerator(ArduinoGnuMakefileGenerator theCaller, Set<MakeRule> subDirMakeRules,
			Collection<IContainer> foldersToBuild) {
		caller = theCaller;
		mySubDirMakeRules = subDirMakeRules;
		myFoldersToBuild = foldersToBuild;
		for (MakeRule curMakeRule : mySubDirMakeRules) {
			myAllSourceTargets.putAll(curMakeRule.getTargets());
			myDependencyMacros.addAll(curMakeRule.getDependecyMacros());
		}
		MakeRules makeRules = new MakeRules();
		Map<IOutputType, Set<IFile>> generatedFiles = new HashMap<>();
		for (MakeRule makeRule : subDirMakeRules) {
			Map<IOutputType, Set<IFile>> targets = makeRule.getTargets();
			for (Entry<IOutputType, Set<IFile>> curTarget : targets.entrySet()) {
				Set<IFile> esxistingTarget = generatedFiles.get(curTarget.getKey());
				if (esxistingTarget != null) {
					esxistingTarget.addAll(curTarget.getValue());
				} else {
					Set<IFile> copySet = new HashSet<>();
					copySet.addAll(curTarget.getValue());
					generatedFiles.put(curTarget.getKey(), copySet);
				}

			}
		}
		int depth = 10;
		while (depth > 0) {
			makeRules = getMakeRulesFromGeneratedFiles(generatedFiles);
			generatedFiles.clear();
			if (makeRules.size() > 0) {
				depth--;
				myMakeRules.addRules(makeRules);
				generatedFiles.putAll(makeRules.getTargets());
			} else {
				depth = 0;
			}
		}

	}

	public void generateMakefile() throws CoreException {
		IProject project = getProject();
		IConfiguration config = getConfig();

		StringBuffer buffer = new StringBuffer();
		buffer.append(addDefaultHeader());

		buffer.append(getMakeIncludeSubDirs());
		buffer.append(getMakeIncludeDependencies());
		buffer.append(getMakeRMCommand());
		buffer.append(getMakeTopTargets());// this is the include dependencies
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
		buffer.append(getMakeMacros());
		buffer.append(getMakeRules());
		buffer.append(getMakeFinalTargets("", ""));

		IFile fileHandle = project.getFile(config.getName() + '/' + MAKEFILE_NAME);
		save(buffer, fileHandle);
	}

	private StringBuffer getMakeIncludeSubDirs() {
		StringBuffer buffer = new StringBuffer();

		for (IContainer subDir : myFoldersToBuild) {
			String includeFile = subDir.getProjectRelativePath().append(MODFILE_NAME).toOSString();
			buffer.append("-include " + includeFile).append(NEWLINE);
		}
		buffer.append("-include sources.mk").append(NEWLINE);
		buffer.append("-include objects.mk").append(NEWLINE).append(NEWLINE);
		return buffer;
	}

	private StringBuffer getMakeRMCommand() {
		IConfiguration config = getConfig();
		StringBuffer buffer = new StringBuffer();
		buffer.append("-include " + ROOT + FILE_SEPARATOR + MAKEFILE_INIT).append(NEWLINE);
		buffer.append(NEWLINE);
		// Get the clean command from the build model
		buffer.append("RM := ");
		// support macros in the clean command
		try {
			String cleanCommand = ManagedBuildManager.getBuildMacroProvider().resolveValueToMakefileFormat(
					config.getCleanCommand(), EMPTY_STRING, WHITESPACE, IBuildMacroProvider.CONTEXT_CONFIGURATION,
					config);
			buffer.append(cleanCommand).append(NEWLINE);
			buffer.append(NEWLINE);
		} catch (BuildMacroException e) {
			// jaba is not going to write this code
		}

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

	private String getPreBuildStep() {
		IConfiguration config = getConfig();
		String prebuildStep = config.getPrebuildStep();
		// JABA issue927 adding recipe.hooks.sketch.prebuild.NUMBER.pattern as cdt
		// prebuild command if needed
		ICConfigurationDescription confDesc = ManagedBuildManager.getDescriptionForConfiguration(config);
		String sketchPrebuild = io.sloeber.core.common.Common.getBuildEnvironmentVariable(confDesc, "sloeber.prebuild",
				new String(), false);
		if (!sketchPrebuild.isEmpty()) {
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
		return prebuildStep.trim();
	}

	private StringBuffer getMakeIncludeDependencies() {

		// JABA add the arduino upload/program targets
		StringBuffer buffer = new StringBuffer();

		String defaultTarget = "all:";
		String prebuildStep = getPreBuildStep();
		if (prebuildStep.length() > 0) {
			// Add the comment for the "All" target
			buffer.append(COMMENT_START).append(MESSAGE_ALL_TARGET).append(NEWLINE);
			buffer.append(defaultTarget).append(NEWLINE);
			buffer.append(TAB).append(MAKE).append(WHITESPACE).append(NO_PRINT_DIR).append(WHITESPACE).append(PREBUILD)
					.append(NEWLINE);
			buffer.append(TAB).append(MAKE).append(WHITESPACE).append(NO_PRINT_DIR).append(WHITESPACE).append(MAINBUILD)
					.append(NEWLINE);
			buffer.append(NEWLINE);
			// defaultTarget = MAINBUILD.concat(COLON);
			buffer.append(COMMENT_SYMBOL).append(WHITESPACE).append(MESSAGE_MAINBUILD_TARGET).append(NEWLINE);

		} else {
			// Add the comment for the "All" target
			buffer.append(COMMENT_START).append(MESSAGE_ALL_TARGET).append(NEWLINE);
		}
		return buffer;
	}

	private StringBuffer getMakeTopTargets() {
		IConfiguration config = getConfig();
		IPath buildFolder = getBuildFolder();

		StringBuffer buffer = new StringBuffer();

		ITool targetTool = config.calculateTargetTool();
		buffer.append("all:").append(WHITESPACE);
		if (targetTool != null) {
			Set<IFile> allTargets = myMakeRules.getTargetsForTool(targetTool);
			for (IFile curTarget : allTargets) {
				String targetString = GetNiceFileName(buildFolder, curTarget.getLocation()).toOSString();
				buffer.append(ensurePathIsGNUMakeTargetRuleCompatibleSyntax(targetString));
				buffer.append(WHITESPACE);
			}
		}

		// Add the Secondary Outputs to the all target, if any
		IOutputType[] secondaryOutputs = config.getToolChain().getSecondaryOutputs();
		if (secondaryOutputs.length > 0) {
			buffer.append(WHITESPACE).append(SECONDARY_OUTPUTS);
		}
		buffer.append(NEWLINE).append(NEWLINE);

		String prebuildStep = getPreBuildStep();
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
		try {
			// try to resolve the build macros in the postbuild step
			postbuildStep = ManagedBuildManager.getBuildMacroProvider().resolveValueToMakefileFormat(postbuildStep,
					EMPTY_STRING, WHITESPACE, IBuildMacroProvider.CONTEXT_CONFIGURATION, config);
		} catch (BuildMacroException e) {
			/* JABA is not going to write this code */
		}
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

	private StringBuffer getMakeFinalTargets(String prebuildStep, String postbuildStep) {
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

	// Get the rules for the generated files
	private MakeRules getMakeRulesFromGeneratedFiles(Map<IOutputType, Set<IFile>> generatedFiles) {
		MakeRules makeRules = new MakeRules();
		IConfiguration config = getConfig();
		IProject project = getProject();
		IPath buildPath = getBuildFolder();

		// Visit the resources in this set
		for (Entry<IOutputType, Set<IFile>> entry : generatedFiles.entrySet()) {
			IOutputType outputTypeIn = entry.getKey();
			Set<IFile> files = entry.getValue();
			String macroName = outputTypeIn.getBuildVariable();
			for (ITool tool : config.getTools()) {
				for (IInputType inputType : tool.getInputTypes()) {
					if (!macroName.equals(inputType.getBuildVariable())) {
						continue;
					}
					if (inputType.getMultipleOfType()) {
						for (IOutputType outputType : tool.getOutputTypes()) {
							INewManagedOutputNameProvider nameProvider = getJABANameProvider(config, buildPath,
									outputType);
							if (nameProvider == null) {
								continue;
							}
							IPath outputFile = nameProvider.getOutputName(project, config, null, null);
							if (outputFile != null) {
								// This is a multiple to 1 based on var name
								IFile correctOutputFile = getProject()
										.getFile(new Path(config.getName()).append(outputFile));
								makeRules.addRule(tool, inputType, macroName, files, outputType, correctOutputFile);

								continue;
							}
							IPath firstFilePath = files.toArray(new IFile[files.size()])[0].getProjectRelativePath();
							outputFile = nameProvider.getOutputName(project, config, tool, firstFilePath);
							if (outputFile == null) {
								continue;
							}
							// This is a multiple to 1 not based on var name
							IPath correctOutputPath = new Path(config.getName()).append(outputFile);
							MakeRule newMakeRule = new MakeRule(caller, tool, inputType, files, outputType,
									project.getFile(correctOutputPath));

							makeRules.addRule(newMakeRule);
						}
					} else {
						// The link is based on the varname but the files are one on one
						for (IOutputType outputType : tool.getOutputTypes()) {
							INewManagedOutputNameProvider nameProvider = getJABANameProvider(config, buildPath,
									outputType);
							if (nameProvider == null) {
								continue;
							}
							for (IFile file : files) {
								IPath outputFile = nameProvider.getOutputName(project, config, tool,
										file.getProjectRelativePath());
								if (outputFile == null) {
									continue;
								}
								// This is a multiple to 1 not based on var name
								IPath correctOutputPath = new Path(config.getName()).append(outputFile);
								MakeRule newMakeRule = new MakeRule(tool, inputType, file, outputType,
										project.getFile(correctOutputPath));
								newMakeRule.addDependencies(caller);
								makeRules.addRule(newMakeRule);
							}
						}
					}
				}
				// else {
				// for (IOutputType outputType : tool.getOutputTypes()) {
				// IManagedOutputNameProviderJaba nameProvider =
				// getJABANameProvider(outputType);
				// if (nameProvider == null) {
				// continue;
				// }
				// for (IFile file : files) {
				// IPath outputFile = nameProvider.getOutputName(getProject(), config, tool,
				// file.getProjectRelativePath());
				// if (outputFile != null) {
				// //We found a tool that provides a outputfile for our source file
				// //TOFIX if this is a multiple to one we should only create one MakeRule
				// IPath correctOutputPath = new Path(config.getName()).append(outputFile);
				// MakeRule newMakeRule = new MakeRule(caller, tool, inputType, file,
				// outputType,
				// project.getFile(correctOutputPath));
				//
				// makeRules.add(newMakeRule);
				// }
				// }
				// }
				// }
				// }
			}
		}
		return makeRules;
	}

	private StringBuffer getMakeMacros() {
		StringBuffer buffer = new StringBuffer();
		IFile buildRoot = caller.getTopBuildDir();
		buffer.append(NEWLINE);
		buffer.append(COMMENT_START).append(MESSAGE_MOD_VARS).append(NEWLINE);

		for (String macroName : myMakeRules.getMacroNames()) {
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

	private StringBuffer getMakeRules() {
		StringBuffer buffer = new StringBuffer();
		IProject project = getProject();
		IConfiguration config = getConfig();
		IFile topBuildDir = caller.getTopBuildDir();
		buffer.append(NEWLINE);
		buffer.append(COMMENT_START).append(MESSAGE_MOD_RULES).append(NEWLINE);

		for (MakeRule makeRule : myMakeRules.getMakeRules()) {
			buffer.append(makeRule.getRule(project, topBuildDir, config));
		}
		return buffer;
	}

}
