package io.sloeber.managedBuild.Internal;

import static io.sloeber.managedBuild.Internal.ManagebBuildCommon.*;
import static io.sloeber.managedBuild.Internal.ManagedBuildConstants.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.cdt.managedbuilder.core.BuildException;
import org.eclipse.cdt.managedbuilder.core.IBuildObject;
import org.eclipse.cdt.managedbuilder.core.IConfiguration;
import org.eclipse.cdt.managedbuilder.core.IInputType;
import org.eclipse.cdt.managedbuilder.core.IOutputType;
import org.eclipse.cdt.managedbuilder.core.IResourceInfo;
import org.eclipse.cdt.managedbuilder.core.ITool;
import org.eclipse.cdt.managedbuilder.core.ManagedBuildManager;
import org.eclipse.cdt.managedbuilder.internal.macros.BuildMacroProvider;
import org.eclipse.cdt.managedbuilder.internal.macros.FileContextData;
import org.eclipse.cdt.managedbuilder.macros.BuildMacroException;
import org.eclipse.cdt.managedbuilder.macros.IBuildMacroProvider;
import org.eclipse.cdt.managedbuilder.makegen.IManagedDependencyCalculator;
import org.eclipse.cdt.managedbuilder.makegen.IManagedDependencyCommands;
import org.eclipse.cdt.managedbuilder.makegen.IManagedDependencyGenerator2;
import org.eclipse.cdt.managedbuilder.makegen.IManagedDependencyGeneratorType;
import org.eclipse.cdt.managedbuilder.makegen.IManagedDependencyInfo;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;

public class MakeRule {

    private Map<IOutputType, List<IFile>> myTargets = new HashMap<>(); //Macro file target map
    private Map<IInputType, List<IFile>> myPrerequisites = new HashMap<>();//Macro file prerequisites map
    private Map<String, List<IFile>> myDependencies = new HashMap<>(); //Macro file target map
    private ITool myTool = null;

    //TOFIX get rid of caller argument
    public MakeRule(ArduinoGnuMakefileGenerator caller, ITool tool, IInputType inputType, IFile inputFile,
            IOutputType outputType, IFile outFile) {
        addPrerequisite(inputType, inputFile);
        addTarget(outputType, outFile);
        myTool = tool;
        calculateDependencies(caller);
    }

    private void calculateDependencies(ArduinoGnuMakefileGenerator caller) {
        myDependencies.clear();
        //TOFIX the stuff below should be calculated
        boolean toolGeneratesDependencyFiles = true;
        if (!toolGeneratesDependencyFiles) {
            return;
        }

        for (Entry<IInputType, List<IFile>> curprerequisite : myPrerequisites.entrySet()) {
            IInputType curInputType = curprerequisite.getKey();
            IManagedDependencyGeneratorType t = curInputType.getDependencyGenerator();
            if (t == null) {
                continue;
            }
            List<IFile> files = curprerequisite.getValue();
            String depkey = curInputType.getBuildVariable() + "_DEPS";
            for (IFile file : files) {
                IResourceInfo rcInfo = caller.getConfig().getResourceInfo(file.getFullPath(), false);
                int calcType = t.getCalculatorType();

                IManagedDependencyGenerator2 depGen = (IManagedDependencyGenerator2) t;
                IBuildObject buildContext = rcInfo;
                IManagedDependencyInfo depInfo = depGen.getDependencySourceInfo(file.getProjectRelativePath(), file,
                        buildContext, myTool, caller.getBuildWorkingDir());

                // if (calcType== IManagedDependencyGeneratorType.TYPE_CUSTOM) {
                if (depInfo instanceof IManagedDependencyCalculator) {
                    IManagedDependencyCalculator depCalculator = (IManagedDependencyCalculator) depInfo;
                    IPath[] addlDeps = calculateDependenciesForSource(caller, depCalculator);
                    IPath[] addlTargets = depCalculator.getAdditionalTargets();
                    //   }
                }
                if (depInfo instanceof IManagedDependencyCommands) {
                    IManagedDependencyCommands tmp = (IManagedDependencyCommands) depInfo;
                    IPath[] addlTargets = tmp.getDependencyFiles();
                    List<IFile> depFiles = new LinkedList<>();
                    for (IPath curPath : addlTargets) {
                        depFiles.add(caller.getProject().getFile(caller.getBuildWorkingDir().append(curPath)));
                    }
                    myDependencies.put(depkey, depFiles);
                }
            }
        }
    }

