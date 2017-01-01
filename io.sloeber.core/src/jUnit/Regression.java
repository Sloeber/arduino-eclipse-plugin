package jUnit;

import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.eclipse.cdt.core.model.CoreModel;
import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.cdt.core.settings.model.ICProjectDescription;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.junit.BeforeClass;
import org.junit.Test;

import io.sloeber.core.api.BoardDescriptor;
import io.sloeber.core.api.BoardsManager;
import io.sloeber.core.api.CodeDescriptor;
import io.sloeber.core.api.CompileOptions;
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
	 * Test wether a platform json redirect is handled properly
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

	/**
	 * make sure when switching between a board with variant file and without
	 * the build still succeeds
	 */
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

	/**
	 * This test will fail if the arduino compile option are not taken into
	 * account To do sa a bunch of defines are added to the command line and the
	 * code checks whether these defines are set properly
	 */
	@SuppressWarnings("static-method")
	@Test
	public void are_jantjes_options_taken_into_account() {
		Map<String, String> unoOptions = new HashMap<>();
		BoardDescriptor unoBoardid = BoardsManager.getBoardID("package_index.json", "arduino", "Arduino AVR Boards",
				"uno", unoOptions);

		IProject theTestProject = null;
		String projectName = "are_defines_found";
		IPath templateFolder = Shared.getTemplateFolder(projectName);
		CodeDescriptor codeDescriptor = CodeDescriptor.createCustomTemplate(templateFolder);

		NullProgressMonitor monitor = new NullProgressMonitor();
		try {

			theTestProject = unoBoardid.createProject(projectName, null,
					ConfigurationDescriptor.getDefaultDescriptors(), codeDescriptor, monitor);
			ICProjectDescription prjCDesc = CoreModel.getDefault().getProjectDescription(theTestProject);
			ICConfigurationDescription confDesc = prjCDesc.getActiveConfiguration();

			CompileOptions compileOptions = new CompileOptions(confDesc);
			compileOptions.setMyAditional_C_andCPP_CompileOptions("-DTEST_C_CPP");
			compileOptions.setMyAditional_C_CompileOptions("-DTEST_C");
			compileOptions.setMyAditional_CPP_CompileOptions("-DTEST_CPP");
			compileOptions.save(confDesc);
			prjCDesc.setActiveConfiguration(confDesc);
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
	 */
	@SuppressWarnings("static-method")
	@Test
	public void are_defines_before_includes_taken_into_account() {
		Map<String, String> unoOptions = new HashMap<>();
		BoardDescriptor unoBoardid = BoardsManager.getBoardID("package_index.json", "arduino", "Arduino AVR Boards",
				"uno", unoOptions);

		IProject theTestProject = null;
		String projectName = "externc";
		IPath templateFolder = Shared.getTemplateFolder(projectName);
		CodeDescriptor codeDescriptor = CodeDescriptor.createCustomTemplate(templateFolder);

		NullProgressMonitor monitor = new NullProgressMonitor();
		try {

			theTestProject = unoBoardid.createProject(projectName, null,
					ConfigurationDescriptor.getDefaultDescriptors(), codeDescriptor, monitor);

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
	 */
	@SuppressWarnings("static-method")
	@Test
	public void is_extern_C_taken_into_account() {
		Map<String, String> unoOptions = new HashMap<>();
		BoardDescriptor unoBoardid = BoardsManager.getBoardID("package_index.json", "arduino", "Arduino AVR Boards",
				"uno", unoOptions);

		IProject theTestProject = null;
		String projectName = "defines_and_includes";
		IPath templateFolder = Shared.getTemplateFolder(projectName);
		CodeDescriptor codeDescriptor = CodeDescriptor.createCustomTemplate(templateFolder);

		NullProgressMonitor monitor = new NullProgressMonitor();
		try {

			theTestProject = unoBoardid.createProject(projectName, null,
					ConfigurationDescriptor.getDefaultDescriptors(), codeDescriptor, monitor);

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
