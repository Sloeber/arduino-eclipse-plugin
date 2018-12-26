package io.sloeber.core;

import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.eclipse.cdt.core.model.CoreModel;
import org.eclipse.cdt.core.settings.model.ICProjectDescription;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.junit.BeforeClass;
import org.junit.Test;

import io.sloeber.core.api.BoardDescriptor;
import io.sloeber.core.api.CodeDescriptor;
import io.sloeber.core.api.CompileOptions;
import io.sloeber.core.api.ConfigurationDescriptor;
import io.sloeber.core.api.PackageManager;
import io.sloeber.core.api.Preferences;
import io.sloeber.providers.Arduino;
import io.sloeber.providers.ESP8266;
import io.sloeber.providers.MCUBoard;

@SuppressWarnings("nls")
public class RegressionTest {
	private static final boolean reinstall_boards_and_libraries = false;

	/*
	 * In new new installations (of the Sloeber development environment) the
	 * installer job will trigger downloads These mmust have finished before we
	 * can start testing
	 */
	@BeforeClass
	public static void WaitForInstallerToFinish() {
		Shared.waitForAllJobsToFinish();
		Preferences.setUseBonjour(false);
		installAdditionalBoards();
	}

	public static void installAdditionalBoards() {

		String[] packageUrlsToAdd = { ESP8266.packageURL };
		PackageManager.addPackageURLs(new HashSet<>(Arrays.asList(packageUrlsToAdd)), true);
		if (reinstall_boards_and_libraries) {
			PackageManager.removeAllInstalledPlatforms();
			PackageManager.installAllLatestPlatforms();
		} else {
			// make sure esp8266 boards are available
			ESP8266.installLatest();
		}
		if (!MySystem.getTeensyPlatform().isEmpty()) {
			PackageManager.addPrivateHardwarePath(MySystem.getTeensyPlatform());
		}
	}


