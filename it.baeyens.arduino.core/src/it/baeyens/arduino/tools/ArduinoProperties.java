package it.baeyens.arduino.tools;

import it.baeyens.arduino.common.ArduinoConst;
import it.baeyens.arduino.common.ArduinoInstancePreferences;
import it.baeyens.arduino.common.Common;
import it.baeyens.avreclipse.core.avrdude.ProgrammerConfig;
import it.baeyens.avreclipse.core.avrdude.ProgrammerConfigManager;
import it.baeyens.avreclipse.core.properties.AVRDudeProperties;
import it.baeyens.avreclipse.core.properties.AVRProjectProperties;
import it.baeyens.avreclipse.core.properties.ProjectPropertyManager;
import it.baeyens.avreclipse.core.util.AVRMCUidConverter;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.Status;
import org.osgi.service.prefs.BackingStoreException;
import org.eclipse.core.resources.IProject;

/**
 * ArduinoProperties controls the arduino properties. It is a class on top of
 * ArduinoPreferences. ArduinoPreferences is pure static this is a real class.
 * This class controls in 1 place settings that are owned by several parties. It
 * creates the arduino view on the data
 * 
 * @author Jan Baeyens
 * 
 */
public class ArduinoProperties {

	// private IProject mProject;

//	private static Path mArduinoPath = ArduinoPreferences.getArduinoPath();;
	private String mArduinoBoardName;
	private String mMCUName;
	private int mMCUFrequency;
	private String mUploadPort;
	private String mUploadBaudrate;
	private boolean mDisabledFlushing;
	private String mBoardVariant;
	private String mProgrammerName;
	private String myBuildCoreFolder;
	private String myCppCompileOptions;
	private String myCCompileOptions;
	private String myLinkOptions;
	private String myBuildVID;
	private String myBuildPID;

	public ArduinoProperties() {
		mArduinoBoardName = ArduinoInstancePreferences.getLastUsedArduinoBoardName();
		mUploadPort = ArduinoInstancePreferences.getLastUsedUploadPort();
		myBuildCoreFolder ="";
	}

	/**
	 * read reads the Arduino preferences for a project
	 * 
	 * @param Project
	 *            the project for which you want the arduino properties
	 */
	public void read(IProject Project) {
		ProgrammerConfigManager AVRConfigManager;
		ProgrammerConfig Programmerconfig;
		AVRProjectProperties AVRproperties;
		ProjectPropertyManager projpropsmanager = ProjectPropertyManager.getPropertyManager(Project);
		AVRproperties = projpropsmanager.getProjectProperties();
		AVRConfigManager = ProgrammerConfigManager.getDefault();
		Programmerconfig = AVRConfigManager.getConfigByName(ArduinoHelpers.ProgrammerConfigName(Project));
		if (Programmerconfig == null) // No programmer configuration exists with the required name so create it
		{
			mUploadPort = "";
			mUploadBaudrate = "";
			mProgrammerName="";
			Programmerconfig = AVRConfigManager.createNewConfig();
			Programmerconfig.setProgrammer(mProgrammerName);
			Programmerconfig.setName(ArduinoHelpers.ProgrammerConfigName(Project));
			Programmerconfig.setPort(mUploadPort);
			Programmerconfig.setBaudrate(mUploadBaudrate);
			Programmerconfig.setDescription(ArduinoConst.ProgrammerConfigDescription);
			try {
				AVRConfigManager.saveConfig(Programmerconfig);
			} catch (BackingStoreException e) {
				IStatus status = new Status(Status.ERROR, ArduinoConst.CORE_PLUGIN_ID, "Failed to save the programmer config " + Programmerconfig.getName(), e);
				Common.log(status);
				e.printStackTrace();
			}
		} else {
			mUploadPort = Programmerconfig.getPort();
			mUploadBaudrate = Programmerconfig.getBaudrate();
			mProgrammerName = Programmerconfig.getProgrammer();
		}

		mMCUName = AVRproperties.getMCUId();
		mMCUFrequency = Integer.parseInt(AVRproperties.getFCPU());

		mArduinoBoardName = Common.getPersistentProperty(Project, ArduinoConst.KEY_ARDUINOBOARD);
		mBoardVariant = Common.getPersistentProperty(Project, ArduinoConst.KEY_ARDUINOBOARDVARIANT);
		myBuildCoreFolder = Common.getPersistentProperty(Project, ArduinoConst.KEY_ARDUINO_CORE_FOLDER);
		myCppCompileOptions= Common.getPersistentProperty(Project, ArduinoConst.KEY_ARDUINO_CPP_COMPILE_OPTIONS) ;
		myCCompileOptions= Common.getPersistentProperty(Project, ArduinoConst.KEY_ARDUINO_C_COMPILE_OPTIONS) ;
		myLinkOptions = Common.getPersistentProperty(Project, ArduinoConst.KEY_ARDUINO_LINK_OPTIONS) ;
		mDisabledFlushing = Common.getPersistentProperty(Project, ArduinoConst.KEY_ARDUINO_DISABLE_FLUSHING).equalsIgnoreCase( "TRUE");
		
		myBuildVID = Common.getPersistentProperty(Project, ArduinoConst.KEY_ARDUINO_BUILD_VID) ;
		myBuildPID = Common.getPersistentProperty(Project, ArduinoConst.KEY_ARDUINO_BUILD_PID) ;
	}

