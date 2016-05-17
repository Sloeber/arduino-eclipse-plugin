package it.baeyens.arduino.tools.uploaders;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPFunction;
import org.eclipse.cdt.core.envvar.EnvironmentVariable;
import org.eclipse.cdt.core.envvar.IContributedEnvironment;
import org.eclipse.cdt.core.envvar.IEnvironmentVariable;
import org.eclipse.cdt.core.envvar.IEnvironmentVariableManager;
import org.eclipse.cdt.core.index.IIndex;
import org.eclipse.cdt.core.index.IIndexBinding;
import org.eclipse.cdt.core.index.IIndexName;
import org.eclipse.cdt.core.index.IndexFilter;
import org.eclipse.cdt.core.model.CoreModel;
import org.eclipse.cdt.core.model.ICProject;
import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.cdt.core.settings.model.ICProjectDescription;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.ui.console.MessageConsole;

import it.baeyens.arduino.common.Const;
import it.baeyens.arduino.communication.ArduinoSerial;
import it.baeyens.arduino.tools.PasswordManager;

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
	    MComPort = envManager.getVariable(Const.ENV_KEY_JANTJE_COM_PORT, configurationDescription, true).getValue();
	    boardName = envManager.getVariable(Const.ENV_KEY_JANTJE_BOARD_NAME, configurationDescription, true).getValue();
	    needsPassword = envManager.getVariable(Const.ENV_KEY_NETWORK_AUTH, configurationDescription, true).getValue()
		    .equalsIgnoreCase(Const.TRUE);
	} catch (Exception e) {// ignore all errors
	}
	String NewSerialPort = ArduinoSerial.makeArduinoUploadready(this.myConsole.newMessageStream(), this.myProject, this.mycConf, MComPort);

	IEnvironmentVariable var = new EnvironmentVariable(Const.ENV_KEY_JANTJE_COM_PORT, NewSerialPort);
	contribEnv.addVariable(var, configurationDescription);
	var = new EnvironmentVariable(Const.ENV_KEY_SERIAL_PORT_FILE, NewSerialPort.replace("/dev/", Const.EMPTY_STRING)); //$NON-NLS-1$
	contribEnv.addVariable(var, configurationDescription);

	// for web authorized upload
	if (needsPassword) {
	    setEnvironmentvarsForAutorizedUpload(contribEnv, configurationDescription, MComPort);
	}

	String command = Const.EMPTY_STRING;
	try {
	    command = envManager.getVariable(Const.get_Jantje_KEY_RECIPE(Const.ACTION_UPLOAD), configurationDescription, true).getValue();
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
	// for web authorized upload
	if (needsPassword) {
	    String passWord = getPasswordFromCode();
	    PasswordManager.setPwd(MComPort, myLogin, passWord);
	}

	return true;
    }

    /**
     * given a project look in the source code for the line of code that sets the password;
     * 
     * 
     * 
     * return the password string of no_pwd_found_in_code
     * 
     * @param iProject
     * @return
     */
    private String getPasswordFromCode() {
	String setupFunctionName = "setup"; //$NON-NLS-1$
	String serialFunctionName = "Serial.begin"; //$NON-NLS-1$
	String atoFunctionName = "Serial.begin"; //$NON-NLS-1$
	String pwdVariableName = "Serial.begin"; //$NON-NLS-1$
	String pwd = "no_pwd_found_in_code";//$NON-NLS-1$

	ICProject curProject = CoreModel.getDefault().getCModel().getCProject(this.myProject.getName());

	IIndex index = null;
	try {
	    index = CCorePlugin.getIndexManager().getIndex(curProject);
	    index.acquireReadLock();
	    // find bindings for name
	    IIndexBinding[] bindings = index.findBindings(setupFunctionName.toCharArray(), IndexFilter.ALL_DECLARED, new NullProgressMonitor());
	    ICPPFunction setupFunc = null;
	    for (IIndexBinding curbinding : bindings) {
		if (curbinding instanceof ICPPFunction) {
		    setupFunc = (ICPPFunction) curbinding;
		}

	    }

	    if (setupFunc == null) {
		return pwd;// that on found binding must be a function
	    }

	    IIndexName[] names = index.findNames(setupFunc, org.eclipse.cdt.core.index.IIndex.FIND_DEFINITIONS);
	    // if (names.length != 1) {
	    // return -3;
	    // }
	    for (IIndexName name : names) {
		String SetupFileName = name.getFileLocation().getFileName();
		String SetupFileContent = FileUtils.readFileToString(new File(SetupFileName));
		int serialBeginStart = SetupFileContent.indexOf(serialFunctionName);
		if (serialBeginStart != -1) {
		    int serialBeginStartbraket = SetupFileContent.indexOf("(", serialBeginStart); //$NON-NLS-1$
		    if (serialBeginStartbraket != -1) {
			int serialBeginCloseBraket = SetupFileContent.indexOf(")", serialBeginStartbraket); //$NON-NLS-1$
			if (serialBeginCloseBraket != -1) {
			    pwd = SetupFileContent.substring(serialBeginStartbraket + 1, serialBeginCloseBraket).trim();
			    return pwd;
			}
		    }
		}
	    }

	} catch (CoreException | InterruptedException | IOException e) {
	    e.printStackTrace();
	} finally {
	    if (index != null) {
		index.releaseReadLock();
	    }
	}

	return pwd;
    }

    private void setEnvironmentvarsForAutorizedUpload(IContributedEnvironment contribEnv, ICConfigurationDescription configurationDescription,
	    String Host) {
	String passWord;
	PasswordManager pwdManager = new PasswordManager();
	if (!pwdManager.setHost(Host)) {
	    passWord = getPasswordFromCode();
	} else {
	    passWord = pwdManager.getPassword();
	}
	IEnvironmentVariable var = new EnvironmentVariable(Const.ENV_KEY_SERIAL_PORT_FILE, passWord);
	contribEnv.addVariable(var, configurationDescription);

    }

}
