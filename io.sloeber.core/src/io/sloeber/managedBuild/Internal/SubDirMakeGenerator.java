package io.sloeber.managedBuild.Internal;

import static io.sloeber.managedBuild.Internal.ManagebBuildCommon.*;
//import static io.sloeber.managedBuild.Internal.ManagebBuildCommon.*;
import static io.sloeber.managedBuild.Internal.ManagedBuildConstants.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Vector;

import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.cdt.managedbuilder.core.BuildException;
import org.eclipse.cdt.managedbuilder.core.IBuildObject;
import org.eclipse.cdt.managedbuilder.core.IConfiguration;
import org.eclipse.cdt.managedbuilder.core.IFileInfo;
import org.eclipse.cdt.managedbuilder.core.IFolderInfo;
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
import org.eclipse.cdt.managedbuilder.internal.macros.BuildMacroProvider;
import org.eclipse.cdt.managedbuilder.internal.macros.FileContextData;
import org.eclipse.cdt.managedbuilder.macros.BuildMacroException;
import org.eclipse.cdt.managedbuilder.macros.IBuildMacroProvider;
import org.eclipse.cdt.managedbuilder.makegen.IManagedBuilderMakefileGenerator;
import org.eclipse.cdt.managedbuilder.makegen.IManagedDependencyCalculator;
import org.eclipse.cdt.managedbuilder.makegen.IManagedDependencyCommands;
import org.eclipse.cdt.managedbuilder.makegen.IManagedDependencyGenerator;
import org.eclipse.cdt.managedbuilder.makegen.IManagedDependencyGenerator2;
import org.eclipse.cdt.managedbuilder.makegen.IManagedDependencyGeneratorType;
import org.eclipse.cdt.managedbuilder.makegen.IManagedDependencyInfo;
import org.eclipse.cdt.managedbuilder.makegen.IManagedDependencyPreBuild;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
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

    private IPath getTopBuildDir() {
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

        //make the file

        StringBuffer makeBuf = getHeader();
        makeBuf.append(GenerateMacros(makeRules));
        makeBuf.append(GenerateRules(makeRules));
        makeBuf.append(addSources(module));

        // Save the files
        save(makeBuf, modMakefile);
    }

    private StringBuffer GenerateMacros(List<MakeRule> makeRules) {
        StringBuffer buffer = new StringBuffer();
        IPath buildRoot = getTopBuildDir();
        buffer.append("#GenerateMacros not yet implemented");
        buffer.append(NEWLINE);
        HashSet<String> macroNames = new HashSet<>();
        for (MakeRule makeRule : makeRules) {
            macroNames.addAll(makeRule.getMacros());
        }
        for (String macroName : macroNames) {
            HashSet<IPath> files = new HashSet<>();
            for (MakeRule makeRule : makeRules) {
                files.addAll(makeRule.getMacroElements(macroName));
            }
            if (files.size() > 0) {
                buffer.append(macroName).append("+=").append(WHITESPACE);
                for (IPath file : files) {
                    buffer.append(LINEBREAK);
                    buffer.append(GetNiceFileName(buildRoot, file)).append(WHITESPACE);
                }
                buffer.append(NEWLINE);
            }
        }
        return buffer;
    }

    private String GetNiceFileName(IPath buildPath, IPath path) {
        if (buildPath.isPrefixOf(path)) {
            return DOT_SLASH_PATH.append(path.makeRelativeTo(buildPath)).toOSString();
        } else {
            if (buildPath.removeLastSegments(1).isPrefixOf(path)) {
                return path.makeRelativeTo(buildPath).toOSString();
            }
        }

        return path.toOSString();
        //        if (dirLocation.isPrefixOf(sourceLocation)) {
        //            IPath srcPath = sourceLocation.removeFirstSegments(dirLocation.segmentCount()).setDevice(null);
        //            if (generatedSource) {
        //                srcName = DOT_SLASH_PATH.append(srcPath).toOSString();
        //            } else {
        //                srcName = ROOT + FILE_SEPARATOR + srcPath.toOSString();
        //            }
        //        } else {
        //            if (generatedSource && !sourceLocation.isAbsolute()) {
        //                srcName = DOT_SLASH_PATH.append(relativePath).append(sourceLocation.lastSegment()).toOSString();
        //            } else {
        //                // TODO: Should we use relative paths when possible (e.g., see
        //                // MbsMacroSupplier.calculateRelPath)
        //                srcName = sourceLocation.toOSString();
        //            }
        //        }
    }

    private StringBuffer GenerateRules(List<MakeRule> makeRules) {
        StringBuffer buffer = new StringBuffer();
        buffer.append("#GenerateRules not yet implemented");
        buffer.append(NEWLINE);
        return buffer;
    }

    static private StringBuffer getHeader() {
        return addDefaultHeader();
    }

    private List<MakeRule> getMakeRules(IContainer module) {
        IConfiguration config = getConfig();
        IPath buildPath = getTopBuildDir();
        List<MakeRule> makeRules = new LinkedList<>();
        // Visit the resources in this folder a
        IResource[] resources;
        try {
            resources = module.members();

            IFolder folder = getProject().getFolder(config.getName());
            for (IResource resource : resources) {
                if (resource.getType() == IResource.FILE) {
                    IFile inputFile = (IFile) resource;
                    // Check whether this resource is excluded from build
                    IPath rcProjRelPath = resource.getProjectRelativePath();
                    if (!caller.isSource(rcProjRelPath))
                        continue;
                    IResourceInfo rcInfo = config.getResourceInfo(rcProjRelPath, false);

                    ITool tool = null;
                    String varName = null;
                    String ext = rcProjRelPath.getFileExtension();
                    if (ext != null) {
                        varName = getSourceMacroName(ext).toString();
                    }

                    //try to find tool 
                    if (rcInfo instanceof IFileInfo) {
                        IFileInfo fi = (IFileInfo) rcInfo;
                        ITool[] tools = fi.getToolsToInvoke();
                        if (tools != null && tools.length > 0) {
                            tool = tools[0];
                        }
                    }

                    //No tool found try other way
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
                    if (tool != null) {
                        MakeRule newMakeRule = new MakeRule();
                        makeRules.add(newMakeRule);
                        // Generate the rule to build this source file
                        IInputType inputType = tool.getPrimaryInputType();
                        if (inputType == null) {
                            inputType = tool.getInputType(ext);
                        }
                        if (inputType != null) {
                            if (inputType.getBuildVariable() != null) {
                                varName = inputType.getBuildVariable();
                            }
                        }
                        newMakeRule.addPrerequisite(varName, inputFile.getLocation());
                        for (String curExtension : tool.getAllOutputExtensions()) {
                            IOutputType outputType = tool.getOutputType(curExtension);
                            String OutMacroName = outputType.getBuildVariable();
                            IManagedOutputNameProviderJaba nameProvider = getJABANameProvider(outputType);
                            if (nameProvider != null) {
                                IPath outputFile = nameProvider.getOutputName(getProject(), config, tool,
                                        resource.getFullPath());
                                if (outputFile != null) {
                                    newMakeRule.addTarget(OutMacroName,
                                            buildPath.append(outputFile.removeFirstSegments(1)));
                                }

                            }
                        }

                        //                    if ((primaryInputType != null && !primaryInputType.getMultipleOfType())
                        //                            || (inputType == null && tool != caller.config.calculateTargetTool())) {
                        //                        // Try to add the rule for the file
                        //                        Vector<IPath> generatedOutputs = new Vector<IPath>();
                        //                        Vector<IPath> generatedDepFiles = new Vector<IPath>();
                        //                        // MODED moved JABA JAn Baeyens get the out type from the add
                        //                        // source call
                        //                        caller.usedOutType = null;
                        //                        addRuleForSource(project, relativePath, ruleBuffer, resource, sourceLocation, rcInfo,
                        //                                generatedSource, generatedDepFiles, generatedOutputs);
                        //                        // If the rule generates a dependency file(s), add the file(s)
                        //                        // to the variable
                        //                        if (generatedDepFiles.size() > 0) {
                        //                            for (int k = 0; k < generatedDepFiles.size(); k++) {
                        //                                IPath generatedDepFile = generatedDepFiles.get(k);
                        //                                addMacroAdditionFile(buildVarToRuleStringMap, getDepMacroName(ext).toString(),
                        //                                        (generatedDepFile.isAbsolute() ? "" : DOT_SLASH_PATH.toOSString())
                        //                                                + generatedDepFile.toOSString());
                        //                            }
                        //                        }
                        //                        // If the generated outputs of this tool are input to another
                        //                        // tool,
                        //                        // 1. add the output to the appropriate macro
                        //                        // 2. If the tool does not have multipleOfType input, generate
                        //                        // the rule.
                        //                        // IOutputType outType = tool.getPrimaryOutputType();
                        //                        // MODED
                        //                        // moved JABA JAn Baeyens get the out type from the add source
                        //                        // call
                        //                        String buildVariable = null;
                        //                        if (caller.usedOutType != null) {
                        //                            if (tool.getCustomBuildStep()) {
                        //                                // TODO: This is somewhat of a hack since a custom build
                        //                                // step
                        //                                // tool does not currently define a build variable
                        //                                if (generatedOutputs.size() > 0) {
                        //                                    IPath firstOutput = generatedOutputs.get(0);
                        //                                    String firstExt = firstOutput.getFileExtension();
                        //                                    ToolInfoHolder tmpH = ToolInfoHolder.getFolderToolInfo(caller, rcInfo.getPath());
                        //                                    ITool[] tmpBuildTools = tmpH.buildTools;
                        //                                    for (ITool tmpBuildTool : tmpBuildTools) {
                        //                                        if (tmpBuildTool.buildsFileType(firstExt)) {
                        //                                            String bV = tmpBuildTool.getPrimaryInputType().getBuildVariable();
                        //                                            if (bV.length() > 0) {
                        //                                                buildVariable = bV;
                        //                                                break;
                        //                                            }
                        //                                        }
                        //                                    }
                        //                                }
                        //                            } else {
                        //                                buildVariable = caller.usedOutType.getBuildVariable();
                        //                            }
                        //                        } else {
                        //                            // For support of pre-CDT 3.0 integrations.
                        //                            buildVariable = OBJS_MACRO;
                        //                        }
                        //                        for (int k = 0; k < generatedOutputs.size(); k++) {
                        //                            IPath generatedOutput;
                        //                            IResource generateOutputResource;
                        //                            if (generatedOutputs.get(k).isAbsolute()) {
                        //                                // TODO: Should we use relative paths when possible
                        //                                // (e.g., see MbsMacroSupplier.calculateRelPath)
                        //                                generatedOutput = generatedOutputs.get(k);
                        //                                // If this file has an absolute path, then the
                        //                                // generateOutputResource will not be correct
                        //                                // because the file is not under the project. We use
                        //                                // this resource in the calls to the dependency
                        //                                // generator
                        //                                generateOutputResource = project.getFile(generatedOutput);
                        //                            } else {
                        //                                generatedOutput = getPathForResource(project).append(caller.getBuildWorkingDir())
                        //                                        .append(generatedOutputs.get(k));
                        //                                generateOutputResource = project
                        //                                        .getFile(caller.getBuildWorkingDir().append(generatedOutputs.get(k)));
                        //                            }
                        //                            IResourceInfo nextRcInfo;
                        //                            if (rcInfo instanceof IFileInfo) {
                        //                                nextRcInfo = caller.config.getResourceInfo(rcInfo.getPath().removeLastSegments(1),
                        //                                        false);
                        //                            } else {
                        //                                nextRcInfo = rcInfo;
                        //                            }
                        //                            addFragmentMakefileEntriesForSource(buildVarToRuleStringMap, ruleBuffer, folder,
                        //                                    relativePath, generateOutputResource, generatedOutput, nextRcInfo, buildVariable,
                        //                                    true);
                        //                        }
                        //                    }
                        //                } else {
                        //                    // If this is a secondary input, add it to build vars
                        //                    if (varName == null) {
                        //                        for (ITool buildTool : buildTools) {
                        //                            if (buildTool.isInputFileType(ext)) {
                        //                                addToBuildVar(buildVarToRuleStringMap, ext, varName, relativePath, sourceLocation,
                        //                                        generatedSource);
                        //                                break;
                        //                            }
                        //                        }
                        //                    }
                        //                    // If this generated output is identified as a secondary output, add
                        //                    // the file to the build variable
                        //                    else {
                        //                        IOutputType[] secondaryOutputs = caller.config.getToolChain().getSecondaryOutputs();
                        //                        if (secondaryOutputs.length > 0) {
                        //                            if (isSecondaryOutputVar(h, secondaryOutputs, varName)) {
                        //                                addMacroAdditionFile(caller, buildVarToRuleStringMap, varName, relativePath,
                        //                                        sourceLocation, generatedSource);
                        //                            }
                        //                        }
                        //                    }
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
     * Returns a <code>StringBuffer</code> containing makefile text for all of the
     * sources contributed by a container (project directory/subdirectory) to the
     * fragement makefile
     *
     * @param module
     *            project resource directory/subdirectory
     * @return StringBuffer generated text for the fragement makefile
     */
    private StringBuffer addSources(IContainer module) throws CoreException {
        IProject project = getProject();
        // Calculate the new directory relative to the build output
        IPath moduleRelativePath = module.getProjectRelativePath();
        String relativePath = moduleRelativePath.toOSString();
        relativePath += relativePath.length() == 0 ? "" : FILE_SEPARATOR;
        // For build macros in the configuration, create a map which will map
        // them
        // to a string which holds its list of sources.
        LinkedHashMap<String, String> buildVarToRuleStringMap = new LinkedHashMap<String, String>();
        // Add statements that add the source files in this folder,
        // and generated source files, and generated dependency files
        // to the build macros
        for (Entry<String, List<IPath>> entry : caller.buildSrcVars.entrySet()) {
            String macroName = entry.getKey();
            addMacroAdditionPrefix(buildVarToRuleStringMap, macroName, null, false);
        }
        for (Entry<String, List<IPath>> entry : caller.buildOutVars.entrySet()) {
            String macroName = entry.getKey();
            addMacroAdditionPrefix(buildVarToRuleStringMap, macroName, DOT_SLASH_PATH.append(relativePath).toOSString(),
                    false);
        }
        // String buffers
        StringBuffer buffer = new StringBuffer();
        StringBuffer ruleBuffer = new StringBuffer(
                COMMENT_SYMBOL + WHITESPACE + ManagedMakeMessages.getResourceString(MOD_RULES) + NEWLINE);
        // Visit the resources in this folder and add each one to a sources
        // macro, and generate a build rule, if appropriate
        IResource[] resources = module.members();
        IResourceInfo rcInfo;
        IFolder folder = project.getFolder(caller.config.getName());
        for (IResource resource : resources) {
            if (resource.getType() == IResource.FILE) {
                // Check whether this resource is excluded from build
                IPath rcProjRelPath = resource.getProjectRelativePath();
                if (!caller.isSource(rcProjRelPath))
                    continue;
                rcInfo = caller.config.getResourceInfo(rcProjRelPath, false);
                // if( (rcInfo.isExcluded()) )
                // continue;
                addFragmentMakefileEntriesForSource(buildVarToRuleStringMap, ruleBuffer, folder, relativePath, resource,
                        getPathForResource(resource), rcInfo, null, false);
            }
        }
        // Write out the macro addition entries to the buffer
        buffer.append(writeAdditionMacros(buildVarToRuleStringMap));
        return buffer.append(ruleBuffer).append(NEWLINE);
    }

    /*
     * (non-Javadoc Adds the entries for a particular source file to the fragment
     * makefile
     *
     * @param buildVarToRuleStringMap map of build variable names to the list of
     * files assigned to the variable
     *
     * @param ruleBuffer buffer to add generated nmakefile text to
     *
     * @param folder the top level build output directory
     *
     * @param relativePath build output directory relative path of the current
     * output directory
     *
     * @param resource the source file for this invocation of the tool - this may be
     * null for a generated output
     *
     * @param sourceLocation the full path of the source
     *
     * @param resConfig the IResourceConfiguration associated with this file or null
     *
     * @param varName the build variable to add this invocation's outputs to if
     * <code>null</code>, use the file extension to find the name
     *
     * @param generatedSource if <code>true</code>, this file was generated by
     * another tool in the tool-chain
     */
    private void addFragmentMakefileEntriesForSource(LinkedHashMap<String, String> buildVarToRuleStringMap,
            StringBuffer ruleBuffer, IFolder folder, String relativePath, IResource resource, IPath sourceLocation,
            IResourceInfo rcInfo, String varName, boolean generatedSource) {
        IProject project = getProject();
        // Determine which tool, if any, builds files with this extension
        String ext = sourceLocation.getFileExtension();
        ITool tool = null;

        if (rcInfo instanceof IFileInfo) {
            IFileInfo fi = (IFileInfo) rcInfo;
            ITool[] tools = fi.getToolsToInvoke();
            if (tools != null && tools.length > 0) {
                tool = tools[0];
                addToBuildVar(buildVarToRuleStringMap, ext, varName, relativePath, sourceLocation, generatedSource);
            }
        }
        ToolInfoHolder h = ToolInfoHolder.getToolInfo(caller, rcInfo.getPath());
        ITool buildTools[] = h.buildTools;

        if (tool == null) {
            h = ToolInfoHolder.getToolInfo(caller, Path.EMPTY);
            buildTools = h.buildTools;
            for (ITool buildTool : buildTools) {
                if (buildTool.buildsFileType(ext)) {
                    tool = buildTool;
                    addToBuildVar(buildVarToRuleStringMap, ext, varName, relativePath, sourceLocation, generatedSource);
                    break;
                }
            }
        }
        if (tool != null) {
            // Generate the rule to build this source file
            IInputType primaryInputType = tool.getPrimaryInputType();
            IInputType inputType = tool.getInputType(ext);
            if ((primaryInputType != null && !primaryInputType.getMultipleOfType())
                    || (inputType == null && tool != caller.config.calculateTargetTool())) {
                // Try to add the rule for the file
                Vector<IPath> generatedOutputs = new Vector<IPath>();
                Vector<IPath> generatedDepFiles = new Vector<IPath>();
                // MODED moved JABA JAn Baeyens get the out type from the add
                // source call
                caller.usedOutType = null;
                addRuleForSource(project, relativePath, ruleBuffer, resource, sourceLocation, rcInfo, generatedSource,
                        generatedDepFiles, generatedOutputs);
                // If the rule generates a dependency file(s), add the file(s)
                // to the variable
                if (generatedDepFiles.size() > 0) {
                    for (int k = 0; k < generatedDepFiles.size(); k++) {
                        IPath generatedDepFile = generatedDepFiles.get(k);
                        addMacroAdditionFile(buildVarToRuleStringMap, getDepMacroName(ext).toString(),
                                (generatedDepFile.isAbsolute() ? "" : DOT_SLASH_PATH.toOSString())
                                        + generatedDepFile.toOSString());
                    }
                }
                // If the generated outputs of this tool are input to another
                // tool,
                // 1. add the output to the appropriate macro
                // 2. If the tool does not have multipleOfType input, generate
                // the rule.
                // IOutputType outType = tool.getPrimaryOutputType();
                // MODED
                // moved JABA JAn Baeyens get the out type from the add source
                // call
                String buildVariable = null;
                if (caller.usedOutType != null) {
                    if (tool.getCustomBuildStep()) {
                        // TODO: This is somewhat of a hack since a custom build
                        // step
                        // tool does not currently define a build variable
                        if (generatedOutputs.size() > 0) {
                            IPath firstOutput = generatedOutputs.get(0);
                            String firstExt = firstOutput.getFileExtension();
                            ToolInfoHolder tmpH = ToolInfoHolder.getFolderToolInfo(caller, rcInfo.getPath());
                            ITool[] tmpBuildTools = tmpH.buildTools;
                            for (ITool tmpBuildTool : tmpBuildTools) {
                                if (tmpBuildTool.buildsFileType(firstExt)) {
                                    String bV = tmpBuildTool.getPrimaryInputType().getBuildVariable();
                                    if (bV.length() > 0) {
                                        buildVariable = bV;
                                        break;
                                    }
                                }
                            }
                        }
                    } else {
                        buildVariable = caller.usedOutType.getBuildVariable();
                    }
                } else {
                    // For support of pre-CDT 3.0 integrations.
                    buildVariable = OBJS_MACRO;
                }
                for (int k = 0; k < generatedOutputs.size(); k++) {
                    IPath generatedOutput;
                    IResource generateOutputResource;
                    if (generatedOutputs.get(k).isAbsolute()) {
                        // TODO: Should we use relative paths when possible
                        // (e.g., see MbsMacroSupplier.calculateRelPath)
                        generatedOutput = generatedOutputs.get(k);
                        // If this file has an absolute path, then the
                        // generateOutputResource will not be correct
                        // because the file is not under the project. We use
                        // this resource in the calls to the dependency
                        // generator
                        generateOutputResource = project.getFile(generatedOutput);
                    } else {
                        generatedOutput = getPathForResource(project).append(caller.getBuildWorkingDir())
                                .append(generatedOutputs.get(k));
                        generateOutputResource = project
                                .getFile(caller.getBuildWorkingDir().append(generatedOutputs.get(k)));
                    }
                    IResourceInfo nextRcInfo;
                    if (rcInfo instanceof IFileInfo) {
                        nextRcInfo = caller.config.getResourceInfo(rcInfo.getPath().removeLastSegments(1), false);
                    } else {
                        nextRcInfo = rcInfo;
                    }
                    addFragmentMakefileEntriesForSource(buildVarToRuleStringMap, ruleBuffer, folder, relativePath,
                            generateOutputResource, generatedOutput, nextRcInfo, buildVariable, true);
                }
            }
        } else {
            // If this is a secondary input, add it to build vars
            if (varName == null) {
                for (ITool buildTool : buildTools) {
                    if (buildTool.isInputFileType(ext)) {
                        addToBuildVar(buildVarToRuleStringMap, ext, varName, relativePath, sourceLocation,
                                generatedSource);
                        break;
                    }
                }
            }
            // If this generated output is identified as a secondary output, add
            // the file to the build variable
            else {
                IOutputType[] secondaryOutputs = caller.config.getToolChain().getSecondaryOutputs();
                if (secondaryOutputs.length > 0) {
                    if (isSecondaryOutputVar(h, secondaryOutputs, varName)) {
                        addMacroAdditionFile(caller, buildVarToRuleStringMap, varName, relativePath, sourceLocation,
                                generatedSource);
                    }
                }
            }
        }
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
     * Create a rule for this source file. We create a pattern rule if possible.
     * This is an example of a pattern rule: <relative_path>/%.<outputExtension>:
     * ../<relative_path>/%. <inputExtension>
     *
     * @echo Building file: $<
     * @echo Invoking tool xxx
     * @echo <tool> <flags> <output_flag><output_prefix>$@ $<
     * @<tool> <flags> <output_flag><output_prefix>$@ $< && \ echo -n $(@:%.o=%.d) '
     *         <relative_path>/' >> $(@:%.o=%.d) && \ <tool> -P -MM -MG <flags> $<
     *         >> $(@:%.o=%.d)
     * @echo Finished building: $<
     * @echo ' ' Note that the macros all come from the build model and are resolved
     *       to a real command before writing to the module makefile, so a real
     *       command might look something like: source1/%.o: ../source1/%.cpp
     * @echo Building file: $<
     * @echo Invoking tool xxx
     * @echo g++ -g -O2 -c -I/cygdrive/c/eclipse/workspace/Project/headers -o$@
     *       $< @g++ -g -O2 -c -I/cygdrive/c/eclipse/workspace/Project/headers -o$@
     *       $< && \ echo -n $(@:%.o=%.d) ' source1/' >> $(@:%.o=%.d) && \ g++ -P
     *       -MM -MG -g -O2 -c -I/cygdrive/c/eclipse/workspace/Project/headers $< >>
     *       $(@:%.o=%.d)
     * @echo Finished building: $<
     * @echo ' '
     * @param relativePath
     *            top build output directory relative path of the current output
     *            directory
     * @param buffer
     *            buffer to populate with the build rule
     * @param resource
     *            the source file for this invocation of the tool
     * @param sourceLocation
     *            the full path of the source
     * @param rcInfo
     *            the IResourceInfo associated with this file or null
     * @param generatedSource
     *            <code>true</code> if the resource is a generated output
     * @param enumeratedOutputs
     *            vector of the filenames that are the output of this rule
     */
    private void addRuleForSource(IProject project, String relativePath, StringBuffer buffer, IResource resource,
            IPath sourceLocation, IResourceInfo rcInfo, boolean generatedSource, List<IPath> generatedDepFiles,
            List<IPath> enumeratedOutputs) {
        IConfiguration config = getConfig();
        String fileName = sourceLocation.removeFileExtension().lastSegment();
        String inputExtension = sourceLocation.getFileExtension();
        String outputExtension = null;
        ITool tool = null;
        if (rcInfo instanceof IFileInfo) {
            IFileInfo fi = (IFileInfo) rcInfo;
            ITool[] tools = fi.getToolsToInvoke();
            if (tools != null && tools.length > 0) {
                tool = tools[0];
            }
        } else {
            IFolderInfo foInfo = (IFolderInfo) rcInfo;
            tool = foInfo.getToolFromInputExtension(inputExtension);
        }
        ToolInfoHolder h = ToolInfoHolder.getToolInfo(caller, rcInfo.getPath());
        if (tool != null)
            outputExtension = tool.getOutputExtension(inputExtension);
        if (outputExtension == null)
            outputExtension = EMPTY_STRING;
        // Get the dependency generator information for this tool and file
        // extension
        IManagedDependencyGenerator oldDepGen = null;
        IManagedDependencyGenerator2 depGen = null;
        IManagedDependencyInfo depInfo = null;
        IManagedDependencyCommands depCommands = null;
        IManagedDependencyPreBuild depPreBuild = null;
        IPath[] depFiles = null;
        boolean doDepGen = false;
        {
            IManagedDependencyGeneratorType t = null;
            if (tool != null)
                t = tool.getDependencyGeneratorForExtension(inputExtension);
            if (t != null) {
                int calcType = t.getCalculatorType();
                if (calcType <= IManagedDependencyGeneratorType.TYPE_OLD_TYPE_LIMIT) {
                    oldDepGen = (IManagedDependencyGenerator) t;
                    doDepGen = (calcType == IManagedDependencyGeneratorType.TYPE_COMMAND);
                    if (doDepGen) {
                        IPath depFile = Path.fromOSString(relativePath + fileName + DOT + DEP_EXT);
                        getDependencyMakefiles(h).add(depFile);
                        generatedDepFiles.add(depFile);
                    }
                } else {
                    depGen = (IManagedDependencyGenerator2) t;
                    doDepGen = (calcType == IManagedDependencyGeneratorType.TYPE_BUILD_COMMANDS);
                    IBuildObject buildContext = rcInfo;
                    depInfo = depGen.getDependencySourceInfo(resource.getProjectRelativePath(), resource, buildContext,
                            tool, getBuildWorkingDir());
                    if (calcType == IManagedDependencyGeneratorType.TYPE_BUILD_COMMANDS) {
                        depCommands = (IManagedDependencyCommands) depInfo;
                        depFiles = depCommands.getDependencyFiles();
                    } else if (calcType == IManagedDependencyGeneratorType.TYPE_PREBUILD_COMMANDS) {
                        depPreBuild = (IManagedDependencyPreBuild) depInfo;
                        depFiles = depPreBuild.getDependencyFiles();
                    }
                    if (depFiles != null) {
                        for (IPath depFile : depFiles) {
                            getDependencyMakefiles(h).add(depFile);
                            generatedDepFiles.add(depFile);
                        }
                    }
                }
            }
        }
        // Figure out the output paths
        String optDotExt = EMPTY_STRING;
        if (outputExtension.length() > 0)
            optDotExt = DOT + outputExtension;
        // JABA
        caller.usedOutType = tool.getPrimaryOutputType();
        calculateOutputsForSource(tool, relativePath, resource, sourceLocation, enumeratedOutputs);
        String primaryOutputName = null;
        if (enumeratedOutputs.size() > 0) {
            primaryOutputName = escapeWhitespaces(enumeratedOutputs.get(0).toOSString());
        } else {
            primaryOutputName = escapeWhitespaces(relativePath + fileName + optDotExt);
        }
        String otherPrimaryOutputs = EMPTY_STRING;
        for (IPath curOutput : enumeratedOutputs) {
            otherPrimaryOutputs += WHITESPACE + escapeWhitespaces(curOutput.toOSString());
        }
        // Output file location needed for the file-build macros
        IPath outputLocation = Path.fromOSString(primaryOutputName);
        if (!outputLocation.isAbsolute()) {
            outputLocation = getPathForResource(project).append(getBuildWorkingDir()).append(primaryOutputName);
        }
        // A separate rule is needed for the resource in the case where explicit
        // file-specific macros
        // are referenced, or if the resource contains special characters in its
        // path (e.g., whitespace)
        /*
         * fix for 137674 We only need an explicit rule if one of the following is true:
         * - The resource is linked, and its full path to its real location contains
         * special characters - The resource is not linked, but its project relative
         * path contains special characters
         */
        boolean resourceNameRequiresExplicitRule = (resource.isLinked()
                && containsSpecialCharacters(sourceLocation.toOSString()))
                || (!resource.isLinked() && containsSpecialCharacters(resource.getProjectRelativePath().toOSString()));
        boolean needExplicitRuleForFile = resourceNameRequiresExplicitRule
                || BuildMacroProvider.getReferencedExplitFileMacros(tool).length > 0
                || BuildMacroProvider.getReferencedExplitFileMacros(tool.getToolCommand(),
                        IBuildMacroProvider.CONTEXT_FILE,
                        new FileContextData(sourceLocation, outputLocation, null, tool)).length > 0;
        // Get and resolve the command
        String cmd = tool.getToolCommand();
        try {
            String resolvedCommand = null;
            if (!needExplicitRuleForFile) {
                resolvedCommand = ManagedBuildManager.getBuildMacroProvider().resolveValueToMakefileFormat(cmd,
                        EMPTY_STRING, WHITESPACE, IBuildMacroProvider.CONTEXT_FILE,
                        new FileContextData(sourceLocation, outputLocation, null, tool));
            } else {
                // if we need an explicit rule then don't use any builder
                // variables, resolve everything
                // to explicit strings
                resolvedCommand = ManagedBuildManager.getBuildMacroProvider().resolveValue(cmd, EMPTY_STRING,
                        WHITESPACE, IBuildMacroProvider.CONTEXT_FILE,
                        new FileContextData(sourceLocation, outputLocation, null, tool));
            }
            if ((resolvedCommand = resolvedCommand.trim()).length() > 0)
                cmd = resolvedCommand;
        } catch (BuildMacroException e) {
            /* JABA is not going to write this code */
        }
        String defaultOutputName = EMPTY_STRING;
        String primaryDependencyName = EMPTY_STRING;
        String patternPrimaryDependencyName = EMPTY_STRING;
        String home = (generatedSource) ? DOT : ROOT;
        String resourcePath = null;
        boolean patternRule = true;
        boolean isItLinked = false;
        if (resource.isLinked(IResource.CHECK_ANCESTORS)) {
            // it IS linked, so use the actual location
            isItLinked = true;
            resourcePath = sourceLocation.toOSString();
            // Need a hardcoded rule, not a pattern rule, as a linked file
            // can reside in any path
            defaultOutputName = escapeWhitespaces(relativePath + fileName + optDotExt);
            primaryDependencyName = escapeWhitespaces(resourcePath);
            patternRule = false;
        } else {
            // Use the relative path (not really needed to store per se but in
            // the future someone may want this)
            resourcePath = relativePath;
            // The rule and command to add to the makefile
            if (rcInfo instanceof IFileInfo || needExplicitRuleForFile) {
                // Need a hardcoded rule, not a pattern rule
                defaultOutputName = escapeWhitespaces(resourcePath + fileName + optDotExt);
                patternRule = false;
            } else {
                defaultOutputName = relativePath + WILDCARD + optDotExt;
            }
            primaryDependencyName = escapeWhitespaces(
                    home + FILE_SEPARATOR + resourcePath + fileName + DOT + inputExtension);
            patternPrimaryDependencyName = home + FILE_SEPARATOR + resourcePath + WILDCARD + DOT + inputExtension;
        } // end fix for PR 70491
          // If the tool specifies a dependency calculator of
          // TYPE_BUILD_COMMANDS,
          // ask whether
          // the dependency commands are "generic" (i.e., we can use a pattern
          // rule)
        boolean needExplicitDependencyCommands = false;
        if (depCommands != null) {
            needExplicitDependencyCommands = !depCommands.areCommandsGeneric();
        }
        // If we still think that we are using a pattern rule, check a few more
        // things
        if (patternRule) {
            patternRule = false;
            // Make sure that at least one of the rule outputs contains a %.
            for (IPath output : enumeratedOutputs) {
                String ruleOutput = output.toOSString();
                if (ruleOutput.indexOf('%') >= 0) {
                    patternRule = true;
                    break;
                }
            }
            if (patternRule) {
                patternRule = !needExplicitDependencyCommands;
            }
        }
        // Begin building the rule for this source file
        String buildRule = EMPTY_STRING;
        if (patternRule) {
            if (enumeratedOutputs.size() == 0) {
                buildRule += defaultOutputName;
            } else {
                boolean first = true;
                for (IPath curOutput : enumeratedOutputs) {
                    String ruleOutput = curOutput.toOSString();
                    if (ruleOutput.indexOf('%') >= 0) {
                        if (first) {
                            first = false;
                        } else {
                            buildRule += WHITESPACE;
                        }
                        buildRule += ruleOutput;
                    }
                }
            }
        } else {
            buildRule += primaryOutputName;
        }
        String buildRuleDependencies = primaryDependencyName;
        String patternBuildRuleDependencies = patternPrimaryDependencyName;
        // Other additional inputs
        // Get any additional dependencies specified for the tool in other
        // InputType elements and AdditionalInput elements
        IPath[] addlDepPaths = tool.getAdditionalDependencies();
        for (IPath addlDepPath : addlDepPaths) {
            // Translate the path from project relative to build directory
            // relative
            IPath addlPath = addlDepPath;
            if (!(addlPath.toString().startsWith("$("))) {
                if (!addlPath.isAbsolute()) {
                    IPath tempPath = project.getLocation().append(new Path(ensureUnquoted(addlPath.toString())));
                    if (tempPath != null) {
                        addlPath = ManagedBuildManager.calculateRelativePath(getTopBuildDir(), tempPath);
                    }
                }
            }
            String suitablePath = ensurePathIsGNUMakeTargetRuleCompatibleSyntax(addlPath.toOSString());
            buildRuleDependencies += WHITESPACE + suitablePath;
            patternBuildRuleDependencies += WHITESPACE + suitablePath;
        }
        buildRule += COLON + WHITESPACE + (patternRule ? patternBuildRuleDependencies : buildRuleDependencies);
        // No duplicates in a makefile. If we already have this rule, don't add
        // it or the commands to build the file
        if (getRuleList().contains(buildRule)) {
            // TODO: Should we assert that this is a pattern rule?
        } else {
            getRuleList().add(buildRule);
            // Echo starting message
            buffer.append(buildRule).append(NEWLINE);
            buffer.append(TAB).append(AT).append(escapedEcho(MESSAGE_START_FILE + WHITESPACE + IN_MACRO));
            buffer.append(TAB).append(AT).append(escapedEcho(tool.getAnnouncement()));
            // If the tool specifies a dependency calculator of
            // TYPE_BUILD_COMMANDS, ask whether
            // there are any pre-tool commands.
            if (depCommands != null) {
                String[] preToolCommands = depCommands.getPreToolDependencyCommands();
                if (preToolCommands != null && preToolCommands.length > 0) {
                    for (String preCmd : preToolCommands) {
                        try {
                            String resolvedCommand;
                            IBuildMacroProvider provider = ManagedBuildManager.getBuildMacroProvider();
                            if (!needExplicitRuleForFile) {
                                resolvedCommand = provider.resolveValueToMakefileFormat(preCmd, EMPTY_STRING,
                                        WHITESPACE, IBuildMacroProvider.CONTEXT_FILE,
                                        new FileContextData(sourceLocation, outputLocation, null, tool));
                            } else {
                                // if we need an explicit rule then don't use
                                // any builder
                                // variables, resolve everything to explicit
                                // strings
                                resolvedCommand = provider.resolveValue(preCmd, EMPTY_STRING, WHITESPACE,
                                        IBuildMacroProvider.CONTEXT_FILE,
                                        new FileContextData(sourceLocation, outputLocation, null, tool));
                            }
                            if (resolvedCommand != null)
                                buffer.append(resolvedCommand).append(NEWLINE);
                        } catch (BuildMacroException e) {
                            /* JABA is not going to write this code */
                        }
                    }
                }
            }
            // Generate the command line
            Vector<String> inputs = new Vector<String>();
            inputs.add(IN_MACRO);
            // Other additional inputs
            // Get any additional dependencies specified for the tool in other
            // InputType elements and AdditionalInput elements
            IPath[] addlInputPaths = getAdditionalResourcesForSource(tool);
            for (IPath addlInputPath : addlInputPaths) {
                // Translate the path from project relative to build directory
                // relative
                IPath addlPath = addlInputPath;
                if (!(addlPath.toString().startsWith("$("))) {
                    if (!addlPath.isAbsolute()) {
                        IPath tempPath = getPathForResource(project).append(addlPath);
                        if (tempPath != null) {
                            addlPath = ManagedBuildManager.calculateRelativePath(getTopBuildDir(), tempPath);
                        }
                    }
                }
                inputs.add(addlPath.toOSString());
            }
            String[] inputStrings = inputs.toArray(new String[inputs.size()]);
            String[] flags = null;
            // Get the tool command line options
            try {
                flags = tool.getToolCommandFlags(sourceLocation, outputLocation);
            } catch (BuildException ex) {
                // TODO add some routines to catch this
                flags = EMPTY_STRING_ARRAY;
            }
            // If we have a TYPE_BUILD_COMMANDS dependency generator, determine
            // if there are any options that
            // it wants added to the command line
            if (depCommands != null) {
                flags = addDependencyOptions(depCommands, flags);
            }
            IManagedCommandLineInfo cmdLInfo = null;
            String outflag = null;
            String outputPrefix = null;
            if (rcInfo instanceof IFileInfo || needExplicitRuleForFile || needExplicitDependencyCommands) {
                outflag = tool.getOutputFlag();
                outputPrefix = tool.getOutputPrefix();
                // Call the command line generator
                IManagedCommandLineGenerator cmdLGen = tool.getCommandLineGenerator();
                cmdLInfo = cmdLGen.generateCommandLineInfo(tool, cmd, flags, outflag, outputPrefix,
                        OUT_MACRO + otherPrimaryOutputs, inputStrings, getToolCommandLinePattern(config, tool));
            } else {
                outflag = tool.getOutputFlag();
                outputPrefix = tool.getOutputPrefix();
                // Call the command line generator
                cmdLInfo = generateToolCommandLineInfo(tool, inputExtension, flags, outflag, outputPrefix,
                        OUT_MACRO + otherPrimaryOutputs, inputStrings, sourceLocation, outputLocation);
            }
            // The command to build
            String buildCmd;
            if (cmdLInfo != null) {
                buildCmd = cmdLInfo.getCommandLine();
            } else {
                StringBuffer buildFlags = new StringBuffer();
                for (String flag : flags) {
                    if (flag != null) {
                        buildFlags.append(flag).append(WHITESPACE);
                    }
                }
                buildCmd = cmd + WHITESPACE + buildFlags.toString().trim() + WHITESPACE + outflag + WHITESPACE
                        + outputPrefix + OUT_MACRO + otherPrimaryOutputs + WHITESPACE + IN_MACRO;
            }
            // resolve any remaining macros in the command after it has been
            // generated
            try {
                String resolvedCommand;
                IBuildMacroProvider provider = ManagedBuildManager.getBuildMacroProvider();
                if (!needExplicitRuleForFile) {
                    resolvedCommand = provider.resolveValueToMakefileFormat(buildCmd, EMPTY_STRING, WHITESPACE,
                            IBuildMacroProvider.CONTEXT_FILE,
                            new FileContextData(sourceLocation, outputLocation, null, tool));
                } else {
                    // if we need an explicit rule then don't use any builder
                    // variables, resolve everything to explicit strings
                    resolvedCommand = provider.resolveValue(buildCmd, EMPTY_STRING, WHITESPACE,
                            IBuildMacroProvider.CONTEXT_FILE,
                            new FileContextData(sourceLocation, outputLocation, null, tool));
                }
                if ((resolvedCommand = resolvedCommand.trim()).length() > 0)
                    buildCmd = resolvedCommand;
            } catch (BuildMacroException e) {
                /* JABA is not going to write this code */
            }
            // buffer.append(TAB).append(AT).append(escapedEcho(buildCmd));
            // buffer.append(TAB).append(AT).append(buildCmd);
            // JABA add sketch.prebuild and postbouild if needed
            if ("sloeber.ino".equals(fileName)) {
                ICConfigurationDescription confDesc = ManagedBuildManager.getDescriptionForConfiguration(config);
                String sketchPrebuild = io.sloeber.core.common.Common.getBuildEnvironmentVariable(confDesc,
                        "sloeber.sketch.prebuild", new String(), true);
                String sketchPostBuild = io.sloeber.core.common.Common.getBuildEnvironmentVariable(confDesc,
                        "sloeber.sketch.postbuild", new String(), true);
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

            // Determine if there are any dependencies to calculate
            if (doDepGen) {
                // Get the dependency rule out of the generator
                String[] depCmds = null;
                if (oldDepGen != null) {
                    depCmds = new String[1];
                    depCmds[0] = oldDepGen.getDependencyCommand(resource, ManagedBuildManager.getBuildInfo(project));
                } else {
                    if (depCommands != null) {
                        depCmds = depCommands.getPostToolDependencyCommands();
                    }
                }
                if (depCmds != null) {
                    for (String depCmd : depCmds) {
                        // Resolve any macros in the dep command after it has
                        // been generated.
                        // Note: do not trim the result because it will strip
                        // out necessary tab characters.
                        buffer.append(WHITESPACE).append(LOGICAL_AND).append(WHITESPACE).append(LINEBREAK);
                        try {
                            if (!needExplicitRuleForFile) {
                                depCmd = ManagedBuildManager.getBuildMacroProvider().resolveValueToMakefileFormat(
                                        depCmd, EMPTY_STRING, WHITESPACE, IBuildMacroProvider.CONTEXT_FILE,
                                        new FileContextData(sourceLocation, outputLocation, null, tool));
                            } else {
                                depCmd = ManagedBuildManager.getBuildMacroProvider().resolveValue(depCmd, EMPTY_STRING,
                                        WHITESPACE, IBuildMacroProvider.CONTEXT_FILE,
                                        new FileContextData(sourceLocation, outputLocation, null, tool));
                            }
                        } catch (BuildMacroException e) {
                            /* JABA is not going to do this */
                        }
                        buffer.append(depCmd);
                    }
                }
            }
            // Echo finished message
            buffer.append(NEWLINE);
            buffer.append(TAB).append(AT).append(escapedEcho(MESSAGE_FINISH_FILE + WHITESPACE + IN_MACRO));
            buffer.append(TAB).append(AT).append(ECHO_BLANK_LINE).append(NEWLINE);
        }
        // Determine if there are calculated dependencies
        IPath[] addlDeps = null;
        IPath[] addlTargets = null;
        String calculatedDependencies = null;
        boolean addedDepLines = false;
        String depLine;
        if (oldDepGen != null && oldDepGen.getCalculatorType() != IManagedDependencyGeneratorType.TYPE_COMMAND) {
            addlDeps = oldCalculateDependenciesForSource(oldDepGen, tool, relativePath, resource);
        } else {
            if (depGen != null && depGen.getCalculatorType() == IManagedDependencyGeneratorType.TYPE_CUSTOM) {
                if (depInfo instanceof IManagedDependencyCalculator) {
                    IManagedDependencyCalculator depCalculator = (IManagedDependencyCalculator) depInfo;
                    addlDeps = calculateDependenciesForSource(depCalculator);
                    addlTargets = depCalculator.getAdditionalTargets();
                }
            }
        }
        if (addlDeps != null && addlDeps.length > 0) {
            calculatedDependencies = "";
            for (IPath addlDep : addlDeps) {
                calculatedDependencies += WHITESPACE + escapeWhitespaces(addlDep.toOSString());
            }
        }
        if (calculatedDependencies != null) {
            depLine = primaryOutputName + COLON + calculatedDependencies + NEWLINE;
            if (!getDepLineList().contains(depLine)) {
                getDepLineList().add(depLine);
                addedDepLines = true;
                buffer.append(depLine);
            }
        }

        for (IPath curOutput : enumeratedOutputs) {
            depLine = escapeWhitespaces(curOutput.toOSString()) + COLON + WHITESPACE + primaryOutputName;
            if (calculatedDependencies != null)
                depLine += calculatedDependencies;
            depLine += NEWLINE;
            if (!getDepLineList().contains(depLine)) {
                getDepLineList().add(depLine);
                addedDepLines = true;
                buffer.append(depLine);
            }
        }
        if (addedDepLines) {
            buffer.append(NEWLINE);
        }
        // If we are using a dependency calculator of type
        // TYPE_PREBUILD_COMMANDS,
        // get the rule to build the dependency file
        if (depPreBuild != null && depFiles != null) {
            addedDepLines = false;
            String[] preBuildCommands = depPreBuild.getDependencyCommands();
            if (preBuildCommands != null) {
                depLine = "";
                // Can we use a pattern rule?
                patternRule = !isItLinked && !needExplicitRuleForFile && depPreBuild.areCommandsGeneric();
                // Begin building the rule
                for (int i = 0; i < depFiles.length; i++) {
                    if (i > 0)
                        depLine += WHITESPACE;
                    if (patternRule) {
                        optDotExt = EMPTY_STRING;
                        String depExt = depFiles[i].getFileExtension();
                        if (depExt != null && depExt.length() > 0)
                            optDotExt = DOT + depExt;
                        depLine += escapeWhitespaces(relativePath + WILDCARD + optDotExt);
                    } else {
                        depLine += escapeWhitespaces((depFiles[i]).toOSString());
                    }
                }
                depLine += COLON + WHITESPACE + (patternRule ? patternBuildRuleDependencies : buildRuleDependencies);
                if (!getDepRuleList().contains(depLine)) {
                    getDepRuleList().add(depLine);
                    addedDepLines = true;
                    buffer.append(depLine).append(NEWLINE);
                    buffer.append(TAB).append(AT)
                            .append(escapedEcho(MESSAGE_START_DEPENDENCY + WHITESPACE + OUT_MACRO));
                    for (String preBuildCommand : preBuildCommands) {
                        depLine = preBuildCommand;
                        // Resolve macros
                        try {
                            if (!needExplicitRuleForFile) {
                                depLine = ManagedBuildManager.getBuildMacroProvider().resolveValueToMakefileFormat(
                                        depLine, EMPTY_STRING, WHITESPACE, IBuildMacroProvider.CONTEXT_FILE,
                                        new FileContextData(sourceLocation, outputLocation, null, tool));
                            } else {
                                depLine = ManagedBuildManager.getBuildMacroProvider().resolveValue(depLine,
                                        EMPTY_STRING, WHITESPACE, IBuildMacroProvider.CONTEXT_FILE,
                                        new FileContextData(sourceLocation, outputLocation, null, tool));
                            }
                        } catch (BuildMacroException e) {
                            // JABA is not going to write this code
                        }
                        // buffer.append(TAB + AT + escapedEcho(depLine));
                        // buffer.append(TAB + AT + depLine + NEWLINE);
                        buffer.append(TAB).append(depLine).append(NEWLINE);
                    }
                }
                if (addedDepLines) {
                    buffer.append(TAB).append(AT).append(ECHO_BLANK_LINE).append(NEWLINE);
                }
            }
        }
    }

    /*************************************************************************
     * F R A G M E N T (subdir.mk) M A K E F I L E M E T H O D S
     ************************************************************************/
    /**
     * Returns a <code>StringBuffer</code> containing the comment(s) for a fragment
     * makefile (subdir.mk).
     */

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
     * Returns any additional resources specified for the tool in other InputType
     * elements and AdditionalInput elements
     */
    protected IPath[] getAdditionalResourcesForSource(ITool tool) {
        IProject project = getProject();
        List<IPath> allRes = new ArrayList<>();
        IInputType[] types = tool.getInputTypes();
        for (IInputType type : types) {
            // Additional resources come from 2 places.
            // 1. From AdditionalInput childen
            IPath[] res = type.getAdditionalResources();
            for (IPath re : res) {
                allRes.add(re);
            }
            // 2. From InputTypes that other than the primary input type
            if (!type.getPrimaryInput() && type != tool.getPrimaryInputType()) {
                String var = type.getBuildVariable();
                if (var != null && var.length() > 0) {
                    allRes.add(Path.fromOSString("$(" + type.getBuildVariable() + ")"));
                } else {
                    // Use file extensions
                    String[] typeExts = type.getSourceExtensions(tool);
                    for (IResource projectResource : caller.projectResources) {
                        if (projectResource.getType() == IResource.FILE) {
                            String fileExt = projectResource.getFileExtension();
                            if (fileExt == null) {
                                fileExt = "";
                            }
                            for (String typeExt : typeExts) {
                                if (fileExt.equals(typeExt)) {
                                    allRes.add(projectResource.getProjectRelativePath());
                                    break;
                                }
                            }
                        }
                    }
                }
                // If an assignToOption has been specified, set the value of the
                // option to the inputs
                IOption assignToOption = tool.getOptionBySuperClassId(type.getAssignToOptionId());
                IOption option = tool.getOptionBySuperClassId(type.getOptionId());
                if (assignToOption != null && option == null) {
                    try {
                        int optType = assignToOption.getValueType();
                        IResourceInfo rcInfo = tool.getParentResourceInfo();
                        if (rcInfo != null) {
                            if (optType == IOption.STRING) {
                                String optVal = "";
                                for (int j = 0; j < allRes.size(); j++) {
                                    if (j != 0) {
                                        optVal += " ";
                                    }
                                    String resPath = allRes.get(j).toString();
                                    if (!resPath.startsWith("$(")) {
                                        IResource addlResource = project.getFile(resPath);
                                        if (addlResource != null) {
                                            IPath addlPath = addlResource.getLocation();
                                            if (addlPath != null) {
                                                resPath = ManagedBuildManager
                                                        .calculateRelativePath(getTopBuildDir(), addlPath).toString();
                                            }
                                        }
                                    }
                                    optVal += ManagedBuildManager
                                            .calculateRelativePath(getTopBuildDir(), Path.fromOSString(resPath))
                                            .toString();
                                }
                                ManagedBuildManager.setOption(rcInfo, tool, assignToOption, optVal);
                            } else if (optType == IOption.STRING_LIST || optType == IOption.LIBRARIES
                                    || optType == IOption.OBJECTS || optType == IOption.INCLUDE_FILES
                                    || optType == IOption.LIBRARY_PATHS || optType == IOption.LIBRARY_FILES
                                    || optType == IOption.MACRO_FILES) {
                                // TODO: do we need to do anything with undefs
                                // here?
                                // Note that the path(s) must be translated from
                                // project relative
                                // to top build directory relative
                                String[] paths = new String[allRes.size()];
                                for (int j = 0; j < allRes.size(); j++) {
                                    paths[j] = allRes.get(j).toString();
                                    if (!paths[j].startsWith("$(")) {
                                        IResource addlResource = project.getFile(paths[j]);
                                        if (addlResource != null) {
                                            IPath addlPath = addlResource.getLocation();
                                            if (addlPath != null) {
                                                paths[j] = ManagedBuildManager
                                                        .calculateRelativePath(getTopBuildDir(), addlPath).toString();
                                            }
                                        }
                                    }
                                }
                                ManagedBuildManager.setOption(rcInfo, tool, assignToOption, paths);
                            } else if (optType == IOption.BOOLEAN) {
                                boolean b = false;
                                if (allRes.size() > 0)
                                    b = true;
                                ManagedBuildManager.setOption(rcInfo, tool, assignToOption, b);
                            } else if (optType == IOption.ENUMERATED || optType == IOption.TREE) {
                                if (allRes.size() > 0) {
                                    String s = allRes.get(0).toString();
                                    ManagedBuildManager.setOption(rcInfo, tool, assignToOption, s);
                                }
                            }
                            allRes.clear();
                        }
                    } catch (BuildException ex) {
                        /* JABA is not going to write this code */
                    }
                }
            }
        }
        return allRes.toArray(new IPath[allRes.size()]);
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

    /**
     * Returns the dependency <code>IPath</code>s for this invocation of the tool
     * with the specified source file
     *
     * @param depGen
     *            the dependency calculator
     * @param tool
     *            tool used to build the source file
     * @param relativePath
     *            build output directory relative path of the current output
     *            directory
     * @param resource
     *            source file to scan for dependencies
     * @return Vector of IPaths that are relative to the build directory
     */
    private IPath[] oldCalculateDependenciesForSource(IManagedDependencyGenerator depGen, ITool tool,
            String relativePath, IResource resource) {
        IProject project = getProject();
        Vector<IPath> deps = new Vector<IPath>();
        int type = depGen.getCalculatorType();
        switch (type) {
        case IManagedDependencyGeneratorType.TYPE_INDEXER:
        case IManagedDependencyGeneratorType.TYPE_EXTERNAL:
            IResource[] res = depGen.findDependencies(resource, project);
            if (res != null) {
                for (IResource re : res) {
                    IPath dep = null;
                    if (re != null) {
                        IPath addlPath = re.getLocation();
                        if (addlPath != null) {
                            dep = ManagedBuildManager.calculateRelativePath(getTopBuildDir(), addlPath);
                        }
                    }
                    if (dep != null) {
                        deps.add(dep);
                    }
                }
            }
            break;
        case IManagedDependencyGeneratorType.TYPE_NODEPS:
        default:
            break;
        }
        return deps.toArray(new IPath[deps.size()]);
    }

    /**
     * Returns the dependency <code>IPath</code>s relative to the build directory
     *
     * @param depCalculator
     *            the dependency calculator
     * @return IPath[] that are relative to the build directory
     */
    private IPath[] calculateDependenciesForSource(IManagedDependencyCalculator depCalculator) {
        IProject project = getProject();
        IPath[] addlDeps = depCalculator.getDependencies();
        if (addlDeps != null) {
            for (int i = 0; i < addlDeps.length; i++) {
                if (!addlDeps[i].isAbsolute()) {
                    // Convert from project relative to build directory relative
                    IPath absolutePath = project.getLocation().append(addlDeps[i]);
                    addlDeps[i] = ManagedBuildManager.calculateRelativePath(getTopBuildDir(), absolutePath);
                }
            }
        }
        return addlDeps;
    }

}
