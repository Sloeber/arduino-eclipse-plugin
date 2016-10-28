package jUnit;

import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.junit.BeforeClass;
import org.junit.Test;

import io.sloeber.core.api.BoardDescriptor;
import io.sloeber.core.api.BoardsManager;
import io.sloeber.core.api.CodeDescriptor;
import io.sloeber.core.api.ConfigurationDescriptor;

@SuppressWarnings("nls")
public class Regression {

	/*
	 * In new new installations (of the Sloeber development environment) the
	 * installer job will trigger downloads These mmust have finished before we
	 * can start testing
	 */
	@BeforeClass
	public static void WaitForInstallerToFinish() {
		Shared.waitForAllJobsToFinish();
		installAdditionalBoards();
		BoardsManager.installAllLatestPlatforms();
	}

	public static void installAdditionalBoards() {
		String[] packageUrlsToAdd = { "http://talk2arduino.wisen.com.au/master/package_talk2.wisen.com_index.json" };
		BoardsManager.addPackageURLs(new HashSet<>(Arrays.asList(packageUrlsToAdd)), true);
	}

	/**
	 * Test wether a json redirect is handled properly
	 * https://github.com/jantje/arduino-eclipse-plugin/issues/393
	 */
	@SuppressWarnings("static-method")
	@Test
	public void redirectedJson() {

		Map<String, String> options = new HashMap<>();
		options.put("mhz", "16MHz");
		BoardDescriptor boardid = BoardsManager.getBoardID("package_talk2.wisen.com_index.json", "Talk2",
				"Talk2 AVR Boards", "whispernode", options);
		if (boardid == null) {
			fail("redirect Json ");
			return;
		}
		CreateAndCompile.BuildAndVerify(boardid);
	}

	@SuppressWarnings("static-method")
	@Test
	public void issue555() {
		Map<String, String> unoOptions = new HashMap<>();
		BoardDescriptor unoBoardid = BoardsManager.getBoardID("package_index.json", "arduino", "Arduino AVR Boards",
				"uno", unoOptions);
		Map<String, String> teensyOptions = new HashMap<>();
		teensyOptions.put("usb", "serial");
		teensyOptions.put("speed", "96");
		teensyOptions.put("keys", "en-us");
		BoardDescriptor teensyBoardid = BoardsManager.getBoardID("local", Shared.teensyBoards_txt, "", "teensy31",
				teensyOptions);
		IProject theTestProject = null;
		CodeDescriptor codeDescriptor = CodeDescriptor.createDefaultIno();
		String projectName = "issue555";
		NullProgressMonitor monitor = new NullProgressMonitor();
		try {

			theTestProject = unoBoardid.createProject(projectName, null,
					ConfigurationDescriptor.getDefaultDescriptors(), codeDescriptor, monitor);
			Shared.waitForAllJobsToFinish(); // for the indexer
		} catch (Exception e) {
			e.printStackTrace();
			fail("Failed to create the project:" + projectName);
			return;
		}
		try {
			theTestProject.build(IncrementalProjectBuilder.FULL_BUILD, monitor);
			if (Shared.hasBuildErrors(theTestProject)) {
				fail("Failed to compile the project:" + projectName + " as teensy build errors");
			}
		} catch (CoreException e) {
			e.printStackTrace();
			fail("Failed to compile the project:" + unoBoardid.getBoardName() + " as uno exception");
		}
		teensyBoardid.configureProject(theTestProject, monitor);
		Shared.waitForAllJobsToFinish();
		try {
			theTestProject.build(IncrementalProjectBuilder.FULL_BUILD, monitor);
			if (Shared.hasBuildErrors(theTestProject)) {
				fail("Failed to compile the project:" + projectName + " as teensy");
			}
		} catch (CoreException e) {
			e.printStackTrace();
			fail("Failed to compile the project:" + unoBoardid.getBoardName() + " as teensy exception");
		}
	}
}
