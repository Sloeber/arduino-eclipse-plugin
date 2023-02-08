package io.sloeber.autoBuild.extensionPoint.providers;

import static io.sloeber.autoBuild.Internal.ManagedBuildConstants.MAKE_ADDITION;
import static io.sloeber.autoBuild.core.Messages.*;
import static io.sloeber.autoBuild.extensionPoint.providers.AutoBuildCommon.*;
import static io.sloeber.autoBuild.integration.Const.*;

import java.text.MessageFormat;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.eclipse.cdt.core.settings.model.CSourceEntry;
import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.cdt.core.settings.model.ICSettingEntry;
import org.eclipse.cdt.core.settings.model.ICSourceEntry;
import org.eclipse.cdt.core.settings.model.util.CDataUtil;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;

import io.sloeber.autoBuild.api.IBuildMacroProvider;
import io.sloeber.autoBuild.core.Activator;
import io.sloeber.autoBuild.extensionPoint.IMakefileGenerator;
import io.sloeber.autoBuild.integration.AutoBuildConfigurationData;
import io.sloeber.schema.api.IBuilder;
import io.sloeber.schema.api.IConfiguration;

/**
 * This is the default makefile generator
 * Feel free to extend to add the flavors you need
 *
 * @noinstantiate This class is not intended to be instantiated by clients.
 */
public class MakefileGenerator implements IMakefileGenerator {
    static private boolean VERBOSE = true;

    // Local variables needed by generator
    IConfiguration myConfig;
    IProject myProject;
    ICConfigurationDescription myCConfigurationDescription;
    IFolder myTopBuildDir;
    ICSourceEntry[] mySrcEntries;
    MakeRules myMakeRules = null;
    Set<IFolder> myFoldersToBuild = null;
    AutoBuildConfigurationData myAutoBuildConfData;

    /****************************************************************************************
     * CONSTRUCTOR / INITIALIZING code / overrides
     *****************************************************************************************/

    public MakefileGenerator() {
        super();
    }

    @Override
    public void initialize(int buildKind, IProject project, AutoBuildConfigurationData autoBuildConfData,
            IBuilder builder) {
        myProject = project;
        myAutoBuildConfData = autoBuildConfData;
        myCConfigurationDescription = myAutoBuildConfData.getCdtConfigurationDescription();
        myConfig = myAutoBuildConfData.getConfiguration();
        myTopBuildDir = myAutoBuildConfData.getBuildFolder();

        // Get the target info
        String buildTargetName;
        String buildTargetExt;
        buildTargetName = myConfig.getArtifactName();
        // Get its extension
        buildTargetExt = myConfig.getArtifactExtension();
        // try to resolve the build macros in the target extension
        buildTargetExt = resolveValueToMakefileFormat(buildTargetExt, EMPTY_STRING, BLANK,
                IBuildMacroProvider.CONTEXT_CONFIGURATION, autoBuildConfData);
        // try to resolve the build macros in the target name
        String resolved = resolveValueToMakefileFormat(buildTargetName, EMPTY_STRING, BLANK,
                IBuildMacroProvider.CONTEXT_CONFIGURATION, autoBuildConfData);
        if (resolved != null) {
            resolved = resolved.trim();
            if (resolved.length() > 0)
                buildTargetName = resolved;
        }
        if (buildTargetExt == null) {
            buildTargetExt = EMPTY_STRING;
        }

        // TOFIX JABA currently the source entries are always null
        // need to revisit this after storing the data to activate the exclude from
        // build functionality
        // get the source entries
        List<ICSourceEntry> srcEntries = myConfig.getSourceEntries();
        if (srcEntries.size() == 0) {
            // srcEntries = new LinkedList<ICSourceEntry>();
            srcEntries.add(
                    new CSourceEntry(Path.EMPTY, null, ICSettingEntry.RESOLVED | ICSettingEntry.VALUE_WORKSPACE_PATH));
        } else {

            ICSourceEntry[] resolvedEntries = CDataUtil.resolveEntries(srcEntries.toArray(new ICSourceEntry[0]),
                    myCConfigurationDescription);
            for (ICSourceEntry curEntry : resolvedEntries) {
                srcEntries.add(curEntry);
            }
        }
        mySrcEntries = srcEntries.toArray(new ICSourceEntry[srcEntries.size()]);
    }

    @Override
    public void regenerateDependencies(boolean force, IProgressMonitor monitor) throws CoreException {
        //Nothing to do here
    }

