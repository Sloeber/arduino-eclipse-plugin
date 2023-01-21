package io.sloeber.autoBuild.Internal;

import static io.sloeber.autoBuild.Internal.ManagebBuildCommon.*;
import static io.sloeber.autoBuild.Internal.ManagedBuildConstants.*;
import static io.sloeber.autoBuild.core.Messages.*;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.cdt.core.model.CoreModel;
import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.cdt.core.settings.model.ICProjectDescription;
//import org.eclipse.cdt.managedbuilder.core.IConfiguration;
//import org.eclipse.cdt.managedbuilder.core.IInputType;
//import org.eclipse.cdt.managedbuilder.core.IOutputType;
//import org.eclipse.cdt.managedbuilder.core.ITool;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;

import io.sloeber.autoBuild.extensionPoint.MakefileGenerator;
import io.sloeber.schema.api.IConfiguration;
import io.sloeber.schema.api.IInputType;
import io.sloeber.schema.api.IOutputType;
import io.sloeber.schema.api.ITool;

public class SubDirMakeGenerator {
    private MakefileGenerator caller;
    private Set<MakeRule> myMakeRules = new LinkedHashSet<>();
    private IFile myMakefile;

    public SubDirMakeGenerator(MakefileGenerator theCaller, IContainer module) {
        caller = theCaller;
        IProject project = getProject();
        IPath buildPath = getBuildPath();
        if (buildPath == null) {
            return;
        }
        IPath moduleOutputPath = buildPath.append(module.getProjectRelativePath());
        myMakefile = project.getFile(moduleOutputPath.append(MODFILE_NAME));
        getMakeRulesFromSourceFiles(module);
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

    public Set<MakeRule> getMakeRules() {
        return myMakeRules;
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

    public Map<IOutputType, Set<IFile>> getTargets() {
        Map<IOutputType, Set<IFile>> ret = new LinkedHashMap<>();
        for (MakeRule curMakeRule : myMakeRules) {
            ret.putAll(curMakeRule.getTargets());
        }
        return ret;
    }

    private IPath getBuildPath() {
        return caller.getBuildWorkingDir();
    }

    private IPath getBuildFolder() {
        return caller.getBuildFolder();
    }

    private IFolder getTopBuildDir() {
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
        // generate the file content
        StringBuffer makeBuf = addDefaultHeader();
        makeBuf.append(GenerateMacros());
        makeBuf.append(GenerateRules(getConfig()));

        // Save the files
        save(makeBuf, myMakefile);
    }

    private StringBuffer GenerateMacros() {
        StringBuffer buffer = new StringBuffer();
        IFolder buildRoot = getTopBuildDir();
        buffer.append(NEWLINE);
        buffer.append(COMMENT_SYMBOL).append(WHITESPACE).append(MakefileGenerator_comment_module_variables)
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
        buffer.append(COMMENT_SYMBOL).append(WHITESPACE).append(MakefileGenerator_comment_build_rule).append(NEWLINE);

        for (MakeRule makeRule : myMakeRules) {
            buffer.append(makeRule.getRule(getProject(), getTopBuildDir(), config));
        }

        return buffer;
    }

    // Get the rules for the source files
    private void getMakeRulesFromSourceFiles(IContainer module) {
        myMakeRules.clear();
        IConfiguration config = getConfig();
        IProject project = getProject();
        IPath buildPath = getBuildFolder();
        ICProjectDescription prjDesc = CoreModel.getDefault().getProjectDescription(project);
        ICConfigurationDescription confDesc = prjDesc.getConfigurationByName(config.getName());

        // Visit the resources in this folder
        try {
            for (IResource resource : module.members()) {
                if (resource.getType() != IResource.FILE) {
                    // only handle files
                    continue;
                }
                IFile inputFile = (IFile) resource;
                String ext = inputFile.getFileExtension();
                if (ext == null || ext.isBlank()) {
                    continue;
                }
                IPath rcProjRelPath = inputFile.getProjectRelativePath();
                if (!caller.isSource(rcProjRelPath)) {
                    // this resource is excluded from build
                    continue;
                }

                for (ITool tool : config.getTools()) {
                    for (IInputType inputType : tool.getInputTypes()) {
                        if (!inputType.isAssociatedWith(inputFile)) {
                            continue;
                        }
                        for (IOutputType outputType : tool.getOutputTypes()) {
                            IFile outputFile = outputType.getOutputName(inputFile, confDesc, inputType);
                            if (outputFile == null) {
                                continue;
                            }
                            // We found a tool that provides a outputfile for our source file
                            // TOFIX if this is a multiple to one we should only create one MakeRule
                            MakeRule newMakeRule = new MakeRule(tool, inputType, inputFile, outputType, outputFile);
                            newMakeRule.addDependencies(caller);

                            myMakeRules.add(newMakeRule);
                        }
                    }
                }

            }
        } catch (CoreException e) {
            e.printStackTrace();
        }
    }

    public boolean isEmpty() {
        return myMakeRules.size() == 0;
    }

}