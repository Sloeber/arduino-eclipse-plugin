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

import io.sloeber.core.api.BoardDescriptor;
import io.sloeber.core.common.Common;
import io.sloeber.core.common.Const;
import io.sloeber.core.common.IndexHelper;
import io.sloeber.core.communication.ArduinoSerial;

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
	public boolean uploadUsingPreferences(IFile hexFile, BoardDescriptor inBoardDescriptor, IProgressMonitor monitor) {
		String MComPort = new String();
		String boardName = new String();
		boolean needsPassword = false;
		boolean needsUpdatedConfig = false;
		BoardDescriptor boardDescriptor = BoardDescriptor.makeBoardDescriptor(inBoardDescriptor);

		IEnvironmentVariableManager envManager = CCorePlugin.getDefault().getBuildEnvironmentManager();
		IContributedEnvironment contribEnv = envManager.getContributedEnvironment();
		ICProjectDescription prjDesc = CoreModel.getDefault().getProjectDescription(this.myProject);
		ICConfigurationDescription configurationDescription = prjDesc.getConfigurationByName(this.mycConf);

		try {
			needsPassword = envManager.getVariable(Const.ENV_KEY_NETWORK_AUTH, configurationDescription, true)
					.getValue().equalsIgnoreCase(Const.TRUE);
		} catch (Exception e) {// ignore all errors
		}

		if (!boardDescriptor.usesProgrammer()) {
			String NewSerialPort = ArduinoSerial.makeArduinoUploadready(this.myConsole.newMessageStream(),
					this.myProject, this.mycConf, boardDescriptor);
			if (!boardDescriptor.getUploadPort().equals(NewSerialPort)) {
				boardDescriptor.setUploadPort(NewSerialPort);
				needsUpdatedConfig = true;
			}
		}

		// for web authorized upload
		if (needsPassword) {
			setEnvironmentvarsForAutorizedUpload(contribEnv, configurationDescription, MComPort);
		}

		if (needsUpdatedConfig) {
			try {
				boardDescriptor.saveConfiguration(configurationDescription, null);
			} catch (Exception e) {
				Common.log(new Status(IStatus.ERROR, Const.CORE_PLUGIN_ID, "Failed to save configuration before upload", //$NON-NLS-1$
						e));
				return false;
			}
		}
		String command = boardDescriptor.getUploadCommand(configurationDescription);
		if (command == null) {
			Common.log(new Status(IStatus.ERROR, Const.CORE_PLUGIN_ID, "Failed to get the Upload recipe ")); //$NON-NLS-1$
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
	 * given a project look in the source code for the line of code that sets the
	 * password;
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
