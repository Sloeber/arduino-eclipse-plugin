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
import io.sloeber.core.boards.AdafruitnCirquitPlaygroundBoard;
import io.sloeber.core.boards.AdafruitnRF52idBoard;
import io.sloeber.core.boards.Due;
import io.sloeber.core.boards.EsploraBoard;
import io.sloeber.core.boards.GenericArduinoAvrBoard;
import io.sloeber.core.boards.IBoard;
import io.sloeber.core.boards.NodeMCUBoard;
import io.sloeber.core.boards.Primo;
import io.sloeber.core.boards.UnoBoard;
import io.sloeber.core.boards.Zero;
import io.sloeber.core.boards.leonardoBoard;
import io.sloeber.core.boards.megaBoard;
import io.sloeber.core.boards.mkrfox1200;

@SuppressWarnings("nls")
@RunWith(Parameterized.class)
public class CreateAndCompileExamples {
	private static final boolean reinstall_boards_and_examples = false;
	private static int mCounter = 0;
	private CodeDescriptor myCodeDescriptor;
	private BoardDescriptor myBoardid;
	private static int totalFails = 0;
	private String myName;

	public CreateAndCompileExamples(String name, BoardDescriptor boardid, CodeDescriptor codeDescriptor) {
		this.myBoardid = boardid;
		this.myCodeDescriptor = codeDescriptor;
		this.myName = name;
	}

	@SuppressWarnings("rawtypes")
	@Parameters(name = "{index}: {0}")
	public static Collection examples() {
		WaitForInstallerToFinish();

		IBoard myBoards[] = { new leonardoBoard(), new UnoBoard(), new EsploraBoard(), new AdafruitnRF52idBoard(),
				new AdafruitnCirquitPlaygroundBoard(), new NodeMCUBoard(), new Primo(), new megaBoard(),
				new GenericArduinoAvrBoard("gemma"), new Zero(), new mkrfox1200(), new Due() };

		LinkedList<Object[]> examples = new LinkedList<>();
		TreeMap<String, IPath> exampleFolders = BoardsManager.getAllLibraryExamples();
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

			// with the current amount of examples only do one
			for (IBoard curBoard : myBoards) {
				if (curBoard.isExampleOk(inoName, libName)) {
					Object[] theData = new Object[] { inoName.trim(), curBoard.getBoardDescriptor(), codeDescriptor };
					examples.add(theData);
					break;
				}

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
		if (totalFails < 40) {
			BuildAndVerify(this.myBoardid, this.myCodeDescriptor);
		} else {
			fail("To many fails. Stopping test");
		}

	}

	public void BuildAndVerify(BoardDescriptor boardid, CodeDescriptor codeDescriptor) {

		IProject theTestProject = null;

		NullProgressMonitor monitor = new NullProgressMonitor();
		String projectName = String.format("%05d_%s", new Integer(mCounter++), this.myName);
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
