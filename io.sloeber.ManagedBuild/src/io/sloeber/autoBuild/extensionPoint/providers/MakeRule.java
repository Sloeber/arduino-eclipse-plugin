package io.sloeber.autoBuild.extensionPoint.providers;

import static io.sloeber.autoBuild.core.Messages.*;
import static io.sloeber.autoBuild.extensionPoint.providers.AutoBuildCommon.*;
import static io.sloeber.autoBuild.integration.AutoBuildConstants.*;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;

import io.sloeber.autoBuild.api.BuildException;
import io.sloeber.autoBuild.integration.AutoBuildConfigurationData;
import io.sloeber.schema.api.IInputType;
import io.sloeber.schema.api.IOutputType;
import io.sloeber.schema.api.ITool;

public class MakeRule {

    private Map<IOutputType, Set<IFile>> myTargets = new LinkedHashMap<>(); //Macro file target map
    private Map<IInputType, Set<IFile>> myPrerequisites = new LinkedHashMap<>();//Macro file prerequisites map
    private Map<String, Set<IFile>> myDependencies = new LinkedHashMap<>(); //Macro file target map
    private ITool myTool = null;
    private int mySequenceGroupID = 0;

    public MakeRule(ITool tool, IInputType inputType, IFile inputFile, IOutputType outputType, IFile outFile,
            int sequenceID) {
        addPrerequisite(inputType, inputFile);
        addTarget(outputType, outFile);
        myTool = tool;
        mySequenceGroupID = sequenceID;
    }

    public MakeRule(ITool tool, IInputType inputType, Set<IFile> inputFiles, IOutputType outputType, IFile outFile,
            int sequenceID) {
        addPrerequisites(inputType, inputFiles);
        addTarget(outputType, outFile);
        myTool = tool;
        mySequenceGroupID = sequenceID;
    }

    public void addDependencies(MakefileGenerator caller) {
        myDependencies.clear();
        //FIXME need new way to know dependencies need to be added
        //
        //        for (Entry<IInputType, Set<IFile>> curprerequisite : myPrerequisites.entrySet()) {
        //            IInputType curInputType = curprerequisite.getKey();
        //            
        //                        IManagedDependencyGeneratorType t = curInputType.getDependencyGenerator();
        //                        if (t == null) {
        //                            continue;
        //                        }
        //            Set<IFile> files = curprerequisite.getValue();
        //            String depkey = curInputType.getBuildVariable() + DEPENDENCY_SUFFIX;
        //            for (IFile file : files) {
        //                IBuildObject buildContext = caller.getConfig().getResourceInfo(file.getFullPath(), false);
        //
        //                IManagedDependencyGenerator2 depGen = (IManagedDependencyGenerator2) t;
        //                IManagedDependencyInfo depInfo = depGen.getDependencySourceInfo(file.getProjectRelativePath(), file,
        //                        buildContext, myTool, caller.getBuildWorkingDir());
        //
        //                if (depInfo instanceof IManagedDependencyCalculator) {
        //                    IManagedDependencyCalculator depCalculator = (IManagedDependencyCalculator) depInfo;
        //                    IPath[] addlDeps = calculateDependenciesForSource(caller, depCalculator);
        //                    IPath[] addlTargets = depCalculator.getAdditionalTargets();
        //                    //TOFIX when is this call path used?
        //                }
        //                if (depInfo instanceof IManagedDependencyCommands) {
        //                    IManagedDependencyCommands tmp = (IManagedDependencyCommands) depInfo;
        //                    IPath[] addlTargets = tmp.getDependencyFiles();
        //                    Set<IFile> depFiles = new HashSet<>();
        //                    for (IPath curPath : addlTargets) {
        //                        depFiles.add(caller.getProject().getFile(caller.getBuildWorkingDir().append(curPath)));
        //                    }
        //                    myDependencies.put(depkey, depFiles);
        //                }
        //            }
        //        }
    }

