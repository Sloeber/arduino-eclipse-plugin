package io.sloeber.core.internal;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import static io.sloeber.core.api.Common.*;
import static io.sloeber.core.api.Const.*;

import org.eclipse.cdt.make.core.IMakeTarget;
import org.eclipse.cdt.make.core.IMakeTargetManager;
import org.eclipse.cdt.make.core.MakeCorePlugin;
import org.eclipse.cdt.make.internal.core.MakeTarget;
import org.eclipse.core.internal.resources.Workspace;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

import io.sloeber.autoBuild.api.AutoBuildConfigurationExtensionDescription;
import io.sloeber.autoBuild.api.IAutoBuildConfigurationDescription;
import io.sloeber.autoBuild.integration.AutoBuildConfigurationDescription;
import io.sloeber.core.Activator;
import io.sloeber.core.Messages;
import io.sloeber.core.api.BoardDescription;
import io.sloeber.core.api.Common;
import io.sloeber.core.api.CompileDescription;
import io.sloeber.core.api.ISloeberConfiguration;
import io.sloeber.core.api.OtherDescription;
import io.sloeber.core.common.ConfigurationPreferences;
import io.sloeber.core.tools.Helpers;
import io.sloeber.core.tools.uploaders.UploadSketchWrapper;

public class SloeberConfiguration extends AutoBuildConfigurationExtensionDescription implements ISloeberConfiguration {

    //configuration data
    BoardDescription myBoardDescription;
    OtherDescription myOtherDesc;
    CompileDescription myCompileDescription;

    //operational data
    boolean myMemoryIsDirty = true;

    //derived data
    private Map<String, String> myEnvironmentVariables = new HashMap<>();
    private boolean myIsCleaningMemory = false;
    private boolean myIsUpdatingProject = false;

    /**
     * copy constructor
     * This constructor must be implemented for each derived class of
     * AutoBuildConfigurationExtensionDescription
     * or you will get run time errors
     * 
     * @param owner
     * @param source
     * @throws Exception
     */
    public SloeberConfiguration(AutoBuildConfigurationDescription owner,
            AutoBuildConfigurationExtensionDescription source) {
        // the code below will throw an error in case source is not an instance of SloeberConfiguration
        // This may sound strange for you but this is exactly what I want JABA

        SloeberConfiguration src = (SloeberConfiguration) source;
        setAutoBuildDescription(owner);
        myBoardDescription = new BoardDescription(src.getBoardDescription());
        myOtherDesc = new OtherDescription(src.getOtherDescription());
        myCompileDescription = new CompileDescription(src.getCompileDescription());
    }

    public SloeberConfiguration(BoardDescription boardDesc, OtherDescription otherDesc,
            CompileDescription compileDescriptor) {
        myBoardDescription = boardDesc;
        myOtherDesc = otherDesc;
        myCompileDescription = compileDescriptor;
    }

    public SloeberConfiguration(IAutoBuildConfigurationDescription autoCfgDescription, String lines, String lineStart,
            String lineEnd) {
        setAutoBuildDescription(autoCfgDescription);
        Map<String, String> envVars = new HashMap<>();
        int lineStartLength = lineStart.length();
        for (String curLine : lines.split(Pattern.quote(lineEnd))) {
            String cleanCurLine = curLine.substring(lineStartLength);
            String[] elements = cleanCurLine.split(EQUAL, 2);
            if (elements.length == 2) {
                envVars.put(elements[0], elements[1]);
            }
        }
        myBoardDescription = new BoardDescription(envVars);
        myOtherDesc = new OtherDescription(envVars);
        myCompileDescription = new CompileDescription(envVars);
        myMemoryIsDirty = true;
        //configure(); Seems I can not dpo the config here
    }

    @Override
    public BoardDescription getBoardDescription() {
        return new BoardDescription(myBoardDescription);
    }

    @Override
    public OtherDescription getOtherDescription() {
        return new OtherDescription(myOtherDesc);
    }

    @Override
    public CompileDescription getCompileDescription() {
        return new CompileDescription(myCompileDescription);
    }

    @Override
    public void copyData(AutoBuildConfigurationExtensionDescription from) {
        // TODO Auto-generated method stub

    }

    @Override
    public StringBuffer serialize(String linePrefix, String lineEnd) {
        StringBuffer ret = new StringBuffer();
        Map<String, String> envVars = myBoardDescription.getEnvVarsConfig();
        envVars.putAll(myOtherDesc.getEnvVarsConfig());
        envVars.putAll(myCompileDescription.getEnvVarsConfig());
        for (Entry<String, String> curEnvVar : envVars.entrySet()) {
            ret.append(linePrefix + curEnvVar.getKey() + EQUAL + curEnvVar.getValue() + lineEnd);
        }
        configureIfDirty();
        return ret;
    }

