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
import io.sloeber.providers.Adafruit;
import io.sloeber.providers.Arduino;
import io.sloeber.providers.ESP8266;
import io.sloeber.providers.MCUBoard;

@SuppressWarnings({"nls","unused"})
@RunWith(Parameterized.class)
public class CreateAndCompileExamplesTest {
	private static final boolean reinstall_boards_and_examples = true;
	private static int mCounter = 0;
	private CodeDescriptor myCodeDescriptor;
	private BoardDescriptor myBoardid;
	private static int totalFails = 0;
	private static int maxFails = 40;
	private String myName;

	public CreateAndCompileExamplesTest(String name, BoardDescriptor boardid, CodeDescriptor codeDescriptor) {
		this.myBoardid = boardid;
		this.myCodeDescriptor = codeDescriptor;
		this.myName = name;
	}

	@SuppressWarnings("rawtypes")
	@Parameters(name = "{index}: {0}")
	public static Collection examples() {
		WaitForInstallerToFinish();

		MCUBoard myBoards[] = { Arduino.leonardo(),
				Arduino.uno(),
				Arduino.esplora(),
				Adafruit.feather(),
				Arduino.adafruitnCirquitPlayground(),
				ESP8266.nodeMCU(),
				Arduino.primo(),
				Arduino.getMega2560Board(),
				Arduino.gemma(),
				Arduino.zero(),
				Arduino.mkrfox1200(),
				Arduino.due() };

		LinkedList<Object[]> examples = new LinkedList<>();
		TreeMap<String, IPath> exampleFolders = LibraryManager.getAllLibraryExamples();
		for (Map.Entry<String, IPath> curexample : exampleFolders.entrySet()) {
			ArrayList<IPath> paths = new ArrayList<>();

			paths.add(new Path(curexample.getValue().toString()));
			CodeDescriptor codeDescriptor = CodeDescriptor.createExample(false, paths);

			String fqn = curexample.getKey();
			String libName = "";
			if (examples.size() == 82) {// use this for debugging based on the
										// project number
				// use this to put breakpoint
				int a = 0;
				a = a + 1;
			}
			try {
				libName = fqn.split(" ")[0].trim();
			} catch (Exception e) {
				// ignore error
			}
			Examples example=new Examples(fqn,libName,curexample.getValue());
			// with the current amount of examples only do one
			BoardDescriptor curBoard =Examples.pickBestBoard(example,myBoards).getBoardDescriptor();
				if (curBoard!=null) {
					Object[] theData = new Object[] { fqn.trim(), curBoard, codeDescriptor };
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
		String[] packageUrlsToAdd = { Shared.ESP8266_BOARDS_URL, Shared.ADAFRUIT_BOARDS_URL };
		PackageManager.addPackageURLs(new HashSet<>(Arrays.asList(packageUrlsToAdd)), true);
		if (reinstall_boards_and_examples) {
			PackageManager.installAllLatestPlatforms();
			PackageManager.onlyKeepLatestPlatforms();
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
		if (totalFails < maxFails) {
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
			try {
				theTestProject.close(null);
			} catch ( CoreException e1) {
				//fully ignore
			}
			fail("Failed to compile the project:" + projectName + " exception");
		}
	}

}
