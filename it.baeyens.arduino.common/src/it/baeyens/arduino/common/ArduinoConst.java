package it.baeyens.arduino.common;


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
	public static final String UploadPortPrefix ="-P\\\\.\\";
	
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
	
	
	//Folder Information
	public static final String LIBRARY_PATH_SUFFIX 			= "\\libraries\\";
	public static final String HEADER_PATH_SUFFIX 			= "\\hardware\\tools\\avr\\avr\\include";
	public static final String AVRDUDE_PATH_SUFFIX 			= "\\hardware\\tools\\avr\\bin";
	public static final String GCC_PATH_SUFFIX 				= "\\hardware\\tools\\avr\\bin";
	public static final String GNU_PATH_SUFFIX 				= "\\hardware\\tools\\avr\\utils\\bin";
	public static final String BOARDS_FILE_SUFFIX 			= "\\hardware\\arduino\\boards.txt";
	public static final String DUDE_CONFIG_SUFFIX 			= "\\hardware\\tools\\avr\\etc\\avrdude.conf";
	

	
}
