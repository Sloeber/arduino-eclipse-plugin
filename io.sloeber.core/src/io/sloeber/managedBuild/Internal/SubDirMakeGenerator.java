package io.sloeber.managedBuild.Internal;

import static io.sloeber.managedBuild.Internal.ManagebBuildCommon.*;
//import static io.sloeber.managedBuild.Internal.ManagebBuildCommon.*;
import static io.sloeber.managedBuild.Internal.ManagedBuildConstants.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;

import org.eclipse.cdt.managedbuilder.core.BuildException;
import org.eclipse.cdt.managedbuilder.core.IConfiguration;
import org.eclipse.cdt.managedbuilder.core.IFileInfo;
import org.eclipse.cdt.managedbuilder.core.IInputType;
import org.eclipse.cdt.managedbuilder.core.IManagedCommandLineGenerator;
import org.eclipse.cdt.managedbuilder.core.IManagedCommandLineInfo;
import org.eclipse.cdt.managedbuilder.core.IOption;
import org.eclipse.cdt.managedbuilder.core.IOutputType;
import org.eclipse.cdt.managedbuilder.core.IResourceInfo;
import org.eclipse.cdt.managedbuilder.core.ITool;
import org.eclipse.cdt.managedbuilder.core.ManagedBuildManager;
import org.eclipse.cdt.managedbuilder.internal.core.ManagedMakeMessages;
import org.eclipse.cdt.managedbuilder.internal.core.Tool;
import org.eclipse.cdt.managedbuilder.internal.macros.FileContextData;
import org.eclipse.cdt.managedbuilder.macros.BuildMacroException;
import org.eclipse.cdt.managedbuilder.macros.IBuildMacroProvider;
import org.eclipse.cdt.managedbuilder.makegen.IManagedBuilderMakefileGenerator;
import org.eclipse.cdt.managedbuilder.makegen.IManagedDependencyCommands;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import io.sloeber.managedBuild.api.IManagedOutputNameProviderJaba;

public class SubDirMakeGenerator {
    private List<String> depLineList = new LinkedList<>();
    private ArduinoGnuMakefileGenerator caller;

    SubDirMakeGenerator(ArduinoGnuMakefileGenerator theCaller) {
        caller = theCaller;
    }

    public List<String> getDepLineList() {
        return depLineList;
    }

    private IPath getBuildWorkingDir() {
        return caller.getBuildWorkingDir();
    }

    private List<IPath> getDependencyMakefiles(ToolInfoHolder h) {
        return caller.getDependencyMakefiles(h);
    }

    private IFile getTopBuildDir() {
        return caller.getTopBuildDir();
    }

    private Vector<String> getRuleList() {
        return caller.getRuleList();
    }

    private IConfiguration getConfig() {
        return caller.getConfig();
    }

    private IProject getProject() {
        return caller.getProject();
    }

    private Vector<String> getDepRuleList() {
        return caller.getDepRuleList();
    }

    /*************************************************************************
     * M A K E F I L E S P O P U L A T I O N M E T H O D S
     ************************************************************************/
    /**
     * This method generates a "fragment" make file (subdir.mk). One of these is
     * generated for each project directory/subdirectory that contains source files.
     */
    public void populateFragmentMakefile(IContainer module) throws CoreException {
        //create the parent folder on disk and file in eclispe
        IProject project = getProject();
        IPath buildRoot = getBuildWorkingDir();
        if (buildRoot == null) {
            return;
        }
        IPath moduleOutputPath = buildRoot.append(module.getProjectRelativePath());
        caller.updateMonitor(ManagedMakeMessages.getFormattedString("MakefileGenerator.message.gen.source.makefile",
                moduleOutputPath.toString()));
        IPath moduleOutputDir = createDirectory(project, moduleOutputPath.toString());
        IFile modMakefile = createFile(moduleOutputDir.append(MODFILE_NAME));

        //get the data
        List<MakeRule> makeRules = getMakeRules(module);

        //generate the file content
        StringBuffer makeBuf = addDefaultHeader();
        makeBuf.append(GenerateMacros(makeRules));
        makeBuf.append(GenerateRules(makeRules, getConfig()));

        // Save the files
        save(makeBuf, modMakefile);
    }

