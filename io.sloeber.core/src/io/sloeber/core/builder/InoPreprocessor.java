package io.sloeber.core.builder;

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

import static io.sloeber.core.api.Const.*;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
import org.eclipse.cdt.core.settings.model.ICSourceEntry;
import org.eclipse.cdt.core.settings.model.util.CDataUtil;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTFunctionDefinition;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceProxy;
import org.eclipse.core.resources.IResourceProxyVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;

import io.sloeber.autoBuild.api.IAutoBuildConfigurationDescription;
import io.sloeber.core.tools.Helpers;

@SuppressWarnings({ "nls", "restriction" })
public class InoPreprocessor {
	private static String generatedFileName = "sloeber.ino.cpp";
	private static final String DEFINE_IN_ECLIPSE = "__IN_ECLIPSE__";
	private static final String NEWLINE = "\n";
	private static Map<IProject, IFile> projectInoFiles = new HashMap<>();
	private static final Set<String> ARDUINO_EXTENSIONS = new HashSet<>(Arrays.asList("ino", "pde"));

	public static void generateSloeberInoCPPFile(boolean canSkip, IAutoBuildConfigurationDescription autoBuildConfDesc,
			IProgressMonitor monitor) throws CoreException {

		// loop through all the files in the project to see we need to generate a file
		// This way we can avoid hitting the indexer when we use .cpp files
		IProject iProject = autoBuildConfDesc.getProject();
		List<IFile> inoResources = getInoFiles(autoBuildConfDesc);
		IFile generatedFile = getSloeberInoCPPFile(autoBuildConfDesc);

		if (inoResources.isEmpty()) {
			// delete the generated file this is to cope with
			// renaming ino files to cpp files removing the need for
			// the generated file
			if ((generatedFile!=null) && generatedFile.exists()) {
				generatedFile.delete(true, monitor);
			}
			return;
		}
		if (canSkip && !CCorePlugin.getIndexManager().isIndexerIdle())
			return;
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
			// adding the generation timestamp forces a rebuild of sloeber.ino.cpp each and
			// every time
//			header += "//This file has been generated on ";
//			header += new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
			header += NEWLINE;
			header += NEWLINE;
			header += "#include \"Arduino.h\"" + NEWLINE;

			String projectNameDot = iProject.getName() + DOT;
			// loop through all the files in the project
			for (IResource curResource : inoResources) {
				// check whether the indexer is properly configured.
				// IPath path = curResource.getFullPath();
				IFile curFile = (IFile) curResource;
				ITranslationUnit tu = (ITranslationUnit) CoreModel.getDefault().create(curFile);
				if (tu == null) {
					methodDeclarations = methodDeclarations + fileNotFoundContent(curResource);
				} else {
					// if the name of the ino/pde file matches the project put
					// the file in front
					// Otherwise add it to the end
					if (curResource.getName().equals(projectNameDot + curResource.getFileExtension())) {
						// This is the ino file with the same name as the project
						// so put it first and generate the generate file name
						includeInoPart = getIncludedInoPartForFile(curFile) + includeInoPart;
						methodDeclarations = getdMethodDeclarationsForFile(index, tu) + methodDeclarations;
						header = getExternCMethodDeclarations(index, tu) + header;
					} else {
						includeInoPart = includeInoPart + getIncludedInoPartForFile(curFile);
						methodDeclarations = methodDeclarations + getdMethodDeclarationsForFile(index, tu);
						header = header + getExternCMethodDeclarations(index, tu);
					}
				}
			}
			writeTheGeneratedFile(generatedFile, header + NEWLINE + methodDeclarations + NEWLINE + includeInoPart,
					monitor);
		} finally {
			index.releaseReadLock();
		}
	}

	private static  boolean resourceFound=false;
	private static boolean projectInoFound=false;
	private static List<IFile> getInoFiles(IAutoBuildConfigurationDescription autoBuildConfDesc) throws CoreException {

		// loop through all the files in the project to see we need to generate a file
		// This way we can avoid hitting the indexer when we use .cpp files
		IProject iProject = autoBuildConfDesc.getProject();
		List<IFile> inoResources = new ArrayList<>();

		ICSourceEntry[] srcEntries = IAutoBuildConfigurationDescription.getResolvedSourceEntries(autoBuildConfDesc);
		IResourceProxyVisitor subDirVisitor = new IResourceProxyVisitor() {

			@Override
			public boolean visit(IResourceProxy proxy) throws CoreException {
				IResource resource = proxy.requestResource();
				if (resource.isDerived()) {
					return false;
				}

				boolean isExcluded = CDataUtil.isExcluded(resource.getProjectRelativePath(), srcEntries);
				if (isExcluded) {
					return false;
				}
				if (proxy.getType() == IResource.FILE) {
					IFile curFile=(IFile)resource;
					String extension = curFile.getFileExtension();
					if (extension != null && !extension.isBlank()) {
						if (ARDUINO_EXTENSIONS.contains(extension)) {
							inoResources.add(curFile);
							resourceFound=true;
							if (resource.getName().startsWith(iProject.getName() + DOT)) {
								projectInoFiles.put(iProject, curFile);
								projectInoFound=true;
							}
						}
					}
					return false;
				}
				return true;
			}
		};
		resourceFound=false;
		projectInoFound=false;
		iProject.accept(subDirVisitor, IResource.NONE);
		if(		resourceFound &&  !projectInoFound) {
			//This is a project with ino files but none with the ${proName}.ino file
			projectInoFiles.put(iProject, inoResources.get(0));
			
		}
		return inoResources;
	}

	/**
	 * Delete the sloeber.ino.cpp file if there is one
	 * 
	 * @param monitor
	 * @param autoBuildCfDes
	 * @throws CoreException
	 */
	public static void deleteSloeberInoCPPFile(IAutoBuildConfigurationDescription autoBuildCfDes,
			IProgressMonitor monitor) throws CoreException {
		IFile sloeberInoCpp = getSloeberInoCPPFile(autoBuildCfDes);
		if (sloeberInoCpp.exists()) {
			sloeberInoCpp.delete(true, monitor);
		}
	}

	/**
	 * Delete the sloeber.ino.cpp file if there is one
	 * 
	 * @param monitor
	 * @param autoBuildCfDes
	 * @throws CoreException
	 */
	public static IFile getSloeberInoCPPFile(IAutoBuildConfigurationDescription autoBuildCfDes) {
		IProject iProject = autoBuildCfDes.getProject();
		IFile projectInoFile = projectInoFiles.get(iProject);
		if (projectInoFile != null && projectInoFile.exists()) {
			return getSloeberInoCppFromProjectIno(projectInoFile);
		}
		return null;
	}

	/**
	 * given the file ${projName}.ino get the sloeber.ino.cpp file This is a
	 * resource method Nor projectInoFile nor the sloeber.ino.cpp variant need to
	 * exists The reason this method exists is because the ${projName}.ino file and
	 * the sloeber.ino.cpp file need to be in the same folder or the include file
	 * references may not exists or reference different files
	 * 
	 * @param projectInoFile
	 * @return
	 */
	private static IFile getSloeberInoCppFromProjectIno(IFile projectInoFile) {
		return ((IFolder)projectInoFile.getParent()).getFile(generatedFileName);
	}

	/**
	 * Add some operational stuff and write the file if changed
	 *
	 * @param iProject the project for which the ino files have been parsed
	 * @param content  the ouput of the ino file parsing
	 * @throws CoreException
	 */
	private static void writeTheGeneratedFile(IFile generatedFile, String content, IProgressMonitor monitor)
			throws CoreException {

		// Make sure the file is not processed by Arduino IDE
		String newFileContent = "#ifdef " + DEFINE_IN_ECLIPSE + NEWLINE + content + NEWLINE + "#endif" + NEWLINE;
		String currentFileContent = null;
		try {
			currentFileContent = Files.readString(java.nio.file.Path.of(generatedFile.getLocation().toOSString()),
					Charset.defaultCharset());
		} catch (IOException e) {
			// This happens when the generated file does not yet exist
		}

		if (!newFileContent.equals(currentFileContent)) {
			Helpers.addFileToProject(generatedFile, new ByteArrayInputStream(newFileContent.getBytes()), null, true);
			generatedFile.setDerived(true, monitor);
		}
	}

	private static String getdMethodDeclarationsForFile(IIndex index, ITranslationUnit tu) throws CoreException {
		// add declarations made in ino files.
		String localBody = new String();
		IASTTranslationUnit asttu = tu.getAST(index,
				ITranslationUnit.AST_SKIP_FUNCTION_BODIES | ITranslationUnit.AST_SKIP_ALL_HEADERS);
		IASTNode astNodes[] = asttu.getChildren();
		for (IASTNode astNode : astNodes) {
			if (astNode instanceof CPPASTFunctionDefinition) {
				String addString = astNode.getRawSignature();
				addString = addString.replace("\r\n", NEWLINE);
				addString = addString.replace("\r", NEWLINE);
				addString = addString.replaceAll("//[^\n]+\n", " ");
				addString = addString.replace("\n", " ");
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

	private static String getExternCMethodDeclarations(IIndex index, ITranslationUnit tu) throws CoreException {
		String localHeader = new String();
		// Locate All lines that are extern "C"
		HashMap<Integer, Integer> externCLines = new HashMap<>();
		IASTTranslationUnit astTuTest = tu.getAST(index, ITranslationUnit.AST_SKIP_ALL_HEADERS);
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
							externCLines.put(Integer.valueOf(curline), null);
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
						if (externCLines.containsKey(Integer.valueOf(curHeaderLine))) {
							localHeader += "extern \"C\" {" + NEWLINE;
							localHeader += curInclude.getSource() + NEWLINE;
							localHeader += "}" + NEWLINE;
						} else {

							localHeader += curInclude.getSource();
							localHeader += NEWLINE;
						}
					}
					break;
				default:
					break;
				}
			}
		}
		return localHeader;
	}

	// the indexer is not properly configured so drop a
	// error in the file
	private static String fileNotFoundContent(IResource curResource) {
		String localBody = NEWLINE;
		localBody += "#error the file: " + curResource.getName()
				+ " is not found in the indexer though it exists on the file system." + NEWLINE;
		localBody += "#error this is probably due to a bad eclipse configuration : ino and pde are not marked as c++ file."
				+ NEWLINE;
		localBody += "#error please check whether *.ino and *.pde are marked as C++ source code in windows->preferences->C/C++->file types."
				+ NEWLINE;
		return localBody;
	}

	private static String getIncludedInoPartForFile(IFile includeFile) {
		if (includeFile.isLinked()) {
			return "#include \"" + includeFile.getLocation() + "\"" + NEWLINE;
		}
		return "#include \"" + includeFile.getLocation().toOSString() + "\"";
	}

}
