package it.baeyens.arduino.ui;

import it.baeyens.arduino.common.ArduinoConst;
import it.baeyens.arduino.tools.ArduinoPreferences;
import it.baeyens.avreclipse.AVRPlugin;
import it.baeyens.avreclipse.core.avrdude.ProgrammerConfig;
import it.baeyens.avreclipse.core.avrdude.ProgrammerConfigManager;
import it.baeyens.avreclipse.core.properties.AVRDudeProperties;
import it.baeyens.avreclipse.core.properties.AVRProjectProperties;
import it.baeyens.avreclipse.core.properties.ProjectPropertyManager;
import it.baeyens.avreclipse.core.util.AVRMCUidConverter;
import java.io.File;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.ui.PlatformUI;
import org.osgi.service.prefs.BackingStoreException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;

/**ArduinoProperties controls the arduino properties. It is a class on top of ArduinoPreferences.
 * ArduinoPreferences is pure static this is a real class.
 * This class controls in 1 place settings that are owned by several parties. It creates the arduino view on the data
 * @author Jan Baeyens
 *
 */
public class ArduinoProperties {

	// private IProject mProject;

	private Path mArduinoPath;
	private String mArduinoBoardName;
	private String mMCUName;
	private int mMCUFrequency;
	private String mUploadPort;
	private String mUploadBaudrate;
	private boolean mDisabledFlushing;

	ArduinoProperties() {
		//ArduinoPreferences.ReadGlobalStuff(mArduinoPath,mArduinoBoardName,mUploadPort);
		mArduinoPath = ArduinoPreferences.getArduinoPath(); 
		mArduinoBoardName = ArduinoPreferences.getArduinoBoardName();
		mUploadPort= ArduinoPreferences.getUploadPort(); 
	}
/**read reads the Arduino preferences for a project
 * 
 * @param Project the project for which you want the arduino properties
 */
	public void read(IProject Project) {
		ProgrammerConfigManager AVRConfigManager;
		ProgrammerConfig Programmerconfig;
		AVRProjectProperties AVRproperties;
		ProjectPropertyManager projpropsmanager = ProjectPropertyManager.getPropertyManager(Project);
		AVRproperties = projpropsmanager.getProjectProperties();
		AVRConfigManager = ProgrammerConfigManager.getDefault();
		Programmerconfig = AVRConfigManager.getConfigByName(ArduinoConst.ProgrammerConfigName);
		if (Programmerconfig == null) // No programmer config exists with the
										// required name so reate it
		{
			mUploadPort = "";
			mUploadBaudrate = "";
			Programmerconfig = AVRConfigManager.createNewConfig();
			Programmerconfig.setProgrammer(ArduinoConst.ProgrammerName);
			Programmerconfig.setName(ArduinoConst.ProgrammerConfigName);
			Programmerconfig.setPort(mUploadPort);
			Programmerconfig.setBaudrate(mUploadBaudrate);
			Programmerconfig.setDescription(ArduinoConst.ProgrammerConfigDescription);
			try {
				AVRConfigManager.saveConfig(Programmerconfig);
			} catch (BackingStoreException e) {
				IStatus status = new Status(Status.ERROR, ArduinoConst.CORE_PLUGIN_ID,	"Failed to save the programmer config " + Programmerconfig.getName() , e);
				AVRPlugin.getDefault().log(status);
					e.printStackTrace();
			}
		} else {
			mUploadPort = Programmerconfig.getPort();
			mUploadBaudrate = Programmerconfig.getBaudrate();
		}

		mMCUName = AVRproperties.getMCUId();
		mMCUFrequency = Integer.parseInt(AVRproperties.getFCPU());

		try {
			mArduinoPath = new Path (Project.getPersistentProperty(new QualifiedName("", ArduinoConst.KEY_ARDUINOPATH)));
			mArduinoBoardName = Project.getPersistentProperty(new QualifiedName("", ArduinoConst.KEY_ARDUINOBOARD));

			if (mArduinoBoardName == null) {
				mArduinoBoardName = "";
			}
			if (mArduinoPath == null) {
				mArduinoPath =  new Path ( ((IResource) Project.getWorkspace()).getPersistentProperty(new QualifiedName("", ArduinoConst.KEY_ARDUINOPATH)));
			}
		} catch (CoreException e) {
			IStatus status = new Status(Status.ERROR, ArduinoConst.CORE_PLUGIN_ID,	"Failed to read arduino properties", e);
			AVRPlugin.getDefault().log(status);
		}

	}

