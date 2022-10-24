package io.sloeber.managedBuild.Internal;

import static io.sloeber.managedBuild.Internal.ManagedBuildConstants.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;

import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.model.CoreModel;
import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.cdt.core.settings.model.ICProjectDescription;
import org.eclipse.cdt.managedbuilder.core.IConfiguration;
import org.eclipse.cdt.managedbuilder.core.IFolderInfo;
import org.eclipse.cdt.managedbuilder.core.IOutputType;
import org.eclipse.cdt.managedbuilder.core.IResourceInfo;
import org.eclipse.cdt.managedbuilder.core.ITool;
import org.eclipse.cdt.managedbuilder.core.ManagedBuildManager;
import org.eclipse.cdt.managedbuilder.internal.core.OutputType;
import org.eclipse.cdt.managedbuilder.macros.BuildMacroException;
import org.eclipse.cdt.managedbuilder.macros.IBuildMacroProvider;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceStatus;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;

import io.sloeber.core.common.Common;
import io.sloeber.managedBuild.api.INewManagedOutputNameProvider;

@SuppressWarnings("nls")
public class ManagebBuildCommon {
    /**
     * Answers the argument with all whitespaces replaced with an escape sequence.
     */
    static public String escapeWhitespaces(String path) {
        // Escape the spaces in the path/filename if it has any
        String[] segments = path.split("\\s");
        if (segments.length > 1) {
            StringBuffer escapedPath = new StringBuffer();
            for (int index = 0; index < segments.length; ++index) {
                escapedPath.append(segments[index]);
                if (index + 1 < segments.length) {
                    escapedPath.append("\\ ");
                }
            }
            return escapedPath.toString().trim();
        }
        return path;
    }

    /**
     * Put COLS_PER_LINE comment charaters in the argument.
     */
    static protected void outputCommentLine(StringBuffer buffer) {
        for (int i = 0; i < COLS_PER_LINE; i++) {
            buffer.append(COMMENT_SYMBOL);
        }
        buffer.append(NEWLINE);
    }

    static public boolean containsSpecialCharacters(String path) {
        return path.matches(".*(\\s|[\\{\\}\\(\\)\\$\\@%=;]).*");
    }

    /**
     * Generates a source macro name from a file extension
     */
    static public StringBuffer getSourceMacroName(String extensionName) {
        StringBuffer macroName = new StringBuffer();
        if (extensionName == null) {
            return null;
        }
        // We need to handle case sensitivity in file extensions (e.g. .c vs
        // .C), so if the
        // extension was already upper case, tack on an "UPPER_" to the macro
        // name.
        // In theory this means there could be a conflict if you had for
        // example,
        // extensions .c_upper, and .C, but realistically speaking the chances
        // of this are
        // practically nil so it doesn't seem worth the hassle of generating a
        // truly
        // unique name.
        if (extensionName.equals(extensionName.toUpperCase())) {
            macroName.append(extensionName.toUpperCase()).append("_UPPER");
        } else {
            // lower case... no need for "UPPER_"
            macroName.append(extensionName.toUpperCase());
        }
        macroName.append("_SRCS");
        return macroName;
    }

    public static void save(StringBuffer buffer, IFile file) throws CoreException {

        byte[] bytes = buffer.toString().getBytes();
        ByteArrayInputStream stream = new ByteArrayInputStream(bytes);
        if (file.exists()) {
            file.setContents(stream, true, false, null);
        } else {
            IFolder fileFolder = file.getProject().getFolder(file.getParent().getProjectRelativePath());
            createFolder(fileFolder);
            file.create(stream, false, null);
        }
        file.setDerived(true, null);
    }

    public static void createFolder(IFolder folder) throws CoreException {
        // Create or get the handle for the build directory
        if (folder.exists()) {
            return;
        }
        if (!folder.getParent().exists()) {
            createFolder(folder.getFolder(".."));
        }
        folder.create(true, true, null);
        folder.setDerived(true, null);
    }

    /**
     * Generates a generated dependency file macro name from a file extension
     */