    @Override
    public void generateDependencies(IProgressMonitor monitor) throws CoreException {
        //Nothing to do here
    }

    @Override
    public MultiStatus generateMakefiles(IResourceDelta delta, IProgressMonitor monitor) throws CoreException {
        return localgenerateMakefiles(monitor);
    }

    @Override
    public MultiStatus regenerateMakefiles(IProgressMonitor monitor) throws CoreException {
        return localgenerateMakefiles(monitor);
    }

    /****************************************************************************************
     * Make rule generation code
     *****************************************************************************************/

    /****************************************************************************************
     * MakeFile generation code
     *****************************************************************************************/

    protected MultiStatus localgenerateMakefiles(IProgressMonitor monitor) throws CoreException {
        if (VERBOSE) {
            System.out.println("Start MakeFile Generation for " + myProject.getName() + " for config " //$NON-NLS-1$ //$NON-NLS-2$
                    + myCConfigurationDescription.getName());
        }
        beforeRuleGeneration();
        MultiStatus status;
        //This object remains alive between builds; therefore we need to reset the field values
        myFoldersToBuild = new HashSet<>();
        myMakeRules = new MakeRules(myProject, myAutoBuildConfData, myTopBuildDir, myConfig, mySrcEntries,
                myFoldersToBuild);

        if (myMakeRules.size() == 0) {
            // Throw an error if no source file make rules have been created
            String info = MessageFormat.format(MakefileGenerator_warning_no_source, myProject.getName());
            updateMonitor(info, monitor);
            status = new MultiStatus(Activator.getId(), IStatus.INFO, EMPTY_STRING, null);
            status.add(new Status(IStatus.ERROR, Activator.getId(), NO_SOURCE_FOLDERS, info, null));
            return status;
        }
        beforeMakefileGeneration();
        //We have all the rules. Time to make the make files
        Set<String> srcMacroNames = new LinkedHashSet<>();
        Set<String> objMacroNames = new LinkedHashSet<>();
        objMacroNames = myMakeRules.getTargetMacros();
        srcMacroNames = myMakeRules.getPrerequisiteMacros();
        // srcMacroNames.addAll(myMakeRules.getDependecyMacros());
        generateSrcMakefiles();
        SrcMakeGenerator.generateSourceMakefile(myTopBuildDir, myProject, srcMacroNames, myFoldersToBuild);
        SrcMakeGenerator.generateObjectsMakefile(myTopBuildDir, myProject, objMacroNames);
        TopMakeFileGenerator.generateMakefile(myTopBuildDir, myAutoBuildConfData, myFoldersToBuild, myMakeRules,
                objMacroNames);

        checkCancel(monitor);
        afterMakefileGeneration();

        if (VERBOSE) {
            System.out.println("MakeFile Generation done for " + myProject.getName() + " for config " //$NON-NLS-1$ //$NON-NLS-2$
                    + myCConfigurationDescription.getName());
        }

        return new MultiStatus(Activator.getId(), IStatus.OK, EMPTY_STRING, null);
    }

    /*************************************************************************
     * M A K E F I L E G E N E R A T I O N C O M M O N M E T H O D S
     ************************************************************************/

    protected void generateSrcMakefiles() throws CoreException {
        for (IFolder curFolder : myFoldersToBuild) {
            // generate the file content
            StringBuffer makeBuf = addDefaultHeader();
            MakeRules applicableMakeRules = myMakeRules.getRulesForFolder(curFolder);
            makeBuf.append(GenerateMacroSection(myTopBuildDir, applicableMakeRules));
            makeBuf.append(GenerateRules(applicableMakeRules));

            // Save the files
            IFolder srcFile = myTopBuildDir.getFolder(curFolder.getProjectRelativePath());
            save(makeBuf, srcFile.getFile(MODFILE_NAME));
        }
    }

    protected StringBuffer GenerateRules(MakeRules makeRules) {
        StringBuffer buffer = new StringBuffer();
        buffer.append(NEWLINE);
        buffer.append(COMMENT_SYMBOL).append(WHITESPACE).append(MakefileGenerator_comment_build_rule).append(NEWLINE);

        for (MakeRule makeRule : makeRules) {
            buffer.append(getRule(makeRule));
        }

        return buffer;
    }

