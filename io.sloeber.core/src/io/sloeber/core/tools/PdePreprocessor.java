package io.sloeber.core.tools;

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
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.dom.ast.IASTDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTLinkageSpecification;
import org.eclipse.cdt.core.index.IIndex;
import org.eclipse.cdt.core.model.CoreModel;
import org.eclipse.cdt.core.model.ICElement;
import org.eclipse.cdt.core.model.ICProject;
import org.eclipse.cdt.core.model.IInclude;
import org.eclipse.cdt.core.model.IMacro;
import org.eclipse.cdt.core.model.ITranslationUnit;
import org.eclipse.cdt.core.model.IVariable;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTFunctionDefinition;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

@SuppressWarnings({ "nls", "restriction","unused" })
public class PdePreprocessor {
	private static String oldGeneratedFile = ".ino.cpp";// somethimes having the
														// file hidden is
														// annoying
	private static String generatedFile = "sloeber.ino.cpp";
	private static final String DEFINE_IN_ECLIPSE = "__IN_ECLIPSE__";
	private static final String NEWLINE = "\n";

	public static void processProject(boolean canSkip,IProject iProject) throws CoreException {
		deleteTheGeneratedFileInPreviousBersionsOfSloeber(iProject);

		// loop through all the files in the project to see we need to generate a file
		//This way we can avoid hitting the indexer when we use .cpp files
		List<IResource> allResources = new ArrayList<>();
		List<IResource> inoResources = new ArrayList<>();
		allResources.addAll(Arrays.asList(iProject.members(0)));
		for (IResource curResource : allResources) {
			String extension = curResource.getFileExtension();
			// only process .pde and .ino files
			if (extension != null && ((extension.equals("pde") || extension.equals("ino")))) {
				inoResources.add(curResource);
			}
		}


		if (inoResources.isEmpty()) {
			// delete the generated .ino.cpp file this is to cope with
			// renaming ino files to cpp files removing the need for
			// .ino.cpp file
			deleteTheGeneratedFile(iProject);
			return;
		}
		if(canSkip&&!CCorePlugin.getIndexManager().isIndexerIdle())return;
		ICProject tt = CoreModel.getDefault().create(iProject);
		IIndex index = CCorePlugin.getIndexManager().getIndex(tt);

		try {
			try {
				index.acquireReadLock();
			} catch (InterruptedException e) {
				// ignore
				e.printStackTrace();
				return;
			}
			String methodDeclarations = new String();
			String includeInoPart = NEWLINE;
			String header = "//This is a automatic generated file" + NEWLINE;
			header += "//Please do not modify this file" + NEWLINE;
			header += "//If you touch this file your change will be overwritten during the next build" + NEWLINE;
			header += "//This file has been generated on ";
			header += new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
			header += NEWLINE;
			header += NEWLINE;
			header += "#include \"Arduino.h\"" + NEWLINE;
			// loop through all the files in the project
			for (IResource curResource : inoResources) {

				// check whether the indexer is properly configured.
				IPath path = curResource.getFullPath();
				IFile file = ResourcesPlugin.getWorkspace().getRoot().getFile(path);
				ITranslationUnit tu = (ITranslationUnit) CoreModel.getDefault().create(file);
				if (tu == null) {
					methodDeclarations = extendBodyWithFileNotFund(methodDeclarations, curResource);
				} else {
					includeInoPart = extendIncludedInoPartForFile(includeInoPart, curResource);
					methodDeclarations = extendMethodDeclarationsForFile(methodDeclarations, index, tu);
					header = extendHeaderForFile(header, index, tu);
				}
			}

			writeTheGeneratedFile(iProject, header + NEWLINE + methodDeclarations + NEWLINE + includeInoPart);

		} finally {
			index.releaseReadLock();
		}
	}

	/**
	 * Add some operational stuff and write the file if changed
	 *
	 * @param iProject
	 *            the project for which the ino files have been parsed
	 * @param content
	 *            the ouput of the ino file parsing
	 * @throws CoreException
	 */
	private static void writeTheGeneratedFile(IProject iProject, String content) throws CoreException {

		// Make sure the file is not processed by Arduino IDE
		String newFileContent = "#ifdef " + DEFINE_IN_ECLIPSE + NEWLINE + content + NEWLINE + "#endif" + NEWLINE;
		String currentFileContent = null;
		try {
			currentFileContent = FileUtils
					.readFileToString(iProject.getFile(new Path(generatedFile)).getLocation().toFile());
		} catch (IOException e) {
			// This happens when the generated file does not yet exist
		}

		if (!newFileContent.equals(currentFileContent)) {
			IFile file = Helpers.addFileToProject(iProject, new Path(generatedFile),
					new ByteArrayInputStream(newFileContent.getBytes()), null, true);
			if (file != null) {
				file.setDerived(true, null);
			}
		}

	}

