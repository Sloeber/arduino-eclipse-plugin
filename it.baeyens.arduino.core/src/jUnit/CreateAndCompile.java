package jUnit;

import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.cdt.core.model.ICModelMarker;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.jobs.IJobManager;
import org.eclipse.core.runtime.jobs.Job;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import io.sloeber.core.api.BoardID;
import io.sloeber.core.api.CodeDescriptor;
import io.sloeber.core.api.ConfigurationDescriptor;
import it.baeyens.arduino.managers.ArduinoPlatform;
import it.baeyens.arduino.managers.Board;
import it.baeyens.arduino.managers.Manager;
import it.baeyens.arduino.managers.Package;
import it.baeyens.arduino.tools.TxtFile;

@SuppressWarnings("nls")
@RunWith(Parameterized.class)
public class CreateAndCompile {
    private String mBoardID;
    private Map<String, String> myOptions = new HashMap<>();
    private String mPackageName;
    private String mPlatform;
    private String mJsonFileName;
    private static int mCounter = 0;

    public CreateAndCompile(String jsonFileName, String packageName, String platform, String boardID, String options) {
	this.mBoardID = boardID;
	this.mPackageName = packageName;
	this.mPlatform = platform;
	this.mJsonFileName = jsonFileName;
	String[] lines = options.split("\n"); //$NON-NLS-1$
	for (String curLine : lines) {
	    String[] values = curLine.split("=", 2); //$NON-NLS-1$
	    if (values.length == 2) {
		this.myOptions.put(values[0], values[1]);
	    }
	}

    }