    public void addLibrariesToProject(IProject newProjectHandle, Map<String, IPath> librariesToAdd) {
        // TODO Auto-generated method stub

    }

    @Override
    public IProject getProject() {
        return getAutoBuildDescription().getCdtConfigurationDescription().getProjectDescription().getProject();
    }

    @Override
    public IFolder getArduinoCodeFolder() {
        String cdtConfDescName = getAutoBuildDescription().getCdtConfigurationDescription().getName();
        IProject project = getProject();
        return project.getFolder(SLOEBER_ARDUINO_FOLDER_NAME).getFolder(cdtConfDescName);
    }

    @Override
    public IFolder getArduinoCoreFolder() {
        return getArduinoCodeFolder().getFolder(SLOEBER_CODE_FOLDER_NAME);
    }

    @Override
    public IFolder getArduinoVariantFolder() {
        return getArduinoCodeFolder().getFolder(SLOEBER_VARIANT_FOLDER_NAME);
    }

    @Override
    public IFolder getArduinoLibraryFolder() {
        return getArduinoCodeFolder().getFolder(SLOEBER_LIBRARY_FOLDER_NAME);
    }

    @Override
    public Map<String, String> getEnvironmentVariables() {
        configureIfDirty();

        return myEnvironmentVariables;
    }

    private void configureIfDirty() {
        if (myMemoryIsDirty && !myIsCleaningMemory) {
            cleanMemory();
        }
        if (!ResourcesPlugin.getWorkspace().isTreeLocked()) {
            if (projectNeedsUpdate()) {
                updateArduinoCodeLinks();
            }
        }
    }

    private boolean projectNeedsUpdate() {
        IPath corePath = myBoardDescription.getActualCoreCodePath();
        IFolder coreFolder = getArduinoCoreFolder();
        if (!coreFolder.getLocation().equals(corePath)) {
            return true;
        }
        IFolder arduinoVariantFolder = getArduinoVariantFolder();
        IPath variantPath = myBoardDescription.getActualVariantPath();
        if ((variantPath == null) && (variantPath.toFile().exists())) {
            return true;
        }
        if (!arduinoVariantFolder.getLocation().equals(variantPath)) {
            return true;
        }

        return false;
    }

    private void cleanMemory() {
        if (getAutoBuildDescription() == null) {
            //We can not configure if the AutoBuildDescription is not known
            System.err.println("SloeberConfiguration can not be configured if the AutoBuildDescription is not known"); //$NON-NLS-1$
            return;
        }
        myIsCleaningMemory = true;
        getEnvVars();
        myIsCleaningMemory = false;
        myMemoryIsDirty = false;

        return;

    }

    private void getEnvVars() {
        IProject project = getProject();

        myEnvironmentVariables.clear();

        myEnvironmentVariables.put(ENV_KEY_BUILD_SOURCE_PATH, project.getLocation().toOSString());
        myEnvironmentVariables.put(ENV_KEY_BUILD_PATH,
                getAutoBuildDescription().getBuildFolder().getLocation().toOSString());

        if (myBoardDescription != null) {
            myEnvironmentVariables.putAll(myBoardDescription.getEnvVars());
        }
        if (myCompileDescription != null) {
            myEnvironmentVariables.putAll(myCompileDescription.getEnvVars());
        }
        if (myOtherDesc != null) {
            myEnvironmentVariables.putAll(myOtherDesc.getEnvVars());
        }
        // set the paths
        String pathDelimiter = makeEnvironmentVar("PathDelimiter"); //$NON-NLS-1$
        if (Common.isWindows) {
            myEnvironmentVariables.put(SLOEBER_MAKE_LOCATION,
                    ConfigurationPreferences.getMakePath().addTrailingSeparator().toOSString());
            myEnvironmentVariables.put(SLOEBER_AWK_LOCATION,
                    ConfigurationPreferences.getAwkPath().addTrailingSeparator().toOSString());

            String systemroot = makeEnvironmentVar("SystemRoot"); //$NON-NLS-1$
            myEnvironmentVariables.put("PATH", //$NON-NLS-1$
                    makeEnvironmentVar(ENV_KEY_COMPILER_PATH) + pathDelimiter
                            + makeEnvironmentVar(ENV_KEY_BUILD_GENERIC_PATH) + pathDelimiter + systemroot + "\\system32" //$NON-NLS-1$
                            + pathDelimiter + systemroot + pathDelimiter + systemroot + "\\system32\\Wbem" //$NON-NLS-1$
                            + pathDelimiter + makeEnvironmentVar("sloeber_path_extension")); //$NON-NLS-1$
        } else {
            myEnvironmentVariables.put("PATH", makeEnvironmentVar(ENV_KEY_COMPILER_PATH) + pathDelimiter //$NON-NLS-1$
                    + makeEnvironmentVar(ENV_KEY_BUILD_GENERIC_PATH) + pathDelimiter + makeEnvironmentVar("PATH")); //$NON-NLS-1$
        }
    }

