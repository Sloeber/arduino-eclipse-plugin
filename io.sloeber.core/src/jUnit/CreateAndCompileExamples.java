package jUnit;

import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.TreeMap;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import io.sloeber.core.api.BoardDescriptor;
import io.sloeber.core.api.BoardsManager;
import io.sloeber.core.api.CodeDescriptor;
import io.sloeber.core.api.CompileOptions;
import io.sloeber.core.api.ConfigurationDescriptor;
import io.sloeber.core.api.LibraryManager;
import jUnit.boards.AdafruitnCirquitPlaygroundBoard;
import jUnit.boards.AdafruitnRF52idBoard;
import jUnit.boards.EsploraBoard;
import jUnit.boards.IBoard;
import jUnit.boards.UnoBoard;

@SuppressWarnings("nls")
@RunWith(Parameterized.class)
public class CreateAndCompileExamples {
	private static int mCounter = 0;
	private CodeDescriptor myCodeDescriptor;
	private BoardDescriptor myBoardid;
	private static int totalFails = 0;

	public static class leonardoBoard extends IBoard {
		public leonardoBoard(Map<String, String> options) {
			this.myBoardDescriptor = BoardsManager.getBoardDescriptor("package_index.json", "arduino",
					"Arduino AVR Boards", "leonardo", options);
			if (this.myBoardDescriptor == null) {
				fail("leonardo Board not found");
			}
			this.myBoardDescriptor.setUploadPort("none");
		}

		@Override
		public boolean isExampleOk(String inoName, String libName) {
			final String[] inoNotOk = { "Esploraexamples?Beginners?EsploraJoystickMouse",
					"Esploraexamples?Experts?EsploraKart", "Esploraexamples?Experts?EsploraTable",
					"Firmataexamples?StandardFirmataWiFi", "cplay_neopixel_picker" };
			final String[] libNotOk = { "A4963", "Adafruit_Motor_Shield_library", "Adafruit_Motor_Shield_library_V2",
					"AccelStepper" };
			if (Arrays.asList(libNotOk).contains(libName)) {
				return false;
			}
			if (inoName.contains("Firmata"))
				return false;
			if (inoName.replace(" ", "").startsWith("TFTexamples?Esplora?Esplora"))
				return false;
			if (Arrays.asList(inoNotOk).contains(inoName.replace(" ", "")))
				return false;
			return true; // default everything is fine
		}

	}

	public CreateAndCompileExamples(String name, BoardDescriptor boardid, CodeDescriptor codeDescriptor) {
		this.myBoardid = boardid;
		this.myCodeDescriptor = codeDescriptor;

	}

	@SuppressWarnings("rawtypes")
	@Parameters(name = "{index}: {0}")
	public static Collection examples() {
		WaitForInstallerToFinish();
		Map<String, String> myOptions = new HashMap<>();
		String[] lines = new String("").split("\n"); //$NON-NLS-1$
		for (String curLine : lines) {
			String[] values = curLine.split("=", 2); //$NON-NLS-1$
			if (values.length == 2) {
				myOptions.put(values[0], values[1]);
			}
		}
		IBoard myBoards[] = { new leonardoBoard(myOptions), new UnoBoard(myOptions), new EsploraBoard(myOptions),
				new AdafruitnRF52idBoard(myOptions), new AdafruitnCirquitPlaygroundBoard(myOptions) };

		LinkedList<Object[]> examples = new LinkedList<>();
		TreeMap<String, IPath> exampleFolders = BoardsManager.getAllExamples(null);
		for (Map.Entry<String, IPath> curexample : exampleFolders.entrySet()) {
			ArrayList<Path> paths = new ArrayList<>();

			paths.add(new Path(curexample.getValue().toString()));
			CodeDescriptor codeDescriptor = CodeDescriptor.createExample(false, paths);
			String inoName = curexample.getKey();
			String libName = "";
			if (examples.size() == 82) {// use this for debugging based on the
										// project number
				// use this to put breakpoint
				int a = 0;
				a = a + 1;
			}
			try {
				libName = inoName.split(" ")[0].trim();
			} catch (Exception e) {
				// ignore error
			}
			if (isExampleOk(inoName, libName)) {
				// with the current amount of examples only do one
				for (IBoard curBoard : myBoards) {
					if (curBoard.isExampleOk(inoName, libName)) {
						Object[] theData = new Object[] { libName + ":" + inoName + ":leonardo",
								curBoard.getBoardDescriptor(), codeDescriptor };
						examples.add(theData);
						break; // TODO check if this exits the for (IBoard
						// curBoard : myBoards) loop
					}
				}

			}
		}

		return examples;

	}