    @SuppressWarnings("rawtypes")
    @Parameterized.Parameters
    public static Collection boards() {
	return Arrays.asList(new Object[][] {
		// Arduino SAM
		{ "package_index.json", "arduino", "Arduino SAM Boards (32-bits ARM Cortex-M3)", "arduino_due_x_dbg",
			"" }, //
		{ "package_index.json", "arduino", "Arduino SAM Boards (32-bits ARM Cortex-M3)", "arduino_due_x", "" }, //

		// Arduino SAMD
		{ "package_index.json", "arduino", "Arduino SAMD Boards (32-bits ARM Cortex-M0+)", "arduino_zero_edbg",
			"" }, //
		{ "package_index.json", "arduino", "Arduino SAMD Boards (32-bits ARM Cortex-M0+)",
			"arduino_zero_native", "" }, //
		{ "package_index.json", "arduino", "Arduino SAMD Boards (32-bits ARM Cortex-M0+)", "mkr1000", "" }, //

		// RedBearLab
		{ "package_redbearlab_index.json", "RedBearLab", "RedBearLab AVR Boards", "blend", "" }, //
		{ "package_redbearlab_index.json", "RedBearLab", "RedBearLab AVR Boards", "blendmicro8", "" }, //
		{ "package_redbearlab_index.json", "RedBearLab", "RedBearLab AVR Boards", "blendmicro16", "" }, //
		{ "package_redbearlab_index.json", "RedBearLab", "RedBearLab nRF51822 Boards (32-bits ARM Cortex-M0)",
			"nRF51822", "" }, //
		{ "package_redbearlab_index.json", "RedBearLab", "RedBearLab nRF51822 Boards (32-bits ARM Cortex-M0)",
			"nRF51822_NANO", "" }, //
		{ "package_redbearlab_index.json", "RedBearLab", "RedBearLab nRF51822 Boards (32-bits ARM Cortex-M0)",
			"nRF51822_32KB", "" }, //
		{ "package_redbearlab_index.json", "RedBearLab", "RedBearLab nRF51822 Boards (32-bits ARM Cortex-M0)",
			"nRF51822_NANO_32KB", "" }, //

		// ESP boards
		{ "package_esp8266com_index.json", "esp8266", "esp8266", "generic",
			".CPU Frequency==80 MHz\nFlash Frequency=40MHz\nFlash Mode=DIO\nUpload Speed=115200\nFlash Size=512K (64K SPIFFS)\nReset Method=ck\nDebug port=Disabled\nDebug Level=None" },
		{ "package_esp8266com_index.json", "esp8266", "esp8266", "esp8285",
			"CPU Frequency=80 MHz\nUpload Speed=115200\nFlash Size=1M (512K SPIFFS)" },
		{ "package_esp8266com_index.json", "esp8266", "esp8266", "espduino",
			"CPU Frequency=80 MHz\nUpload Speed=115200\nFlash Size=4M (3M SPIFFS)" },
		{ "package_esp8266com_index.json", "esp8266", "esp8266", "huzzah",
			"CPU Frequency=80 MHz\nUpload Speed=115200\nFlash Size=4M (3M SPIFFS)" },
		{ "package_esp8266com_index.json", "esp8266", "esp8266", "espresso_lite_v1",
			"CPU Frequency=80 MHz\nUpload Speed=115200\nFlash Size=4M (3M SPIFFS)\nReset Method=nodemcu\nDebug port=Disabled\nDebug Level=None" },
		{ "package_esp8266com_index.json", "esp8266", "esp8266", "espresso_lite_v2",
			"CPU Frequency=80 MHz\nUpload Speed=115200\nFlash Size=4M (3M SPIFFS)\nReset Method=nodemcu\nDebug port=Disabled\nDebug Level=None" },
		{ "package_esp8266com_index.json", "esp8266", "esp8266", "phoenix_v1",
			"CPU Frequency=80 MHz\nUpload Speed=115200\nFlash Size=4M (3M SPIFFS)\nReset Method=nodemcu\nDebug port=Disabled\nDebug Level=None" },
		{ "package_esp8266com_index.json", "esp8266", "esp8266", "phoenix_v2",
			"CPU Frequency=80 MHz\nUpload Speed=115200\nFlash Size=4M (3M SPIFFS)\nReset Method=nodemcu\nDebug port=Disabled\nDebug Level=None" },
		{ "package_esp8266com_index.json", "esp8266", "esp8266", "nodemcu",
			"CPU Frequency=80 MHz\nUpload Speed=115200\nFlash Size=4M (3M SPIFFS)" },
		{ "package_esp8266com_index.json", "esp8266", "esp8266", "nodemcuv2",
			"CPU Frequency=80 MHz\nUpload Speed=115200\nFlash Size=4M (3M SPIFFS)" },
		{ "package_esp8266com_index.json", "esp8266", "esp8266", "modwifi",
			"CPU Frequency=80 MHz\nUpload Speed=115200" },
		{ "package_esp8266com_index.json", "esp8266", "esp8266", "thing",
			"CPU Frequency=80 MHz\nUpload Speed=115200" },
		{ "package_esp8266com_index.json", "esp8266", "esp8266", "thingdev",
			"CPU Frequency=80 MHz\nUpload Speed=115200" },
		{ "package_esp8266com_index.json", "esp8266", "esp8266", "esp210",
			"CPU Frequency=80 MHz\nUpload Speed=115200\nFlash Size=4M (3M SPIFFS)" },
		{ "package_esp8266com_index.json", "esp8266", "esp8266", "d1_mini",
			"CPU Frequency=80 MHz\nUpload Speed=115200\nFlash Size=4M (3M SPIFFS)" },
		{ "package_esp8266com_index.json", "esp8266", "esp8266", "d1",
			"CPU Frequency=80 MHz\nUpload Speed=115200\nFlash Size=4M (3M SPIFFS)" },
		{ "package_esp8266com_index.json", "esp8266", "esp8266", "espino",
			"CPU Frequency=80 MHz\nUpload Speed=115200\nFlash Size=4M (3M SPIFFS)\nFlash Mode=DIO\nReset Method=ck" },
		{ "package_esp8266com_index.json", "esp8266", "esp8266", "espinotee",
			"CPU Frequency=80 MHz\nUpload Speed=115200\nFlash Size=4M (3M SPIFFS)" },
		{ "package_esp8266com_index.json", "esp8266", "esp8266", "wifinfo",
			"CPU Frequency=80 MHz\nUpload Speed=115200\nModule=ESP07 (1M/192K SPIFFS)\nReset Method=nodemcu\nDebug port=Disabled\nDebug Level=None\nFlash Frequency=40MHz\nFlash Mode=QIO" },
		{ "package_esp8266com_index.json", "esp8266", "esp8266", "coredev",
			"CPU Frequency=80 MHz\nUpload Speed=115200\nFlash Size=4M (3M SPIFFS)\nReset Method=nodemcu\nDebug port=Disabled\nDebug Level=None\nFlash Frequency=40MHz\nFlash Mode=QIO\nlwIP Variant=Espressif (xcc)" },

		// mighty core
		{ "package_MCUdude_MightyCore_index.json", "MightyCore", "MightyCore", "1284",
			"Clock=16MHz external\nPinout=Bobuino\nB.O.D=2.7v\nVariant=1284P" }, //
		{ "package_MCUdude_MightyCore_index.json", "MightyCore", "MightyCore", "1284",
			"Clock=16MHz external\nPinout=Standard\nB.O.D=2.7v\nVariant=1284P" }, //
		{ "package_MCUdude_MightyCore_index.json", "MightyCore", "MightyCore", "644",
			"Clock=20MHz external\nPinout=Standard\nB.O.D=2.7v\nVariant=644P / 644PA" }, //
		{ "package_MCUdude_MightyCore_index.json", "MightyCore", "MightyCore", "324",
			"Clock=12MHz external\nPinout=Bobuino\nB.O.D=1.8v\nVariant=324P" }, //
		{ "package_MCUdude_MightyCore_index.json", "MightyCore", "MightyCore", "164",
			"Clock=8MHz external\nPinout=Bobuino\nB.O.D=Disabled\nVariant=164A" }, //
		{ "package_MCUdude_MightyCore_index.json", "MightyCore", "MightyCore", "32",
			"Clock=8MHz external  (BOD 2.7v)\nPinout=Standard" }, //
		{ "package_MCUdude_MightyCore_index.json", "MightyCore", "MightyCore", "16",
			"Clock=16MHz external (BOD 2.7v)\nPinout=Bobuino" }, //
		{ "package_MCUdude_MightyCore_index.json", "MightyCore", "MightyCore", "8535",
			"Clock=20MHz external (BOD 4.0v)\nPinout=Bobuino" }, //

		// Adafruit AVR
		{ "package_adafruit_index.json", "adafruit", "Adafruit AVR Boards", "flora8", "" }, //
		{ "package_adafruit_index.json", "adafruit", "Adafruit AVR Boards", "bluefruitmicro", "" }, //
		{ "package_adafruit_index.json", "adafruit", "Adafruit AVR Boards", "gemma", "" }, //
		{ "package_adafruit_index.json", "adafruit", "Adafruit AVR Boards", "feather32u4", "" }, //
		{ "package_adafruit_index.json", "adafruit", "Adafruit AVR Boards", "trinket3", "" }, //
		{ "package_adafruit_index.json", "adafruit", "Adafruit AVR Boards", "trinket5", "" }, //
		{ "package_adafruit_index.json", "adafruit", "Adafruit AVR Boards", "protrinket5", "" }, //
		{ "package_adafruit_index.json", "adafruit", "Adafruit AVR Boards", "protrinket3", "" }, //
		{ "package_adafruit_index.json", "adafruit", "Adafruit AVR Boards", "protrinket5ftdi", "" }, //
		{ "package_adafruit_index.json", "adafruit", "Adafruit AVR Boards", "protrinket3ftdi", "" }, //
		{ "package_adafruit_index.json", "adafruit", "Adafruit AVR Boards", "adafruit32u4", "" }, //
		{ "package_adafruit_index.json", "adafruit", "Adafruit AVR Boards", "circuitplay32u4cat", "" }, //
		{ "package_adafruit_index.json", "adafruit", "Adafruit SAMD Boards", "adafruit_feather_m0", "" }, //

		// Arduino.cc AVR boards
		{ "package_index.json", "arduino", "Arduino AVR Boards", "yun", "" }, //
		{ "package_index.json", "arduino", "Arduino AVR Boards", "uno", "" }, //
		{ "package_index.json", "arduino", "Arduino AVR Boards", "diecimila", "Processor=ATMega328" },
		{ "package_index.json", "arduino", "Arduino AVR Boards", "nano", "Processor=ATMega328" },
		{ "package_index.json", "arduino", "Arduino AVR Boards", "mega", "Processor=ATMega2560 (Mega 2560)" }, // comment
		{ "package_index.json", "arduino", "Arduino AVR Boards", "megaADK", "" }, //
		{ "package_index.json", "arduino", "Arduino AVR Boards", "leonardo", "" }, //
		{ "package_index.json", "arduino", "Arduino AVR Boards", "micro", "" }, //
		{ "package_index.json", "arduino", "Arduino AVR Boards", "esplora", "" }, //
		{ "package_index.json", "arduino", "Arduino AVR Boards", "mini", "Processor=ATMega328" }, // comment
		{ "package_index.json", "arduino", "Arduino AVR Boards", "ethernet", "" }, //
		{ "package_index.json", "arduino", "Arduino AVR Boards", "fio", "" }, //
		{ "package_index.json", "arduino", "Arduino AVR Boards", "bt", "Processor=ATMega328" }, // comment
		{ "package_index.json", "arduino", "Arduino AVR Boards", "LilyPadUSB", "" }, // comment
		{ "package_index.json", "arduino", "Arduino AVR Boards", "lilypad", "Processor=ATMega328" }, // comment
		{ "package_index.json", "arduino", "Arduino AVR Boards", "pro", "Processor=ATMega328 (3.3V, 8 MHz)" }, // comment
		{ "package_index.json", "arduino", "Arduino AVR Boards", "atmegang", "Processor=ATMega8" }, //
		{ "package_index.json", "arduino", "Arduino AVR Boards", "robotControl", "" }, //
		{ "package_index.json", "arduino", "Arduino AVR Boards", "robotMotor", "" }, //
		{ "package_index.json", "arduino", "Arduino AVR Boards", "gemma", "" },

	});
    }

