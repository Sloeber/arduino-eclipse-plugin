package io.sloeber.managedBuild.Internal;

import static io.sloeber.managedBuild.Internal.ManagebBuildCommon.*;
import static io.sloeber.managedBuild.Internal.ManagedBuildConstants.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.cdt.core.settings.model.util.IPathSettingsContainerVisitor;
import org.eclipse.cdt.core.settings.model.util.PathSettingsContainer;
import org.eclipse.cdt.managedbuilder.core.IOutputType;
import org.eclipse.cdt.managedbuilder.core.ITool;
import org.eclipse.cdt.managedbuilder.internal.core.ManagedMakeMessages;
import org.eclipse.cdt.managedbuilder.makegen.IManagedDependencyGeneratorType;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

public class SrcMakeGenerator {

    private ArduinoGnuMakefileGenerator caller;

    SrcMakeGenerator(ArduinoGnuMakefileGenerator theCaller) {
        caller = theCaller;
    }

    private IProject getProject() {
        return caller.getProject();
    }

    public void populateSourcesMakefile(IFile fileHandle, PathSettingsContainer toolInfos,
            Collection<IContainer> subDirs) throws CoreException {
        // Add the comment
        StringBuffer buffer = addDefaultHeader();
        // Determine the set of macros
        toolInfos.accept(new IPathSettingsContainerVisitor() {
            @Override
            public boolean visit(PathSettingsContainer container) {
                ToolInfoHolder h = (ToolInfoHolder) container.getValue();
                ITool[] buildTools = h.buildTools;
                HashSet<String> handledInputExtensions = new HashSet<>();
                String buildMacro;
                for (ITool buildTool : buildTools) {
                    if (buildTool.getCustomBuildStep())
                        continue;
                    // Add the known sources macros
                    String[] extensionsList = buildTool.getAllInputExtensions();
                    for (String ext : extensionsList) {
                        // create a macro of the form "EXTENSION_SRCS :="
                        String extensionName = ext;
                        if (!handledInputExtensions.contains(extensionName)) {
                            handledInputExtensions.add(extensionName);
                            buildMacro = getSourceMacroName(extensionName).toString();
                            if (!caller.buildSrcVars.containsKey(buildMacro)) {
                                caller.buildSrcVars.put(buildMacro, new ArrayList<IPath>());
                            }
                            // Add any generated dependency file macros
                            IManagedDependencyGeneratorType depType = buildTool
                                    .getDependencyGeneratorForExtension(extensionName);
                            if (depType != null) {
                                int calcType = depType.getCalculatorType();
                                if (calcType == IManagedDependencyGeneratorType.TYPE_COMMAND
                                        || calcType == IManagedDependencyGeneratorType.TYPE_BUILD_COMMANDS
                                        || calcType == IManagedDependencyGeneratorType.TYPE_PREBUILD_COMMANDS) {
                                    buildMacro = getDepMacroName(extensionName).toString();
                                    if (!caller.buildDepVars.containsKey(buildMacro)) {
                                        caller.buildDepVars.put(buildMacro, new ArduinoGnuDependencyGroupInfo(
                                                buildMacro,
                                                (calcType != IManagedDependencyGeneratorType.TYPE_PREBUILD_COMMANDS)));
                                    }
                                    if (!caller.buildOutVars.containsKey(buildMacro)) {
                                        caller.buildOutVars.put(buildMacro, new ArrayList<IPath>());
                                    }
                                }
                            }
                        }
                    }
                    // Add the specified output build variables
                    IOutputType[] outTypes = buildTool.getOutputTypes();
                    if (outTypes != null && outTypes.length > 0) {
                        for (IOutputType outputType : outTypes) {
                            buildMacro = outputType.getBuildVariable();
                            if (!caller.buildOutVars.containsKey(buildMacro)) {
                                caller.buildOutVars.put(buildMacro, new ArrayList<IPath>());
                            }
                        }
                    } else {
                        // For support of pre-CDT 3.0 integrations.
                        buildMacro = OBJS_MACRO;
                        if (!caller.buildOutVars.containsKey(buildMacro)) {
                            caller.buildOutVars.put(buildMacro, new ArrayList<IPath>());
                        }
                    }
                }
                return true;
            }
        });
        // Add the macros to the makefile
        for (Entry<String, List<IPath>> entry : caller.buildSrcVars.entrySet()) {
            String macroName = new Path(entry.getKey()).toOSString();
            buffer.append(macroName).append(WHITESPACE).append(":=").append(WHITESPACE).append(NEWLINE); //$NON-NLS-1$
        }
        Set<Entry<String, List<IPath>>> set = caller.buildOutVars.entrySet();
        for (Entry<String, List<IPath>> entry : set) {
            String macroName = new Path(entry.getKey()).toOSString();
            buffer.append(macroName).append(WHITESPACE).append(":=").append(WHITESPACE).append(NEWLINE); //$NON-NLS-1$
        }
        // Add a list of subdirectories to the makefile
        buffer.append(NEWLINE).append(addSubdirectories(subDirs));
        // Save the file
        save(buffer, fileHandle);
    }

    /*************************************************************************
     * S O U R C E S (sources.mk) M A K E F I L E M E T H O D S
     ************************************************************************/
    private StringBuffer addSubdirectories(Collection<IContainer> subDirs) {
        IProject project = getProject();
        StringBuffer buffer = new StringBuffer();
        // Add the comment
        buffer.append(COMMENT_SYMBOL).append(WHITESPACE).append(MOD_LIST_MESSAGE).append(NEWLINE);
        buffer.append("SUBDIRS := ").append(LINEBREAK); //$NON-NLS-1$
        // Get all the module names
        for (IResource container : subDirs) {
            caller.updateMonitor(ManagedMakeMessages.getFormattedString(
                    "MakefileGenerator.message.adding.source.folder", container.getFullPath().toOSString())); //$NON-NLS-1$
            // Check the special case where the module is the project root
            if (container.getFullPath() == project.getFullPath()) {
                buffer.append(DOT).append(WHITESPACE).append(LINEBREAK);
            } else {
                IPath path = container.getProjectRelativePath();
                buffer.append(escapeWhitespaces(path.toOSString())).append(WHITESPACE).append(LINEBREAK);
            }
        }
        buffer.append(NEWLINE);
        return buffer;
    }
}