	private static String extendHeaderForFile(String header, IIndex index, ITranslationUnit tu) throws CoreException {
		String localHeader = header;
		// Locate All lines that are extern "C"
		HashMap<Integer, Integer> externCLines = new HashMap<>();
		IASTTranslationUnit astTuTest = tu.getAST(index, 0);
		IASTDeclaration[] topDeclaratons = astTuTest.getDeclarations();
		for (IASTDeclaration curTopDeclaration : topDeclaratons) {

			ICPPASTLinkageSpecification test = curTopDeclaration instanceof ICPPASTLinkageSpecification
					? (ICPPASTLinkageSpecification) curTopDeclaration
					: null;
			if (test != null) {
				if (test.getLiteral().equals("\"C\"")) {
					Path curFile = new Path(curTopDeclaration.getContainingFilename());
					if (curFile.equals(tu.getFile().getLocation())) {
						int startLine = test.getFileLocation().getStartingLineNumber();
						int endLine = test.getFileLocation().getEndingLineNumber();
						for (int curline = startLine; curline <= endLine; curline++) {
							externCLines.put(new Integer(curline), null);
						}
					}
				}
			}
		}

		// find the last line containing a include
		IInclude includes[] = tu.getIncludes();
		int lastHeaderLine = 0;
		for (IInclude include : includes) {
			int curHeaderLine = include.getSourceRange().getEndLine();
			lastHeaderLine = Math.max(lastHeaderLine, curHeaderLine);
		}

		// parse line by line until all includes have been parsed
		for (int curline = 1; curline <= lastHeaderLine; curline++) {
			ICElement curElement = tu.getElementAtLine(curline);
			if (curElement != null) {
				switch (curElement.getElementType()) {
				case ICElement.C_MACRO:
					IMacro curMacro = (IMacro) curElement;
					if (curMacro.isActive()) {
						localHeader += curMacro.getSource() + NEWLINE;
					}
					break;
				case ICElement.C_VARIABLE:
					IVariable curVardeclar = (IVariable) curElement;
					if (curVardeclar.isActive()) {
						String fullTypeName = curVardeclar.getTypeName();
						// ignore double arrays
						if (fullTypeName.indexOf('[') == fullTypeName.lastIndexOf('[')) {
							String typeName = fullTypeName.replace('[', ' ').replace(']', ' ').trim();
							String typeExtensions = fullTypeName.replace(typeName, "");
							localHeader += "extern " + typeName + " " + curVardeclar.getElementName() + typeExtensions
									+ ";" + NEWLINE;
						}
					}
					break;
				case ICElement.C_INCLUDE:
					IInclude curInclude = (IInclude) curElement;

					int curHeaderLine = curInclude.getSourceRange().getStartLine();
					if (curInclude.isActive()) {
						if (externCLines.containsKey(new Integer(curHeaderLine))) {
							localHeader += "extern \"C\" {" + NEWLINE;
							localHeader += curInclude.getSource() + NEWLINE;
							localHeader += "}" + NEWLINE;
						} else {

							localHeader += curInclude.getSource();
							localHeader += NEWLINE;
						}
					}
					break;
				}
			}
		}
		return localHeader;
	}

	// the indexer is not properly configured so drop a
	// error in the file
	private static String extendBodyWithFileNotFund(String body, IResource curResource) {
		String localBody = body + NEWLINE;
		localBody += "#error the file: " + curResource.getName()
				+ " is not found in the indexer though it exists on the file system." + NEWLINE;
		localBody += "#error this is probably due to a bad eclipse configuration : ino and pde are not marked as c++ file."
				+ NEWLINE;
		localBody += "#error please check whether *.ino and *.pde are marked as C++ source code in windows->preferences->C/C++->file types."
				+ NEWLINE;
		return localBody;
	}

	private static String extendMethodDeclarationsForFile(String body, IIndex index, ITranslationUnit tu)
			throws CoreException {
		// add declarations made in ino files.
		String localBody = body;
		IASTTranslationUnit asttu = tu.getAST(index,
				ITranslationUnit.AST_SKIP_FUNCTION_BODIES | ITranslationUnit.AST_SKIP_ALL_HEADERS);
		IASTNode astNodes[] = asttu.getChildren();
		for (IASTNode astNode : astNodes) {
			if (astNode instanceof CPPASTFunctionDefinition) {
				String addString = astNode.getRawSignature();
				addString = addString.replaceAll("\r\n", NEWLINE);
				addString = addString.replaceAll("\r", NEWLINE);
				addString = addString.replaceAll("//[^\n]+\n", " ");
				addString = addString.replaceAll("\n", " ");
				addString = addString.replaceAll("\\{.*\\}", "");
				if (addString.contains("=") || addString.contains("::")) {
					// ignore when there are assignments in the
					// declaration
					// or when it is a class function
				} else {
					localBody += addString + ';' + NEWLINE;
				}

			}
		}
		return localBody;

	}

	private static String extendIncludedInoPartForFile(String existingIncludeCodePart, IResource curResource) {
		String addLine;
		if (curResource.isLinked()) {
			addLine = "#include \"" + curResource.getLocation() + "\"" + NEWLINE;
		} else {
			addLine = "#include \"" + curResource.getName() + "\"" + NEWLINE;
		}
		// if the name of the ino/pde file matches the project put
		// the file in front
		// Otherwise add it to the end
		if (curResource.getName().equals(curResource.getProject().getName() + "." + curResource.getFileExtension())) {
			return addLine + existingIncludeCodePart;
		}
		return existingIncludeCodePart + addLine;

	}

	private static void deleteTheGeneratedFileInPreviousBersionsOfSloeber(IProject iProject) throws CoreException {
		IResource inofile = iProject.findMember(oldGeneratedFile);
		if (inofile != null) {
			inofile.delete(true, null);
		}
	}

	private static void deleteTheGeneratedFile(IProject iProject) throws CoreException {
		IResource inofile = iProject.findMember(generatedFile);
		if (inofile != null) {
			inofile.delete(true, null);
		}
	}
}