    /*
     * In new new installations (of the Sloeber development environment) the
     * installer job will trigger downloads These mmust have finished before we
     * can start testing
     */
    @BeforeClass
    public static void WaitForInstallerToFinish() {
	installAdditionalBoards();
	waitForAllJobsToFinish();
    }

    public static void waitForAllJobsToFinish() {
	try {
	    Thread.sleep(10000);

	    IJobManager jobMan = Job.getJobManager();

	    while (!jobMan.isIdle()) {
		Thread.sleep(5000);
	    }
	    // As nothing is running now we can start installing

	} catch (InterruptedException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	    fail("can not find installerjob");
	}
    }

    public static void installAdditionalBoards() {
	String packageUrlsToAdd[] = { "https://adafruit.github.io/arduino-board-index/package_adafruit_index.json",
		"https://mcudude.github.io/MightyCore/package_MCUdude_MightyCore_index.json",
		"https://raw.githubusercontent.com/mikaelpatel/Cosa/master/package_cosa_index.json",
		"https://raw.githubusercontent.com/sparkfun/Arduino_Boards/master/IDE_Board_Manager/package_sparkfun_index.json",
		"https://redbearlab.github.io/arduino/package_redbearlab_index.json" };
	Manager.addPackageURLs(packageUrlsToAdd);
	NullProgressMonitor monitor = new NullProgressMonitor();
	List<Package> allPackages = Manager.getPackages();
	for (Package curPackage : allPackages) {
	    Collection<ArduinoPlatform> latestPlatforms = curPackage.getLatestPlatforms();
	    for (ArduinoPlatform curPlatform : latestPlatforms) {
		curPlatform.install(monitor);
	    }
	}
    }