	/**
	 * Save stores the arduino important properties at the correct locations
	 * 
	 * @param Project
	 */
	public void save(IProject Project) {
		try {
			Project.setPersistentProperty(new QualifiedName("", ArduinoConst.KEY_ARDUINOBOARD), mArduinoBoardName);
			Project.setPersistentProperty(new QualifiedName("", ArduinoConst.KEY_ARDUINOBOARDVARIANT), mBoardVariant);
			Project.setPersistentProperty(new QualifiedName("", ArduinoConst.KEY_ARDUINO_CORE_FOLDER), myBuildCoreFolder);
			Project.setPersistentProperty(new QualifiedName("", ArduinoConst.KEY_ARDUINO_CPP_COMPILE_OPTIONS), myCppCompileOptions);
			Project.setPersistentProperty(new QualifiedName("", ArduinoConst.KEY_ARDUINO_C_COMPILE_OPTIONS), myCCompileOptions);
			Project.setPersistentProperty(new QualifiedName("", ArduinoConst.KEY_ARDUINO_LINK_OPTIONS), myLinkOptions);
			Project.setPersistentProperty(new QualifiedName("", ArduinoConst.KEY_ARDUINO_DISABLE_FLUSHING), mDisabledFlushing?"TRUE":"FALSE");
			
			Project.setPersistentProperty(new QualifiedName("", ArduinoConst.KEY_ARDUINO_BUILD_VID), myBuildVID);
			Project.setPersistentProperty(new QualifiedName("", ArduinoConst.KEY_ARDUINO_BUILD_PID), myBuildPID);
			
			ArduinoInstancePreferences.SetLastUsedArduinoBoard(mArduinoBoardName);
			ArduinoInstancePreferences.SetLastUsedUploadPort(mUploadPort);
		} catch (CoreException e) {
			IStatus status = new Status(Status.ERROR, ArduinoConst.CORE_PLUGIN_ID, "Failed to write arduino properties", e);
			Common.log(status);

		}
		ProgrammerConfigManager AVRConfigManager;
		ProgrammerConfig Programmerconfig;
		AVRProjectProperties AVRproperties;
		AVRDudeProperties AVRDudeProperties;
		
		ProjectPropertyManager projpropsmanager = ProjectPropertyManager.getPropertyManager(Project);
		AVRproperties = projpropsmanager.getProjectProperties();
		AVRDudeProperties = AVRproperties.getAVRDudeProperties();
		AVRConfigManager = ProgrammerConfigManager.getDefault();
		Programmerconfig = AVRConfigManager.getConfigByName(ArduinoHelpers.ProgrammerConfigName(Project));
		if (Programmerconfig == null) // No programmer configuration exists with the required name so create it
		{
			Programmerconfig = AVRConfigManager.createNewConfig();
		}
		Programmerconfig.setProgrammer(mProgrammerName);
		Programmerconfig.setName(ArduinoHelpers.ProgrammerConfigName(Project));
		Programmerconfig.setDescription(ArduinoConst.ProgrammerConfigDescription);
		Programmerconfig.setPort(mUploadPort);
		Programmerconfig.setBaudrate(mUploadBaudrate);
		AVRproperties.setMCUId(AVRMCUidConverter.name2id(mMCUName));
		AVRproperties.setFCPU(Integer.toString(mMCUFrequency));
		AVRDudeProperties.setProgrammer(Programmerconfig);
		
		try {
			AVRConfigManager.saveConfig(Programmerconfig); // save the settings
			AVRDudeProperties.save();
			AVRproperties.save();
		} catch (BackingStoreException e) {
			IStatus status = new Status(IStatus.ERROR, ArduinoConst.CORE_PLUGIN_ID, "Could not write project properties to the preferences.", e);
			Common.log(status);
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
	 * setUploadPort sets the upload port in the properties. This does not save
	 * the value
	 * 
	 * @param Port
	 *            to be stored.
	 */
	public void setUploadPort(String Port) {
		this.mUploadPort = Port;
	}

	/**
	 * getUploadBaudrate get the upload baut rate currently set in the
	 * properties.
	 * 
	 * @return The uploadbaut rate
	 */
	public String getUploadBaudrate() {
		return (mUploadBaudrate == null) ? "" : mUploadBaudrate;
	}

	/**
	 * setUploadBaudrate sets the upload baud rate in the properties. This does
	 * not save the value
	 * 
	 * @param Baudrate
	 *            to be stored.
	 */
	public void setUploadBaudrate(String Baudrate) {
		this.mUploadBaudrate = Baudrate;
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

	public String getBoardVariant() {
		return mBoardVariant;
	}

	public void setBoardVariant(String variant) {
		this.mBoardVariant = variant;
	}

	public String getUploadProtocol() {
		return mProgrammerName;
	}

	public void setUploadProtocol(String uploadProtocol) {
		mProgrammerName = uploadProtocol;
	}

	public boolean getDisabledFlushing() {
		return mDisabledFlushing;
	}

	public void setDisabledFlushing(boolean Disabled) {
		mDisabledFlushing = Disabled;
	}

//	public IPath getArduinoSourceCodeLocation() {
//		String fullPath = ArduinoInstancePreferences.getArduinoPath() + File.separator + "hardware" + File.separator + "arduino" + File.separator + "cores" + File.separator + "arduino";
//		return (IPath) new org.eclipse.core.runtime.Path(fullPath);
//	}

	public void setArduinoBoard(String boardName) {
		ArduinoBoards TheBoards = new ArduinoBoards();
		TheBoards.Load( ArduinoInstancePreferences.getArduinoPath());
		mArduinoBoardName = boardName;
		mMCUName = TheBoards.getMCUName(mArduinoBoardName);
		mMCUFrequency = Common.ToInt(TheBoards.getMCUFrequency(boardName));
		// mUploadPort does not need to be set
		mUploadBaudrate = TheBoards.getUploadBaudRate(boardName);
		mDisabledFlushing= TheBoards.getDisableFlushing(boardName);
		mBoardVariant= TheBoards.getBoardVariant(boardName);
		mProgrammerName = TheBoards.getUploadProtocol(boardName);
		myBuildCoreFolder=TheBoards.getBuildCoreFolder(boardName);
		myCppCompileOptions=TheBoards.getCppCompileOptions(boardName);
		myCCompileOptions=TheBoards.getCCompileOptions(boardName);
		myLinkOptions=TheBoards.getLinkOptions(boardName);
		myBuildVID=TheBoards.getBuildVID(boardName);
		myBuildPID=TheBoards.getBuildPID(boardName);
	}

	/**
	 * Returns the board name in such a way that files and projects can be
	 * created out of it Basically it replaces all dangerous characters to safe
	 * characters
	 * 
	 */
	public String getSafeArduinoBoardName() {
		return Common.MakeNameCompileSafe(mArduinoBoardName);
	}

	public void setBuildCoreFolder(String BuildCoreFolder)
		{
			myBuildCoreFolder=BuildCoreFolder;
		}

	public String getBuildCoreFolder()
		{
			return myBuildCoreFolder;
		}

	public void setCppCompileOptions(String CppCompileOptions)
		{
			myCppCompileOptions=CppCompileOptions;
			
		}

	public void setCCompileOptions(String CCompileOptions)
		{
			myCCompileOptions=CCompileOptions;
			
		}

	public void setLinkOptions(String LinkOptions)
		{
			myLinkOptions=LinkOptions;
		}

	public void setBuildVID(String BuildVID)
		{
			myBuildVID=BuildVID;
			
		}

	public void setBuildPID(String BuildPID)
		{
			myBuildPID=BuildPID;
			
		}
}
