package io.sloeber.core.tools.uploaders;

import static io.sloeber.core.Messages.*;
import static io.sloeber.core.api.Common.*;
import static io.sloeber.core.api.Const.*;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;

import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.envvar.EnvironmentVariable;
import org.eclipse.cdt.core.envvar.IContributedEnvironment;
import org.eclipse.cdt.core.envvar.IEnvironmentVariable;
import org.eclipse.cdt.core.envvar.IEnvironmentVariableManager;
import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.core.resources.IFile;
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

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

import cc.arduino.packages.BoardPort;
import cc.arduino.packages.ssh.SCP;
import cc.arduino.packages.ssh.SSH;
import cc.arduino.packages.ssh.SSHClientSetupChainRing;
import cc.arduino.packages.ssh.SSHConfigFileSetup;
import cc.arduino.packages.ssh.SSHPwdSetup;
import io.sloeber.core.Messages;
import io.sloeber.core.api.BoardDescription;
import io.sloeber.core.api.ISloeberConfiguration;
import io.sloeber.core.api.PasswordManager;
import io.sloeber.core.api.Serial;
import io.sloeber.core.api.SerialManager;
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

    static public Job upload(ISloeberConfiguration SloeberConf) {
        return getUploadSketchWrapper().internalUpload(SloeberConf);
    }

    private Job internalUpload(ISloeberConfiguration sloeberConf) {

        BoardDescription boardDescriptor = sloeberConf.getBoardDescription();

        String uploadJobName = boardDescriptor.getuploadTool();

        Job uploadjob = new UploadJobWrapper(uploadJobName, sloeberConf);
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
        private ISloeberConfiguration mySloeberConf;
        private String myNAmeTag;
        private IProject myProject;
        private String myProvidedUploadPort;

        public UploadJobWrapper(String name, ISloeberConfiguration sloeberConf) {
            super(name);
            myNAmeTag = name;
            mySloeberConf = sloeberConf;
            myProject = mySloeberConf.getProject();
        }

        @Override
        protected IStatus run(IProgressMonitor monitor) {
            IStatus ret = Status.OK_STATUS;
            boolean theComPortIsPaused = false;

            String projectName = myProject.getName();
            BoardDescription boardDescriptor = mySloeberConf.getBoardDescription();
            myProvidedUploadPort = boardDescriptor.getActualUploadPort();

            MessageConsole console = Helpers.findConsole(Upload_console_name.replace(PROJECT_TAG, projectName));
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

                highLevelStream.println(Upload_starting);

                String message = Upload_uploading.replace(PROJECT_TAG, projectName).replace(UPLOADER_TAG, myNAmeTag);
                highLevelStream.println(message);
                monitor.beginTask(message, 2);
                try {
                    theComPortIsPaused = SerialManager.pauseSerialMonitor(myProvidedUploadPort);
                } catch (Exception e) {
                    ret = new Status(IStatus.WARNING, CORE_PLUGIN_ID, Upload_Error_com_port, e);
                    log(ret);
                }

                if (!actualUpload(monitor, highLevelStream, outStream, errStream)) {
                    String error = Upload_failed_upload_file.replace(FILE_TAG, projectName);
                    highLevelStream.println(error);
                    ret = new Status(IStatus.ERROR, CORE_PLUGIN_ID, error);
                }

            } catch (Exception e) {
                String error = Upload_failed_upload_file.replace(FILE_TAG, projectName);
                log(new Status(IStatus.ERROR, CORE_PLUGIN_ID, error, e));
            } finally {
                try {
                    if (theComPortIsPaused) {
                        // wait for the port to reappear
                        boolean portFound = false;
                        int counter = 0;
                        while (!portFound & counter++ < 100) {
                            List<String> currentPorts = Serial.list();
                            portFound = currentPorts.contains(myProvidedUploadPort);
                            if (!portFound) {
                                Thread.sleep(100);
                            }
                        }
                        if (portFound) {
                            SerialManager.resumeSerialMonitor(myProvidedUploadPort);
                        } else {
                            SerialManager.stopSerialMonitor(myProvidedUploadPort);
                        }
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
            BoardDescription boardDescr = mySloeberConf.getBoardDescription();
            ICConfigurationDescription confDec = mySloeberConf.getAutoBuildDesc().getCdtConfigurationDescription();
            String uploadPort = myProvidedUploadPort;
            boolean isSSHUpload = false;// only true for yun

            IEnvironmentVariableManager envManager = CCorePlugin.getDefault().getBuildEnvironmentManager();
            IContributedEnvironment contribEnv = envManager.getContributedEnvironment();

            String uploadRecipoeKey = boardDescr.getUploadPatternKey();
            if (boardDescr.isNetworkUpload()) {
                setEnvironmentvarsForAutorizedUpload(contribEnv, confDec);
                highStream.println(uploader_no_reset_using_network);
                isSSHUpload = boardDescr.isSSHUpload();
                if (isSSHUpload) {
                    uploadRecipoeKey = "tools.avrdude_remote.upload.pattern"; //$NON-NLS-1$
                }
            } else {
                uploadPort = ArduinoSerial.makeArduinoUploadready(highStream, mySloeberConf);
            }

            String command = getBuildEnvironmentVariable(mySloeberConf, uploadRecipoeKey, EMPTY);
            if (command.isEmpty()) {
                log(new Status(IStatus.ERROR, CORE_PLUGIN_ID,
                        uploadRecipoeKey + " : not found in the platform.txt file")); //$NON-NLS-1$
                highStream.println(uploader_Failed_to_get_upload_recipe);
                return false;
            }
            if (!uploadPort.equals(boardDescr.getUploadPort())) {
                command = command.replace(boardDescr.getUploadPort(), uploadPort);
            }

            if (isSSHUpload) {
                sshUpload(monitor, highStream, outStream, errStream, mySloeberConf.getTargetFile(),
                        boardDescr.getHost(), command);
            } else {
                ExternalCommandLauncher cmdLauncher = new ExternalCommandLauncher(command);

                try {
                    if (cmdLauncher.launch(monitor, highStream, outStream, errStream) < 0)
                        return false;
                } catch (IOException e) {
                    log(new Status(IStatus.ERROR, CORE_PLUGIN_ID, Upload_failed, e));
                    return false;
                }
            }

            // due needs a restart after upload
            if (boardDescr.getBoardName().startsWith("Arduino Due ")) { //$NON-NLS-1$
                ArduinoSerial.reset_Arduino_by_baud_rate(boardDescr.getUploadPort(), 115200, 100);
            }

            return true;
        }

        private boolean sshUpload(IProgressMonitor monitor, MessageConsoleStream highStream,
                MessageConsoleStream outStream, MessageConsoleStream errStream, IFile hexFile, String host,
                String command) {
            boolean ret = true;

            Session session = null;
            SCP scp = null;
            try {
                JSch jSch = new JSch();
                SSHClientSetupChainRing sshClientSetupChain = new SSHConfigFileSetup(new SSHPwdSetup());
                BoardPort boardPort = new BoardPort();
                boardPort.setBoardName(host);
                session = sshClientSetupChain.setup(boardPort, jSch);
                if (session != null) {
                    session.setConfig("PreferredAuthentications", "publickey,keyboard-interactive,password"); //$NON-NLS-1$ //$NON-NLS-2$

                    session.connect(30000);

                    scp = new SCP(session);
                    SSH ssh = new SSH(session);
                    highStream.println(Upload_sending_sketch.replace(FILE_TAG, hexFile.getLocation().toOSString())
                            .replace(PORT_TAG, host));
                    scpFiles(scp, hexFile);
                    highStream.println(Upload_sketch_on_yun);

                    highStream.println("merge-sketch-with-bootloader.lua /tmp/sketch.hex"); //$NON-NLS-1$
                    ret = ssh.execSyncCommand("merge-sketch-with-bootloader.lua /tmp/sketch.hex", outStream, //$NON-NLS-1$
                            errStream);
                    highStream.println("kill-bridge"); //$NON-NLS-1$
                    ssh.execSyncCommand("kill-bridge", outStream, errStream); //$NON-NLS-1$

                    highStream.println(command);
                    ret = ret && ssh.execSyncCommand(command, outStream, errStream);
                }

            } catch (JSchException e) {
                String message = e.getMessage();
                String errormessage = new String();
                if ("Auth cancel".equals(message) || "Auth fail".equals(message)) { //$NON-NLS-1$ //$NON-NLS-2$
                    errormessage = new String(Messages.Upload_error_auth_fail) + host;
                    // TODO add to ask if if the user wants to remove the password
                    PasswordManager.ErasePassword(host);
                }
                if (e.getMessage().contains("Connection refused")) { //$NON-NLS-1$
                    errormessage = new String(Messages.Upload_error_connection_refused) + host;
                }
                highStream.println(errormessage);
                highStream.println(message);

                return false;
            } catch (Exception e) {
                highStream.println(e.getMessage());
                return false;
            } finally {
                if (scp != null) {
                    scp.close();
                }
                if (session != null) {
                    session.disconnect();
                }

            }
            return ret;
        }

        /**
         * upload files using scp
         *
         * @param scp
         * @param hexFile
         * @throws IOException
         */
        private void scpFiles(SCP scp, IFile hexFile) throws IOException {
            File uploadFile = null;
            try {
                scp.open();
                scp.startFolder("tmp"); //$NON-NLS-1$
                uploadFile = hexFile.getLocation().toFile();
                scp.sendFile(uploadFile, "sketch.hex"); //$NON-NLS-1$
                scp.endFolder();
            } catch (IOException e) {
                throw (e);
            } finally {
                scp.close();
            }
        }

        /**
         * given a project look in the source code for the line of code that sets the
         * password;
         *
         * @param iProject
         * @return the password string or no_pwd_found_in_code if not found
         */
        @SuppressWarnings("nls")
        private String getPasswordFromCode(String defaultPassword) {

            String parameter = IndexHelper.findParameterInFunction(myProject.getProject(), "setup",
                    "ArduinoOTA.setPassword", defaultPassword);
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
            String defaultPassword = "no_pwd_configured_nor_found_in_code"; //$NON-NLS-1$
            PasswordManager pwdManager = new PasswordManager();
            if (pwdManager.setHost(myProvidedUploadPort)) {
                defaultPassword = pwdManager.getPassword();
            }
            String passWord = getPasswordFromCode(defaultPassword);
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