    @Test
    public void testBoard() {
	String boardID = this.mBoardID;
	List<Board> boards = null;
	try {
	    Package thePackage = Manager.getPackage(this.mJsonFileName, this.mPackageName);
	    if (thePackage == null) {
		fail("failed to find package:" + this.mPackageName);
		return;
	    }
	    ArduinoPlatform platform = thePackage.getLatestPlatform(this.mPlatform);
	    if (platform == null) {
		fail("failed to find platform " + this.mPlatform + " in package:" + this.mPackageName);
		return;
	    }
	    boards = platform.getBoards();
	} catch (CoreException e1) {
	    // TODO Auto-generated catch block
	    e1.printStackTrace();
	}
	if (boards == null) {
	    fail("No boards found");
	    return;
	}
	for (Board curBoard : boards) {
	    if (curBoard.getId().equals(boardID)) {
		BuildAndVerify(curBoard);
		return;
	    }

	}
	fail("Board " + boardID + " not found");
    }

    private void BuildAndVerify(Board board) {
	java.io.File boardsFile = board.getPlatform().getBoardsFile();
	TxtFile boardsTxtFile = new TxtFile(boardsFile);
	System.out.println("Testing board: " + board.getName());
	BoardID boardid = createBoardID(boardsTxtFile, board.getId());

	boardid.setOptions(this.myOptions);

	IProject theTestProject = null;
	CodeDescriptor codeDescriptor = CodeDescriptor.createDefaultIno();
	NullProgressMonitor monitor = new NullProgressMonitor();
	String formatted = String.format("%03d_", new Integer(mCounter++));
	try {

	    theTestProject = boardid.createProject(formatted + board.getId(), null,
		    ConfigurationDescriptor.getDefaultDescriptors(), codeDescriptor, monitor);
	    waitForAllJobsToFinish(); // for the indexer
	} catch (Exception e) {
	    e.printStackTrace();
	    fail("Failed to create the project:" + formatted + board.getId());
	    return;
	}
	try {
	    theTestProject.build(IncrementalProjectBuilder.FULL_BUILD, monitor);
	    if (hasBuildErrors(theTestProject)) {
		fail("Failed to compile the project:" + formatted + board.getId() + " build errors");
	    }
	} catch (CoreException e) {
	    e.printStackTrace();
	    fail("Failed to compile the project:" + board.getName() + " exception");
	}
    }

    private static BoardID createBoardID(TxtFile boardsFile, String boardID) {
	BoardID boardid = new BoardID(null);

	boardid.setBoardsFile(boardsFile);
	boardid.setBoardID(boardID);
	return boardid;
    }

    private static boolean hasBuildErrors(IProject project) throws CoreException {
	IMarker[] markers = project.findMarkers(ICModelMarker.C_MODEL_PROBLEM_MARKER, true, IResource.DEPTH_INFINITE);
	for (IMarker marker : markers) {
	    if (marker.getAttribute(IMarker.SEVERITY, IMarker.SEVERITY_INFO) == IMarker.SEVERITY_ERROR) {
		return true;
	    }
	}
	return false;
    }
}
