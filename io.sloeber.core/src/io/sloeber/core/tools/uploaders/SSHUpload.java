package io.sloeber.core.tools.uploaders;

import java.io.File;
import java.io.IOException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ui.console.MessageConsoleStream;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

import cc.arduino.packages.BoardPort;
import cc.arduino.packages.ssh.SCP;
import cc.arduino.packages.ssh.SSH;
import cc.arduino.packages.ssh.SSHClientSetupChainRing;
import cc.arduino.packages.ssh.SSHConfigFileSetup;
import cc.arduino.packages.ssh.SSHPwdSetup;
import io.sloeber.core.api.BoardDescriptor;
import io.sloeber.core.api.PasswordManager;
import io.sloeber.core.common.Common;

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
	public boolean uploadUsingPreferences(IFile hexFile, BoardDescriptor boardDescriptor, IProgressMonitor monitor) {
		boolean ret = true;
		if (boardDescriptor.usesProgrammer()) {
			this.myHighLevelConsoleStream.println(Messages.Upload_error_network);
			return false;
		}

		Session session = null;
		SCP scp = null;
		try {
			JSch jSch = new JSch();
			SSHClientSetupChainRing sshClientSetupChain = new SSHConfigFileSetup(new SSHPwdSetup());
			BoardPort boardPort = new BoardPort();
			boardPort.setBoardName(this.myHost);
			session = sshClientSetupChain.setup(boardPort, jSch);
			if (session != null) {
				session.setConfig("PreferredAuthentications", "publickey,keyboard-interactive,password"); //$NON-NLS-1$ //$NON-NLS-2$

				session.connect(30000);

				scp = new SCP(session);
				SSH ssh = new SSH(session);
				this.myHighLevelConsoleStream
						.println(Messages.Upload_sending_sketch + hexFile + Messages.Upload_to + this.myHost);
				scpFiles(scp, hexFile);
				this.myHighLevelConsoleStream.println(Messages.Upload_sketch_on_yun);

				String remoteUploadCommand = Common.getBuildEnvironmentVariable(this.myProject,
						"A.TOOLS." + this.myUpLoadTool.toUpperCase() + "_REMOTE.UPLOAD.PATTERN", //$NON-NLS-1$ //$NON-NLS-2$
						"run-avrdude /tmp/sketch.hex "); //$NON-NLS-1$

				this.myHighLevelConsoleStream.println("merge-sketch-with-bootloader.lua /tmp/sketch.hex"); //$NON-NLS-1$
				ret = ssh.execSyncCommand("merge-sketch-with-bootloader.lua /tmp/sketch.hex", this.myOutconsole, //$NON-NLS-1$
						this.myErrconsole);
				this.myHighLevelConsoleStream.println("kill-bridge"); //$NON-NLS-1$
				ssh.execSyncCommand("kill-bridge", this.myOutconsole, this.myErrconsole); //$NON-NLS-1$
				this.myHighLevelConsoleStream.println(remoteUploadCommand);
				ret = ret && ssh.execSyncCommand(remoteUploadCommand, this.myOutconsole, this.myErrconsole);
			}

		} catch (JSchException e) {
			String message = e.getMessage();
			String errormessage = new String();
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
				scp.close();
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