    /**
     * get the text for the decorator
     * 
     * @param text
     * @return
     */
    @Override
    public String getDecoratedText(String text) {
        String boardName = myBoardDescription.getBoardName();
        String portName = myBoardDescription.getActualUploadPort();
        if (portName.isEmpty()) {
            portName = Messages.decorator_no_port;
        }
        if (boardName.isEmpty()) {
            boardName = Messages.decorator_no_platform;
        }

        return text + ' ' + boardName + ' ' + ':' + portName;
    }

    /**
     * Synchronous upload of the sketch to the board returning the status.
     *
     * @param project
     * @return the status of the upload. Status.OK means upload is OK
     */
    @Override
    public IStatus upload() {

        Job upLoadJob = UploadSketchWrapper.upload(this);

        if (upLoadJob == null)
            return new Status(IStatus.ERROR, CORE_PLUGIN_ID, Messages.Upload_failed, null);
        try {
            upLoadJob.join();
            return upLoadJob.getResult();
        } catch (InterruptedException e) {
            // not sure if this is needed
            return new Status(IStatus.ERROR, CORE_PLUGIN_ID, Messages.Upload_failed, e);
        }
    }

    @Override
    public IStatus upLoadUsingProgrammer() {
        return BuildTarget("uploadWithProgrammerWithoutBuild"); //$NON-NLS-1$
    }

    @Override
    public IStatus burnBootloader() {
        return BuildTarget("BurnBootLoader"); //$NON-NLS-1$
    }

    private IStatus BuildTarget(String targetName) {

        try {
            IMakeTargetManager targetManager = MakeCorePlugin.getDefault().getTargetManager();
            IContainer targetResource = getAutoBuildDesc().getBuildFolder();
            IMakeTarget itarget = targetManager.findTarget(targetResource, targetName);
            if (itarget == null) {
                itarget = targetManager.createTarget(getProject(), targetName,
                        "org.eclipse.cdt.build.MakeTargetBuilder"); //$NON-NLS-1$
                if (itarget instanceof MakeTarget) {
                    ((MakeTarget) itarget).setBuildTarget(targetName);
                    targetManager.addTarget(targetResource, itarget);
                }
            }
            if (itarget != null) {
                itarget.build(new NullProgressMonitor());
            }
        } catch (CoreException e) {
            return new Status(IStatus.ERROR, CORE_PLUGIN_ID, e.getMessage(), e);
        }
        return Status.OK_STATUS;
    }

    @Override
    public boolean canBeIndexed() {
        return true;
    }

    @Override
    public void setBoardDescription(BoardDescription boardDescription) {
        myBoardDescription = new BoardDescription(boardDescription);
        setIsDirty();
    }

    private void setIsDirty() {
        myMemoryIsDirty = true;
    }

    @Override
    public IFile getTargetFile() {
        // I assume the extension is .hex as the Arduino Framework does not provide the
        // extension nor a key for the uploadable sketch (=build target)
        // as currently this method is only used for network upload via yun this is ok
        // for now
        IProject project = getProject();
        return getAutoBuildDescription().getBuildFolder().getFile(project.getName() + ".hex"); //$NON-NLS-1$
    }

    @Override
    public String getBundelName() {
        return Activator.getId();
    }

    @Override
    public IAutoBuildConfigurationDescription getAutoBuildDesc() {
        return getAutoBuildDescription();
    }

    /**
     * This method adds or updates the Arduino code links in a subfolder named
     * Arduino/[cfg name].
     * 2 linked subfolders named core and variant link to the real Arduino code note
     *
     * 
     */
    private void updateArduinoCodeLinks() {
        if (myIsUpdatingProject) {
            return;
        }
        myIsUpdatingProject = true;
        try {
            IPath corePath = myBoardDescription.getActualCoreCodePath();
            IProject project = getProject();
            IFolder arduinoVariantFolder = getArduinoVariantFolder();
            if (corePath != null) {
                Helpers.addCodeFolder(corePath, getArduinoCoreFolder(), true);
                IPath variantPath = myBoardDescription.getActualVariantPath();
                if ((variantPath == null) || (!variantPath.toFile().exists())) {
                    // remove the existing link
                    Helpers.removeCodeFolder(arduinoVariantFolder);
                } else {
                    Helpers.addCodeFolder(variantPath, arduinoVariantFolder, false);
                }
            }
        } finally {
            myIsUpdatingProject = false;
        }
    }
}
