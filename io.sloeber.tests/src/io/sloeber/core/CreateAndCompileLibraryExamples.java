package io.sloeber.core;

import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
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
import io.sloeber.core.api.Preferences;
import io.sloeber.core.boards.AdafruitnCirquitPlaygroundBoard;
import io.sloeber.core.boards.AdafruitnRF52idBoard;
import io.sloeber.core.boards.EsploraBoard;
import io.sloeber.core.boards.IBoard;
import io.sloeber.core.boards.NodeMCUBoard;
import io.sloeber.core.boards.UnoBoard;
import io.sloeber.core.boards.leonardoBoard;

@SuppressWarnings("nls")
@RunWith(Parameterized.class)
public class CreateAndCompileLibraryExamples {
	private static final boolean reinstall_boards_and_examples = false;
	private static int mCounter = 0;
	private CodeDescriptor myCodeDescriptor;
	private BoardDescriptor myBoardid;
	private static int totalFails = 0;

	public CreateAndCompileLibraryExamples(String name, BoardDescriptor boardid, CodeDescriptor codeDescriptor) {
		this.myBoardid = boardid;
		this.myCodeDescriptor = codeDescriptor;

	}

	@SuppressWarnings("rawtypes")
	@Parameters(name = "{index}: {0}")
	public static Collection examples() {
		WaitForInstallerToFinish();
		Preferences.setUseArduinoToolSelection(true);
		IBoard myBoards[] = { new leonardoBoard(), new UnoBoard(), new EsploraBoard(), new AdafruitnRF52idBoard(),
				new AdafruitnCirquitPlaygroundBoard(), new NodeMCUBoard() };

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
						Object[] theData = new Object[] { libName + ":" + inoName + ":" + curBoard.getName(),
								curBoard.getBoardDescriptor(), codeDescriptor };
						examples.add(theData);
						break;
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
		final String[] inoNotOK = { "AD7193_VoltageMeasurePsuedoDifferential_Example", "bunny_cuberotate?cuberotate",
				"XPT2046_Touchscreen?ILI9341Test" };
		final String[] libNotOk = { "ACROBOTIC_SSD1306", "XLR8Servo", "Adafruit_CC3000_Library" };
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
		String[] packageUrlsToAdd = { Shared.ESP8266_BOARDS_URL, Shared.ADAFRUIT_BOARDS_URL, Shared.TEST_LIBRARY_INDEX_URL };
		BoardsManager.addPackageURLs(new HashSet<>(Arrays.asList(packageUrlsToAdd)), true);
		if (reinstall_boards_and_examples) {
			BoardsManager.installAllLatestPlatforms();
			// deal with removal of json files or libs from json files
			LibraryManager.removeAllLibs();
			LibraryManager.installAllLatestLibraries();
		}

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
				try {
					Thread.sleep(3000);// seen sometimes the libs were still not
										// added
				} catch (InterruptedException e) {
					// ignore
				}
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