    public static StringBuffer getDepMacroName(String extensionName) {
        StringBuffer macroName = new StringBuffer();
        // We need to handle case sensitivity in file extensions (e.g. .c vs
        // .C), so if the
        // extension was already upper case, tack on an "UPPER_" to the macro
        // name.
        // In theory this means there could be a conflict if you had for
        // example,
        // extensions .c_upper, and .C, but realistically speaking the chances
        // of this are
        // practically nil so it doesn't seem worth the hassle of generating a
        // truly
        // unique name.
        if (extensionName.equals(extensionName.toUpperCase())) {
            macroName.append(extensionName.toUpperCase()).append("_UPPER");
        } else {
            // lower case... no need for "UPPER_"
            macroName.append(extensionName.toUpperCase());
        }
        macroName.append("_DEPS");
        return macroName;
    }

    /**
     * This method postprocesses a .d file created by a build. It's main job is to
     * add dummy targets for the header files dependencies. This prevents make from
     * aborting the build if the header file does not exist. A secondary job is to
     * work in tandem with the "echo" command that is used by some tool-chains in
     * order to get the "targets" part of the dependency rule correct. This method
     * adds a comment to the beginning of the dependency file which it checks for to
     * determine if this dependency file has already been updated.
     *
     * @return a <code>true</code> if the dependency file is modified
     */
    static public boolean populateDummyTargets(IConfiguration cfg, IFile makefile, boolean force)
            throws CoreException, IOException {
        return populateDummyTargets(cfg.getRootFolderInfo(), makefile, force);
    }