    protected static StringBuffer GenerateMacroSection(IFolder buildRoot, MakeRules makeRules) {
        StringBuffer buffer = new StringBuffer();
        buffer.append(NEWLINE);
        buffer.append(COMMENT_SYMBOL).append(WHITESPACE).append(MakefileGenerator_comment_module_variables)
                .append(NEWLINE);
        HashSet<String> macroNames = new HashSet<>();
        for (MakeRule makeRule : makeRules) {
            macroNames.addAll(makeRule.getAllMacros());
        }
        macroNames.remove(EMPTY_STRING);
        for (String macroName : macroNames) {
            HashSet<IFile> files = new HashSet<>();
            for (MakeRule makeRule : makeRules) {
                files.addAll(makeRule.getMacroElements(macroName));
            }
            if (files.size() > 0) {
                buffer.append(macroName).append(MAKE_ADDITION);
                for (IFile file : files) {
                    buffer.append(LINEBREAK);
                    buffer.append(GetNiceFileName(buildRoot, file)).append(WHITESPACE);
                }
                buffer.append(NEWLINE);
                buffer.append(NEWLINE);
            }
        }
        return buffer;
    }

    /****************************************************************************************
     * Convenience methods to interfere in the makefile generation
     *****************************************************************************************/
    /***
     * Method that asks the rule from the makerule
     * Override this if you want to modify the rule of all/some targets
     * 
     * @param makeRule
     * @return
     */
    protected StringBuffer getRule(MakeRule makeRule) {
        return makeRule.getRule(myProject, myTopBuildDir, myAutoBuildConfData);
    }

    protected void afterMakefileGeneration() {
        // nothing to do. 
    }

    protected void beforeMakefileGeneration() {
        //nothing to do. 
    }

    protected void beforeRuleGeneration() {
        // nothing to do. 
        // TOFIX this should be done differently
        // JABA SLOEBER create the size.awk file
        // ICConfigurationDescription confDesc =
        // ManagedBuildManager.getDescriptionForConfiguration(config);
        // IWorkspaceRoot root = CCorePlugin.getWorkspace().getRoot();
        // IFile sizeAwkFile1 =
        // root.getFile(topBuildDir.getFullPath().append("size.awk"));
        // File sizeAwkFile = sizeAwkFile1.getLocation().toFile();
        // String regex = Common.getBuildEnvironmentVariable(confDesc,
        // "recipe.size.regex", EMPTY);
        // String awkContent = "/" + regex + "/ {arduino_size += $2 }\n";
        // regex = Common.getBuildEnvironmentVariable(confDesc,
        // "recipe.size.regex.data", EMPTY);
        // awkContent += "/" + regex + "/ {arduino_data += $2 }\n";
        // regex = Common.getBuildEnvironmentVariable(confDesc,
        // "recipe.size.regex.eeprom", EMPTY);
        // awkContent += "/" + regex + "/ {arduino_eeprom += $2 }\n";
        // awkContent += "END { print \"\\n";
        // String max = Common.getBuildEnvironmentVariable(confDesc,
        // "upload.maximum_size", "10000");
        // awkContent += Messages.sizeReportSketch.replace("maximum_size", max);
        // awkContent += "\\n";
        // max = Common.getBuildEnvironmentVariable(confDesc,
        // "upload.maximum_data_size", "10000");
        // awkContent += Messages.sizeReportData.replace("maximum_data_size", max);
        // awkContent += "\\n";
        // awkContent += "\"}";
        //
        // try {
        // FileUtils.write(sizeAwkFile, awkContent, Charset.defaultCharset());
        // } catch (IOException e) {
        // // TODO Auto-generated catch block
        // e.printStackTrace();
        // }
        // END JABA SLOEBER create the size.awk file
    }

    /****************************************************************************************
     * End of Convenience methods to interfere in the makefile generation
     *****************************************************************************************/

    /****************************************************************************************
     * Some Static house keeping methods
     *****************************************************************************************/

    /**
     * Check whether the build has been cancelled. Cancellation requests propagated
     * to the caller by throwing <code>OperationCanceledException</code>.
     *
     * @see org.eclipse.core.runtime.OperationCanceledException#OperationCanceledException()
     */
    private static void checkCancel(IProgressMonitor monitor) {
        if (monitor != null && monitor.isCanceled()) {
            throw new OperationCanceledException();
        }
    }

    private static void updateMonitor(String msg, IProgressMonitor monitor) {
        if (monitor != null && !monitor.isCanceled()) {
            monitor.subTask(msg);
            monitor.worked(1);
        }
    }
}
