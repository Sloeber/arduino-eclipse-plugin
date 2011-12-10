package it.baeyens.arduino.common;



import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.Status;


public class Common extends ArduinoConst{
//	/**
//	 * GetAvrDudeComPortPrefix is used to determine the prefix to be used with
//	 * the com port.<br/>
//	 * Avr dude delivered in WinAVR does not need a prefix.<br/>
//	 * Avr dude delivered with Arduino needs a prefix <br/>
//	 * This code checks the flag "use ide settings" that can be set in the
//	 * preferences and when set this method will return a prefix.<br/>
//	 * In all other cases a empty string will be returned.
//	 * 
//	 * @return The prefix needed for the com port for AvrDude
//	 */
//	public static String GetAvrDudeComPortPrefix() {
//		if (ArduinoPreferences.getUseIDESettings())
//			return "";
//		return "";
//	}

	/**
	 * This method makes sure that a string can be used as a file or folder name<br/>
	 * To do this it replaces all unacceptable characters with underscores.<br/>
	 * Currently it replaces slash back slash and space
	 * 
	 * @param Name the string that needs to be checked
	 * @return a name safe to create files or folders
	 */
	public static String MakeNameCompileSafe(String Name)
	{
		return Name.trim().replace(" ","_").replace("/","_").replace("\\","_");
	}
	

	
//	public static String getPersistentProperty(String Tag)
//	{ 
//		return getPersistentProperty(getProject(),  Tag);
//	}
	/**
	 * Gets a persistent project property
	 * 
	 * @param project The project for which the property is needed
	 * 
	 * @param Tag The tag identifying the property to read
	 * @return returns the property when found. When not found returns an empty string
	 */
	public static String getPersistentProperty(IProject project, String Tag)
	{ 
		try {
			String sret= project.getPersistentProperty (new QualifiedName("", Tag) );
			if (sret == null) sret ="";
			return sret;
		} catch (CoreException e) {
			log (new Status(Status.ERROR, ArduinoConst.CORE_PLUGIN_ID, "Failed to save read persistent setting " + Tag, e));
			//e.printStackTrace();
			return "";
		}
	}
	
	/**
	 * Logs the status information 
	 * 
	 * @param status the status information to log
	 */
	public static void log(IStatus status)
	{
		 Activator.getDefault().getLog().log(status);
	}
	
	
	/**
	 * Given a project name give back the project itself
	 * 
	 * @param Projectname
	 *            The name of the project you want to find
	 * @return The project if found. Else null
	 */
	public static IProject findProjectByName(String Projectname) {
		IProject AllProjects[] = ResourcesPlugin.getWorkspace().getRoot().getProjects();
		for (int CurProject = AllProjects.length - 1; CurProject >= 0; --CurProject) {
			if (AllProjects[CurProject].getName().equals(Projectname))
				return AllProjects[CurProject];
		}
		return null;
	}
	
	/**
	 * This method returns the dude config suffix which is dependent on the platform
	 * @return dude config suffix
	 */
	public static String DUDE_CONFIG_SUFFIX()
	{
		if (Platform.getOS().equals(Platform.OS_WIN32)) return DUDE_CONFIG_SUFFIX_WIN;
		if (Platform.getOS().equals(Platform.OS_LINUX)) return DUDE_CONFIG_SUFFIX_LINUX;
		Common.log( new Status(IStatus.ERROR,	ArduinoConst.CORE_PLUGIN_ID, "Unsupported operating system", null));
		return DUDE_CONFIG_SUFFIX_WIN;
	}
	
	/**
	 * This method returns the avrdude path suffix which is dependent on the platform
	 * 
	 * @return avrdude path suffix
	 */
	public static String AVRDUDE_PATH_SUFFIX()
	{
		if (Platform.getOS().equals(Platform.OS_WIN32)) return AVRDUDE_PATH_SUFFIX_WIN;
		if (Platform.getOS().equals(Platform.OS_LINUX)) return AVRDUDE_PATH_SUFFIX_LINUX;
		Common.log( new Status(IStatus.ERROR,	ArduinoConst.CORE_PLUGIN_ID, "Unsupported operating system", null));
		return AVRDUDE_PATH_SUFFIX_WIN;
	}

	/**
	 * This method returns the avrdude upload port prefix which is dependent on the platform
	 * 
	 * @return  avrdude upload port prefix
	 */
	public static String UploadPortPrefix()
	{
		if (Platform.getOS().equals(Platform.OS_WIN32)) return UploadPortPrefix_WIN;
		if (Platform.getOS().equals(Platform.OS_LINUX)) return UploadPortPrefix_LINUX;
		Common.log( new Status(IStatus.ERROR,	ArduinoConst.CORE_PLUGIN_ID, "Unsupported operating system", null));
		return UploadPortPrefix_WIN;
	}

	



//	/**
//	 * Returns the default programmer name
//	 * 
//	 * This is called the upload.protocol in the board.txt
//	 * 
//	 * @return the default programmer
//	 */
//	public static String DefaultProgrammerName() {
//		if (isArduinoIdeOne())
//		{
//			return ProgrammerNameOne;
//		}
//		return ProgrammerName;
//	}
//	

	/**
	 * ToInt converts a string to a integer in a save way
	 * 
	 * @param Number
	 *            is a String that will be converted to an integer. Number can
	 *            be null or empty and can contain leading and trailing white
	 *            space
	 * @return The integer value represented in the string based on parseInt
	 * @see parseInt. After error checking and modifications parseInt is used
	 *      for the conversion
	 **/
	public static int ToInt(String Number) {
		if (Number == null)
			return 0;
		if (Number.equals(""))
			return 0;
		return Integer.parseInt(Number.trim());
	}	
	
}
