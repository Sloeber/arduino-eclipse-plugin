package it.baeyens.arduino.tools.uploaders;

import it.baeyens.arduino.common.ArduinoConst;
import it.baeyens.arduino.common.Common;
import it.baeyens.arduino.communication.ArduinoSerial;

import java.io.IOException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.ui.console.MessageConsole;

public class arduinoUploader implements IRealUpload {

    private IProject myProject;
    private String mycConf;
    private String myUploadTool;
    private MessageConsole myConsole;

    arduinoUploader(IProject Project, String cConf, String UploadTool, MessageConsole Console) {
	myProject = Project;
	mycConf = cConf;
	myUploadTool = UploadTool;
	myConsole = Console;
    }

    @Override
    public boolean uploadUsingPreferences(IFile hexFile, IProject project, boolean usingProgrammer, IProgressMonitor monitor) {
	String MComPort = Common.getBuildEnvironmentVariable(myProject, mycConf, ArduinoConst.ENV_KEY_JANTJE_COM_PORT, "");
	String boardName = Common.getBuildEnvironmentVariable(myProject, mycConf, ArduinoConst.ENV_KEY_JANTJE_BOARD_NAME, "");
	String NewComPort = MComPort;
	String command = Common.getBuildEnvironmentVariable(myProject, mycConf, "A.TOOLS." + myUploadTool.toUpperCase() + ".UPLOAD.PATTERN", "");
	NewComPort = ArduinoSerial.makeArduinoUploadready(myProject, mycConf, MComPort);

	command = command.replaceAll(" -P ", " -P " + NewComPort + " ");
	String nakedPort = NewComPort.replace("/dev/", "");
	command = command.replaceAll(" --port= ", " --port=" + nakedPort + " ");

	try {
	    GenericLocalUploader.RunConsoledCommand(myConsole, command, new SubProgressMonitor(monitor, 1));
	} catch (IOException e1) {
	    e1.printStackTrace();

	    return false;
	}
	if (boardName.startsWith("Arduino Due ")) {
	    ArduinoSerial.resetArduinoByBaudRate(MComPort, 115200, 100);
	}

	return true;
    }

}
