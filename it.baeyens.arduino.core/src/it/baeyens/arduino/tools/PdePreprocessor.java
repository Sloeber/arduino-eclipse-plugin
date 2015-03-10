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

@SuppressWarnings("restriction")
public class PdePreprocessor {
    private static String tempFile = ".ino.cpp";

    public static void processProject(IProject iProject) throws CoreException {
	String body = "";
	String includeHeaderPart = "#include \"Arduino.h\"\n";
	String includeCodePart = "\n";
	String header = "//This is a automatic generated file\n";
	header += "//Please do not modify this file\n";
	header += "//If you touch this file your change will be overwritten during the next build\n";
	header += "//This file has been generated on ";
	header += new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
	header += "\n";
	header += "\n";
	ICProject tt = CoreModel.getDefault().create(iProject);
	IIndex index = CCorePlugin.getIndexManager().getIndex(tt);

	IResource allResources[] = iProject.members(0);// .getFolder("").members(0);
	int numInoFiles = 0;
	for (IResource curResource : allResources) {
	    String extension = curResource.getFileExtension();
	    if (extension != null && ((extension.equals("pde") || extension.equals("ino")))) {
		numInoFiles++;
		if (curResource.isLinked()) {
		    includeCodePart += "#include \"" + curResource.getLocation() + "\"\n";
		} else {
		    includeCodePart += "#include \"" + curResource.getName() + "\"\n";
		}

		IPath path = curResource.getFullPath();// ff.getFullPath().append(inoFile);

		IFile file = ResourcesPlugin.getWorkspace().getRoot().getFile(path);
		ITranslationUnit tu = (ITranslationUnit) CoreModel.getDefault().create(file);
		if (tu == null) {
		    body += "\n";
		    body += "#error the file: " + curResource.getName() + " is not found in the indexer though it exists on the file system.\n";
		    body += "#error this is probably due to a bad eclipse configuration : ino and pde are not marked as c++ file.\n";
		    body += "#error please check wether *.ino and *.pde are marked as C++ source code in windows->preferences->C/C++->file types.\n";
		} else {
		    IASTTranslationUnit asttu = tu.getAST(index, ITranslationUnit.AST_SKIP_FUNCTION_BODIES | ITranslationUnit.AST_SKIP_ALL_HEADERS);
		    IASTNode astNodes[] = asttu.getChildren();
		    for (IASTNode astNode : astNodes) {
			if (astNode instanceof CPPASTFunctionDefinition) {
			    String addString = astNode.getRawSignature();
			    addString = addString.replaceAll("\r\n", "\n");
			    addString = addString.replaceAll("\r", "\n");
			    addString = addString.replaceAll("//[^\n]+\n", " ");
			    addString = addString.replaceAll("\n", " ");
			    addString = addString.replaceAll("\\{.+\\}", "");
			    if (addString.contains("=")) {
				// ignore when there are assignements in the declaration
			    } else {
				body += addString + ";\n";
			    }

			}
		    }
		    IInclude includes[] = tu.getIncludes();
		    for (IInclude include : includes) {
			includeHeaderPart += include.getSource();
			includeHeaderPart += "\n";
		    }
		}
	    }
	}

	if (numInoFiles == 0) {
	    IResource inofile = iProject.findMember(tempFile);
	    if (inofile != null) {
		inofile.delete(true, null);
	    }
	    return;
	}
	// for (String inoFile : allInoFiles) {
	// IPath path = ff.getFullPath().append(inoFile);
	//
	// IFile file = ResourcesPlugin.getWorkspace().getRoot().getFile(path);
	// // Create translation unit for file
	// ITranslationUnit tu = (ITranslationUnit) CoreModel.getDefault().create(file);
	// if (tu == null) {
	// body += "\n";
	// body += "#error the file: " + inoFile + " is not found in the indexer though it exists on the file system.\n";
	// body += "#error this is probably due to a bad eclipse configuration : ino and pde are not marked as c++ file.\n";
	// body += "#error please check wether *.ino and *.pde are marked as C++ source code in windows->preferences->C/C++->file types.\n";
	// } else {
	// IASTTranslationUnit asttu = tu.getAST(index, ITranslationUnit.AST_SKIP_FUNCTION_BODIES | ITranslationUnit.AST_SKIP_ALL_HEADERS);
	// IASTNode astNodes[] = asttu.getChildren();
	// for (IASTNode astNode : astNodes) {
	// if (astNode instanceof CPPASTFunctionDefinition) {
	// // String debug = astNode.getRawSignature();
	// body += astNode.getRawSignature().replaceAll("//[^\n]+\n", " ").replaceAll("\n", " ").replaceAll("\\{.+\\}", "");
	// body += ";\n";
	// }
	// }
	// IInclude includes[] = tu.getIncludes();
	// for (IInclude include : includes) {
	// includePart += include.getSource();
	// includePart += "\n";
	// }
	// }
	// }
	body += "\n";
	// for (String inoFile : allInoFiles) {
	// includeCodePart += "#include \"" + inoFile + "\"\n";
	// }
	String output = header + includeHeaderPart + body + includeCodePart;
	ArduinoHelpers.addFileToProject(iProject, new Path(tempFile), new ByteArrayInputStream(output.getBytes()), null);

    }
}