    public Set<IFile> getPrerequisiteFiles() {
        HashSet<IFile> ret = new HashSet<>();
        for (Set<IFile> cur : myPrerequisites.values()) {
            ret.addAll(cur);
        }
        return ret;
    }

    public Map<IInputType, Set<IFile>> getPrerequisites() {
        return myPrerequisites;
    }

    public Set<IFile> getTargetFiles() {
        Set<IFile> ret = new HashSet<>();
        for (Set<IFile> cur : myTargets.values()) {
            ret.addAll(cur);
        }
        return ret;
    }

    public Set<IFile> getDependencyFiles() {
        Set<IFile> ret = new HashSet<>();
        for (Set<IFile> cur : myDependencies.values()) {
            ret.addAll(cur);
        }
        return ret;
    }

    public Map<IOutputType, Set<IFile>> getTargets() {
        return myTargets;
    }

    public Set<String> getAllMacros() {
        Set<String> ret = getTargetMacros();
        ret.addAll(getPrerequisiteMacros());
        ret.addAll(getDependecyMacros());
        return ret;
    }

    public Set<String> getTargetMacros() {
        HashSet<String> ret = new LinkedHashSet<>();
        for (IOutputType cur : myTargets.keySet()) {
            ret.add(cur.getBuildVariable());
        }
        return ret;
    }

    public Set<String> getPrerequisiteMacros() {
        HashSet<String> ret = new LinkedHashSet<>();
        for (IInputType cur : myPrerequisites.keySet()) {
            ret.add(cur.getBuildVariable());
        }
        ret.remove(EMPTY_STRING);
        return ret;
    }

    public Set<String> getDependecyMacros() {
        HashSet<String> ret = new LinkedHashSet<>();
        for (String cur : myDependencies.keySet()) {
            ret.add(cur);
        }
        return ret;
    }

    public HashSet<IFile> getMacroElements(String macroName) {
        HashSet<IFile> ret = new HashSet<>();

        for (Entry<IOutputType, Set<IFile>> cur : myTargets.entrySet()) {
            if (macroName.equals(cur.getKey().getBuildVariable())) {
                ret.addAll(cur.getValue());
            }
        }
        for (Entry<IInputType, Set<IFile>> cur : myPrerequisites.entrySet()) {
            if (macroName.equals(cur.getKey().getBuildVariable())) {
                ret.addAll(cur.getValue());
            }
        }
        Set<IFile> tmp = myDependencies.get(macroName);
        if (tmp != null) {
            ret.addAll(tmp);
        }
        return ret;
    }

    private void addTarget(IOutputType outputType, IFile file) {
        Set<IFile> files = myTargets.get(outputType);
        if (files == null) {
            files = new HashSet<>();
            files.add(file);
            myTargets.put(outputType, files);
        } else {
            files.add(file);
        }
    }

    private void addPrerequisite(IInputType inputType, IFile file) {
        Set<IFile> files = myPrerequisites.get(inputType);
        if (files == null) {
            files = new HashSet<>();
            files.add(file);
            myPrerequisites.put(inputType, files);
        } else {
            files.add(file);
        }
    }

    private String enumTargets(IFolder buildFolder) {
        String ret = new String();
        for (Set<IFile> curFiles : myTargets.values()) {
            for (IFile curFile : curFiles) {
                ret = ret + GetNiceFileName(buildFolder, curFile) + WHITESPACE;
            }
        }
        return ret;
    }

    private String enumPrerequisites(IFolder buildFolder) {
        String ret = new String();
        for (Set<IFile> curFiles : myPrerequisites.values()) {
            for (IFile curFile : curFiles) {
                ret = ret + GetNiceFileName(buildFolder, curFile) + WHITESPACE;
            }
        }
        return ret;
    }

