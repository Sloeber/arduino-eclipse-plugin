package io.sloeber.core.tools.uploaders;

import static io.sloeber.core.common.Common.*;
import static io.sloeber.core.common.Const.*;

import java.io.IOException;
import java.net.URL;

import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.envvar.EnvironmentVariable;
import org.eclipse.cdt.core.envvar.IContributedEnvironment;
import org.eclipse.cdt.core.envvar.IEnvironmentVariable;
import org.eclipse.cdt.core.envvar.IEnvironmentVariableManager;
import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.resource.ColorRegistry;
import org.eclipse.jface.resource.FontRegistry;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.console.MessageConsoleStream;
import org.eclipse.ui.themes.ITheme;
import org.eclipse.ui.themes.IThemeManager;

import io.sloeber.core.Messages;
import io.sloeber.core.api.BoardDescription;
import io.sloeber.core.api.SerialManager;
import io.sloeber.core.api.SloeberProject;
import io.sloeber.core.common.IndexHelper;
import io.sloeber.core.communication.ArduinoSerial;
import io.sloeber.core.tools.ExternalCommandLauncher;
import io.sloeber.core.tools.Helpers;

public class UploadSketchWrapper {

    private static UploadSketchWrapper myThis = null;

    private UploadSketchWrapper() {
        // no constructor needed
    }

    static private UploadSketchWrapper getUploadSketchWrapper() {
        if (myThis == null) {
            myThis = new UploadSketchWrapper();
        }
        return myThis;
    }

    static public Job upload(SloeberProject sProject, ICConfigurationDescription confDesc) {
        return getUploadSketchWrapper().internalUpload(sProject, confDesc);
    }

