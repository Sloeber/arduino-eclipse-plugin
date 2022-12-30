package io.sloeber.autoBuild.Internal;

import static io.sloeber.autoBuild.Internal.ManagebBuildCommon.*;
import static io.sloeber.autoBuild.Internal.ManagedBuildConstants.*;
import static io.sloeber.autoBuild.core.Messages.*;

import java.util.Collection;
import java.util.Set;

//import org.eclipse.cdt.managedbuilder.core.IConfiguration;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;

import io.sloeber.autoBuild.api.IConfiguration;

public class SrcMakeGenerator {

    static public void generateSourceMakefile(IProject project, IConfiguration config, Set<String> macroNames,
            Collection<IContainer> subDirs) throws CoreException {
        // Add the comment
        StringBuffer buffer = addDefaultHeader();
        // Add the macros to the makefile
        for (String macroName : macroNames) {
            buffer.append(macroName).append(WHITESPACE).append(":=").append(WHITESPACE).append(NEWLINE); //$NON-NLS-1$
        }
        // Add a list of subdirectories to the makefile
        buffer.append(NEWLINE);
        // Add the comment
        buffer.append(COMMENT_SYMBOL).append(WHITESPACE).append(MakefileGenerator_comment_module_list).append(NEWLINE);
        buffer.append("SUBDIRS := ").append(LINEBREAK); //$NON-NLS-1$
        // Get all the module names
        for (IResource container : subDirs) {
            if (container.getFullPath() == project.getFullPath()) {
                buffer.append(DOT).append(WHITESPACE).append(LINEBREAK);
            } else {
                IPath path = container.getProjectRelativePath();
                buffer.append(escapeWhitespaces(path.toOSString())).append(WHITESPACE).append(LINEBREAK);
            }
        }
        buffer.append(NEWLINE);
        // Save the file
        IFile fileHandle = project.getFile(config.getName() + '/' + SRCSFILE_NAME);
        save(buffer, fileHandle);
    }

    /**
     * The makefile generator generates a Macro for each type of output, other than
     * final artifact, created by the build.
     *
     * @param fileHandle
     *            The file that should be populated with the output
     */
    public static void generateObjectsMakefile(IProject project, IConfiguration config, Set<String> outputMacros)
            throws CoreException {
        StringBuffer macroBuffer = new StringBuffer();
        macroBuffer.append(addDefaultHeader());

        for (String macroName : outputMacros) {
            macroBuffer.append(macroName).append(MAKE_EQUAL);
            macroBuffer.append(NEWLINE);
            macroBuffer.append(NEWLINE);
        }
        IFile fileHandle = project.getFile(config.getName() + '/' + OBJECTS_MAKFILE);
        save(macroBuffer, fileHandle);
    }

}