    /**
     * Returns the dependency <code>IPath</code>s relative to the build directory
     *
     * @param depCalculator
     *            the dependency calculator
     * @return IPath[] that are relative to the build directory
     */
    private IPath[] calculateDependenciesForSource(ArduinoGnuMakefileGenerator caller,
            IManagedDependencyCalculator depCalculator) {
        IPath[] addlDeps = depCalculator.getDependencies();
        if (addlDeps != null) {
            for (int i = 0; i < addlDeps.length; i++) {
                if (!addlDeps[i].isAbsolute()) {
                    // Convert from project relative to build directory relative
                    IPath absolutePath = caller.getProject().getLocation().append(addlDeps[i]);
                    addlDeps[i] = ManagedBuildManager.calculateRelativePath(caller.getTopBuildDir().getLocation(),
                            absolutePath);
                }
            }
        }
        return addlDeps;
    }

    public HashSet<IFile> getPrerequisites() {
        HashSet<IFile> ret = new HashSet<>();
        for (List<IFile> cur : myPrerequisites.values()) {
            ret.addAll(cur);
        }
        return ret;
    }

    public HashSet<IFile> getTargets() {
        HashSet<IFile> ret = new HashSet<>();
        for (List<IFile> cur : myTargets.values()) {
            ret.addAll(cur);
        }
        return ret;
    }

    public HashSet<String> getMacros() {
        HashSet<String> ret = new HashSet<>();
        for (IOutputType cur : myTargets.keySet()) {
            ret.add(cur.getBuildVariable());
        }
        for (IInputType cur : myPrerequisites.keySet()) {
            ret.add(cur.getBuildVariable());
        }
        for (String cur : myDependencies.keySet()) {
            ret.add(cur);
        }
        return ret;
    }

    public HashSet<IFile> getMacroElements(String macroName) {
        HashSet<IFile> ret = new HashSet<>();

        for (Entry<IOutputType, List<IFile>> cur : myTargets.entrySet()) {
            if (macroName.equals(cur.getKey().getBuildVariable())) {
                ret.addAll(cur.getValue());
            }
        }
        for (Entry<IInputType, List<IFile>> cur : myPrerequisites.entrySet()) {
            if (macroName.equals(cur.getKey().getBuildVariable())) {
                ret.addAll(cur.getValue());
            }
        }
        List<IFile> tmp = myDependencies.get(macroName);
        if (tmp != null) {
            ret.addAll(tmp);
        }
        return ret;
    }

    private void addTarget(IOutputType outputType, IFile file) {
        List<IFile> files = myTargets.get(outputType);
        if (files == null) {
            files = new LinkedList<>();
            files.add(file);
            myTargets.put(outputType, files);
        } else {
            files.add(file);
        }
    }

    private void addPrerequisite(IInputType inputType, IFile file) {
        List<IFile> files = myPrerequisites.get(inputType);
        if (files == null) {
            files = new LinkedList<>();
            files.add(file);
            myPrerequisites.put(inputType, files);
        } else {
            files.add(file);
        }
    }

    private String enumTargets(IFile buildFolder) {
        String ret = new String();
        for (List<IFile> curFiles : myTargets.values()) {
            for (IFile curFile : curFiles) {
                ret = ret + GetNiceFileName(buildFolder, curFile) + WHITESPACE;
            }
        }
        return ret;
    }

    private String enumPrerequisites(IFile buildFolder) {
        String ret = new String();
        for (List<IFile> curFiles : myPrerequisites.values()) {
            for (IFile curFile : curFiles) {
                ret = ret + GetNiceFileName(buildFolder, curFile) + WHITESPACE;
            }
        }
        return ret;
    }

