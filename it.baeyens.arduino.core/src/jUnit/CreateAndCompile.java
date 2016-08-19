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
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import io.sloeber.core.api.BoardID;
import io.sloeber.core.api.CodeDescriptor;
import io.sloeber.core.api.ConfigurationDescriptor;
import it.baeyens.arduino.managers.Board;
import it.baeyens.arduino.managers.Manager;
import it.baeyens.arduino.tools.TxtFile;

@SuppressWarnings("nls")
@RunWith(Parameterized.class)
public class CreateAndCompile {
    private String mBoardID;
    private Map<String, String> myOptions = new HashMap<>();

    public CreateAndCompile(String boardID, String options) {
	this.mBoardID = boardID;
	String[] lines = options.split("\n"); //$NON-NLS-1$
	for (String curLine : lines) {
	    String[] values = curLine.split("=", 2); //$NON-NLS-1$
	    if (values.length == 2) {
		this.myOptions.put(values[0], values[1]);
	    }
	}

    }

    @Parameterized.Parameters
    public static Collection boards() {
	return Arrays.asList(new Object[][] {
		// Arduino.cc AVR boards
		{ "yun", "" }, { "uno", "" }, { "diecimila", "Processor=ATMega328" }, { "nano", "Processor=ATMega328" },
		{ "mega", "Processor=ATMega2560 (Mega 2560)" }, { "megaADK", "" }, { "leonardo", "" }, { "micro", "" },
		{ "esplora", "" }, { "mini", "Processor=ATMega328" }, { "ethernet", "" }, { "fio", "" },
		{ "bt", "Processor=ATMega328" }, { "LilyPadUSB", "" }, { "lilypad", "Processor=ATMega328" },
		{ "pro", "Processor=ATMega328 (3.3V, 8 MHz)" }, { "atmegang", "Processor=ATMega8" },
		{ "robotControl", "" }, { "robotMotor", "" }, { "gemma", "" },
		// now we have ESP boards
		{ "generic",
			".CPU Frequency==80 MHz\nFlash Frequency=40MHz\nFlash Mode=DIO\nUpload Speed=115200\nFlash Size=512K (64K SPIFFS)\nReset Method=ck\nDebug port=Disabled\nDebug Level=None" },
		{ "esp8285", "CPU Frequency=80 MHz\nUpload Speed=115200\nFlash Size=1M (512K SPIFFS)" },
		{ "espduino", "CPU Frequency=80 MHz\nUpload Speed=115200\nFlash Size=4M (3M SPIFFS)" },
		{ "huzzah", "CPU Frequency=80 MHz\nUpload Speed=115200\nFlash Size=4M (3M SPIFFS)" },
		{ "espresso_lite_v1",
			"CPU Frequency=80 MHz\nUpload Speed=115200\nFlash Size=4M (3M SPIFFS)\nReset Method=nodemcu\nDebug port=Disabled\nDebug Level=None" },
		{ "espresso_lite_v2",
			"CPU Frequency=80 MHz\nUpload Speed=115200\nFlash Size=4M (3M SPIFFS)\nReset Method=nodemcu\nDebug port=Disabled\nDebug Level=None" },
		{ "phoenix_v1",
			"CPU Frequency=80 MHz\nUpload Speed=115200\nFlash Size=4M (3M SPIFFS)\nReset Method=nodemcu\nDebug port=Disabled\nDebug Level=None" },
		{ "phoenix_v2",
			"CPU Frequency=80 MHz\nUpload Speed=115200\nFlash Size=4M (3M SPIFFS)\nReset Method=nodemcu\nDebug port=Disabled\nDebug Level=None" },
		{ "nodemcu", "CPU Frequency=80 MHz\nUpload Speed=115200\nFlash Size=4M (3M SPIFFS)" },
		{ "nodemcuv2", "CPU Frequency=80 MHz\nUpload Speed=115200\nFlash Size=4M (3M SPIFFS)" },
		{ "modwifi", "CPU Frequency=80 MHz\nUpload Speed=115200" },
		{ "thing", "CPU Frequency=80 MHz\nUpload Speed=115200" },
		{ "thingdev", "CPU Frequency=80 MHz\nUpload Speed=115200" },
		{ "esp210", "CPU Frequency=80 MHz\nUpload Speed=115200\nFlash Size=4M (3M SPIFFS)" },
		{ "d1_mini", "CPU Frequency=80 MHz\nUpload Speed=115200\nFlash Size=4M (3M SPIFFS)" },
		{ "d1", "CPU Frequency=80 MHz\nUpload Speed=115200\nFlash Size=4M (3M SPIFFS)" },
		{ "espino",
			"CPU Frequency=80 MHz\nUpload Speed=115200\nFlash Size=4M (3M SPIFFS)\nFlash Mode=DIO\nReset Method=ck" },
		{ "espinotee", "CPU Frequency=80 MHz\nUpload Speed=115200\nFlash Size=4M (3M SPIFFS)" },
		{ "wifinfo",
			"CPU Frequency=80 MHz\nUpload Speed=115200\nModule=ESP07 (1M/192K SPIFFS)\nReset Method=nodemcu\nDebug port=Disabled\nDebug Level=None\nFlash Frequency=40MHz\nFlash Mode=QIO" },
		{ "coredev",
			"CPU Frequency=80 MHz\nUpload Speed=115200\nFlash Size=4M (3M SPIFFS)\nReset Method=nodemcu\nDebug port=Disabled\nDebug Level=None\nFlash Frequency=40MHz\nFlash Mode=QIO\nlwIP Variant=Espressif (xcc)" },

	});
    }

    @SuppressWarnings("static-method")
    @Before
    public void WaitForInstallerToFinish() {
	try {
	    Thread.sleep(5000);

	    IJobManager jobMan = Job.getJobManager();

	    while (!jobMan.isIdle()) {
		Thread.sleep(1000);
	    }
	} catch (InterruptedException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	    fail("can not find installerjob");
	}
    }

    @Test
    public void testBoard() {
	String boardID = this.mBoardID;
	List<Board> boards = null;
	try {
	    boards = Manager.getInstalledBoards();
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
	try {
	    theTestProject = boardid.createProject(board.getId(), null, ConfigurationDescriptor.getDefaultDescriptors(),
		    codeDescriptor, monitor);
	} catch (Exception e) {
	    e.printStackTrace();
	    fail("Failed to create the project:" + board.getName());
	    return;
	}
	try {
	    theTestProject.build(IncrementalProjectBuilder.FULL_BUILD, monitor);
	    if (hasBuildErrors(theTestProject)) {
		fail("Failed to compile the project:" + board.getName() + " build errors");
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