    private StringBuffer GenerateMacros(List<MakeRule> makeRules) {
        StringBuffer buffer = new StringBuffer();
        IFile buildRoot = getTopBuildDir();
        buffer.append(NEWLINE);
        HashSet<String> macroNames = new HashSet<>();
        for (MakeRule makeRule : makeRules) {
            macroNames.addAll(makeRule.getMacros());
        }
        for (String macroName : macroNames) {
            HashSet<IFile> files = new HashSet<>();
            for (MakeRule makeRule : makeRules) {
                files.addAll(makeRule.getMacroElements(macroName));
            }
            if (files.size() > 0) {
                buffer.append(macroName).append(JAVA_ADDITION).append(WHITESPACE);
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

    private StringBuffer GenerateRules(List<MakeRule> makeRules, IConfiguration config) {
        StringBuffer buffer = new StringBuffer();
        buffer.append(NEWLINE);

        for (MakeRule makeRule : makeRules) {
            buffer.append(makeRule.getRule(getProject(), getTopBuildDir(), config));
        }

        return buffer;
    }

    //Get the rules for the source files
    private List<MakeRule> getMakeRules(IContainer module) {
        IConfiguration config = getConfig();
        IFile buildPath = getTopBuildDir();
        IProject project = getProject();
        List<MakeRule> makeRules = new LinkedList<>();
        // Visit the resources in this folder 
        try {
            for (IResource resource : module.members()) {
                if (resource.getType() != IResource.FILE) {
                    //only handle files
                    continue;
                }
                IFile inputFile = (IFile) resource;
                IPath rcProjRelPath = resource.getProjectRelativePath();
                if (!caller.isSource(rcProjRelPath)) {
                    // this resource is excluded from build
                    continue;
                }
                IResourceInfo rcInfo = config.getResourceInfo(rcProjRelPath, false);
                String ext = rcProjRelPath.getFileExtension();

                ITool tool = null;
                //try to find tool 
                if (rcInfo instanceof IFileInfo) {
                    IFileInfo fi = (IFileInfo) rcInfo;
                    ITool[] tools = fi.getToolsToInvoke();
                    if (tools != null && tools.length > 0) {
                        tool = tools[0];
                    }
                }

                //No tool found yet try other way
                if (tool == null) {
                    ToolInfoHolder h = ToolInfoHolder.getToolInfo(caller, rcInfo.getPath());
                    ITool buildTools[] = h.buildTools;
                    h = ToolInfoHolder.getToolInfo(caller, Path.EMPTY);
                    buildTools = h.buildTools;
                    for (ITool buildTool : buildTools) {
                        if (buildTool.buildsFileType(ext)) {
                            tool = buildTool;
                            break;
                        }
                    }
                }

                //We found a tool get the other info
                //TOFIX we should simply loop over all available tools
                if (tool != null) {

                    // Generate the rule to build this source file
                    //TOFIX check wether this tool can handle this file
                    IInputType inputType = tool.getPrimaryInputType();
                    if (inputType == null) {
                        inputType = tool.getInputType(ext);
                    }

                    for (IOutputType outputType : tool.getOutputTypes()) {
                        IManagedOutputNameProviderJaba nameProvider = getJABANameProvider(outputType);
                        if (nameProvider != null) {
                            IPath outputFile = nameProvider.getOutputName(getProject(), config, tool,
                                    resource.getFullPath());
                            if (outputFile != null) {
                                //We found a tool that provides a outputfile for our source file
                                //TOFIX if this is a multiple to one we should only create one MakeRule
                                IPath correctOutputPath = new Path(config.getName())
                                        .append(outputFile.removeFirstSegments(1));
                                MakeRule newMakeRule = new MakeRule();
                                newMakeRule.addPrerequisite(inputType, inputFile);
                                newMakeRule.addTarget(outputType, project.getFile(correctOutputPath));
                                newMakeRule.tool = tool;
                                makeRules.add(newMakeRule);

                            }

                        }
                    }

                }
            }
        } catch (CoreException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return makeRules;
    }

    /**
     * Write all macro addition entries in a map to the buffer
     */
    static private StringBuffer writeAdditionMacros(LinkedHashMap<String, String> map) {
        StringBuffer buffer = new StringBuffer();
        // Add the comment
        buffer.append(COMMENT_SYMBOL).append(WHITESPACE).append(ManagedMakeMessages.getResourceString(MOD_VARS))
                .append(NEWLINE);
        for (String macroString : map.values()) {
            // Check if we added any files to the rule
            // Currently, we do this by comparing the end of the rule buffer to
            // MACRO_ADDITION_PREFIX_SUFFIX
            if (!(macroString.endsWith(MACRO_ADDITION_PREFIX_SUFFIX))) {
                StringBuffer currentBuffer = new StringBuffer();
                // Remove the final "/"
                if (macroString.endsWith(LINEBREAK)) {
                    macroString = macroString.substring(0, (macroString.length() - 2)) + NEWLINE;
                }
                currentBuffer.append(macroString);
                currentBuffer.append(NEWLINE);
                // append the contents of the buffer to the master buffer for
                // the whole file
                buffer.append(currentBuffer);
            }
        }
        return buffer.append(NEWLINE);
    }

    static private boolean isSecondaryOutputVar(ToolInfoHolder h, IOutputType[] secondaryOutputs, String varName) {
        ITool[] buildTools = h.buildTools;
        for (ITool buildTool : buildTools) {
            // Add the specified output build variables
            IOutputType[] outTypes = buildTool.getOutputTypes();
            if (outTypes != null && outTypes.length > 0) {
                for (IOutputType outType : outTypes) {
                    // Is this one of the secondary outputs?
                    // Look for an outputType with this ID, or one with a
                    // superclass with this id
                    for (IOutputType secondaryOutput : secondaryOutputs) {
                        IOutputType matchType = outType;
                        do {
                            if (matchType.getId().equals(secondaryOutput.getId())) {
                                if (outType.getBuildVariable().equals(varName)) {
                                    return true;
                                }
                            }
                            matchType = matchType.getSuperClass();
                        } while (matchType != null);
                    }
                }
            }
        }
        return false;
    }

    /**
     * Adds the source file to the appropriate build variable
     *
     * @param buildVarToRuleStringMap
     *            map of build variable names to the list of files assigned to the
     *            variable
     * @param ext
     *            the file extension of the file
     * @param varName
     *            the build variable to add this invocation's outputs to if
     *            <code>null</code>, use the file extension to find the name
     * @param relativePath
     *            build output directory relative path of the current output
     *            directory
     * @param sourceLocation
     *            the full path of the source
     * @param generatedSource
     *            if <code>true</code>, this file was generated by another tool in
     *            the tool-chain
     */
    private void addToBuildVar(LinkedHashMap<String, String> buildVarToRuleStringMap, String ext, String varName,
            String relativePath, IPath sourceLocation, boolean generatedSource) {
        List<IPath> varList = null;
        if (varName == null) {
            // Get the proper source build variable based upon the extension
            varName = getSourceMacroName(ext).toString();
            varList = caller.buildSrcVars.get(varName);
        } else {
            varList = caller.buildOutVars.get(varName);
        }
        // Add the resource to the list of all resources associated with a
        // variable.
        // Do not allow duplicates - there is no reason to and it can be 'bad' -
        // e.g., having the same object in the OBJS list can cause duplicate
        // symbol errors from the linker
        if ((varList != null) && !(varList.contains(sourceLocation))) {
            // Since we don't know how these files will be used, we store them
            // using a "location"
            // path rather than a relative path
            varList.add(sourceLocation);
            if (!buildVarToRuleStringMap.containsKey(varName)) {
                // TODO - is this an error?
            } else {
                // Add the resource name to the makefile line that adds
                // resources to the build variable
                addMacroAdditionFile(caller, buildVarToRuleStringMap, varName, relativePath, sourceLocation,
                        generatedSource);
            }
        }
    }

    /**
     * Returns the output <code>IPath</code>s for this invocation of the tool with
     * the specified source file The priorities for determining the names of the
     * outputs of a tool are: 1. If the tool is the build target and primary output,
     * use artifact name & extension - This case does not apply here... 2. If an
     * option is specified, use the value of the option 3. If a nameProvider is
     * specified, call it 4. If outputNames is specified, use it 5. Use the name
     * pattern to generate a transformation macro so that the source names can be
     * transformed into the target names using the built-in string substitution
     * functions of <code>make</code>.
     *
     * @param relativePath
     *            build output directory relative path of the current output
     *            directory
     * @param enumeratedOutputs
     *            Vector of IPaths of outputs that are relative to the build
     *            directory
     */
    private void calculateOutputsForSource(ITool tool, String relativePath, IResource resource, IPath sourceLocation,
            List<IPath> enumeratedOutputs) {
        IProject project = getProject();
        IConfiguration config = getConfig();
        String inExt = sourceLocation.getFileExtension();
        String outExt = tool.getOutputExtension(inExt);
        // IResourceInfo rcInfo = tool.getParentResourceInfo();
        IOutputType[] outTypes = tool.getOutputTypes();
        if (outTypes != null && outTypes.length > 0) {
            for (IOutputType type : outTypes) {
                String outputPrefix = type.getOutputPrefix();

                try {
                    if (containsSpecialCharacters(sourceLocation.toOSString())) {
                        outputPrefix = ManagedBuildManager.getBuildMacroProvider().resolveValue(outputPrefix, "", " ",
                                IBuildMacroProvider.CONTEXT_CONFIGURATION, config);
                    } else {
                        outputPrefix = ManagedBuildManager.getBuildMacroProvider().resolveValueToMakefileFormat(
                                outputPrefix, "", " ", IBuildMacroProvider.CONTEXT_CONFIGURATION, config);
                    }
                } catch (BuildMacroException e) {
                    /* JABA is not going to write this code */
                }
                // }
                boolean multOfType = type.getMultipleOfType();
                IOption option = tool.getOptionBySuperClassId(type.getOptionId());
                IManagedOutputNameProviderJaba nameProvider = getJABANameProvider(type);
                String[] outputNames = type.getOutputNames();
                // 1. If the tool is the build target and this is the primary
                // output,
                // use artifact name & extension
                // Not appropriate here...
                // 2. If an option is specified, use the value of the option
                if (option != null) {
                    try {
                        List<String> outputList = new ArrayList<String>();
                        int optType = option.getValueType();
                        if (optType == IOption.STRING) {
                            outputList.add(outputPrefix + option.getStringValue());
                        } else if (optType == IOption.STRING_LIST || optType == IOption.LIBRARIES
                                || optType == IOption.OBJECTS || optType == IOption.INCLUDE_FILES
                                || optType == IOption.LIBRARY_PATHS || optType == IOption.LIBRARY_FILES
                                || optType == IOption.MACRO_FILES) {
                            List<String> value = (List<String>) option.getValue();
                            outputList = value;
                            ((Tool) tool).filterValues(optType, outputList);
                            // Add outputPrefix to each if necessary
                            if (outputPrefix.length() > 0) {
                                for (int j = 0; j < outputList.size(); j++) {
                                    outputList.set(j, outputPrefix + outputList.get(j));
                                }
                            }
                        }
                        for (int j = 0; j < outputList.size(); j++) {
                            String outputName = outputList.get(j);
                            // try to resolve the build macros in the output
                            // names
                            try {
                                String resolved = null;
                                if (containsSpecialCharacters(sourceLocation.toOSString())) {
                                    resolved = ManagedBuildManager.getBuildMacroProvider().resolveValue(outputName, "",
                                            " ", IBuildMacroProvider.CONTEXT_FILE,
                                            new FileContextData(sourceLocation, null, option, tool));
                                } else {
                                    resolved = ManagedBuildManager.getBuildMacroProvider().resolveValueToMakefileFormat(
                                            outputName, "", " ", IBuildMacroProvider.CONTEXT_FILE,
                                            new FileContextData(sourceLocation, null, option, tool));
                                }
                                if ((resolved = resolved.trim()).length() > 0)
                                    outputName = resolved;
                            } catch (BuildMacroException e) {
                                /* JABA is not going to write this code */
                            }
                            IPath outPath = Path.fromOSString(outputName);
                            // If only a file name is specified, add the
                            // relative path of this output directory
                            if (outPath.segmentCount() == 1) {
                                outPath = Path.fromOSString(relativePath + outputList.get(j));
                            }
                            enumeratedOutputs.add(resolvePercent(outPath, sourceLocation));
                        }
                    } catch (BuildException ex) {
                        /* JABA is not going to write this code */
                    }
                } else
                // 3. If a nameProvider is specified, call it
                if (nameProvider != null) {
                    IPath outPath = null;
                    outPath = nameProvider.getOutputName(project, config, tool, resource.getProjectRelativePath());
                    if (outPath != null) { // MODDED BY JABA ADDED to handle
                        // null as return value
                        caller.usedOutType = type; // MODDED By JABA added to
                        // return the
                        // output type used to generate the
                        // command line
                        String outputName = outPath.toOSString();
                        // try to resolve the build macros in the output
                        // names
                        try {
                            String resolved = null;
                            if (containsSpecialCharacters(sourceLocation.toOSString())) {
                                resolved = ManagedBuildManager.getBuildMacroProvider().resolveValue(outputName, "", " ",
                                        IBuildMacroProvider.CONTEXT_FILE,
                                        new FileContextData(sourceLocation, null, option, tool));
                            } else {
                                resolved = ManagedBuildManager.getBuildMacroProvider().resolveValueToMakefileFormat(
                                        outputName, "", " ", IBuildMacroProvider.CONTEXT_FILE,
                                        new FileContextData(sourceLocation, null, option, tool));
                            }
                            if ((resolved = resolved.trim()).length() > 0)
                                outputName = resolved;
                        } catch (BuildMacroException e) {
                            // JABA is not
                            // going to write
                            // this code
                        }
                        // If only a file name is specified, add the
                        // relative path of this output directory
                        if (outPath.segmentCount() == 1) {
                            outPath = Path.fromOSString(relativePath + outPath.toOSString());
                        }
                        enumeratedOutputs.add(resolvePercent(outPath, sourceLocation));
                    } // MODDED BY JABA ADDED
                } else
                // 4. If outputNames is specified, use it
                if (outputNames != null) {
                    for (int j = 0; j < outputNames.length; j++) {
                        String outputName = outputNames[j];
                        try {
                            // try to resolve the build macros in the output
                            // names
                            String resolved = ManagedBuildManager.getBuildMacroProvider().resolveValueToMakefileFormat(
                                    outputName, "", " ", IBuildMacroProvider.CONTEXT_FILE,
                                    new FileContextData(sourceLocation, null, option, tool));
                            if ((resolved = resolved.trim()).length() > 0)
                                outputName = resolved;
                        } catch (BuildMacroException e) {
                            /* JABA is not going to write this code */
                        }
                        IPath outPath = Path.fromOSString(outputName);
                        // If only a file name is specified, add the relative
                        // path of this output directory
                        if (outPath.segmentCount() == 1) {
                            outPath = Path.fromOSString(relativePath + outPath.toOSString());
                        }
                        enumeratedOutputs.add(resolvePercent(outPath, sourceLocation));
                    }
                } else {
                    // 5. Use the name pattern to generate a transformation
                    // macro
                    // so that the source names can be transformed into the
                    // target names
                    // using the built-in string substitution functions of
                    // <code>make</code>.
                    if (multOfType) {
                        // This case is not handled - a nameProvider or
                        // outputNames must be specified
                        // TODO - report error
                    } else {
                        String namePattern = type.getNamePattern();
                        IPath namePatternPath = null;
                        if (namePattern == null || namePattern.length() == 0) {
                            namePattern = relativePath + outputPrefix + IManagedBuilderMakefileGenerator.WILDCARD;
                            if (outExt != null && outExt.length() > 0) {
                                namePattern += DOT + outExt;
                            }
                            namePatternPath = Path.fromOSString(namePattern);
                        } else {
                            if (outputPrefix.length() > 0) {
                                namePattern = outputPrefix + namePattern;
                            }
                            namePatternPath = Path.fromOSString(namePattern);
                            // If only a file name is specified, add the
                            // relative path of this output directory
                            if (namePatternPath.segmentCount() == 1) {
                                namePatternPath = Path.fromOSString(relativePath + namePatternPath.toOSString());
                            }
                        }
                        enumeratedOutputs.add(resolvePercent(namePatternPath, sourceLocation));
                    }
                }
            }
        }
    }

    /**
     * If the path contains a %, returns the path resolved using the resource name
     */
    private IPath resolvePercent(IPath outPath, IPath sourceLocation) {
        // Get the input file name
        String fileName = sourceLocation.removeFileExtension().lastSegment();
        // Replace the % with the file name
        String outName = outPath.toOSString().replace("%", fileName);
        IPath result = Path.fromOSString(outName);
        return DOT_SLASH_PATH.isPrefixOf(outPath) ? DOT_SLASH_PATH.append(result) : result;
    }

    /*
     * Add any dependency calculator options to the tool options
     */
    private String[] addDependencyOptions(IManagedDependencyCommands depCommands, String[] flags) {
        String[] depOptions = depCommands.getDependencyCommandOptions();
        if (depOptions != null && depOptions.length > 0) {
            int flagsLen = flags.length;
            String[] flagsCopy = new String[flags.length + depOptions.length];
            for (int i = 0; i < flags.length; i++) {
                flagsCopy[i] = flags[i];
            }
            for (int i = 0; i < depOptions.length; i++) {
                flagsCopy[i + flagsLen] = depOptions[i];
            }
            flags = flagsCopy;
        }
        return flags;
    }

    private IManagedCommandLineInfo generateToolCommandLineInfo(ITool tool, String sourceExtension, String[] flags,
            String outputFlag, String outputPrefix, String outputName, String[] inputResources, IPath inputLocation,
            IPath outputLocation) {
        IConfiguration config = getConfig();
        String cmd = tool.getToolCommand();
        // try to resolve the build macros in the tool command
        try {
            String resolvedCommand = null;
            if ((inputLocation != null && inputLocation.toString().indexOf(" ") != -1)
                    || (outputLocation != null && outputLocation.toString().indexOf(" ") != -1)) {
                resolvedCommand = ManagedBuildManager.getBuildMacroProvider().resolveValue(cmd, "", " ",
                        IBuildMacroProvider.CONTEXT_FILE,
                        new FileContextData(inputLocation, outputLocation, null, tool));
            } else {
                resolvedCommand = ManagedBuildManager.getBuildMacroProvider().resolveValueToMakefileFormat(cmd, "", " ",
                        IBuildMacroProvider.CONTEXT_FILE,
                        new FileContextData(inputLocation, outputLocation, null, tool));
            }
            if ((resolvedCommand = resolvedCommand.trim()).length() > 0)
                cmd = resolvedCommand;
        } catch (BuildMacroException e) {
            /* JABA is not going to write this code */
        }
        IManagedCommandLineGenerator gen = tool.getCommandLineGenerator();
        return gen.generateCommandLineInfo(tool, cmd, flags, outputFlag, outputPrefix, outputName, inputResources,
                getToolCommandLinePattern(config, tool));
    }

}