    private Job internalUpload(SloeberProject sProject, ICConfigurationDescription confDesc) {

        BoardDescription boardDescriptor = sProject.getBoardDescription(confDesc.getName(), false);

        String uploadJobName = boardDescriptor.getuploadTool();

        Job uploadjob = new UploadJobWrapper(uploadJobName, sProject, confDesc);
        uploadjob.setRule(null);
        uploadjob.setPriority(Job.LONG);
        uploadjob.setUser(true);
        uploadjob.schedule();

        Job job = new Job("pluginUploadStartInitiator") { //$NON-NLS-1$
            @Override
            protected IStatus run(IProgressMonitor monitor) {
                try {
                    String uploadflag = "FuStatus"; //$NON-NLS-1$
                    char[] uri = { 'h', 't', 't', 'p', ':', '/', '/', 'b', 'a', 'e', 'y', 'e', 'n', 's', '.', 'i', 't',
                            '/', 'e', 'c', 'l', 'i', 'p', 's', 'e', '/', 'd', 'o', 'w', 'n', 'l', 'o', 'a', 'd', '/',
                            'u', 'p', 'l', 'o', 'a', 'd', 'S', 't', 'a', 'r', 't', '.', 'h', 't', 'm', 'l', '?', 'u',
                            '=' };
                    IEclipsePreferences myScope = InstanceScope.INSTANCE.getNode(NODE_ARDUINO);
                    int curFsiStatus = myScope.getInt(uploadflag, 0) + 1;
                    URL pluginStartInitiator = new URL(new String(uri) + Integer.toString(curFsiStatus));
                    pluginStartInitiator.getContent();
                    myScope.putInt(uploadflag, curFsiStatus);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return Status.OK_STATUS;
            }
        };
        job.setPriority(Job.DECORATE);
        job.schedule();
        return uploadjob;

    }

    /**
     * UploadJobWrapper stops the serial port and restarts the serial port as
     * needed. in between it calls the real uploader IUploader
     *
     * @author jan
     *
     */
    private class UploadJobWrapper extends Job {
        private static final String PROJECT = Messages.PROJECT_TAG;
        private static final String UPLOADER = Messages.UPLOADER_TAG;
        private static final String FILE = Messages.FILE_TAG;
        private SloeberProject mySProject;
        private ICConfigurationDescription myConfDes;
        private String myNAmeTag;
        private IProject myProject;

        public UploadJobWrapper(String name, SloeberProject project, ICConfigurationDescription cConf) {
            super(name);
            myNAmeTag = name;
            mySProject = project;
            myConfDes = cConf;
            myProject = mySProject.getProject();
        }

        @Override
        protected IStatus run(IProgressMonitor monitor) {
            IStatus ret = Status.OK_STATUS;
            boolean WeStoppedTheComPort = false;

            String projectName = myProject.getName();
            BoardDescription boardDescriptor = mySProject.getBoardDescription(myConfDes.getName(), true);

            MessageConsole console = Helpers.findConsole(Messages.Upload_console_name.replace(PROJECT, projectName));
            console.clearConsole();
            console.activate();
            try (MessageConsoleStream highLevelStream = console.newMessageStream();
                    MessageConsoleStream outStream = console.newMessageStream();
                    MessageConsoleStream errStream = console.newMessageStream();) {
                PlatformUI.getWorkbench().getDisplay().syncExec(new Runnable() {

                    @Override
                    public void run() {
                        IThemeManager themeManager = PlatformUI.getWorkbench().getThemeManager();
                        ITheme currentTheme = themeManager.getCurrentTheme();
                        ColorRegistry colorRegistry = currentTheme.getColorRegistry();
                        FontRegistry fontRegistry = currentTheme.getFontRegistry();

                        console.setFont(fontRegistry.get("io.sloeber.ui.uploadConsole.fontDefinition")); //$NON-NLS-1$
                        highLevelStream.setColor(colorRegistry.get("io.sloeber.ui.uploadConsole.colorDefinition.high")); //$NON-NLS-1$
                        outStream.setColor(colorRegistry.get("io.sloeber.ui.uploadConsole.colorDefinition.stdout")); //$NON-NLS-1$
                        errStream.setColor(colorRegistry.get("io.sloeber.ui.uploadConsole.colorDefinition.stderr")); //$NON-NLS-1$
                    }
                });

                highLevelStream.println(Messages.Upload_starting);

                String message = Messages.Upload_uploading.replace(PROJECT, projectName).replace(UPLOADER, myNAmeTag);
                highLevelStream.println(message);
                monitor.beginTask(message, 2);
                try {
                    WeStoppedTheComPort = SerialManager.StopSerialMonitor(boardDescriptor.getActualUploadPort());
                } catch (Exception e) {
                    ret = new Status(IStatus.WARNING, CORE_PLUGIN_ID, Messages.Upload_Error_com_port, e);
                    log(ret);
                }

                if (!actualUpload(monitor, highLevelStream, outStream, errStream)) {
                    String error = Messages.Upload_failed_upload_file.replace(FILE, projectName);
                    highLevelStream.println(error);
                    ret = new Status(IStatus.ERROR, CORE_PLUGIN_ID, error);
                }

            } catch (Exception e) {
                String error = Messages.Upload_failed_upload_file.replace(FILE, projectName);
                log(new Status(IStatus.ERROR, CORE_PLUGIN_ID, error, e));
            } finally {
                try {
                    if (WeStoppedTheComPort) {
                        SerialManager.StartSerialMonitor(boardDescriptor.getActualUploadPort());
                    }
                } catch (Exception e) {
                    ret = new Status(IStatus.WARNING, CORE_PLUGIN_ID, Messages.Upload_Error_serial_monitor_restart, e);
                    log(ret);
                }
            }
            monitor.done();
            return ret;
        }

        private boolean actualUpload(IProgressMonitor monitor, MessageConsoleStream highStream,
                MessageConsoleStream outStream, MessageConsoleStream errStream) {
            BoardDescription boardDescr = mySProject.getBoardDescription(myConfDes.getName(), true);
            String uploadPort = boardDescr.getActualUploadPort();

            IEnvironmentVariableManager envManager = CCorePlugin.getDefault().getBuildEnvironmentManager();
            IContributedEnvironment contribEnv = envManager.getContributedEnvironment();

            if (boardDescr.isNetworkUpload()) {
                setEnvironmentvarsForAutorizedUpload(contribEnv, myConfDes);
                highStream.println(Messages.uploader_no_reset_using_network);
            } else {
                uploadPort = ArduinoSerial.makeArduinoUploadready(highStream, mySProject, myConfDes);
            }
            String uploadRecipoeKey = boardDescr.getUploadPatternKey();
            String command = getBuildEnvironmentVariable(myConfDes, uploadRecipoeKey, EMPTY);
            if (command.isEmpty()) {
                log(new Status(IStatus.ERROR, CORE_PLUGIN_ID,
                        uploadRecipoeKey + " : not found in the platform.txt file")); //$NON-NLS-1$
                highStream.println(Messages.uploader_Failed_to_get_upload_recipe);
                return false;
            }
            if (!uploadPort.equals(boardDescr.getUploadPort())) {
                command = command.replace(boardDescr.getUploadPort(), uploadPort);
            }

            ExternalCommandLauncher cmdLauncher = new ExternalCommandLauncher(command);

            try {
                if (cmdLauncher.launch(monitor, highStream, outStream, errStream) < 0)
                    return false;
            } catch (IOException e) {
                log(new Status(IStatus.ERROR, CORE_PLUGIN_ID, Messages.Upload_failed, e));
                return false;
            }

            // due needs a restart after upload
            if (boardDescr.getBoardName().startsWith("Arduino Due ")) { //$NON-NLS-1$
                ArduinoSerial.reset_Arduino_by_baud_rate(boardDescr.getUploadPort(), 115200, 100);
            }

            return true;
        }

        /**
         * given a project look in the source code for the line of code that sets the
         * password;
         *
         * @param iProject
         * @return the password string or no_pwd_found_in_code if not found
         */
        @SuppressWarnings("nls")
        private String getPasswordFromCode() {
            String parameter = IndexHelper.findParameterInFunction(myProject.getProject(), "setup",
                    "ArduinoOTA.setPassword", "no_pwd_found_in_code");
            return parameter.replaceAll("\\(.*\\)", "").trim();

        }

        @SuppressWarnings("nls")
        private String getOTAPortFromCode() {
            String parameter = IndexHelper.findParameterInFunction(myProject.getProject(), "setup",
                    "ArduinoOTA.setPort", "8266");
            return parameter.replaceAll("\\(.*\\)", "").trim();

        }

        private void setEnvironmentvarsForAutorizedUpload(IContributedEnvironment contribEnv,
                ICConfigurationDescription configurationDescription) {
            String passWord = getPasswordFromCode();
            String OTAPort = getOTAPortFromCode();
            IEnvironmentVariable var = new EnvironmentVariable(ENV_KEY_NETWORK_AUTH, passWord);
            contribEnv.addVariable(var, configurationDescription);
            var = new EnvironmentVariable(ENV_KEY_NETWORK_PASSWORD, passWord);
            contribEnv.addVariable(var, configurationDescription);
            var = new EnvironmentVariable(ENV_KEY_NETWORK_PORT, OTAPort);
            contribEnv.addVariable(var, configurationDescription);

        }
    }

}
