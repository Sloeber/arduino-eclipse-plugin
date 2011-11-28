package it.baeyens.arduino.common;

import org.eclipse.core.runtime.*;
import java.io.File;

import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.osgi.framework.Bundle;


/**
 * ArduinoConst only contains global strings used in the eclipse plugin.
 * @author Jan Baeyens
 * 
 */
public class ArduinoConst {
    //General stuff
	public static final String PluginStart 					= "it.baeyens."; //"de .innot.avreclipse.";
	public static final String CORE_PLUGIN_ID				= PluginStart +"core";
	public static final String UI_PLUGIN_ID					= PluginStart + "avreclipse.ui";
	public static final String MCU_SELECT_PAGE_ID 			= PluginStart +"mcuselectpage";

	
	//programmer information
	public static final String ProgrammerConfigName = "AVR Eclipse controlled Arduino settings";
	public static final String ProgrammerConfigDescription = "AVReclipse controlled arduino settings. You should not changes this manually";
	public static final String ProgrammerName = "stk500v1"; // "arduino" "atmelSTK500 Version1.x Firmware";

	// the prefix used when creating a core library
	public static final String CoreProjectNamePrefix = "arduino_";
	//prefix to be added to the arduino environment
	private static final String UploadPortPrefix_WIN ="-P\\\\.\\";
	private static final String UploadPortPrefix_LINUX ="-P";
	
	//natures
	public static final String Cnatureid 		= "org.eclipse.cdt.core.cnature";
	public static final String CCnatureid 		= "org.eclipse.cdt.core.ccnature";
	public static final String Buildnatureid 	= "org.eclipse.cdt.managedbuilder.core.managedBuildNature";
	public static final String Scannernatureid 	= "org.eclipse.cdt.managedbuilder.core.ScannerConfigNature";
	public static final String AVRnatureid 		= PluginStart + "arduinonature";//"avrnature";
	
	
	//Static lib versus application
	public static final String buildArtefactType = "org.eclipse.cdt.build.core.buildArtefactType";
	public static final String StaticLibTag 	= PluginStart + "buildArtefactType.staticLib";
	public static final String ApplicationTag 	= PluginStart + "buildArtefactType.app";
	
	
	//GUI related stuff
	public static final String gdbservertool				= PluginStart + "ui.targets.gdbservertool";
	public static final String nameandmcu 					=  PluginStart + "ui.targets.nameandmcu";
	public static final String programmer 					= PluginStart + "ui.targets.programmer";
	public static final String programmertool 				= PluginStart + "ui.targets.programmertool";
	public static final String TargetConfigurationEditor 	= PluginStart + "ui.editors.TargetConfigurationEditor";
	public static final String fuseeditor 					= PluginStart + "fuseeditor";
	


	//preference nodes
	public static final String NODE_PATHS					= CORE_PLUGIN_ID	+ "/avrpaths";
	public static final String NODE_ARDUINO					= PluginStart + "arduino";
	//preference keys
	public static final String KEY_HEADER_PATH 				= "AVRINCLUDE";
	public static final String KEY_GNU_PATH 				= "MAKE";
	public static final String KEY_GCC_PATH 				= "AVRGCC";
	public static final String KEY_AVRDUDE_PATH				= "AVRDUDE";
	public static final String KEY_ARDUINOPATH 				= "Arduino Path";
	public static final String KEY_ARDUINOBOARD 			= "Arduino Board";
	public static final String KEY_ARDUINOPORT 				= "Arduino Port";
	public static final String KEY_USE_ARDUINO_IDE_TOOLS 	= "Use Arduino IDE Tools";
	public static final String KEY_NO_SCAN_AT_STARTUP 		= "NoScanAtStartup";
	private static final String KEY_SYSTEM_PATH 			= "/systempath/";
	public static final String KEY_SYSTEM 					= "System";
	public static final String KEY_GCC_SYSTEM_PATH			= KEY_SYSTEM_PATH +KEY_GCC_PATH;
	public static final String KEY_GNU_SYSTEM_PATH			= KEY_SYSTEM_PATH +KEY_GNU_PATH;
	public static final String KEY_HEADER_SYSTEM_PATH		= KEY_SYSTEM_PATH +KEY_HEADER_PATH;
	
	
	//Folder Information
	public static final String LIBRARY_PATH_SUFFIX 			=  "libraries" ;
	public static final String HEADER_PATH_SUFFIX 			= "hardware/tools/avr/avr/include";
	private static final String AVRDUDE_PATH_SUFFIX_WIN		= "hardware/tools/avr/bin";
	private static final String AVRDUDE_PATH_SUFFIX_LINUX	= "hardware/tools";
	public static final String GCC_PATH_SUFFIX 				= "hardware/tools/avr/bin";
	public static final String GNU_PATH_SUFFIX 				= "hardware/tools/avr/utils/bin";
	private static final String DUDE_CONFIG_SUFFIX_WIN 		= "hardware/tools/avr/etc/avrdude.conf";
	private static final String DUDE_CONFIG_SUFFIX_LINUX 		= "hardware/tools/avrdude.conf";
	public static final String BOARDS_FILE_SUFFIX 			= "hardware/arduino/boards.txt";
	
	public static String DUDE_CONFIG_SUFFIX()
	{
		if (Platform.getOS().equals(Platform.OS_WIN32)) return DUDE_CONFIG_SUFFIX_WIN;
		if (Platform.getOS().equals(Platform.OS_LINUX)) return DUDE_CONFIG_SUFFIX_LINUX;
		IStatus status = new Status(IStatus.ERROR,	ArduinoConst.CORE_PLUGIN_ID, "Unsupported operating system", null);
		Bundle bundle = Platform.getBundle(Platform.PI_RUNTIME);
		ILog log = Platform.getLog(bundle);
		log.log(status);
		return DUDE_CONFIG_SUFFIX_LINUX;
	}
	
	public static String AVRDUDE_PATH_SUFFIX()
	{
		if (Platform.getOS().equals(Platform.OS_WIN32)) return AVRDUDE_PATH_SUFFIX_WIN;
		if (Platform.getOS().equals(Platform.OS_LINUX)) return AVRDUDE_PATH_SUFFIX_LINUX;
		IStatus status = new Status(IStatus.ERROR,	ArduinoConst.CORE_PLUGIN_ID, "Unsupported operating system", null);
		Bundle bundle = Platform.getBundle(Platform.PI_RUNTIME);
		ILog log = Platform.getLog(bundle);
		log.log(status);
		return AVRDUDE_PATH_SUFFIX_LINUX;
	}

	public static String UploadPortPrefix()
	{
		if (Platform.getOS().equals(Platform.OS_WIN32)) return UploadPortPrefix_WIN;
		if (Platform.getOS().equals(Platform.OS_LINUX)) return UploadPortPrefix_LINUX;
		IStatus status = new Status(IStatus.ERROR,	ArduinoConst.CORE_PLUGIN_ID, "Unsupported operating system", null);
		Bundle bundle = Platform.getBundle(Platform.PI_RUNTIME);
		ILog log = Platform.getLog(bundle);
		log.log(status);
		return UploadPortPrefix_LINUX;
	}
	
}