    static public boolean populateDummyTargets(IResourceInfo rcInfo, IFile makefile, boolean force)
            throws CoreException, IOException {
        if (makefile == null || !makefile.exists())
            return false;
        // Get the contents of the dependency file
        InputStream contentStream = makefile.getContents(false);
        StringBuffer inBuffer = null;
        // JABA made sure thgere are no emory leaks
        try (Reader in = new InputStreamReader(contentStream);) {
            int chunkSize = contentStream.available();
            inBuffer = new StringBuffer(chunkSize);
            char[] readBuffer = new char[chunkSize];
            int n = in.read(readBuffer);
            while (n > 0) {
                inBuffer.append(readBuffer);
                n = in.read(readBuffer);
            }
            contentStream.close();
            in.close();
        }
        // The rest of this operation is equally expensive, so
        // if we are doing an incremental build, only update the
        // files that do not have a comment
        String inBufferString = inBuffer.toString();
        if (!force && inBufferString.startsWith(COMMENT_SYMBOL)) {
            return false;
        }
        // Try to determine if this file already has dummy targets defined.
        // If so, we will only add the comment.
        String[] bufferLines = inBufferString.split("[\\r\\n]");
        for (String bufferLine : bufferLines) {
            if (bufferLine.endsWith(":")) {
                StringBuffer outBuffer = addDefaultHeader();
                outBuffer.append(inBuffer);
                save(outBuffer, makefile);
                return true;
            }
        }
        // Reconstruct the buffer tokens into useful chunks of dependency
        // information
        Vector<String> bufferTokens = new Vector<>(Arrays.asList(inBufferString.split("\\s")));
        Vector<String> deps = new Vector<>(bufferTokens.size());
        Iterator<String> tokenIter = bufferTokens.iterator();
        while (tokenIter.hasNext()) {
            String token = tokenIter.next();
            if (token.lastIndexOf("\\") == token.length() - 1 && token.length() > 1) {
                // This is escaped so keep adding to the token until we find the
                // end
                while (tokenIter.hasNext()) {
                    String nextToken = tokenIter.next();
                    token += WHITESPACE + nextToken;
                    if (!nextToken.endsWith("\\")) {
                        break;
                    }
                }
            }
            deps.add(token);
        }
        deps.trimToSize();
        // Now find the header file dependencies and make dummy targets for them
        boolean save = false;
        StringBuffer outBuffer = null;
        // If we are doing an incremental build, only update the files that do
        // not have a comment
        String firstToken;
        try {
            firstToken = deps.get(0);
        } catch (@SuppressWarnings("unused") ArrayIndexOutOfBoundsException e) {
            // This makes no sense so bail
            return false;
        }
        // Put the generated comments in the output buffer
        if (!firstToken.startsWith(COMMENT_SYMBOL)) {
            outBuffer = addDefaultHeader();
        } else {
            outBuffer = new StringBuffer();
        }
        // Some echo implementations misbehave and put the -n and newline in the
        // output
        if (firstToken.startsWith("-n")) {
            // Now let's parse:
            // Win32 outputs -n '<path>/<file>.d <path>/'
            // POSIX outputs -n <path>/<file>.d <path>/
            // Get the dep file name
            String secondToken;
            try {
                secondToken = deps.get(1);
            } catch (ArrayIndexOutOfBoundsException e) {
                secondToken = "";
            }
            if (secondToken.startsWith("'")) {
                // This is the Win32 implementation of echo (MinGW without MSYS)
                outBuffer.append(secondToken.substring(1)).append(WHITESPACE);
            } else {
                outBuffer.append(secondToken).append(WHITESPACE);
            }
            // The relative path to the build goal comes next
            String thirdToken;
            try {
                thirdToken = deps.get(2);
            } catch (ArrayIndexOutOfBoundsException e) {
                thirdToken = "";
            }
            int lastIndex = thirdToken.lastIndexOf("'");
            if (lastIndex != -1) {
                if (lastIndex == 0) {
                    outBuffer.append(WHITESPACE);
                } else {
                    outBuffer.append(thirdToken.substring(0, lastIndex - 1));
                }
            } else {
                outBuffer.append(thirdToken);
            }
            // Followed by the target output by the compiler plus ':'
            // If we see any empty tokens here, assume they are the result of
            // a line feed output by "echo" and skip them
            String fourthToken;
            int nToken = 3;
            try {
                do {
                    fourthToken = deps.get(nToken++);
                } while (fourthToken.length() == 0);
            } catch (ArrayIndexOutOfBoundsException e) {
                fourthToken = "";
            }
            outBuffer.append(fourthToken).append(WHITESPACE);
            // Followed by the actual dependencies
            try {
                for (String nextElement : deps) {
                    if (nextElement.endsWith("\\")) {
                        outBuffer.append(nextElement).append(NEWLINE).append(WHITESPACE);
                    } else {
                        outBuffer.append(nextElement).append(WHITESPACE);
                    }
                }
            } catch (IndexOutOfBoundsException e) {
                /* JABA is not going to write this code */
            }
        } else {
            outBuffer.append(inBuffer);
        }
        outBuffer.append(NEWLINE);
        save = true;
        IFolderInfo fo = null;
        if (rcInfo instanceof IFolderInfo) {
            fo = (IFolderInfo) rcInfo;
        } else {
            IConfiguration c = rcInfo.getParent();
            fo = (IFolderInfo) c.getResourceInfo(rcInfo.getPath().removeLastSegments(1), false);
        }
        // Dummy targets to add to the makefile
        for (String dummy : deps) {
            IPath dep = new Path(dummy);
            String extension = dep.getFileExtension();
            if (fo.isHeaderFile(extension)) {
                /*
                 * The formatting here is <dummy_target>:
                 */
                outBuffer.append(dummy).append(COLON).append(NEWLINE).append(NEWLINE);
            }
        }
        // Write them out to the makefile
        if (save) {
            save(outBuffer, makefile);
            return true;
        }
        return false;
    }

    /**
     * prepend all instanced of '\' or '"' with a backslash
     *
     * @return resulting string
     */
    static public String escapedEcho(String string) {
        String escapedString = string.replace("'", "'\"'\"'");
        return ECHO + WHITESPACE + SINGLE_QUOTE + escapedString + SINGLE_QUOTE + NEWLINE;
    }

    static public String ECHO_BLANK_LINE = ECHO + WHITESPACE + SINGLE_QUOTE + WHITESPACE + SINGLE_QUOTE + NEWLINE;

