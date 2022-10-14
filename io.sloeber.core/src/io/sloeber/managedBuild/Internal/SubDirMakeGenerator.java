package io.sloeber.managedBuild.Internal;

import static io.sloeber.managedBuild.Internal.ManagebBuildCommon.*;
//import static io.sloeber.managedBuild.Internal.ManagebBuildCommon.*;
import static io.sloeber.managedBuild.Internal.ManagedBuildConstants.*;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

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
    public void populateFragmentMakefile(IContainer module) throws CoreException {
        //create the parent folder on disk and file in eclispe
        IProject project = getProject();
        IPath buildRoot = getBuildWorkingDir();
        if (buildRoot == null) {
            return;
        }
        IPath moduleOutputPath = buildRoot.append(module.getProjectRelativePath());
        caller.updateMonitor(ManagedMakeMessages.getFormattedString("MakefileGenerator.message.gen.source.makefile", //$NON-NLS-1$
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

}
