package io.sloeber.core.tools.uploaders;

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
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.console.MessageConsole;

import io.sloeber.core.common.Common;
import io.sloeber.core.common.Const;
import io.sloeber.core.common.IndexHelper;
import io.sloeber.core.communication.ArduinoSerial;

public class arduinoUploader implements IRealUpload {

	private IProject myProject;
	private String mycConf;
	private MessageConsole myConsole;
	private static final String myLogin = "root"; //$NON-NLS-1$

	arduinoUploader(IProject Project, String cConf, String UploadTool, MessageConsole Console) {
		this.myProject = Project;
		this.mycConf = cConf;
		this.myConsole = Console;
	}

	@Override
	public boolean uploadUsingPreferences(IFile hexFile, boolean usingProgrammer, IProgressMonitor monitor) {
		String MComPort = Const.EMPTY_STRING;
		String boardName = Const.EMPTY_STRING;
		boolean needsPassword = false;

		IEnvironmentVariableManager envManager = CCorePlugin.getDefault().getBuildEnvironmentManager();
		IContributedEnvironment contribEnv = envManager.getContributedEnvironment();
		ICProjectDescription prjDesc = CoreModel.getDefault().getProjectDescription(this.myProject);
		ICConfigurationDescription configurationDescription = prjDesc.getConfigurationByName(this.mycConf);

		try {
			MComPort = envManager.getVariable(Const.ENV_KEY_JANTJE_UPLOAD_PORT, configurationDescription, true)
					.getValue();
			boardName = envManager.getVariable(Const.ENV_KEY_JANTJE_BOARD_NAME, configurationDescription, true)
					.getValue();
			needsPassword = envManager.getVariable(Const.ENV_KEY_NETWORK_AUTH, configurationDescription, true)
					.getValue().equalsIgnoreCase(Const.TRUE);
		} catch (Exception e) {// ignore all errors
		}
		String NewSerialPort = MComPort;
		if (!usingProgrammer) {
			NewSerialPort = ArduinoSerial.makeArduinoUploadready(this.myConsole.newMessageStream(), this.myProject,
					this.mycConf, MComPort);
		}

		IEnvironmentVariable var = new EnvironmentVariable(Const.ENV_KEY_JANTJE_UPLOAD_PORT, NewSerialPort);
		contribEnv.addVariable(var, configurationDescription);
		var = new EnvironmentVariable(Const.ENV_KEY_SERIAL_PORT_FILE,
				NewSerialPort.replace("/dev/", Const.EMPTY_STRING)); //$NON-NLS-1$
		contribEnv.addVariable(var, configurationDescription);

		// for web authorized upload
		if (needsPassword) {
			setEnvironmentvarsForAutorizedUpload(contribEnv, configurationDescription, MComPort);
		}

		String command = Const.EMPTY_STRING;
		try {
			command = envManager
					.getVariable(Common.get_Jantje_KEY_RECIPE(Const.ACTION_UPLOAD), configurationDescription, true)
					.getValue();
		} catch (Exception e) {
			Common.log(new Status(IStatus.ERROR, Const.CORE_PLUGIN_ID, "Failed to get the Upload recipe ", e)); //$NON-NLS-1$
			return false;
		}

		try {
			GenericLocalUploader.RunConsoledCommand(this.myConsole, command, monitor);
		} catch (IOException e1) {
			Common.log(new Status(IStatus.ERROR, Const.CORE_PLUGIN_ID, "Failed to run the Upload recipe ", e1)); //$NON-NLS-1$
			return false;
		}
		if (boardName.startsWith("Arduino Due ")) { //$NON-NLS-1$
			ArduinoSerial.reset_Arduino_by_baud_rate(MComPort, 115200, 100);
		}

		return true;
	}

	/**
	 * given a project look in the source code for the line of code that sets
	 * the password;
	 *
	 * @param iProject
	 * @return the password string or no_pwd_found_in_code if not found
	 */
	@SuppressWarnings("nls")
	private String getPasswordFromCode() {
		String parameter = IndexHelper.findParameterInFunction(this.myProject, "setup", "ArduinoOTA.setPassword",
				"no_pwd_found_in_code");
		return parameter.replaceAll("\\(.*\\)", "").trim();

	}

	private void setEnvironmentvarsForAutorizedUpload(IContributedEnvironment contribEnv,
			ICConfigurationDescription configurationDescription, String host) {
		String passWord = null;
		passWord = getPasswordFromCode();
		IEnvironmentVariable var = new EnvironmentVariable(Const.ENV_KEY_NETWORK_AUTH, passWord);
		contribEnv.addVariable(var, configurationDescription);

	}

}