    /**
     * Outputs a comment formatted as follows: ##### ....... ##### # <Comment
     * message> ##### ....... #####
     */
    static protected StringBuffer addDefaultHeader() {
        StringBuffer buffer = new StringBuffer();
        outputCommentLine(buffer);
        buffer.append(COMMENT_SYMBOL).append(WHITESPACE).append(MESSAGE_HEADER)
                .append(NEWLINE);
        outputCommentLine(buffer);
        buffer.append(NEWLINE);
        return buffer;
    }

    /**
     * Strips outermost quotes of Strings of the form "a" and 'a' or returns the
     * original string if the input is not of this form.
     *
     * @throws NullPointerException
     *             if path is null
     * @return a String without the outermost quotes (if the input has them)
     */
    public static String ensureUnquoted(String path) {
        boolean doubleQuoted = path.startsWith("\"") && path.endsWith("\"");
        boolean singleQuoted = path.startsWith("'") && path.endsWith("'");
        return doubleQuoted || singleQuoted ? path.substring(1, path.length() - 1) : path;
    }

    public static List<String> resolvePaths(List<IPath> toResolve, IConfiguration config) {
        List<String> ret = new LinkedList<>();
        if (toResolve.isEmpty())
            return ret;
        for (IPath curOutputPath : toResolve) {
            try {
                if (curOutputPath != null) {
                    String curOutput = curOutputPath.toOSString();
                    // try to resolve the build macros in the
                    // output names
                    String resolved = ManagedBuildManager.getBuildMacroProvider().resolveValueToMakefileFormat(
                            curOutput, "", //$NON-NLS-1$
                            " ", //$NON-NLS-1$
                            IBuildMacroProvider.CONTEXT_CONFIGURATION, config);
                    if ((resolved = resolved.trim()).length() > 0) {
                        ret.add(resolved);
                    } else {
                        ret.add(curOutput);
                    }
                }
            } catch (BuildMacroException e) {
                //If we can not resolve we keep the original
            }
        }
        return ret;
    }

    /**
     * Return or create the folder needed for the build output. If we are creating
     * the folder, set the derived bit to true so the CM system ignores the
     * contents. If the resource exists, respect the existing derived setting.
     */
    public static IPath createDirectory(IProject project, String dirName) throws CoreException {
        // Create or get the handle for the build directory
        IFolder folder = project.getFolder(dirName);
        if (!folder.exists()) {
            // Make sure that parent folders exist
            IPath parentPath = (new Path(dirName)).removeLastSegments(1);
            // Assume that the parent exists if the path is empty
            if (!parentPath.isEmpty()) {
                IFolder parent = project.getFolder(parentPath);
                if (!parent.exists()) {
                    createDirectory(project, parentPath.toString());
                }
            }
            // Now make the requested folder
            try {
                folder.create(true, true, null);
            } catch (CoreException e) {
                if (e.getStatus().getCode() == IResourceStatus.PATH_OCCUPIED)
                    folder.refreshLocal(IResource.DEPTH_ZERO, null);
                else
                    throw e;
            }
            // Make sure the folder is marked as derived so it is not added to
            // CM
            if (!folder.isDerived()) {
                folder.setDerived(true, null);
            }
        }
        return folder.getFullPath();
    }

    /**
     * Return or create the makefile needed for the build. If we are creating the
     * resource, set the derived bit to true so the CM system ignores the contents.
     * If the resource exists, respect the existing derived setting.
     */
    public static IFile createFile(IPath makefilePath) throws CoreException {
        // Create or get the handle for the makefile
        IWorkspaceRoot root = CCorePlugin.getWorkspace().getRoot();
        IFile newFile = root.getFileForLocation(makefilePath);
        if (newFile == null) {
            newFile = root.getFile(makefilePath);
        }
        // Create the file if it does not exist
        ByteArrayInputStream contents = new ByteArrayInputStream(new byte[0]);
        try {
            newFile.create(contents, false, new NullProgressMonitor());
            // Make sure the new file is marked as derived
            if (!newFile.isDerived()) {
                newFile.setDerived(true, null);
            }
        } catch (CoreException e) {
            // If the file already existed locally, just refresh to get contents
            if (e.getStatus().getCode() == IResourceStatus.PATH_OCCUPIED)
                newFile.refreshLocal(IResource.DEPTH_ZERO, null);
            else
                throw e;
        }
        return newFile;
    }