    public StringBuffer getRule(IProject project, IFile niceBuildFolder, IConfiguration config) {

        String cmd = myTool.getToolCommand();
        //For now assume 1 target with 1 or more prerequisites
        // if there is more than 1 prerequisite we take the flags of the first prerequisite only
        HashSet<IFile> local_targets = getTargets();
        HashSet<IFile> local_prerequisites = getPrerequisites();
        if (local_targets.size() != 1) {
            System.err.println("Only 1 target per build rule is supported in this managed build"); //$NON-NLS-1$
            return new StringBuffer();
        }
        if (local_prerequisites.size() == 0) {
            System.err.println("0 prerequisites is not supported in this managed build"); //$NON-NLS-1$
            return new StringBuffer();
        }
        IFile outputLocation = local_targets.toArray(new IFile[0])[0];

        //Primary outputs is not supported
        String otherPrimaryOutputs = EMPTY_STRING;
        Set<String> niceNameList = new HashSet<>();
        IFile sourceLocation = null;
        for (IFile curPrerequisite : local_prerequisites) {
            niceNameList.add(GetNiceFileName(niceBuildFolder, curPrerequisite));
            sourceLocation = curPrerequisite;
        }
        final String fileName = sourceLocation.getFullPath().removeFileExtension().lastSegment();
        final String inputExtension = sourceLocation.getFileExtension();

        Set<String> flags = getBuildFlags(niceBuildFolder, config, sourceLocation, outputLocation);

        boolean needExplicitRuleForFile = false;
        boolean needExplicitDependencyCommands = false;
        boolean resourceNameRequiresExplicitRule = containsSpecialCharacters(sourceLocation.getLocation().toOSString());
        needExplicitRuleForFile = resourceNameRequiresExplicitRule
                || BuildMacroProvider.getReferencedExplitFileMacros(myTool).length > 0
                || BuildMacroProvider.getReferencedExplitFileMacros(cmd, IBuildMacroProvider.CONTEXT_FILE,
                        new FileContextData(sourceLocation.getFullPath(), outputLocation.getFullPath(), null,
                                myTool)).length > 0;

        String outflag = myTool.getOutputFlag();
        String buildCmd = cmd + WHITESPACE + flags.toString().trim() + WHITESPACE + outflag + WHITESPACE
                + myTool.getOutputPrefix() + OUT_MACRO + otherPrimaryOutputs + WHITESPACE + IN_MACRO;
        if (needExplicitRuleForFile || needExplicitDependencyCommands) {
            buildCmd = expandCommandLinePattern(cmd, flags, outflag, OUT_MACRO + otherPrimaryOutputs, niceNameList,
                    getToolCommandLinePattern(config, myTool));
        } else {
            buildCmd = expandCommandLinePattern(config, inputExtension, flags, outflag, OUT_MACRO + otherPrimaryOutputs,
                    niceNameList, sourceLocation, outputLocation);
        }
        // resolve any remaining macros in the command after it has been
        // generated
        try {
            String resolvedCommand;
            IBuildMacroProvider provider = ManagedBuildManager.getBuildMacroProvider();
            if (!needExplicitRuleForFile) {
                resolvedCommand = provider.resolveValueToMakefileFormat(buildCmd, EMPTY_STRING, WHITESPACE,
                        IBuildMacroProvider.CONTEXT_FILE,
                        new FileContextData(sourceLocation.getFullPath(), outputLocation.getFullPath(), null, myTool));
            } else {
                // if we need an explicit rule then don't use any builder
                // variables, resolve everything to explicit strings
                resolvedCommand = provider.resolveValue(buildCmd, EMPTY_STRING, WHITESPACE,
                        IBuildMacroProvider.CONTEXT_FILE,
                        new FileContextData(sourceLocation.getFullPath(), outputLocation.getFullPath(), null, myTool));
            }
            if (!resolvedCommand.isBlank())
                buildCmd = resolvedCommand.trim();
        } catch (BuildMacroException e) {
            /* JABA is not going to write this code */
        }

        StringBuffer buffer = new StringBuffer();
        buffer.append(enumTargets(niceBuildFolder)).append(COLON).append(WHITESPACE);
        buffer.append(enumPrerequisites(niceBuildFolder)).append(NEWLINE);
        buffer.append(TAB).append(AT).append(escapedEcho(MESSAGE_START_FILE + WHITESPACE + IN_MACRO));
        buffer.append(TAB).append(AT).append(escapedEcho(myTool.getAnnouncement()));

        // JABA add sketch.prebuild and postbouild if needed
        //TOFIX this should not be here
        if ("sloeber.ino".equals(fileName)) { //$NON-NLS-1$
            ICConfigurationDescription confDesc = ManagedBuildManager.getDescriptionForConfiguration(config);
            String sketchPrebuild = io.sloeber.core.common.Common.getBuildEnvironmentVariable(confDesc,
                    "sloeber.sketch.prebuild", new String(), true); //$NON-NLS-1$
            String sketchPostBuild = io.sloeber.core.common.Common.getBuildEnvironmentVariable(confDesc,
                    "sloeber.sketch.postbuild", new String(), true); //$NON-NLS-1$
            if (!sketchPrebuild.isEmpty()) {
                buffer.append(TAB).append(sketchPrebuild);
            }
            buffer.append(TAB).append(buildCmd).append(NEWLINE);
            if (!sketchPostBuild.isEmpty()) {
                buffer.append(TAB).append(sketchPostBuild);
            }
        } else {
            buffer.append(TAB).append(buildCmd);
        }
        // end JABA add sketch.prebuild and postbouild if needed

        buffer.append(NEWLINE);
        buffer.append(TAB).append(AT).append(escapedEcho(MESSAGE_FINISH_FILE + WHITESPACE + IN_MACRO));
        buffer.append(TAB).append(AT).append(ECHO_BLANK_LINE).append(NEWLINE);
        return buffer;
    }

