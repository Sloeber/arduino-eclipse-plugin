package it.baeyens.arduino.eclipse;

import it.baeyens.arduino.ArduinoConst;
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
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.ui.PlatformUI;
import org.osgi.service.prefs.BackingStoreException;


import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;


public class ArduinoProperties {

	// private IProject mProject;

	private String mArduinoPath;
	private String mArduinoBoardName;
	private String mMCUName;
	private int mMCUFrequency;
	private String mUploadPort;
	private String mUploadBaudrate;
	// TODO identify which value in AVREclipse needs to be set for this
	private boolean mDisabledFlushing;

	ArduinoProperties() {
		ReadGlobalStuff();
	}

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
			mArduinoPath = Project.getPersistentProperty(new QualifiedName("", ArduinoConst.ARDUINOPATH_PROPERTY));
			mArduinoBoardName = Project.getPersistentProperty(new QualifiedName("", ArduinoConst.ARDUINOBOARD_PROPERTY));

			if (mArduinoBoardName == null) {
				mArduinoBoardName = "";
			}
			if (mArduinoPath == null) {
				mArduinoPath = ((IResource) Project.getWorkspace()).getPersistentProperty(new QualifiedName("", ArduinoConst.ARDUINOPATH_PROPERTY));
				mArduinoPath = "";
			}
		} catch (CoreException e) {
			IStatus status = new Status(Status.ERROR, ArduinoConst.CORE_PLUGIN_ID,	"Failed to read arduino properties", e);
			AVRPlugin.getDefault().log(status);
		}

	}

	public void save(IProject Project) {
		try {
			Project.setPersistentProperty(new QualifiedName("", ArduinoConst.ARDUINOPATH_PROPERTY), mArduinoPath);
			Project.setPersistentProperty(new QualifiedName("", ArduinoConst.ARDUINOBOARD_PROPERTY), mArduinoBoardName);
			StoreGlobalStuff();
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

	public String getUploadPort() {
		return (mUploadPort == null) ? "" : mUploadPort;
	}

	public void setUploadPort(String Port) {
		this.mUploadPort = Port;
	}

	public String getUploadBaudrate() {
		return (mUploadBaudrate == null) ? "" : mUploadBaudrate;
	}

	public void setUploadBaudrate(String Baudrate) {
		this.mUploadBaudrate = Baudrate;
	}

	public String getArduinoPath() {
		return (mArduinoPath == null) ? "" : mArduinoPath;
	}

	public void setArduinoPath(String ArduinoPath) {
		this.mArduinoPath = ArduinoPath;
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

	private  void StoreGlobalStuff() {
		ArduinoHelpers.SetGlobalValue(ArduinoConst.ARDUINOPATH_PROPERTY, mArduinoPath);
		ArduinoHelpers.SetGlobalValue (ArduinoConst.ARDUINOBOARD_PROPERTY,mArduinoBoardName);
		ArduinoHelpers.SetGlobalValue (ArduinoConst.ARDUINOPORT_PROPERTY,mUploadPort);
	
	}

	private  void  ReadGlobalStuff() {
		mArduinoPath = ArduinoHelpers.GetGlobal (ArduinoConst.ARDUINOPATH_PROPERTY);
		mArduinoBoardName = ArduinoHelpers.GetGlobal (ArduinoConst.ARDUINOBOARD_PROPERTY);
		mUploadPort= ArduinoHelpers.GetGlobal (ArduinoConst.ARDUINOPORT_PROPERTY);

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