    //FIXME JABA says: this code is weirdly crazy and way longer then I would expect. Should see why
    public StringBuffer getRule(IProject project, IFolder niceBuildFolder,
            AutoBuildConfigurationData autoBuildConfData) {

        Set<IFile> local_targets = getTargetFiles();
        Set<IFile> local_prerequisites = getPrerequisiteFiles();
        if (local_targets.size() != 1) {
            System.err.println("Only 1 target per build rule is supported in this managed build"); //$NON-NLS-1$
            return new StringBuffer();
        }
        if (local_prerequisites.size() == 0) {
            System.err.println("0 prerequisites is not supported in this managed build"); //$NON-NLS-1$
            return new StringBuffer();
        }
        IFile targetFile = local_targets.toArray(new IFile[1])[0];

        Set<String> niceNameList = new HashSet<>();
        Set<String> flags = new LinkedHashSet<>();
        for (IFile curPrerequisite : local_prerequisites) {
            niceNameList.add(GetNiceFileName(niceBuildFolder, curPrerequisite));
            try {
                flags.addAll(Arrays.asList(myTool.getToolCommandFlags(autoBuildConfData, curPrerequisite, targetFile)));
            } catch (BuildException e) {
                e.printStackTrace();
            }
        }

        String buildCmd = expandCommandLinePattern(autoBuildConfData, flags, myTool.getOutputFlag(), OUT_MACRO,
                niceNameList);

        // resolve any remaining macros in the command after it has been
        // generated
        String resolvedCommand = resolve(buildCmd, EMPTY_STRING, WHITESPACE, autoBuildConfData);
        if (resolvedCommand != null && !resolvedCommand.isBlank())
            buildCmd = resolvedCommand.trim();

        StringBuffer buffer = new StringBuffer();
        buffer.append(enumTargets(niceBuildFolder)).append(COLON).append(WHITESPACE);
        buffer.append(enumPrerequisites(niceBuildFolder)).append(NEWLINE);
        buffer.append(TAB).append(AT)
                .append(escapedEcho(MakefileGenerator_message_start_file + WHITESPACE + OUT_MACRO));
        buffer.append(TAB).append(AT).append(escapedEcho(myTool.getAnnouncement()));

        //        // JABA add sketch.prebuild and postbuild if needed
        //        //TOFIX this should not be here
        //        if ("sloeber.ino".equals(fileName)) { //$NON-NLS-1$
        //
        //            //            String sketchPrebuild = io.sloeber.core.common.Common.getBuildEnvironmentVariable(confDesc,
        //            //                    "sloeber.sketch.prebuild", new String(), true); //$NON-NLS-1$
        //            //            String sketchPostBuild = io.sloeber.core.common.Common.getBuildEnvironmentVariable(confDesc,
        //            //                    "sloeber.sketch.postbuild", new String(), true); //$NON-NLS-1$
        //            String sketchPrebuild = resolve("sloeber.sketch.prebuild", EMPTY_STRING, WHITESPACE, autoBuildConfData);
        //            String sketchPostBuild = resolve("sloeber.sketch.postbuild", EMPTY_STRING, WHITESPACE,autoBuildConfData);
        //            if (!sketchPrebuild.isEmpty()) {
        //                buffer.append(TAB).append(sketchPrebuild);
        //            }
        //            buffer.append(TAB).append(buildCmd).append(NEWLINE);
        //            if (!sketchPostBuild.isEmpty()) {
        //                buffer.append(TAB).append(sketchPostBuild);
        //            }
        //        } else {
        buffer.append(TAB).append(buildCmd);
        //        }
        //        // end JABA add sketch.prebuild and postbuild if needed

        buffer.append(NEWLINE);
        buffer.append(TAB).append(AT)
                .append(escapedEcho(MakefileGenerator_message_finish_file + WHITESPACE + OUT_MACRO));
        buffer.append(TAB).append(AT).append(ECHO_BLANK_LINE).append(NEWLINE);
        return buffer;
    }

    private Set<String> getBuildFlags(AutoBuildConfigurationData autoBuildConfData, IFile sourceFile,
            IFile outputFile) {
        Set<String> flags = new LinkedHashSet<>();
        // Get the tool command line options
        try {

            //IResourceInfo buildContext = config.getResourceInfo(sourceFile.getFullPath().removeLastSegments(1), false);
            flags.addAll(Arrays.asList(myTool.getToolCommandFlags(autoBuildConfData, sourceFile, outputFile)));

            myTool.getInputTypes();
        } catch (BuildException e) {
            e.printStackTrace();
        }
        return flags;
    }

