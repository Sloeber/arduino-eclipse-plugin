package it.baeyens.arduino.tools;

/** this package makes the .ino.cpp file.
 * the .ino.cpp file includes all include directives and definitions in all the ino and pde files
 * it also includes a include statement for all the ino and pde files themelves
 * This way compiling the ino.cpp file compiles all ino and pde files in 1 file with declarations on top just like arduino ide does
 * 
 * the custom managed build system delivered with the plugin ignores the ino and pde files
 * this way the ino and pde files are only build once 
 * 
 * because I do not touch the ino and pde files the references returned by the toolchain
 * are still perfectly valid removing the need for post processing
 * 
 * Arduino ide ignores files starting with a . making the solution 100% compatible between arduino IDE and eclipse
 * 
 * in standard configuration eclipse does not show the .ino.cpp file in the project explorer making the solution nice and clean from a visual perspective.
 * 
 * I'm currently aware of 1 drawbacks of this solution 
 * If you have a file called .ino.cpp already in your project that file will be overwritten.
 */

import java.io.ByteArrayInputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.cdt.core.index.IIndex;
import org.eclipse.cdt.core.model.CoreModel;
import org.eclipse.cdt.core.model.ICProject;
import org.eclipse.cdt.core.model.IInclude;
import org.eclipse.cdt.core.model.ITranslationUnit;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTFunctionDefinition;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import it.baeyens.arduino.common.ArduinoConst;

@SuppressWarnings("restriction")
public class PdePreprocessor {
    private static String tempFile = ".ino.cpp"; //$NON-NLS-1$

    public static void processProject(IProject iProject) throws CoreException {
	// first write some standard bla bla
	final String NEWLINE = ArduinoConst.NEWLINE;
	String body = ArduinoConst.EMPTY_STRING;
	String includeHeaderPart = "#include \"Arduino.h\"" + NEWLINE; //$NON-NLS-1$
	String includeCodePart = NEWLINE;
	String header = "//This is a automatic generated file" + NEWLINE; //$NON-NLS-1$
	header += "//Please do not modify this file" + NEWLINE; //$NON-NLS-1$
	header += "//If you touch this file your change will be overwritten during the next build" + NEWLINE; //$NON-NLS-1$
	header += "//This file has been generated on "; //$NON-NLS-1$
	header += new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()); //$NON-NLS-1$
	header += NEWLINE;
	header += NEWLINE;
	ICProject tt = CoreModel.getDefault().create(iProject);
	IIndex index = CCorePlugin.getIndexManager().getIndex(tt);
	try {
	    index.acquireReadLock();

	    // take all the files in the project
	    IResource allResources[] = iProject.members(0);// .getFolder("").members(0);
	    int numInoFiles = 0;
	    for (IResource curResource : allResources) {
		String extension = curResource.getFileExtension();
		// only process .pde and .ino files
		if (extension != null && ((extension.equals("pde") || extension.equals("ino")))) { //$NON-NLS-1$ //$NON-NLS-2$
		    numInoFiles++;
		    String addLine;
		    if (curResource.isLinked()) {
			addLine = "#include \"" + curResource.getLocation() + "\"" + NEWLINE; //$NON-NLS-1$ //$NON-NLS-2$
		    } else {
			addLine = "#include \"" + curResource.getName() + "\"" + NEWLINE; //$NON-NLS-1$ //$NON-NLS-2$
		    }
		    // if the name of the ino/pde file matches the project put
		    // the file in front
		    // Otherwise add it to the end
		    if (curResource.getName().equals(iProject.getName() + "." + extension)) { //$NON-NLS-1$
			includeCodePart = addLine + includeCodePart;
		    } else {
			includeCodePart += addLine;
		    }

		    IPath path = curResource.getFullPath();

		    // check whether the indexer is properly configured.
		    IFile file = ResourcesPlugin.getWorkspace().getRoot().getFile(path);
		    ITranslationUnit tu = (ITranslationUnit) CoreModel.getDefault().create(file);
		    if (tu == null) {
			// the indexer is not properly configured so drop a
			// error in the file
			body += NEWLINE;
			body += "#error the file: " + curResource.getName() //$NON-NLS-1$
				+ " is not found in the indexer though it exists on the file system." //$NON-NLS-1$
				+ NEWLINE;
			body += "#error this is probably due to a bad eclipse configuration : ino and pde are not marked as c++ file." //$NON-NLS-1$
				+ NEWLINE;
			body += "#error please check whether *.ino and *.pde are marked as C++ source code in windows->preferences->C/C++->file types." //$NON-NLS-1$
				+ NEWLINE;
		    } else {
			// add declarations made in ino files.
			IASTTranslationUnit asttu = tu.getAST(index,
				ITranslationUnit.AST_SKIP_FUNCTION_BODIES | ITranslationUnit.AST_SKIP_ALL_HEADERS);
			IASTNode astNodes[] = asttu.getChildren();
			for (IASTNode astNode : astNodes) {
			    if (astNode instanceof CPPASTFunctionDefinition) {
				String addString = astNode.getRawSignature();
				addString = addString.replaceAll("\r\n", NEWLINE); //$NON-NLS-1$
				addString = addString.replaceAll("\r", NEWLINE); //$NON-NLS-1$
				addString = addString.replaceAll("//[^\n]+\n", " "); //$NON-NLS-1$ //$NON-NLS-2$
				addString = addString.replaceAll("\n", " "); //$NON-NLS-1$ //$NON-NLS-2$
				addString = addString.replaceAll("\\{.+\\}", ""); //$NON-NLS-1$//$NON-NLS-2$
				if (addString.contains("=")) { //$NON-NLS-1$
				    // ignore when there are assignments in the
				    // declaration
				} else {
				    body += addString + ';' + NEWLINE;
				}

			    }
			}
			// list all includes found in the source files.
			IInclude includes[] = tu.getIncludes();
			for (IInclude include : includes) {
			    includeHeaderPart += include.getSource();
			    includeHeaderPart += NEWLINE;
			}
		    }
		}
	    }
	    body = body + NEWLINE;

	    // delete the generated .ino.cpp file this is to cope with people
	    // renaming the ino files to cpp files removing the need for
	    // .ino.cpp file
	    if (numInoFiles == 0) {
		IResource inofile = iProject.findMember(tempFile);
		if (inofile != null) {
		    inofile.delete(true, null);
		}
		return;
	    }

	    // concatenate the parts and make the .ino.cpp file
	    String output = header + includeHeaderPart + body + includeCodePart;
	    ArduinoHelpers.addFileToProject(iProject, new Path(tempFile), new ByteArrayInputStream(output.getBytes()),
		    null);
	    index.releaseReadLock();
	} catch (InterruptedException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	}
    }
}
