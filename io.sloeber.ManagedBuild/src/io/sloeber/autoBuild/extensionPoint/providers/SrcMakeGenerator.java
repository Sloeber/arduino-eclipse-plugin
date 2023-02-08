package io.sloeber.autoBuild.extensionPoint.providers;

import static io.sloeber.autoBuild.Internal.ManagedBuildConstants.*;
import static io.sloeber.autoBuild.core.Messages.*;
import static io.sloeber.autoBuild.extensionPoint.providers.AutoBuildCommon.*;
import static io.sloeber.autoBuild.integration.Const.*;

import java.util.Collection;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;

public class SrcMakeGenerator {

	static public void generateSourceMakefile(IFolder buildFolder,IProject project, Set<String> macroNames,
			Collection<IFolder> subDirs) throws CoreException {
		StringBuffer buffer = addDefaultHeader();
		for (String macroName : macroNames) {
			if (!macroName.isBlank()) {
				buffer.append(macroName).append(WHITESPACE).append(":=").append(WHITESPACE).append(NEWLINE); //$NON-NLS-1$
			}
		}
		// Add a list of subdirectories to the makefile
		buffer.append(NEWLINE);
		// Add the comment
		buffer.append(COMMENT_SYMBOL).append(WHITESPACE).append(MakefileGenerator_comment_module_list).append(NEWLINE);
		buffer.append("SUBDIRS := ").append(LINEBREAK); //$NON-NLS-1$

		for (IFolder container : subDirs) {
			if (container.getFullPath() == project.getFullPath()) {
				buffer.append(DOT).append(WHITESPACE).append(LINEBREAK);
			} else {
				IPath path = container.getProjectRelativePath();
				buffer.append(escapeWhitespaces(path.toOSString())).append(WHITESPACE).append(LINEBREAK);
			}
		}
		buffer.append(NEWLINE);
		// Save the file
		IFile fileHandle = buildFolder.getFile(SRCSFILE_NAME);
		save(buffer, fileHandle);
	}

	/**
	 * The makefile generator generates a Macro for each type of output, other than
	 * final artifact, created by the build.
	 *
	 * @param fileHandle The file that should be populated with the output
	 */
	public static void generateObjectsMakefile(IFolder buildFolder,IProject project,  Set<String> outputMacros)
			throws CoreException {
		StringBuffer macroBuffer = new StringBuffer();
		macroBuffer.append(addDefaultHeader());

		for (String macroName : outputMacros) {
			if (!macroName.isBlank()) {
				macroBuffer.append(macroName).append(MAKE_EQUAL);
				macroBuffer.append(NEWLINE);
				macroBuffer.append(NEWLINE);
			}
		}
		IFile fileHandle = buildFolder.getFile( OBJECTS_MAKFILE);
		save(macroBuffer, fileHandle);
	}

}