    private String expandCommandLinePattern(AutoBuildConfigurationData autoBuildConfData, Set<String> flags,
            String outputFlag, String outputName, Set<String> inputResources) {
        String cmd = myTool.getToolCommand();
        // try to resolve the build macros in the tool command
        String resolvedCommand = resolve(cmd, EMPTY_STRING, WHITESPACE, autoBuildConfData);
        if (resolvedCommand != null && (resolvedCommand = resolvedCommand.trim()).length() > 0)
            cmd = resolvedCommand;
        return expandCommandLinePattern(cmd, flags, outputFlag, outputName, inputResources,
                getToolCommandLinePattern(autoBuildConfData, myTool));
    }

    private static String expandCommandLinePattern(String commandName, Set<String> flags, String outputFlag,
            String outputName, Set<String> inputResources, String commandLinePattern) {

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
        // command = command.replace(makeVariable(OUTPUT_PREFIX_PRM_NAME), myTool.getOutputPrefix());
        command = command.replace(makeVariable(OUTPUT_PRM_NAME), quotedOutputName);
        command = command.replace(makeVariable(INPUTS_PRM_NAME), inputsStr);

        return command;
    }

    public void addPrerequisites(IInputType inputType, Set<IFile> files) {
        Set<IFile> entrypoint = myPrerequisites.get(inputType);
        if (entrypoint != null) {
            entrypoint.addAll(files);
        } else {
            Set<IFile> copyOfFiles = new HashSet<>();
            copyOfFiles.addAll(files);
            myPrerequisites.put(inputType, copyOfFiles);
        }
    }

    /**
     * A simple rule is a rule that takes exactly 1 input type
     * and exactly 1 output type containing exactly 1 file
     * 
     * @return true if this rule is a simple rule
     *         otherwise false
     */

    public boolean isSimpleRule() {
        if ((myTargets.size() != 1) || (myTargets.size() != 1)) {
            return false;
        }
        //        int counter = 0;
        //        for (Set<IFile> files : myTargets.values()) {
        //            if ((++counter > 1) || (files.size() != 1)) {
        //                return false;
        //            }
        //        }
        //        counter = 0;
        //        for (Set<IFile> files : myPrerequisites.values()) {
        //            if ((++counter > 1)) {
        //                return false;
        //            }
        //        }
        return true;

    }

    public boolean isTool(ITool targetTool) {
        return myTool.getName().equals(targetTool.getName());
    }

    public int getSequenceGroupID() {
        return mySequenceGroupID;
    }

    public void setSequenceGroupID(int mySequenceGroupID) {
        this.mySequenceGroupID = mySequenceGroupID;
    }

    public boolean isForFolder(IFolder folder) {
        for (Set<IFile> files : myPrerequisites.values()) {
            for (IFile file : files) {
                if (file.getParent().equals(folder)) {
                    return true;
                }
            }
        }
        return false;
    }

}
//    /**
//     * Returns the dependency <code>IPath</code>s relative to the build directory
//     *
//     * @param depCalculator
//     *            the dependency calculator
//     * @return IPath[] that are relative to the build directory
//     */
//    private IPath[] calculateDependenciesForSource(ArduinoGnuMakefileGenerator caller,
//            IManagedDependencyCalculator depCalculator) {
//        IPath[] addlDeps = depCalculator.getDependencies();
//        if (addlDeps != null) {
//            for (int i = 0; i < addlDeps.length; i++) {
//                if (!addlDeps[i].isAbsolute()) {
//                    // Convert from project relative to build directory relative
//                    IPath absolutePath = caller.getProject().getLocation().append(addlDeps[i]);
//                    addlDeps[i] = ManagedBuildManager.calculateRelativePath(caller.getTopBuildDir().getLocation(),
//                            absolutePath);
//                }
//            }
//        }
//        return addlDeps;
//    }