	/**
	 * if the example fails it is known not to compile(under sloeber?)
	 *
	 * @param inoName
	 * @param libName
	 * @return
	 */
	private static boolean isExampleOk(String inoName, String libName) {
		final String[] inoNotOK = { "AD7193_VoltageMeasurePsuedoDifferential_Example" };
		final String[] libNotOk = { "ACROBOTIC_SSD1306" };
		if (Arrays.asList(libNotOk).contains(libName)) {
			return false;
		}
		if (Arrays.asList(inoNotOK).contains(inoName.replace(" ", "")))
			return false;
		if (inoName.endsWith("AD7193_VoltageMeasurePsuedoDifferential_Example"))
			return false;
		// following examples also fail in Arduino IDE at the time of writing
		// these unit tests
		if (inoName.endsWith("ahrs_mahony")
				|| ("Adafruit_BLEFirmata".equals(libName) && inoName.endsWith("StandardFirmata"))) {
			return false;
		}
		return true; // default everything is fine
	}

	/*
	 * In new new installations (of the Sloeber development environment) the
	 * installer job will trigger downloads These mmust have finished before we
	 * can start testing
	 */

	public static void WaitForInstallerToFinish() {
		installAdditionalBoards();
		Shared.waitForAllJobsToFinish();
	}

	public static void installAdditionalBoards() {
		String[] packageUrlsToAdd = { "http://arduino.esp8266.com/stable/package_esp8266com_index.json",
				"https://adafruit.github.io/arduino-board-index/package_adafruit_index.json",
				"https://raw.githubusercontent.com/Sloeber/arduino-eclipse-plugin/Better_library_support/io.sloeber.core/src/jUnit/library_sloeber_index.json" };
		BoardsManager.addPackageURLs(new HashSet<>(Arrays.asList(packageUrlsToAdd)), true);
		BoardsManager.installAllLatestPlatforms();
		// deal with removal of json files or libs from json files
		LibraryManager.removeAllLibs();
		LibraryManager.installAllLatestLibraries();

	}

	@Test
	public void testExamples() {
		// Stop after X fails because
		// the fails stays open in eclipse and it becomes really slow
		// There are only a number of issues you can handle
		// best is to focus on the first ones and then rerun starting with the
		// failures
		if (totalFails < 20) {
			BuildAndVerify(this.myBoardid, this.myCodeDescriptor);
		} else {
			fail("To many fails. Stopping test");
		}

	}

	public static void BuildAndVerify(BoardDescriptor boardid, CodeDescriptor codeDescriptor) {

		IProject theTestProject = null;

		NullProgressMonitor monitor = new NullProgressMonitor();
		String projectName = String.format("%05d_:%s:%s:%s", new Integer(mCounter++), codeDescriptor.getLibraryName(),
				codeDescriptor.getExampleName(), boardid.getBoardID());
		try {

			theTestProject = boardid.createProject(projectName, null, ConfigurationDescriptor.getDefaultDescriptors(),
					codeDescriptor, new CompileOptions(null), monitor);
			Shared.waitForAllJobsToFinish(); // for the indexer
		} catch (Exception e) {
			e.printStackTrace();
			totalFails++;
			fail("Failed to create the project:" + projectName);
			return;
		}
		try {
			theTestProject.build(IncrementalProjectBuilder.FULL_BUILD, monitor);
			if (Shared.hasBuildErrors(theTestProject)) {
				// try again because the libraries may not yet been added
				Shared.waitForAllJobsToFinish(); // for the indexer
				theTestProject.build(IncrementalProjectBuilder.FULL_BUILD, monitor);
				if (Shared.hasBuildErrors(theTestProject)) {
					// give up
					totalFails++;
					fail("Failed to compile the project:" + projectName + " build errors");
				} else {
					theTestProject.delete(true, null);
				}
			} else {
				theTestProject.delete(true, null);
			}
		} catch (CoreException e) {
			e.printStackTrace();
			totalFails++;
			fail("Failed to compile the project:" + projectName + " exception");
		}
	}

}
