package it.baeyens.arduino.tools.uploaders;

import java.io.IOException;

import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.envvar.EnvironmentVariable;
import org.eclipse.cdt.core.envvar.IContributedEnvironment;
import org.eclipse.cdt.core.envvar.IEnvironmentVariable;
import org.eclipse.cdt.core.envvar.IEnvironmentVariableManager;
import org.eclipse.cdt.core.model.CoreModel;
import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.cdt.core.settings.model.ICProjectDescription;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.ui.console.MessageConsole;

import it.baeyens.arduino.common.Const;
import it.baeyens.arduino.communication.ArduinoSerial;

public class arduinoUploader implements IRealUpload {

    private IProject myProject;
    private String mycConf;
    private MessageConsole myConsole;

    arduinoUploader(IProject Project, String cConf, String UploadTool, MessageConsole Console) {
	this.myProject = Project;
	this.mycConf = cConf;
	this.myConsole = Console;
    }

    @Override
    public boolean uploadUsingPreferences(IFile hexFile, IProject project, boolean usingProgrammer,
	    IProgressMonitor monitor) {
	String MComPort = Const.EMPTY_STRING;
	String boardName = Const.EMPTY_STRING;

	IEnvironmentVariableManager envManager = CCorePlugin.getDefault().getBuildEnvironmentManager();
	IContributedEnvironment contribEnv = envManager.getContributedEnvironment();
	ICProjectDescription prjDesc = CoreModel.getDefault().getProjectDescription(project);
	ICConfigurationDescription configurationDescription = prjDesc.getConfigurationByName(this.mycConf);

	try {
	    MComPort = envManager.getVariable(Const.ENV_KEY_JANTJE_COM_PORT, configurationDescription, true).getValue();
	} catch (Exception e) {// ignore all errors
	}
	try {
	    boardName = envManager.getVariable(Const.ENV_KEY_JANTJE_BOARD_NAME, configurationDescription, true)
		    .getValue();
	} catch (Exception e) {// ignore all errors
	}
	String NewSerialPort = ArduinoSerial.makeArduinoUploadready(this.myConsole.newMessageStream(), this.myProject,
		this.mycConf, MComPort);

	IEnvironmentVariable var = new EnvironmentVariable(Const.ENV_KEY_JANTJE_COM_PORT, NewSerialPort);
	contribEnv.addVariable(var, configurationDescription);
	var = new EnvironmentVariable(Const.ENV_KEY_SERIAL_PORT_FILE,
		NewSerialPort.replace("/dev/", Const.EMPTY_STRING)); //$NON-NLS-1$
	contribEnv.addVariable(var, configurationDescription);

	String command = Const.EMPTY_STRING;
	try {
	    command = envManager
		    .getVariable(Const.get_Jantje_KEY_RECIPE(Const.ACTION_UPLOAD), configurationDescription, true)
		    .getValue();
	} catch (Exception e) {// ignore all errors
	}

	try {
	    GenericLocalUploader.RunConsoledCommand(this.myConsole, command, new SubProgressMonitor(monitor, 1));
	} catch (IOException e1) {
	    e1.printStackTrace();

	    return false;
	}
	if (boardName.startsWith("Arduino Due ")) { //$NON-NLS-1$
	    ArduinoSerial.reset_Arduino_by_baud_rate(MComPort, 115200, 100);
	}

	return true;
    }

}