	/** Save stores the arduino important properties at the correct locations
	 * 
	 * @param Project
	 */
	public void save(IProject Project) {
		try {
			Project.setPersistentProperty(new QualifiedName("", ArduinoConst.KEY_ARDUINOPATH), mArduinoPath.toOSString());
			Project.setPersistentProperty(new QualifiedName("", ArduinoConst.KEY_ARDUINOBOARD), mArduinoBoardName);
			ArduinoPreferences.StoreGlobalStuff(mArduinoPath,mArduinoBoardName,mUploadPort);
		} catch (CoreException e) {
			IStatus status = new Status(Status.ERROR, ArduinoConst.CORE_PLUGIN_ID,	"Failed to write arduino properties", e);
			AVRPlugin.getDefault().log(status);

		}
		ProgrammerConfigManager AVRConfigManager;
		ProgrammerConfig Programmerconfig;
		AVRProjectProperties AVRproperties;
		AVRDudeProperties AVRDudeProperties;
		// mProject=Project;
		ProjectPropertyManager projpropsmanager = ProjectPropertyManager.getPropertyManager(Project);
		AVRproperties = projpropsmanager.getProjectProperties();
		AVRDudeProperties = AVRproperties.getAVRDudeProperties();
		AVRConfigManager = ProgrammerConfigManager.getDefault();
		Programmerconfig = AVRConfigManager.getConfigByName(ArduinoConst.ProgrammerConfigName);
		if (Programmerconfig == null) // No programmer configuration exists with the
										// required name so create it
		{
			Programmerconfig = AVRConfigManager.createNewConfig();
			Programmerconfig.setProgrammer(ArduinoConst.ProgrammerName);
			Programmerconfig.setName(ArduinoConst.ProgrammerConfigName);
			Programmerconfig.setDescription(ArduinoConst.ProgrammerConfigDescription);
		}
		Programmerconfig.setPort( mUploadPort);
		Programmerconfig.setBaudrate(mUploadBaudrate);
		AVRproperties.setMCUId(AVRMCUidConverter.name2id(mMCUName));
		AVRproperties.setFCPU(Integer.toString(mMCUFrequency));
		AVRDudeProperties.setProgrammer(Programmerconfig);
		// save the settings
		try {
			AVRConfigManager.saveConfig(Programmerconfig);
			AVRDudeProperties.save();
			AVRproperties.save();
		} catch (BackingStoreException e) {
			IStatus status = new Status(IStatus.ERROR, ArduinoConst.CORE_PLUGIN_ID, "Could not write project properties to the preferences.", e);
			AVRPlugin.getDefault().log(status);
			ErrorDialog.openError(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), "AVR Project Wizard Error", null, status);
		}

	}

	/**
	 * getUploadPort returns the uploadPort related to the current project
	 * 
	 * @return the upload port
	 */
	public String getUploadPort() {
		return (mUploadPort == null) ? "" : mUploadPort;
	}

	/**
	 * setUploadPort sets the upload port in the properties. This does not save the value
	 * 
	 * @param Port to be stored.
	 */
	public void setUploadPort(String Port) {
		this.mUploadPort = Port;
	}

	/** getUploadBaudrate get the upload baut rate currently set in the properties.
	 * 
	 * @return The uploadbaut rate
	 */
	public String getUploadBaudrate() {
		return (mUploadBaudrate == null) ? "" : mUploadBaudrate;
	}

	/**
	 * setUploadBaudrate sets the upload baud rate in the properties. This does not save the value
	 * 
	 * @param Baudrate to be stored.
	 */
	public void setUploadBaudrate(String Baudrate) {
		this.mUploadBaudrate = Baudrate;
	}

	/** getArduinoPath returns the Arduino Path
	 * 
	 * @return the arduino path in the properties
	 */
	public IPath getArduinoPath() {
		return mArduinoPath ;
	}

	/**
	 * setArduinoPath sets the arduino path in the properties. This does not save the value
	 * 
	 * @param ArduinoPath to be stored.
	 */
	public void setArduinoPath(IPath ArduinoPath) {
		this.mArduinoPath = (Path) ArduinoPath;
	}

	public String getArduinoBoardName() {
		return (mArduinoBoardName == null) ? "" : mArduinoBoardName;
	}

	public void setArduinoBoardName(String ArduinoBoardName) {
		this.mArduinoBoardName = ArduinoBoardName;
	}

	public String getMCUName() {
		return (mMCUName == null) ? "" : mMCUName;
	}

	public void setMCUName(String MCUName) {
		this.mMCUName = MCUName;
	}

	public int getMCUFrequency() {
		return mMCUFrequency;
	}

	public void setMCUFrequency(int MCUFrequency) {
		this.mMCUFrequency = MCUFrequency;
	}

	public boolean getDisabledFlushing() {
		return mDisabledFlushing;
	}

	public void setDisabledFlushing(boolean Disabled) {
		mDisabledFlushing = Disabled;
	}



	public IPath getArduinoSourceCodeLocation() {
		String fullPath = getArduinoPath()+ File.separator+ "hardware" + File.separator + "arduino"+ File.separator+ "cores" + File.separator + "arduino";
		return (IPath) new org.eclipse.core.runtime.Path(fullPath);
	}

	public void setArduinoBoard(String boardName) {
		ArduinoBoards TheBoards = new ArduinoBoards();
		TheBoards.Load(mArduinoPath);
		mMCUName=TheBoards.getMCUName(boardName);
		mMCUFrequency=Integer.parseInt(TheBoards.getMCUFrequency(boardName));
		mUploadBaudrate=TheBoards.getUploadBaudRate(boardName);
		mArduinoBoardName=boardName;
		
	}
}