    /**
     * Adds a macro addition prefix to a map of macro names to entries. Entry
     * prefixes look like: C_SRCS += \ ${addprefix $(ROOT)/, \
     */
    public static void addMacroAdditionPrefix(LinkedHashMap<String, String> map, String macroName, String relativePath,
            boolean addPrefix) {
        // there is no entry in the map, so create a buffer for this macro
        StringBuffer tempBuffer = new StringBuffer();
        tempBuffer.append(macroName).append(MACRO_ADDITION_PREFIX_SUFFIX);
        if (addPrefix) {
            tempBuffer.append(new Path(MACRO_ADDITION_ADDPREFIX_HEADER).append(relativePath)
                    .append(MACRO_ADDITION_ADDPREFIX_SUFFIX).toOSString());
        }
        // have to store the buffer in String form as StringBuffer is not a
        // sublcass of Object
        map.put(macroName, tempBuffer.toString());
    }

    /**
     * Gets a path for a resource by extracting the Path field from its location
     * URI.
     *
     * @return IPath
     * @since 6.0
     */

    public static IPath getPathForResource(IResource resource) {
        return new Path(resource.getLocationURI().getPath());
    }

    /**
     * Adds a file to an entry in a map of macro names to entries. File additions
     * look like: example.c, \
     */
    static public void addMacroAdditionFile(HashMap<String, String> map, String macroName, String filename) {
        StringBuffer buffer = new StringBuffer();
        buffer.append(map.get(macroName));
        String escapedFilename = escapeWhitespaces(filename);
        buffer.append(escapedFilename).append(WHITESPACE).append(LINEBREAK);
        map.put(macroName, buffer.toString());
    }

    /**
     * Adds a file to an entry in a map of macro names to entries. File additions
     * look like: example.c, \
     */
    static public void addMacroAdditionFile(ArduinoGnuMakefileGenerator caller, HashMap<String, String> map,
            String macroName, String relativePath, IPath sourceLocation, boolean generatedSource) {
        // Add the source file path to the makefile line that adds source files
        // to the build variable
        IProject project = caller.getProject();
        IPath buildWorkingDir = caller.getBuildWorkingDir();
        String srcName;
        IPath projectLocation = getPathForResource(project);
        IPath dirLocation = projectLocation;
        if (generatedSource) {
            dirLocation = dirLocation.append(buildWorkingDir);
        }
        if (dirLocation.isPrefixOf(sourceLocation)) {
            IPath srcPath = sourceLocation.removeFirstSegments(dirLocation.segmentCount()).setDevice(null);
            if (generatedSource) {
                srcName = DOT_SLASH_PATH.append(srcPath).toOSString();
            } else {
                srcName = ROOT + FILE_SEPARATOR + srcPath.toOSString();
            }
        } else {
            if (generatedSource && !sourceLocation.isAbsolute()) {
                srcName = DOT_SLASH_PATH.append(relativePath).append(sourceLocation.lastSegment()).toOSString();
            } else {
                // TODO: Should we use relative paths when possible (e.g., see
                // MbsMacroSupplier.calculateRelPath)
                srcName = sourceLocation.toOSString();
            }
        }
        addMacroAdditionFile(map, macroName, srcName);
    }