	/**
	 * make sure when switching between a board with variant file and without
	 * the build still succeeds
	 */
	@SuppressWarnings("static-method")
	@Test
	public void issue555() {
		if (MySystem.getTeensyPlatform().isEmpty()) {
			//skip test due to no teensy install folder provided
			//do not fail as this will always fail on travis
			return;
		}
		Map<String, String> unoOptions = new HashMap<>();
		BoardDescriptor unoBoardid = PackageManager.getBoardDescriptor("package_index.json", "arduino", "Arduino AVR Boards",
				"uno", unoOptions);
		Map<String, String> teensyOptions = new HashMap<>();
		teensyOptions.put("usb", "serial");
		teensyOptions.put("speed", "96");
		teensyOptions.put("keys", "en-us");
		BoardDescriptor teensyBoardid = PackageManager.getBoardDescriptor("local", MySystem.getTeensyBoard_txt(), "", "teensy31",
				teensyOptions);
		IProject theTestProject = null;
		CodeDescriptor codeDescriptor = CodeDescriptor.createDefaultIno();
		String projectName = "issue555";
		NullProgressMonitor monitor = new NullProgressMonitor();
		try {

			theTestProject = unoBoardid.createProject(projectName, null,
					ConfigurationDescriptor.getDefaultDescriptors(), codeDescriptor, new CompileOptions(null), monitor);
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

	/**
	 * support void loop{};
	 * @throws Exception
	 */
	@SuppressWarnings("static-method")
	@Test
	public void issue687() throws Exception {
	    Arduino.installLatestAVRBoards();
		Map<String, String> unoOptions = new HashMap<>();
		BoardDescriptor unoBoardid = PackageManager.getBoardDescriptor("package_index.json", "arduino", "Arduino AVR Boards",
				"uno", unoOptions);

		IProject theTestProject = null;
		String projectName = "issue687";
		IPath templateFolder = Shared.getTemplateFolder(projectName);
		CodeDescriptor codeDescriptor = CodeDescriptor.createCustomTemplate(templateFolder);
		try {
			theTestProject = unoBoardid.createProject(projectName, null,
					ConfigurationDescriptor.getDefaultDescriptors(), codeDescriptor, new CompileOptions(null),
					new NullProgressMonitor());
			Shared.waitForAllJobsToFinish(); // for the indexer
			theTestProject.build(IncrementalProjectBuilder.FULL_BUILD, new NullProgressMonitor());
			if (Shared.hasBuildErrors(theTestProject)) {
				fail("Failed to compile the project:" + projectName + " issue687 is not fixed");
			}
		} catch (Exception e) {
			e.printStackTrace();
			fail("Failed to create the project:" + projectName);
			return;
		}

	}

	
	/**
	 * support void loop{};
	 * @throws Exception
	 */
	@SuppressWarnings("static-method")
	@Test
	public void issue1047_Board_Names_Can_Be_used_as_Strings() throws Exception {
		MCUBoard unoBoard = ESP8266.nodeMCU();
	
		String projectName = "issue1047_Board_Names_Can_Be_used_as_Strings";
		IPath templateFolder = Shared.getTemplateFolder(projectName);
		CodeDescriptor codeDescriptor = CodeDescriptor.createCustomTemplate(templateFolder);
		try {
			IProject theTestProject = unoBoard.getBoardDescriptor().createProject(projectName, null,
					ConfigurationDescriptor.getDefaultDescriptors(), codeDescriptor, new CompileOptions(null),
					new NullProgressMonitor());
			Shared.waitForAllJobsToFinish(); // for the indexer
			theTestProject.build(IncrementalProjectBuilder.FULL_BUILD, new NullProgressMonitor());
			if (Shared.hasBuildErrors(theTestProject)) {
				fail("Failed to compile the project:" + projectName + " issue1047 is not fixed");
			}
		} catch (Exception e) {
			e.printStackTrace();
			fail("Failed to create the project:" + projectName);
			return;
		}

	}

	
	
	
	/**
	 * This test will fail if the arduino compile option are not taken into
	 * account To do sa a bunch of defines are added to the command line and the
	 * code checks whether these defines are set properly
	 * @throws Exception
	 */
	@SuppressWarnings("static-method")
	@Test
	public void are_jantjes_options_taken_into_account() throws Exception {
	    Arduino.installLatestAVRBoards();
		Map<String, String> unoOptions = new HashMap<>();
		BoardDescriptor unoBoardid = PackageManager.getBoardDescriptor("package_index.json", "arduino", "Arduino AVR Boards",
				"uno", unoOptions);

		IProject theTestProject = null;
		String projectName = "are_defines_found";
		IPath templateFolder = Shared.getTemplateFolder(projectName);
		CodeDescriptor codeDescriptor = CodeDescriptor.createCustomTemplate(templateFolder);

		NullProgressMonitor monitor = new NullProgressMonitor();
		try {
			CompileOptions compileOptions = new CompileOptions(null);
			compileOptions.set_C_andCPP_CompileOptions("-DTEST_C_CPP");
			compileOptions.set_C_CompileOptions("-DTEST_C");
			compileOptions.set_CPP_CompileOptions("-DTEST_CPP");
			theTestProject = unoBoardid.createProject(projectName, null,
					ConfigurationDescriptor.getDefaultDescriptors(), codeDescriptor, compileOptions, monitor);
			ICProjectDescription prjCDesc = CoreModel.getDefault().getProjectDescription(theTestProject);

			CoreModel.getDefault().getProjectDescriptionManager().setProjectDescription(theTestProject, prjCDesc, true,
					null);
			Shared.waitForAllJobsToFinish(); // for the indexer
			theTestProject.build(IncrementalProjectBuilder.FULL_BUILD, monitor);
			if (Shared.hasBuildErrors(theTestProject)) {
				fail("Failed to compile the project:" + projectName
						+ " The defines have not been taken into account properly");
			}
		} catch (Exception e) {
			e.printStackTrace();
			fail("Failed to create the project:" + projectName);
			return;
		}
	}

	/**
	 * If a .ino file is including a include using extern C is this handled
	 * properly by the ino to cpp parser
	 * @throws Exception
	 */
	@SuppressWarnings("static-method")
	@Test
	public void are_defines_before_includes_taken_into_account() throws Exception {
	    Arduino.installLatestAVRBoards();
		Map<String, String> unoOptions = new HashMap<>();
		BoardDescriptor unoBoardid = PackageManager.getBoardDescriptor("package_index.json", "arduino", "Arduino AVR Boards",
				"uno", unoOptions);

		IProject theTestProject = null;
		String projectName = "externc";
		IPath templateFolder = Shared.getTemplateFolder(projectName);
		CodeDescriptor codeDescriptor = CodeDescriptor.createCustomTemplate(templateFolder);

		NullProgressMonitor monitor = new NullProgressMonitor();
		try {

			theTestProject = unoBoardid.createProject(projectName, null,
					ConfigurationDescriptor.getDefaultDescriptors(), codeDescriptor, new CompileOptions(null), monitor);

			Shared.waitForAllJobsToFinish(); // for the indexer
			theTestProject.build(IncrementalProjectBuilder.FULL_BUILD, monitor);
			if (Shared.hasBuildErrors(theTestProject)) {
				fail("Failed to compile the project:" + projectName
						+ " extern \"C\" has not been taken into account properly.");
			}
		} catch (Exception e) {
			e.printStackTrace();
			fail("Failed to create the project:" + projectName);
			return;
		}
	}

	/**
	 * If a .ino file is defining defines before including a include this should
	 * be handled properly by the ino to cpp parser
	 * @throws Exception
	 */
	@SuppressWarnings("static-method")
	@Test
	public void is_extern_C_taken_into_account() throws Exception {
	    Arduino.installLatestAVRBoards();
		Map<String, String> unoOptions = new HashMap<>();
		BoardDescriptor unoBoardid = PackageManager.getBoardDescriptor("package_index.json", "arduino", "Arduino AVR Boards",
				"uno", unoOptions);

		IProject theTestProject = null;
		String projectName = "defines_and_includes";
		IPath templateFolder = Shared.getTemplateFolder(projectName);
		CodeDescriptor codeDescriptor = CodeDescriptor.createCustomTemplate(templateFolder);

		NullProgressMonitor monitor = new NullProgressMonitor();
		try {

			theTestProject = unoBoardid.createProject(projectName, null,
					ConfigurationDescriptor.getDefaultDescriptors(), codeDescriptor, new CompileOptions(null), monitor);

			Shared.waitForAllJobsToFinish(); // for the indexer
			theTestProject.build(IncrementalProjectBuilder.FULL_BUILD, monitor);
			if (Shared.hasBuildErrors(theTestProject)) {
				fail("Failed to compile the project:" + projectName
						+ " defines have not been taken into account properly.");
			}
		} catch (Exception e) {
			e.printStackTrace();
			fail("Failed to create the project:" + projectName);
			return;
		}
	}
}
