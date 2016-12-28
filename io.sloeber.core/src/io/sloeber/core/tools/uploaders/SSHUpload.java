package io.sloeber.core.tools.uploaders;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.console.MessageConsoleStream;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

import cc.arduino.packages.ssh.NoInteractionUserInfo;
import cc.arduino.packages.ssh.SCP;
import cc.arduino.packages.ssh.SSH;
import io.sloeber.common.Common;
import io.sloeber.common.Const;
import io.sloeber.core.api.PasswordManager;

public class SSHUpload implements IRealUpload {

    String myHost;

    private MessageConsoleStream myHighLevelConsoleStream;
    private MessageConsoleStream myOutconsole;
    private MessageConsoleStream myErrconsole;
    private String myUpLoadTool;
    private IProject myProject;

    SSHUpload(IProject project, String upLoadTool, MessageConsoleStream HighLevelConsoleStream,
	    MessageConsoleStream Outconsole, MessageConsoleStream Errconsole, String host) {

	this.myHost = host;

	this.myHighLevelConsoleStream = HighLevelConsoleStream;
	this.myErrconsole = Errconsole;
	this.myOutconsole = Outconsole;
	this.myUpLoadTool = upLoadTool;
	this.myProject = project;
    }

    @Override
    public boolean uploadUsingPreferences(IFile hexFile, boolean usingProgrammer, IProgressMonitor monitor) {
	boolean ret = true;
	if (usingProgrammer) {
	    this.myHighLevelConsoleStream.println(Messages.Upload_error_network);
	    return false;
	}
	PasswordManager pwdManager = new PasswordManager();
	if (!pwdManager.setHost(this.myHost)) {
	    // TODO need a way to get to the password now the gui is no longer
	    // in the method
	    Common.log(new Status(IStatus.ERROR, Const.CORE_PLUGIN_ID,
		    Messages.Upload_login_credentials_missing + this.myHost));
	}

	Session session = null;
	SCP scp = null;
	try {
	    JSch jSch = new JSch();
	    session = jSch.getSession(pwdManager.getLogin(), this.myHost, 22);

	    session.setUserInfo(new NoInteractionUserInfo(pwdManager.getPassword()));
	    session.connect(30000);

	    scp = new SCP(session);
	    SSH ssh = new SSH(session);
	    this.myHighLevelConsoleStream
		    .println(Messages.Upload_sending_sketch + hexFile + Messages.Upload_to + this.myHost);
	    scpFiles(scp, hexFile);
	    this.myHighLevelConsoleStream.println(Messages.Upload_sketch_on_yun);

	    // String additionalParams = verbose ?
	    // prefs.get("upload.params.verbose") :
	    // prefs.get("upload.params.quiet");
	    String remoteUploadCommand = Common.getBuildEnvironmentVariable(this.myProject,
		    "A.TOOLS." + this.myUpLoadTool.toUpperCase() + "_REMOTE.UPLOAD.PATTERN", //$NON-NLS-1$ //$NON-NLS-2$
		    "run-avrdude /tmp/sketch.hex "); //$NON-NLS-1$

	    // not sure why but I need to swap err and out not to get red text
	    PrintStream stderr = new PrintStream(this.myOutconsole);
	    PrintStream stdout = new PrintStream(this.myErrconsole);

	    this.myHighLevelConsoleStream.println("merge-sketch-with-bootloader.lua /tmp/sketch.hex"); //$NON-NLS-1$
	    ret = ssh.execSyncCommand("merge-sketch-with-bootloader.lua /tmp/sketch.hex", stdout, stderr); //$NON-NLS-1$
	    this.myHighLevelConsoleStream.println("kill-bridge"); //$NON-NLS-1$
	    ssh.execSyncCommand("kill-bridge", stdout, stderr); //$NON-NLS-1$
	    this.myHighLevelConsoleStream.println(remoteUploadCommand);
	    ret = ret && ssh.execSyncCommand(remoteUploadCommand, stdout, stderr);

	} catch (JSchException e) {
	    String message = e.getMessage();
	    String errormessage = Const.EMPTY_STRING;
	    if (Messages.Upload_auth_cancel.equals(message) || Messages.Upload_auth_fail.equals(message)) {
		errormessage = new String(Messages.Upload_error_auth_fail) + this.myHost;
		// TODO add to ask if if the user wants to remove the password
		PasswordManager.ErasePassword(this.myHost);
	    }
	    if (e.getMessage().contains(Messages.Upload_connection_refused)) {
		errormessage = new String(Messages.Upload_error_connection_refused) + this.myHost;
	    }
	    this.myHighLevelConsoleStream.println(errormessage);
	    this.myHighLevelConsoleStream.println(message);

	    return false;
	} catch (Exception e) {
	    this.myHighLevelConsoleStream.println(e.getMessage());
	    return false;
	} finally {
	    if (scp != null) {
		try {
		    scp.close();
		} catch (IOException e) {
		    this.myHighLevelConsoleStream.println(e.getMessage());
		    return false;
		}
	    }
	    if (session != null) {
		session.disconnect();
	    }

	}
	return ret;
    }

    private void scpFiles(SCP scp, IFile hexFile) throws IOException {
	File uploadFile = null;
	try {
	    scp.open();
	    scp.startFolder("tmp"); //$NON-NLS-1$
	    uploadFile = hexFile.getLocation().toFile();
	    scp.sendFile(uploadFile, "sketch.hex"); //$NON-NLS-1$
	    scp.endFolder();
	} catch (IOException e) {
	    this.myHighLevelConsoleStream.println(Messages.Upload_failed_upload + uploadFile);
	    throw (e);

	} finally {
	    scp.close();
	}
    }

}
