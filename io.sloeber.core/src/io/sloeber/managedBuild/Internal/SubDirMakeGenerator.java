package io.sloeber.managedBuild.Internal;

import static io.sloeber.managedBuild.Internal.ManagebBuildCommon.*;
//import static io.sloeber.managedBuild.Internal.ManagebBuildCommon.*;
import static io.sloeber.managedBuild.Internal.ManagedBuildConstants.*;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.cdt.managedbuilder.core.IConfiguration;
import org.eclipse.cdt.managedbuilder.core.IFileInfo;
import org.eclipse.cdt.managedbuilder.core.IInputType;
import org.eclipse.cdt.managedbuilder.core.IOutputType;
import org.eclipse.cdt.managedbuilder.core.IResourceInfo;
import org.eclipse.cdt.managedbuilder.core.ITool;
import org.eclipse.cdt.managedbuilder.internal.core.ManagedMakeMessages;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import io.sloeber.managedBuild.api.IManagedOutputNameProviderJaba;

public class SubDirMakeGenerator {
    private ArduinoGnuMakefileGenerator caller;
    private Set<MakeRule> myMakeRules = new LinkedHashSet<>();
    private IContainer myModule;

    SubDirMakeGenerator(ArduinoGnuMakefileGenerator theCaller, IContainer module) {
        caller = theCaller;
        myModule = module;
        getMakeRules();
    }

    public Set<String> getDependecyMacros() {
        HashSet<String> ret = new LinkedHashSet<>();
        for (MakeRule curMakeRule : myMakeRules) {
            ret.addAll(curMakeRule.getDependecyMacros());
        }
        return ret;
    }

    public Set<String> getAllMacros() {
        HashSet<String> ret = new LinkedHashSet<>();
        for (MakeRule curMakeRule : myMakeRules) {
            ret.addAll(curMakeRule.getAllMacros());
        }
        return ret;
    }

    public Set<String> getTargetMacros() {
        HashSet<String> ret = new LinkedHashSet<>();
        for (MakeRule curMakeRule : myMakeRules) {
            ret.addAll(curMakeRule.getTargetMacros());
        }
        return ret;
    }

    public Set<String> getPrerequisiteMacros() {
        HashSet<String> ret = new LinkedHashSet<>();
        for (MakeRule curMakeRule : myMakeRules) {
            ret.addAll(curMakeRule.getPrerequisiteMacros());
        }
        return ret;
    }

    public Set<IFile> getTargetFiles() {
        Set<IFile> ret = new LinkedHashSet<>();
        for (MakeRule curMakeRule : myMakeRules) {
            ret.addAll(curMakeRule.getTargetFiles());
        }
        return ret;
    }

    public Set<IFile> getDependencyFiles() {
        Set<IFile> ret = new LinkedHashSet<>();
        for (MakeRule curMakeRule : myMakeRules) {
            ret.addAll(curMakeRule.getDependencyFiles());
        }
        return ret;
    }

    public Map<IOutputType, List<IFile>> getTargets() {
        Map<IOutputType, List<IFile>> ret = new LinkedHashMap<>();
        for (MakeRule curMakeRule : myMakeRules) {
            ret.putAll(curMakeRule.getTargets());
        }
        return ret;
    }

    private IPath getBuildWorkingDir() {
        return caller.getBuildWorkingDir();
    }

    private IFile getTopBuildDir() {
        return caller.getTopBuildDir();
    }

    private IConfiguration getConfig() {
        return caller.getConfig();
    }

    private IProject getProject() {
        return caller.getProject();
    }

    /*************************************************************************
     * M A K E F I L E S P O P U L A T I O N M E T H O D S
     ************************************************************************/
    /**
     * This method generates a "fragment" make file (subdir.mk). One of these is
     * generated for each project directory/subdirectory that contains source files.
     */
    public void generateMakefile() throws CoreException {
        //create the parent folder on disk and file in eclispe
        IProject project = getProject();
        IPath buildRoot = getBuildWorkingDir();
        if (buildRoot == null) {
            return;
        }
        IPath moduleOutputPath = buildRoot.append(myModule.getProjectRelativePath());
        IFile modMakefile = project.getFile(moduleOutputPath.append(MODFILE_NAME));

        //generate the file content
        StringBuffer makeBuf = addDefaultHeader();
        makeBuf.append(GenerateMacros());
        makeBuf.append(GenerateRules(getConfig()));

        // Save the files
        save(makeBuf, modMakefile);
    }

    private StringBuffer GenerateMacros() {
        StringBuffer buffer = new StringBuffer();
        IFile buildRoot = getTopBuildDir();
        buffer.append(NEWLINE);
        buffer.append(COMMENT_SYMBOL).append(WHITESPACE).append(ManagedMakeMessages.getResourceString(MOD_VARS))
                .append(NEWLINE);
        HashSet<String> macroNames = new HashSet<>();
        for (MakeRule makeRule : myMakeRules) {
            macroNames.addAll(makeRule.getAllMacros());
        }
        for (String macroName : macroNames) {
            HashSet<IFile> files = new HashSet<>();
            for (MakeRule makeRule : myMakeRules) {
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

    private StringBuffer GenerateRules(IConfiguration config) {
        StringBuffer buffer = new StringBuffer();
        buffer.append(NEWLINE);
        buffer.append(COMMENT_SYMBOL).append(WHITESPACE).append(ManagedMakeMessages.getResourceString(MOD_RULES))
                .append(NEWLINE);

        for (MakeRule makeRule : myMakeRules) {
            buffer.append(makeRule.getRule(getProject(), getTopBuildDir(), config));
        }

        return buffer;
    }

    //Get the rules for the source files
    private void getMakeRules() {
        myMakeRules.clear();
        IConfiguration config = getConfig();
        IProject project = getProject();

        // Visit the resources in this folder 
        try {
            for (IResource resource : myModule.members()) {
                if (resource.getType() != IResource.FILE) {
                    //only handle files
                    continue;
                }
                IFile inputFile = (IFile) resource;
                IPath rcProjRelPath = inputFile.getProjectRelativePath();
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
                if (tool == null) {
                    continue;
                }

                // Generate the rule to build this source file
                //TOFIX check wether this tool can handle this file
                IInputType inputType = tool.getPrimaryInputType();
                if (inputType == null) {
                    inputType = tool.getInputType(ext);
                }

                for (IOutputType outputType : tool.getOutputTypes()) {
                    IManagedOutputNameProviderJaba nameProvider = getJABANameProvider(outputType);
                    if (nameProvider == null) {
                        continue;
                    }
                    IPath outputFile = nameProvider.getOutputName(getProject(), config, tool,
                            resource.getProjectRelativePath());
                    if (outputFile != null) {
                        //We found a tool that provides a outputfile for our source file
                        //TOFIX if this is a multiple to one we should only create one MakeRule
                        IPath correctOutputPath = new Path(config.getName()).append(outputFile);
                        MakeRule newMakeRule = new MakeRule(caller, tool, inputType, inputFile, outputType,
                                project.getFile(correctOutputPath));

                        myMakeRules.add(newMakeRule);
                    }
                }
            }
        } catch (CoreException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
