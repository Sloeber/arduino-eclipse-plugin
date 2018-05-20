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
import io.sloeber.core.Messages;
import io.sloeber.core.api.BoardDescriptor;
import io.sloeber.core.api.PasswordManager;
import io.sloeber.core.common.Common;

public class SSHUpload implements IRealUpload {

	private static final String FILE = Messages.FILE;
	private static final String PORT =Messages.PORT;
	private static final String HOST =Messages.HOST;


	String myHost;
 

	private String myUpLoadTool;
	private IProject myProject;

	SSHUpload(IProject project, String upLoadTool, String host) {

		myHost = host;
		myUpLoadTool = upLoadTool;
		myProject = project;
	}

	@Override
	public boolean uploadUsingPreferences(IFile hexFile, BoardDescriptor boardDescriptor, IProgressMonitor monitor, 
			MessageConsoleStream highStream,
			MessageConsoleStream outStream,
			MessageConsoleStream errStream) {
		boolean ret = true;
		if (boardDescriptor.usesProgrammer()) {
			highStream.println(Messages.Upload_error_network);
			return false;
		}

		Session session = null;
		SCP scp = null;
		try {
			JSch jSch = new JSch();
			SSHClientSetupChainRing sshClientSetupChain = new SSHConfigFileSetup(new SSHPwdSetup());
			BoardPort boardPort = new BoardPort();
			boardPort.setBoardName(myHost);
			session = sshClientSetupChain.setup(boardPort, jSch);
			if (session != null) {
				session.setConfig("PreferredAuthentications", "publickey,keyboard-interactive,password"); //$NON-NLS-1$ //$NON-NLS-2$

				session.connect(30000);

				scp = new SCP(session);
				SSH ssh = new SSH(session);
				highStream
						.println(Messages.Upload_sending_sketch.replace(FILE, hexFile.toString()).replace(PORT, myHost));
				scpFiles(scp, hexFile,highStream);
				highStream.println(Messages.Upload_sketch_on_yun);

				String remoteUploadCommand = Common.getBuildEnvironmentVariable(myProject,
						"A.TOOLS." + myUpLoadTool.toUpperCase() + "_REMOTE.UPLOAD.PATTERN", //$NON-NLS-1$ //$NON-NLS-2$
						"run-avrdude /tmp/sketch.hex "); //$NON-NLS-1$

				highStream.println("merge-sketch-with-bootloader.lua /tmp/sketch.hex"); //$NON-NLS-1$
				ret = ssh.execSyncCommand("merge-sketch-with-bootloader.lua /tmp/sketch.hex", outStream, //$NON-NLS-1$
						errStream);
				highStream.println("kill-bridge"); //$NON-NLS-1$
				ssh.execSyncCommand("kill-bridge", outStream, errStream); //$NON-NLS-1$
				highStream.println(remoteUploadCommand);
				ret = ret && ssh.execSyncCommand(remoteUploadCommand, outStream, errStream);
			}

		} catch (JSchException e) {
			String message = e.getMessage();
			String errormessage = new String();
			if ("Auth cancel".equals(message) || "Auth fail".equals(message)) { //$NON-NLS-1$ //$NON-NLS-2$
				errormessage = Messages.Upload_error_auth_fail.replace(HOST, myHost);
				// TODO add to ask if if the user wants to remove the password
				PasswordManager.ErasePassword(myHost);
			}
			if (e.getMessage().contains("Connection refused")) { //$NON-NLS-1$
				errormessage = Messages.Upload_error_connection_refused.replace(HOST, myHost);
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

	private static void scpFiles(SCP scp, IFile hexFile,MessageConsoleStream myHighStream) throws IOException {
		File uploadFile = null;
		try {
			scp.open();
			scp.startFolder("tmp"); //$NON-NLS-1$
			uploadFile = hexFile.getLocation().toFile();
			scp.sendFile(uploadFile, "sketch.hex"); //$NON-NLS-1$
			scp.endFolder();
		} catch (IOException e) {
			myHighStream.println(Messages.Upload_failed_upload_file.replace(FILE, uploadFile.toString()));
			throw (e);

		} finally {
			scp.close();
		}
	}

}
