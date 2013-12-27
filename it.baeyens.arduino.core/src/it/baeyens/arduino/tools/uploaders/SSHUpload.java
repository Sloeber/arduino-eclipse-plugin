package it.baeyens.arduino.tools.uploaders;

import it.baeyens.arduino.tools.PasswordManager;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ui.console.MessageConsoleStream;

import cc.arduino.packages.ssh.NoInteractionUserInfo;
import cc.arduino.packages.ssh.SCP;
import cc.arduino.packages.ssh.SSH;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

public class SSHUpload implements IRealUpload {
    // private static final List<String> FILES_NOT_TO_COPY = Arrays.asList(".DS_Store", ".Trash", "Thumbs.db", "__MACOSX");
    String myPassword;
    String myHost;
    String myUser;
    private MessageConsoleStream myHighLevelConsoleStream;
    private MessageConsoleStream myOutconsole;
    private MessageConsoleStream myErrconsole;

    SSHUpload(MessageConsoleStream HighLevelConsoleStream, MessageConsoleStream Outconsole, MessageConsoleStream Errconsole, String password,
	    String host, String user) {
	myPassword = password;
	myHost = host;
	myUser = user;
	myHighLevelConsoleStream = HighLevelConsoleStream;
	myErrconsole = Errconsole;
	myOutconsole = Outconsole;
    }

    @Override
    public boolean uploadUsingPreferences(IFile hexFile, IProject project, boolean usingProgrammer, IProgressMonitor monitor) {
	boolean ret = true;
	if (usingProgrammer) {
	    myHighLevelConsoleStream.println("ERROR: Network upload using programmer not supported");
	    return false;
	}

	Session session = null;
	SCP scp = null;
	try {
	    JSch jSch = new JSch();
	    session = jSch.getSession(myUser, myHost, 22);

	    session.setUserInfo(new NoInteractionUserInfo(myPassword));
	    session.connect(30000);

	    scp = new SCP(session);
	    SSH ssh = new SSH(session);
	    myHighLevelConsoleStream.println("Sending sketch " + hexFile + " to " + myHost);
	    scpFiles(scp, hexFile);
	    myHighLevelConsoleStream.println("Sketch is now on yun: /tmp/sketch.hex");

	    // String additionalParams = verbose ? prefs.get("upload.params.verbose") : prefs.get("upload.params.quiet");
	    String additionalParams = "";// Common.getBuildEnvironmentVariable(myProject, myCConf, ArduinoConst. upload.params.quiet, "");

	    // not sure why but I need to swap err and out not to get red text
	    PrintStream stderr = new PrintStream(myOutconsole);
	    PrintStream stdout = new PrintStream(myErrconsole);

	    myHighLevelConsoleStream.println("merge-sketch-with-bootloader.lua /tmp/sketch.hex");
	    ret = ssh.execSyncCommand("merge-sketch-with-bootloader.lua /tmp/sketch.hex", stdout, stderr);
	    myHighLevelConsoleStream.println("kill-bridge");
	    ssh.execSyncCommand("kill-bridge", stdout, stderr);
	    myHighLevelConsoleStream.println("run-avrdude /tmp/sketch.hex '" + additionalParams + "'");
	    ret = ret && ssh.execSyncCommand("run-avrdude /tmp/sketch.hex '" + additionalParams + "'", stdout, stderr);

	} catch (JSchException e) {
	    String message = e.getMessage();
	    String errormessage = "";
	    if ("Auth cancel".equals(message) || "Auth fail".equals(message)) {
		errormessage = new String("ERROR: Authentication failed ") + myHost;
		// TODO add to ask if if the user wants to remove the password
		PasswordManager.ErasePassword(myHost);
	    }
	    if (e.getMessage().contains("Connection refused")) {
		errormessage = new String("ERROR: Unable to connect to ") + myHost;
	    }
	    myHighLevelConsoleStream.println(errormessage);
	    myHighLevelConsoleStream.println(message);

	    return false;
	} catch (Exception e) {
	    myHighLevelConsoleStream.println(e.getMessage());
	    return false;
	} finally {
	    if (scp != null) {
		try {
		    scp.close();
		} catch (IOException e) {
		    myHighLevelConsoleStream.println(e.getMessage());
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
	    scp.startFolder("tmp");
	    uploadFile = hexFile.getLocation().toFile();
	    scp.sendFile(uploadFile, "sketch.hex");
	    scp.endFolder();

	    // if (canUploadWWWFiles(project, ssh)) {
	    // scp.startFolder("www");
	    // scp.startFolder("sd");
	    // scp.startFolder(sourcePath.getName());
	    // recursiveSCP(new File(sourcePath.toString(), "www"), scp);
	    // scp.endFolder();
	    // scp.endFolder();
	    // scp.endFolder();
	    // }
	} catch (IOException e) {
	    myHighLevelConsoleStream.println("failed to upload " + uploadFile);
	    throw (e);

	} finally {
	    scp.close();
	}
    }

    // private void recursiveSCP(File from, SCP scp) throws IOException {
    // File[] files = from.listFiles();
    // if (files == null) {
    // return;
    // }
    //
    // for (File file : files) {
    // // if (!StringUtils. (file.getName(), FILES_NOT_TO_COPY)) {
    // if (file.isDirectory() && file.canExecute()) {
    // scp.startFolder(file.getName());
    // recursiveSCP(file, scp);
    // scp.endFolder();
    // } else if (file.isFile() && file.canRead()) {
    // scp.sendFile(file);
    // }
    // // }
    // }
    // }
    //
    // @SuppressWarnings("static-method")
    // private boolean canUploadWWWFiles(IProject project, SSH ssh) throws IOException, JSchException {
    // File www = new File(project.getLocationURI().toString(), "www");
    // if (!www.exists() || !www.isDirectory()) {
    // return false;
    // }
    // if (!www.canExecute()) {
    // // warningsAccumulator.add(_("Problem accessing files in folder ") + www);
    // return false;
    // }
    // if (!ssh.execSyncCommand("special-storage-available")) {
    // // warningsAccumulator.add(_("Problem accessing board folder /www/sd"));
    // return false;
    // }
    // return true;
    // }

}