    private Set<String> getBuildFlags(IFile buildFolder, IConfiguration config, IFile sourceFile, IFile outputFile) {
        Set<String> flags = new HashSet<>();
        // Get the tool command line options
        try {

            IResourceInfo buildContext = config.getResourceInfo(sourceFile.getFullPath().removeLastSegments(1), false);
            flags.addAll(Arrays.asList(myTool.getToolCommandFlags(sourceFile.getLocation(), outputFile.getLocation())));

            IInputType[] inputTypes = myTool.getInputTypes(); //.getDependencyGeneratorForExtension(inputExtension);
            for (IInputType inputType : inputTypes) {
                IManagedDependencyGeneratorType t = inputType.getDependencyGenerator();
                if (t != null) {
                    if (t.getCalculatorType() == IManagedDependencyGeneratorType.TYPE_BUILD_COMMANDS) {
                        IManagedDependencyGenerator2 depGen = (IManagedDependencyGenerator2) t;
                        IManagedDependencyInfo depInfo = depGen.getDependencySourceInfo(
                                sourceFile.getProjectRelativePath(), sourceFile, buildContext, myTool,
                                buildFolder.getFullPath());
                        IManagedDependencyCommands depCommands = (IManagedDependencyCommands) depInfo;
                        if (depCommands != null) {
                            flags.addAll(Arrays.asList(depCommands.getDependencyCommandOptions()));
                        }

                    }
                }
            }
        } catch (BuildException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return flags;
    }

    private String expandCommandLinePattern(IConfiguration config, String sourceExtension, Set<String> flags,
            String outputFlag, String outputName, Set<String> inputResources, IFile inputLocation,
            IFile outputLocation) {
        String cmd = myTool.getToolCommand();
        // try to resolve the build macros in the tool command
        try {
            String resolvedCommand = null;
            if ((inputLocation != null && inputLocation.toString().indexOf(WHITESPACE) != -1)
                    || (outputLocation != null && outputLocation.toString().indexOf(WHITESPACE) != -1)) {
                resolvedCommand = ManagedBuildManager.getBuildMacroProvider().resolveValue(cmd, EMPTY_STRING,
                        WHITESPACE, IBuildMacroProvider.CONTEXT_FILE,
                        new FileContextData(inputLocation.getFullPath(), outputLocation.getFullPath(), null, myTool));
            } else {
                resolvedCommand = ManagedBuildManager.getBuildMacroProvider().resolveValueToMakefileFormat(cmd,
                        EMPTY_STRING, WHITESPACE, IBuildMacroProvider.CONTEXT_FILE,
                        new FileContextData(inputLocation.getFullPath(), outputLocation.getFullPath(), null, myTool));
            }
            if ((resolvedCommand = resolvedCommand.trim()).length() > 0)
                cmd = resolvedCommand;
        } catch (BuildMacroException e) {
            /* JABA is not going to write this code */
        }
        return expandCommandLinePattern(cmd, flags, outputFlag, outputName, inputResources,
                getToolCommandLinePattern(config, myTool));
    }

    //    /**
    //     * Returns any additional resources specified for the tool in other InputType
    //     * elements and AdditionalInput elements
    //     */
    //    private IPath[] getAdditionalResourcesForSource(ITool tool) {
    //        IProject project = getProject();
    //        List<IPath> allRes = new ArrayList<>();
    //        IInputType[] types = tool.getInputTypes();
    //        for (IInputType type : types) {
    //            // Additional resources come from 2 places.
    //            // 1. From AdditionalInput childen
    //            IPath[] res = type.getAdditionalResources();
    //            for (IPath re : res) {
    //                allRes.add(re);
    //            }
    //            // 2. From InputTypes that other than the primary input type
    //            if (!type.getPrimaryInput() && type != tool.getPrimaryInputType()) {
    //                String var = type.getBuildVariable();
    //                if (var != null && var.length() > 0) {
    //                    allRes.add(Path.fromOSString("$(" + type.getBuildVariable() + ")"));
    //                } else {
    //                    // Use file extensions
    //                    String[] typeExts = type.getSourceExtensions(tool);
    //                    for (IResource projectResource : caller.projectResources) {
    //                        if (projectResource.getType() == IResource.FILE) {
    //                            String fileExt = projectResource.getFileExtension();
    //                            if (fileExt == null) {
    //                                fileExt = "";
    //                            }
    //                            for (String typeExt : typeExts) {
    //                                if (fileExt.equals(typeExt)) {
    //                                    allRes.add(projectResource.getProjectRelativePath());
    //                                    break;
    //                                }
    //                            }
    //                        }
    //                    }
    //                }
    //                // If an assignToOption has been specified, set the value of the
    //                // option to the inputs
    //                IOption assignToOption = tool.getOptionBySuperClassId(type.getAssignToOptionId());
    //                IOption option = tool.getOptionBySuperClassId(type.getOptionId());
    //                if (assignToOption != null && option == null) {
    //                    try {
    //                        int optType = assignToOption.getValueType();
    //                        IResourceInfo rcInfo = tool.getParentResourceInfo();
    //                        if (rcInfo != null) {
    //                            if (optType == IOption.STRING) {
    //                                String optVal = "";
    //                                for (int j = 0; j < allRes.size(); j++) {
    //                                    if (j != 0) {
    //                                        optVal += " ";
    //                                    }
    //                                    String resPath = allRes.get(j).toString();
    //                                    if (!resPath.startsWith("$(")) {
    //                                        IResource addlResource = project.getFile(resPath);
    //                                        if (addlResource != null) {
    //                                            IPath addlPath = addlResource.getLocation();
    //                                            if (addlPath != null) {
    //                                                resPath = ManagedBuildManager
    //                                                        .calculateRelativePath(getTopBuildDir(), addlPath).toString();
    //                                            }
    //                                        }
    //                                    }
    //                                    optVal += ManagedBuildManager
    //                                            .calculateRelativePath(getTopBuildDir(), Path.fromOSString(resPath))
    //                                            .toString();
    //                                }
    //                                ManagedBuildManager.setOption(rcInfo, tool, assignToOption, optVal);
    //                            } else if (optType == IOption.STRING_LIST || optType == IOption.LIBRARIES
    //                                    || optType == IOption.OBJECTS || optType == IOption.INCLUDE_FILES
    //                                    || optType == IOption.LIBRARY_PATHS || optType == IOption.LIBRARY_FILES
    //                                    || optType == IOption.MACRO_FILES) {
    //                                // TODO: do we need to do anything with undefs
    //                                // here?
    //                                // Note that the path(s) must be translated from
    //                                // project relative
    //                                // to top build directory relative
    //                                String[] paths = new String[allRes.size()];
    //                                for (int j = 0; j < allRes.size(); j++) {
    //                                    paths[j] = allRes.get(j).toString();
    //                                    if (!paths[j].startsWith("$(")) {
    //                                        IResource addlResource = project.getFile(paths[j]);
    //                                        if (addlResource != null) {
    //                                            IPath addlPath = addlResource.getLocation();
    //                                            if (addlPath != null) {
    //                                                paths[j] = ManagedBuildManager
    //                                                        .calculateRelativePath(getTopBuildDir(), addlPath).toString();
    //                                            }
    //                                        }
    //                                    }
    //                                }
    //                                ManagedBuildManager.setOption(rcInfo, tool, assignToOption, paths);
    //                            } else if (optType == IOption.BOOLEAN) {
    //                                boolean b = false;
    //                                if (allRes.size() > 0)
    //                                    b = true;
    //                                ManagedBuildManager.setOption(rcInfo, tool, assignToOption, b);
    //                            } else if (optType == IOption.ENUMERATED || optType == IOption.TREE) {
    //                                if (allRes.size() > 0) {
    //                                    String s = allRes.get(0).toString();
    //                                    ManagedBuildManager.setOption(rcInfo, tool, assignToOption, s);
    //                                }
    //                            }
    //                            allRes.clear();
    //                        }
    //                    } catch (BuildException ex) {
    //                        /* JABA is not going to write this code */
    //                    }
    //                }
    //            }
    //        }
    //        return allRes.toArray(new IPath[allRes.size()]);
    //    }

    private String expandCommandLinePattern(String commandName, Set<String> flags, String outputFlag, String outputName,
            Set<String> inputResources, String commandLinePattern) {

        String command = commandLinePattern;
        if (commandLinePattern == null || commandLinePattern.length() <= 0) {
            command = DEFAULT_PATTERN;
        }

        String quotedOutputName = outputName;
        // if the output name isn't a variable then quote it
        if (quotedOutputName.length() > 0 && quotedOutputName.indexOf("$(") != 0) { //$NON-NLS-1$
            quotedOutputName = DOUBLE_QUOTE + quotedOutputName + DOUBLE_QUOTE;
        }

        String inputsStr = ""; //$NON-NLS-1$
        if (inputResources != null) {
            for (String inp : inputResources) {
                if (inp != null && !inp.isEmpty()) {
                    // if the input resource isn't a variable then quote it
                    if (inp.indexOf("$(") != 0) { //$NON-NLS-1$
                        inp = DOUBLE_QUOTE + inp + DOUBLE_QUOTE;
                    }
                    inputsStr = inputsStr + inp + WHITESPACE;
                }
            }
            inputsStr = inputsStr.trim();
        }

        String flagsStr = String.join(WHITESPACE, flags);

        command = command.replace(makeVariable(CMD_LINE_PRM_NAME), commandName);
        command = command.replace(makeVariable(FLAGS_PRM_NAME), flagsStr);
        command = command.replace(makeVariable(OUTPUT_FLAG_PRM_NAME), outputFlag);
        command = command.replace(makeVariable(OUTPUT_PREFIX_PRM_NAME), myTool.getOutputPrefix());
        command = command.replace(makeVariable(OUTPUT_PRM_NAME), quotedOutputName);
        command = command.replace(makeVariable(INPUTS_PRM_NAME), inputsStr);

        return command;
    }

}
