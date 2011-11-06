package it.baeyens.arduino;

public class ArduinoConst {
	public static final String ProgrammerConfigName = "AVR Eclipse controlled Arduino settings";
	public static final String ProgrammerConfigDescription = "AVReclipse controlled arduino settings. You should not changes this manually";
	public static final String ARDUINOPATH_PROPERTY = "Arduino Path";
	public static final String ARDUINOBOARD_PROPERTY = "Arduino Board";
	public static final String ARDUINOPORT_PROPERTY = "Arduino Port";
	//public static final String PLUGIN_ID = "cc.arduino.eclipse";
	public static final String CoreProjectNamePrefix = "arduino_";
	public static final String ProgrammerName = "stk500v1"; // "arduino" "atmelSTK500 Version1.x Firmware";
	public static final String UploadPortPrefix ="-P\\\\.\\";
	public static final String Cnatureid = "org.eclipse.cdt.core.cnature";
	public static final String CCnatureid = "org.eclipse.cdt.core.ccnature";
	public static final String Buildnatureid = "org.eclipse.cdt.managedbuilder.core.managedBuildNature";
	public static final String Scannernatureid = "org.eclipse.cdt.managedbuilder.core.ScannerConfigNature";
	public static final String buildArtefactType = "org.eclipse.cdt.build.core.buildArtefactType";
	
	public static final String PluginStart = "it.baeyens."; //"de .innot.avreclipse.";
	public static final String AVRnatureid = PluginStart + "arduinonature";//"avrnature";
	public static final String StaticLibTag = PluginStart + "buildArtefactType.staticLib";
	public static final String ApplicationTag = PluginStart + "buildArtefactType.app";
	public static final String gdbservertool= PluginStart + "ui.targets.gdbservertool";
	public static final String nameandmcu =  PluginStart + "ui.targets.nameandmcu";
	public static final String programmer = PluginStart + "ui.targets.programmer";
	public static final String programmertool = PluginStart + "ui.targets.programmertool";
	public static final String TargetConfigurationEditor = PluginStart + "ui.editors.TargetConfigurationEditor";
	public static final String fuseeditor = PluginStart + "fuseeditor";
	
	public static final String	CORE_PLUGIN_ID		= PluginStart +"core";
	public static final String	UI_PLUGIN_ID	= PluginStart + "avreclipse.ui";
	public static final String MCU_SELECT_PAGE_ID = PluginStart +"mcuselectpage";
	
}
