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
import io.sloeber.core.api.CodeDescriptor;
import io.sloeber.core.api.CompileOptions;
import io.sloeber.core.api.ConfigurationDescriptor;
import io.sloeber.core.api.LibraryManager;
import io.sloeber.core.api.PackageManager;
import io.sloeber.core.api.Preferences;
import io.sloeber.core.boards.AdafruitnRF52idBoard;
import io.sloeber.core.boards.ArduinoBoards;
import io.sloeber.core.boards.ESP8266Boards;
import io.sloeber.core.boards.ESPressoLite;
import io.sloeber.core.boards.IBoard;

@SuppressWarnings("nls")
@RunWith(Parameterized.class)
public class CreateAndCompileLibraryExamplesTest {
	private static final boolean reinstall_boards_and_examples = false;
	private static int mCounter = 0;
	private CodeDescriptor myCodeDescriptor;
	private BoardDescriptor myBoardid;
	private static int totalFails = 0;

	public CreateAndCompileLibraryExamplesTest(String name, BoardDescriptor boardid, CodeDescriptor codeDescriptor) {
		this.myBoardid = boardid;
		this.myCodeDescriptor = codeDescriptor;

	}

	@SuppressWarnings("rawtypes")
	@Parameters(name = "{index}: {0}")
	public static Collection examples() {
		WaitForInstallerToFinish();
		Preferences.setUseArduinoToolSelection(true);
		IBoard myBoards[] = { ArduinoBoards.leonardo(), ArduinoBoards.uno(), ArduinoBoards.getEsploraBoard(), new AdafruitnRF52idBoard(),
				ArduinoBoards.AdafruitnCirquitPlaygroundBoard(), ESP8266Boards.NodeMCUBoard() ,new ESPressoLite()};

		LinkedList<Object[]> examples = new LinkedList<>();
		TreeMap<String, IPath> exampleFolders = LibraryManager.getAllExamples(null);
		for (Map.Entry<String, IPath> curexample : exampleFolders.entrySet()) {
			ArrayList<IPath> paths = new ArrayList<>();

			paths.add(new Path(curexample.getValue().toString()));
			CodeDescriptor codeDescriptor = CodeDescriptor.createExample(false, paths);

			String inoName = curexample.getKey();
			String libName = "";
			if (examples.size() >= 100) {// use this for debugging based on the
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
			Example example=new Example(inoName,libName,curexample.getValue());
				// with the current amount of examples only do one
				BoardDescriptor curBoard =IBoard.pickBestBoard(example,myBoards);
				if(curBoard!=null) {
						Object[] theData = new Object[] { libName + ":" + inoName + ":" + curBoard.getBoardName(),
								curBoard, codeDescriptor };
						examples.add(theData);
				}
		}

		return examples;

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
		String[] packageUrlsToAdd = { Shared.ESP8266_BOARDS_URL, Shared.ADAFRUIT_BOARDS_URL};
		PackageManager.addPackageURLs(new HashSet<>(Arrays.asList(packageUrlsToAdd)), reinstall_boards_and_examples);
		if (reinstall_boards_and_examples) {
			PackageManager.installAllLatestPlatforms();
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
		if (totalFails < 200) {
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
