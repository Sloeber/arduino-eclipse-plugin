package io.sloeber.core.api;

import static io.sloeber.core.common.Common.*;
import static io.sloeber.core.common.Const.*;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import org.apache.commons.lang.StringUtils;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;

import io.sloeber.core.common.ConfigurationPreferences;
import io.sloeber.core.common.InstancePreferences;
import io.sloeber.core.tools.FileModifiers;
import io.sloeber.core.tools.Helpers;
import io.sloeber.core.tools.Libraries;
import io.sloeber.core.tools.Stream;

/**
 * A class to describe the code that needs to be attached to the project
 *
 * @author jan
 *
 */

public class CodeDescription {
    public enum CodeTypes {
        None, defaultIno, defaultCPP, CustomTemplate, sample
    }

    static private final String DEFAULT_SKETCH_BASE = "sketch"; //$NON-NLS-1$
    public static final String DEFAULT_SKETCH_INO = DEFAULT_SKETCH_BASE + ".ino"; //$NON-NLS-1$
    public static final String DEFAULT_SKETCH_CPP = DEFAULT_SKETCH_BASE + ".cpp"; //$NON-NLS-1$
    public static final String DEFAULT_SKETCH_H = DEFAULT_SKETCH_BASE + ".h"; //$NON-NLS-1$
    //
    // template Sketch information

    static private final String SLOEBER_SKETCH_TEMPLATE_FOLDER = ENV_KEY_SLOEBER_START + "TEMPLATE_FOLDER"; //$NON-NLS-1$
    static private final String SLOEBER_SKETCH_TEMPLATE_USE_DEFAULT = ENV_KEY_SLOEBER_START + "TEMPLATE_USE_DEFAULT"; //$NON-NLS-1$

    private CodeTypes myCodeType;
    private IPath myTemPlateFoldername;
    private boolean myMakeLinks = false;
    private ArrayList<IPath> myExamples = new ArrayList<>();
    private Map<String, String> myReplacers = null;

    public IPath getTemPlateFoldername() {
        return myTemPlateFoldername;
    }

    /**
     * set key value pairs that will be used to do a replace all in the code
     * the key is the search key and the value is the replacement
     * The keys are regular expressions.
     * The idea is to use clear keys like #include {include} in the source code
     * note that the keys
     * \{include\} \{title\} \{SerialMonitorSerial\}
     * are already used. Please use other keys as your replacements will overwrite
     * the
     * ones already used
     * 
     * @param myReplacers
     *            a list of find an replace key value pairs. null is no replace
     *            needed
     */
    public void setReplacers(Map<String, String> myReplacers) {
        this.myReplacers = myReplacers;
    }

    public CodeDescription(CodeTypes codeType) {
        myCodeType = codeType;
    }

    public static CodeDescription createNone() {
        return new CodeDescription(CodeTypes.None);
    }

    public static CodeDescription createDefaultIno() {
        return new CodeDescription(CodeTypes.defaultIno);
    }

    public static CodeDescription createDefaultCPP() {
        return new CodeDescription(CodeTypes.defaultCPP);
    }

    public static CodeDescription createExample(boolean link, ArrayList<IPath> sampleFolders) {
        CodeDescription codeDescriptor = new CodeDescription(CodeTypes.sample);
        codeDescriptor.myMakeLinks = link;
        codeDescriptor.myExamples = sampleFolders;
        return codeDescriptor;
    }

    public static CodeDescription createCustomTemplate(IPath temPlateFoldername) {
        CodeDescription codeDescriptor = new CodeDescription(CodeTypes.CustomTemplate);
        codeDescriptor.myTemPlateFoldername = temPlateFoldername;
        return codeDescriptor;
    }

    public static CodeDescription createLastUsed() {

        String typeDescriptor = InstancePreferences.getString(SLOEBER_SKETCH_TEMPLATE_USE_DEFAULT, new String());
        CodeTypes codeType = codeTypeFromDescription(typeDescriptor);
        CodeDescription ret = new CodeDescription(codeType);
        ret.myTemPlateFoldername = new Path(
                InstancePreferences.getString(SLOEBER_SKETCH_TEMPLATE_FOLDER, new String()));
        ret.loadLastUsedExamples();
        return ret;
    }

    private static CodeTypes codeTypeFromDescription(String typeDescriptor) {

        for (CodeTypes codeType : CodeTypes.values()) {
            if (codeType.toString().equals(typeDescriptor)) {
                return codeType;
            }
        }
        return CodeTypes.defaultIno;

    }

    /**
     * Save the setting in the last used
     */
    private void save() {
        if (myTemPlateFoldername != null) {
            InstancePreferences.setGlobalValue(SLOEBER_SKETCH_TEMPLATE_FOLDER, myTemPlateFoldername.toString());
        }
        InstancePreferences.setGlobalValue(SLOEBER_SKETCH_TEMPLATE_USE_DEFAULT, myCodeType.toString());
        saveLastUsedExamples();
    }

