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
import java.io.File;
import java.io.FilenameFilter;
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
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

public class PdePreprocessor {
    private static String tempFile = ".ino.cpp";

    public static void processProject(IProject ff) throws CoreException {
	String body = "";
	String includePart = "#include \"Arduino.h\"\n";
	String header = "//This is a automatic generated file\n";
	header += "//Please do not modify this file\n";
	header += "//If you touch this file your change will be overwritten during the next build\n";
	header += "//This file has been generated on ";
	header += new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
	header += "\n";
	header += "\n";
	ICProject tt = CoreModel.getDefault().create(ff);
	IIndex index = CCorePlugin.getIndexManager().getIndex(tt);
	FilenameFilter inoFileFilter = new FilenameFilter() {

	    @Override
	    public boolean accept(File dir, String name) {
		if (name.endsWith(".pde"))
		    return true;
		return name.endsWith(".ino");
	    }
	};

	String allInoFiles[] = ff.getLocation().toFile().list(inoFileFilter);
	for (String inoFile : allInoFiles) {
	    IPath path = ff.getFullPath().append(inoFile);

	    IFile file = ResourcesPlugin.getWorkspace().getRoot().getFile(path);
	    // Create translation unit for file
	    ITranslationUnit tu = (ITranslationUnit) CoreModel.getDefault().create(file);
	    if (tu == null) {
		body += "\n";
		body += "#error the file: " + inoFile + " is not found in the indexer though it exists on the file system.\n";
		body += "#error this is probably due to a bad eclipse configuration : ino and pde are not marked as c++ file.\n";
		body += "#error please check wether *.ino and *.pde are marked as C++ source code in windows->preferences->C/C++->file types.\n";
	    } else {
		IASTTranslationUnit asttu = tu.getAST(index, ITranslationUnit.AST_SKIP_FUNCTION_BODIES | ITranslationUnit.AST_SKIP_ALL_HEADERS);
		IASTNode astNodes[] = asttu.getChildren();
		for (IASTNode astNode : astNodes) {
		    if (astNode instanceof CPPASTFunctionDefinition) {
			// String debug = astNode.getRawSignature();
			body += astNode.getRawSignature().replaceAll("//[^\n]+\n", " ").replaceAll("\n", " ").replaceAll("\\{.+\\}", "");
			body += ";\n";
		    }
		}
		IInclude includes[] = tu.getIncludes();
		for (IInclude include : includes) {
		    includePart += include.getSource();
		    includePart += "\n";
		}
	    }
	}
	body += "\n";
	for (String inoFile : allInoFiles) {
	    body += "#include \"" + inoFile + "\"\n";
	}
	String output = header + includePart + body;
	ArduinoHelpers.addFileToProject(ff, new Path(tempFile), new ByteArrayInputStream(output.getBytes()), null);

    }
}