    /**
     * Process a String denoting a filepath in a way compatible for GNU Make rules,
     * handling windows drive letters and whitespace appropriately.
     * <p>
     * <p>
     * The context these paths appear in is on the right hand side of a rule header.
     * i.e.
     * <p>
     * <p>
     * target : dep1 dep2 dep3
     * <p>
     *
     * @param path
     *            the String denoting the path to process
     * @throws NullPointerException
     *             is path is null
     * @return a suitable Make rule compatible path
     */
    /* see https://bugs.eclipse.org/bugs/show_bug.cgi?id=129782 */
    static public String ensurePathIsGNUMakeTargetRuleCompatibleSyntax(String path) {
        return escapeWhitespaces(ensureUnquoted(path));
    }

    static public String getToolCommandLinePattern(IConfiguration config, ITool tool) {
        IProject project = config.getOwner().getProject();
        String orgPattern = tool.getCommandLinePattern();
        if (orgPattern.contains("$")) {
            //if the pattern contains a space no use to try to expand it
            return orgPattern;
        }
        ICProjectDescription prjDesc = CoreModel.getDefault().getProjectDescription(project);
        ICConfigurationDescription confDesc = prjDesc.getConfigurationByName(config.getName());
        return Common.getBuildEnvironmentVariable(confDesc, orgPattern, orgPattern, false);

    }

    static public INewManagedOutputNameProvider getJABANameProvider(IConfiguration cConf, IPath referencedFrom,
            IOutputType iType) {
        OutputType type = (OutputType) iType;
        IConfigurationElement element = type.getNameProviderElement();
        if (element != null) {
            try {
                if (element.getAttribute(IOutputType.NAME_PROVIDER) != null) {
                    return (INewManagedOutputNameProvider) element.createExecutableExtension(IOutputType.NAME_PROVIDER);
                }
            } catch (@SuppressWarnings("unused") CoreException e) {
                //ignore errors
            }
        }
        String[] outputNames = type.getOutputNames();
        if (outputNames != null && outputNames.length > 0) {
            String outputName = outputNames[0];
            IBuildMacroProvider provider = ManagedBuildManager.getBuildMacroProvider();
            String expanded = outputName;
            try {
                expanded = provider.resolveValue(outputName, EMPTY_STRING, WHITESPACE,
                        IBuildMacroProvider.CONTEXT_CONFIGURATION, cConf);
            } catch (BuildMacroException e) {
                // default will do
                e.printStackTrace();
            }
            IPath expandedPath = new Path(expanded);
            if (expandedPath.segmentCount() > 1) {
                expandedPath = GetNiceFileName(referencedFrom, new Path(expanded));
            }
            final IPath ret=expandedPath;
            return new INewManagedOutputNameProvider() {

                @Override
                public IPath getOutputName(IProject project, IConfiguration cConf, ITool tool, IPath inputName) {
                    return ret;
                }
            };
        }

        String[] outputExtensions = type.getOutputExtensionsAttribute();
        if (outputExtensions != null && outputExtensions.length > 0) {
            String outputExtension = outputExtensions[0];
            return new INewManagedOutputNameProvider() {

                @Override
                public IPath getOutputName(IProject project, IConfiguration cConf, ITool tool, IPath inputName) {
                    ICProjectDescription prjDesc = CoreModel.getDefault().getProjectDescription(project);
                    ICConfigurationDescription confDesc = prjDesc.getConfigurationByName(cConf.getName());
                    String expanded = Common.getBuildEnvironmentVariable(confDesc, inputName.toString(),
                            inputName.toString(), true);
                    return new Path(expanded).addFileExtension(outputExtension);
                }
            };
        }

        return null;
    }

    static public String GetNiceFileName(IFile buildPath, IFile path) {
        return GetNiceFileName(buildPath.getLocation(), path.getLocation()).toOSString();
    }

    static public IPath GetNiceFileName(IPath buildPath, IPath filePath) {
        if (buildPath.isPrefixOf(filePath)) {
            return filePath.makeRelativeTo(buildPath);
        }
        if (buildPath.removeLastSegments(1).isPrefixOf(filePath)) {
            return filePath.makeRelativeTo(buildPath);

        }
        return filePath;
    }

    static public String makeVariable(String variableName) {
        return VARIABLE_PREFIX + variableName + VARIABLE_SUFFIX;
    }
}