    /**
     * given the source descriptor, add the sources to the project returns a set
     * of libraries that need to be installed
     **/
    @SuppressWarnings("nls")
    Map<String, IPath> createFiles(IProject project, IProgressMonitor monitor) throws CoreException {
        Map<String, IPath> libraries = new TreeMap<>();

        save();
        Map<String, String> replacers = new TreeMap<>();
        replacers.put("{Include}", "Arduino.h");
        replacers.put("{title}", project.getName());
        if (myReplacers != null) {
            replacers.putAll(myReplacers);
        }

        switch (myCodeType) {
        case None:
            break;
        case defaultIno:
            Helpers.addFileToProject(project, new Path(project.getName() + ".ino"),
                    Stream.openContentStream("/io/sloeber/core/templates/" + DEFAULT_SKETCH_INO, false, replacers),
                    monitor, false);
            break;
        case defaultCPP:
            Helpers.addFileToProject(project, new Path(project.getName() + ".cpp"),
                    Stream.openContentStream("/io/sloeber/core/templates/" + DEFAULT_SKETCH_CPP, false, replacers),
                    monitor, false);
            Helpers.addFileToProject(project, new Path(project.getName() + ".h"),
                    Stream.openContentStream("/io/sloeber/core/templates/" + DEFAULT_SKETCH_H, false, replacers),
                    monitor, false);
            break;
        case CustomTemplate:
            IPath folderName = myTemPlateFoldername;
            String files[] = folderName.toFile().list();
            if (files == null) {
                log(new Status(IStatus.WARNING, CORE_PLUGIN_ID, "No files found in template folder :" + folderName,
                        null));
            } else {
                for (String file : files) {
                    if (!(file.equals(".") || file.equals(".."))) {
                        File sourceFile = folderName.append(file).toFile();
                        String renamedFile = file;
                        if (DEFAULT_SKETCH_INO.equalsIgnoreCase(file)) {
                            renamedFile = project.getName() + ".ino";
                        }
                        try (InputStream theFileStream = Stream.openContentStream(sourceFile.toString(), true,
                                replacers);) {
                            Helpers.addFileToProject(project, new Path(renamedFile), theFileStream, monitor, false);
                        } catch (IOException e) {
                            log(new Status(IStatus.WARNING, CORE_PLUGIN_ID,
                                    "Failed to add template file :" + sourceFile.toString(), e));
                        }

                    }
                }
            }

            break;
        case sample:
                for (IPath curPath : myExamples) {
                    String libName = getLibraryName(curPath);
                    if (libName != null) {
                        libraries.put(libName, Libraries.getLibraryCodeFolder(curPath));
                    }
                }
            break;
        }
        return libraries;
    }

    @SuppressWarnings("nls")
    private void loadLastUsedExamples() {
        String examplePathNames[] = InstancePreferences
                .getString(KEY_LAST_USED_EXAMPLES, Defaults.getPrivateLibraryPath()).split("\n");

        for (String curpath : examplePathNames) {
            myExamples.add(new Path(curpath));
        }
    }

    public ArrayList<IPath> getExamples() {
        return myExamples;
    }

    private void saveLastUsedExamples() {
        if (myExamples != null) {
            String toStore = myExamples.stream().map(Object::toString).collect(Collectors.joining("\n")); //$NON-NLS-1$
            InstancePreferences.setGlobalValue(KEY_LAST_USED_EXAMPLES, toStore);
        } else {
            InstancePreferences.setGlobalValue(KEY_LAST_USED_EXAMPLES, new String());
        }

    }

    public CodeTypes getCodeType() {
        return myCodeType;
    }

    /**
     * Get the name of the example for this project descriptor This is only
     * "known in case of examples" as in all other cases the name will be
     * project related which is unknown in this case. This method only exists to
     * support unit testing where one knows only 1 example is selected
     *
     * @return the name of the first selected example in case of sample in all
     *         other cases null
     */
    public String getExampleName() {
        switch (myCodeType) {
        case sample:
            return myExamples.get(0).lastSegment();
        default:
            break;
        }
        return null;
    }

    /**
     * Get the name of the library for this project descriptor This is only
     * "known in case of examples" as in all other cases the name will be
     * project related which is unknown in this case. This method only exists to
     * support unit testing where one knows only 1 example is selected
     *
     * @return the name of the first selected example in case of sample in all
     *         other cases null
     */
    public String getLibraryName() {
        switch (myCodeType) {
        case sample:
            return getLibraryName(myExamples.get(0));
        default:
            break;
        }
        return null;
    }

    @SuppressWarnings("nls")
    private static String getLibraryName(IPath examplePath) {
        if (ConfigurationPreferences.getInstallationPathExamples().isPrefixOf(examplePath)) {
            return null;
        }
        if ("libraries".equalsIgnoreCase(examplePath.removeLastSegments(4).lastSegment())) {
            return examplePath.removeLastSegments(3).lastSegment();
        }
        if ("libraries".equalsIgnoreCase(examplePath.removeLastSegments(5).lastSegment())) {
            return examplePath.removeLastSegments(4).lastSegment();
        }
        return examplePath.removeLastSegments(2).lastSegment();
    }

    public boolean isLinkedExample() {

        return (myCodeType == CodeTypes.sample) && myMakeLinks;
    }

    /**
     * returns the path of the first linked example.
     * This method is used to add a include folders to the FIRST linked example
     * You could argue that ALL linked examples should be in the
     * include path ...
     * I don't want to support this use case because if you want to do
     * so you should know what you are doing and you don't need this
     * 
     * 
     * @return the path to the first linked example or NULL
     */
    public IPath getLinkedExamplePath() {
        if (!isLinkedExample())
            return null;
        return myExamples.get(0);
    }
}
