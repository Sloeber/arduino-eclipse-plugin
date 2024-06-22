package io.sloeber.core;

import static org.junit.Assert.*;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Stream;
import java.net.URI;
import org.junit.Assert;
import org.apache.commons.io.FileUtils;
import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.cdt.core.settings.model.ICProjectDescription;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import io.sloeber.core.api.BoardDescription;
import io.sloeber.core.api.BoardsManager;
import io.sloeber.core.api.CodeDescription;
import io.sloeber.core.api.CompileDescription;
import io.sloeber.core.api.IExample;
import io.sloeber.core.api.CompileDescription.SizeCommands;
import io.sloeber.core.api.CompileDescription.WarningLevels;
import io.sloeber.core.api.ISloeberConfiguration;
import io.sloeber.core.api.LibraryManager;
import io.sloeber.core.api.OtherDescription;
import io.sloeber.core.api.Preferences;
import io.sloeber.core.api.SloeberProject;
import io.sloeber.providers.Arduino;
import io.sloeber.providers.ESP32;
import io.sloeber.providers.ESP8266;
import io.sloeber.providers.MCUBoard;
import io.sloeber.providers.Teensy;

@SuppressWarnings({ "nls", "static-method", "null" })
public class RegressionTest {
	private static final boolean reinstall_boards_and_libraries = false;
	private final static String AUTOBUILD_CFG = ".autoBuildProject";

	/*
	 * In new new installations (of the Sloeber development environment) the
	 * installer job will trigger downloads These must have finished before we can
	 * start testing
	 */
	@BeforeClass
	public static void beforeClass() throws Exception {
		Shared.waitForBoardsManager();
		Shared.setDeleteProjects(false);
		Preferences.setUseBonjour(false);
		installAdditionalBoards();
	}

	public static void installAdditionalBoards() throws Exception {

		String[] packageUrlsToAdd = { ESP8266.packageURL, ESP32.packageURL,
				"http://talk2arduino.wisen.com.au/master/package_talk2.wisen.com_index.json" };
		BoardsManager.addPackageURLs(new HashSet<>(Arrays.asList(packageUrlsToAdd)), false);
		Shared.waitForBoardsManager();
		if (reinstall_boards_and_libraries) {
			BoardsManager.removeAllInstalledPlatforms();
		}
		// make sure the needed boards are available

		ESP8266.installLatest();
		ESP32.installLatest();
		Arduino.installLatestAVRBoards();
		Teensy.installLatest();
		Arduino.installLatestSamDBoards();
		LibraryManager.installLibrary("RTCZero");

	}

	/**
	 * make sure when switching between a board with variant file and without the
	 * build still succeeds
	 *
	 * @throws CoreException
	 */
	@Test
	public void issue555() throws CoreException {
		BoardDescription unoBoardid = Arduino.uno().getBoardDescriptor();
		BoardDescription teensyBoardid = Teensy.Teensy3_1().getBoardDescriptor();

		IProject theTestProject = null;
		CodeDescription codeDescriptor = CodeDescription.createDefaultIno();
		String projectName = "issue555";
		NullProgressMonitor monitor = new NullProgressMonitor();
		theTestProject = SloeberProject.createArduinoProject(projectName, null, unoBoardid, codeDescriptor,
				new CompileDescription(), monitor);

		theTestProject.build(IncrementalProjectBuilder.FULL_BUILD, monitor);
		Assert.assertNull(Shared.hasBuildErrors(theTestProject));

		CCorePlugin cCorePlugin = CCorePlugin.getDefault();
		ICProjectDescription cProjectDescription = cCorePlugin.getProjectDescription(theTestProject);
		ICConfigurationDescription activeCfg = cProjectDescription.getActiveConfiguration();
		ISloeberConfiguration sloeberConf = ISloeberConfiguration.getConfig(activeCfg);
		sloeberConf.setBoardDescription(teensyBoardid);
		cCorePlugin.setProjectDescription(theTestProject, cProjectDescription);

		if (sloeberConf.getArduinoVariantFolder().exists()) {
			fail("variant folder exists");
		}

		theTestProject.build(IncrementalProjectBuilder.FULL_BUILD, monitor);
		if (Shared.hasBuildErrors(theTestProject) != null) {
			theTestProject.build(IncrementalProjectBuilder.FULL_BUILD, monitor);
			Assert.assertNull(Shared.hasBuildErrors(theTestProject));
		}
	}

	/**
	 * support void loop{};
	 *
	 * @throws Exception
	 */
	@Test
	public void issue687() throws Exception {
		BoardDescription unoBoardid = Arduino.uno().getBoardDescriptor();

		IProject theTestProject = null;
		String projectName = "issue687";
		IPath templateFolder = Shared.getTemplateFolder(projectName);
		CodeDescription codeDescriptor = CodeDescription.createCustomTemplate(templateFolder);
		theTestProject = SloeberProject.createArduinoProject(projectName, null, unoBoardid, codeDescriptor,
				new CompileDescription(), new NullProgressMonitor());
		theTestProject.build(IncrementalProjectBuilder.FULL_BUILD, new NullProgressMonitor());
		Assert.assertNull(Shared.hasBuildErrors(theTestProject));
	}

	@Test
	public void create_CPP_based_Sloeber_Project() throws Exception {
		BoardDescription unoBoardid = Arduino.uno().getBoardDescriptor();

		IProject theTestProject = null;
		String projectName = "createCPPProject";

		CodeDescription codeDescriptor = CodeDescription.createDefaultCPP();
		theTestProject = SloeberProject.createArduinoProject(projectName, null, unoBoardid, codeDescriptor,
				new CompileDescription(), new NullProgressMonitor());
		theTestProject.build(IncrementalProjectBuilder.FULL_BUILD, new NullProgressMonitor());
		Assert.assertNull(Shared.hasBuildErrors(theTestProject));

	}

	@Test
	public void createDefaultInoProject() throws Exception {
		BoardDescription unoBoardid = Arduino.uno().getBoardDescriptor();

		IProject theTestProject = null;
		String projectName = "createDefaultInoProject";

		CodeDescription codeDescriptor = CodeDescription.createDefaultIno();
		theTestProject = SloeberProject.createArduinoProject(projectName, null, unoBoardid, codeDescriptor,
				new CompileDescription(), new NullProgressMonitor());
		theTestProject.build(IncrementalProjectBuilder.FULL_BUILD, new NullProgressMonitor());
		Assert.assertNull(Shared.hasBuildErrors(theTestProject));
	}

	/**
	 * support void loop{};
	 *
	 * @throws Exception
	 */
	@Test
	public void issue1047_Board_Names_Can_Be_used_as_Strings() throws Exception {
		MCUBoard unoBoard = ESP8266.nodeMCU();

		String projectName = "issue1047_Board_Names_Can_Be_used_as_Strings";
		IPath templateFolder = Shared.getTemplateFolder(projectName);
		CodeDescription codeDescriptor = CodeDescription.createCustomTemplate(templateFolder);
		IProject theTestProject = SloeberProject.createArduinoProject(projectName, null, unoBoard.getBoardDescriptor(),
				codeDescriptor, new CompileDescription(), new NullProgressMonitor());
		theTestProject.build(IncrementalProjectBuilder.FULL_BUILD, new NullProgressMonitor());
		Assert.assertNull(Shared.hasBuildErrors(theTestProject));

	}

	/**
	 * This test will fail if the arduino compile option are not taken into account
	 * To do sa a bunch of defines are added to the command line and the code checks
	 * whether these defines are set properly
	 *
	 * @throws Exception
	 */
	@Test
	public void are_jantjes_options_taken_into_account() throws Exception {
		BoardDescription unoBoardid = Arduino.uno().getBoardDescriptor();

		IProject theTestProject = null;
		String projectName = "are_defines_found";
		IPath templateFolder = Shared.getTemplateFolder(projectName);
		CodeDescription codeDescriptor = CodeDescription.createCustomTemplate(templateFolder);

		NullProgressMonitor monitor = new NullProgressMonitor();
		CompileDescription compileOptions = new CompileDescription();
		compileOptions.set_C_andCPP_CompileOptions("-DTEST_C_CPP");
		compileOptions.set_C_CompileOptions("-DTEST_C");
		compileOptions.set_CPP_CompileOptions("-DTEST_CPP");
		theTestProject = SloeberProject.createArduinoProject(projectName, null, unoBoardid, codeDescriptor,
				compileOptions, new NullProgressMonitor());

		theTestProject.build(IncrementalProjectBuilder.FULL_BUILD, monitor);
		Assert.assertNull(Shared.hasBuildErrors(theTestProject));
	}

	/**
	 * If a .ino file is including a include using extern C is this handled properly
	 * by the ino to cpp parser
	 *
	 * @throws Exception
	 */
	@Test
	public void is_extern_C_taken_into_account() throws Exception {
		BoardDescription unoBoardid = Arduino.uno().getBoardDescriptor();

		IProject theTestProject = null;
		String projectName = "externc";
		IPath templateFolder = Shared.getTemplateFolder(projectName);
		CodeDescription codeDescriptor = CodeDescription.createCustomTemplate(templateFolder);

		NullProgressMonitor monitor = new NullProgressMonitor();
		theTestProject = SloeberProject.createArduinoProject(projectName, null, unoBoardid, codeDescriptor,
				new CompileDescription(), new NullProgressMonitor());

		theTestProject.build(IncrementalProjectBuilder.FULL_BUILD, monitor);
		Assert.assertNull(Shared.hasBuildErrors(theTestProject));
	}

	/**
	 * If a .ino file is defining defines before including a include this should be
	 * handled properly by the ino to cpp parser
	 *
	 * @throws Exception
	 */
	@Test
	public void are_defines_before_includes_taken_into_account() throws Exception {
		BoardDescription unoBoardid = Arduino.uno().getBoardDescriptor();

		IProject theTestProject = null;
		String projectName = "defines_and_includes";
		IPath templateFolder = Shared.getTemplateFolder(projectName);
		CodeDescription codeDescriptor = CodeDescription.createCustomTemplate(templateFolder);

		NullProgressMonitor monitor = new NullProgressMonitor();
		theTestProject = SloeberProject.createArduinoProject(projectName, null, unoBoardid, codeDescriptor,
				new CompileDescription(), new NullProgressMonitor());

		theTestProject.build(IncrementalProjectBuilder.FULL_BUILD, monitor);
		Assert.assertNull(Shared.hasBuildErrors(theTestProject));

	}

	/**
	 * Does Sloeber still compile after a configuration renamen
	 *
	 * @throws Exception
	 */
	@Test
	public void rename_Configuration() throws Exception {
		BoardDescription unoBoardid = Arduino.uno().getBoardDescriptor();

		IProject theTestProject = null;
		String projectName = "rename_Configuration";

		CodeDescription codeDescriptor = CodeDescription.createDefaultIno();

		NullProgressMonitor monitor = new NullProgressMonitor();
		theTestProject = SloeberProject.createArduinoProject(projectName, null, unoBoardid, codeDescriptor,
				new CompileDescription(), new NullProgressMonitor());

		theTestProject.build(IncrementalProjectBuilder.FULL_BUILD, monitor);
		Assert.assertNull("Failed to compile the project before config rename", Shared.hasBuildErrors(theTestProject));

		CCorePlugin cCorePlugin = CCorePlugin.getDefault();
		ICProjectDescription prjCDesc = cCorePlugin.getProjectDescription(theTestProject);
		ICConfigurationDescription activeConfig = prjCDesc.getActiveConfiguration();
		activeConfig.setName("renamedConfig");
		cCorePlugin.setProjectDescription(theTestProject, prjCDesc);

		theTestProject.build(IncrementalProjectBuilder.FULL_BUILD, monitor);
		Assert.assertNull("Failed to compile the project after config rename", Shared.hasBuildErrors(theTestProject));
	}

	// /**
	// * Does Sloeber still compile after a project rename
	// *
	// * @throws Exception
	// */
	// @Test
	// public void rename_Project() throws Exception {
	// BoardDescription unoBoardid = Arduino.uno().getBoardDescriptor();
	//
	// IProject theTestProject = null;
	// String projectName = "rename_project";
	// String projectNameRenamed = "renamed_project";
	//
	// CodeDescription codeDescriptor = CodeDescription.createDefaultIno();
	//
	// NullProgressMonitor monitor = new NullProgressMonitor();
	// theTestProject = SloeberProject.createArduinoProject(projectName, null,
	// unoBoardid, codeDescriptor,
	// new CompileDescription(), new NullProgressMonitor());
	//
	// Shared.waitForAllJobsToFinish(); // for the indexer
	// theTestProject.build(IncrementalProjectBuilder.FULL_BUILD, monitor);
	// if (Shared.hasBuildErrors(theTestProject)) {
	// fail("Failed to compile the project before project rename");
	// }
	// theTestProject.build(IncrementalProjectBuilder.CLEAN_BUILD, monitor);
	//
	// CCorePlugin cCorePlugin = CCorePlugin.getDefault();
	// ICProjectDescription prjCDesc =
	// cCorePlugin.getProjectDescription(theTestProject);
	//
	// IProjectDescription descr = theTestProject.getDescription();
	// descr.ren(projectNameRenamed);
	// theTestProject.setDescription(descr, null);
	//
	// Shared.waitForAllJobsToFinish(); // for the indexer
	// theTestProject.build(IncrementalProjectBuilder.FULL_BUILD, monitor);
	// if (Shared.hasBuildErrors(theTestProject)) {
	// fail("Failed to compile the project after project rename");
	// }
	// }

	/**
	 * open and close a project should keep the compileDescription and
	 * BoardDescriotion
	 *
	 * @throws Exception
	 */
	@ParameterizedTest
	@MethodSource("openAndClosePreservesSettingsValueCmd")
	public void openAndClosePreservesSettings(String projectName, CodeDescription codeDescriptor) throws Exception {
		BoardDescription unoBoardid = Arduino.uno().getBoardDescriptor();

		IProject theTestProject = null;

		CompileDescription inCompileDescription = getBunkersCompileDescription();

		theTestProject = SloeberProject.createArduinoProject(projectName, null, unoBoardid, codeDescriptor,
				inCompileDescription, new NullProgressMonitor());

		// also do a build
		theTestProject.build(IncrementalProjectBuilder.FULL_BUILD, null);
		Assert.assertNull("Failed to compile the project before close: " + projectName,
				Shared.hasBuildErrors(theTestProject));

		// also do a clean
		theTestProject.build(IncrementalProjectBuilder.CLEAN_BUILD, null);

		// Read the data we want to test
		ISloeberConfiguration sloeberConf = ISloeberConfiguration.getActiveConfig(theTestProject);
		BoardDescription createdBoardDesc = sloeberConf.getBoardDescription();
		CompileDescription createdCompileDesc = sloeberConf.getCompileDescription();

		// close and reopen the project
		theTestProject.close(null);
		// just wait a while
		Thread.sleep(1000);
		theTestProject.open(null);

		// read the data we want to test
		sloeberConf = ISloeberConfiguration.getActiveConfig(theTestProject);
		if (sloeberConf == null) {
			fail("failed to open and close project");
		}
		BoardDescription reopenedBoardDesc = sloeberConf.getBoardDescription();
		CompileDescription reopenedCompileDesc = sloeberConf.getCompileDescription();

		// check the data is equal
		boolean createBoardsDiff = !unoBoardid.equals(createdBoardDesc);
		boolean createCompileDiff = !inCompileDescription.equals(createdCompileDesc);
		boolean openBoardsDiff = !reopenedBoardDesc.equals(createdBoardDesc);
		boolean openCompileDiff = !reopenedCompileDesc.equals(createdCompileDesc);

		if (createBoardsDiff || createCompileDiff) {
			fail("Created project does not match creation parameters.");
		}
		if (openBoardsDiff || openCompileDiff) {
			fail("Opened project does not match closed project parameters.");
		}

		// also do a build
		theTestProject.build(IncrementalProjectBuilder.FULL_BUILD, null);
		Assert.assertNull("Failed to compile the project after open: " + projectName,
				Shared.hasBuildErrors(theTestProject));

	}

	/**
	 * open and close a project should keep the compileDescription and
	 * BoardDescriotion
	 *
	 * @throws Exception
	 */

	@Test
	public void openModAndCloseUsesSavedSettings() throws Exception {
		CodeDescription codeDesc = new CodeDescription(CodeDescription.CodeTypes.defaultCPP);

		String proj1Name = "openModAndClose1";
		BoardDescription proj1BoardDesc = Arduino.uno().getBoardDescriptor();
		OtherDescription otherDesc = new OtherDescription();
		otherDesc.setVersionControlled(true);
		CompileDescription proj1CompileDesc = getBunkersCompileDescription();
		IProject proj1 = SloeberProject.createArduinoProject(proj1Name, null, proj1BoardDesc, codeDesc,
				proj1CompileDesc, otherDesc, new NullProgressMonitor());

		String proj2Name = "openModAndClose2";
		BoardDescription proj2BoardDesc = Arduino.mega2560Board().getBoardDescriptor();
		CompileDescription proj2CompileDesc = new CompileDescription();
		IProject proj2 = SloeberProject.createArduinoProject(proj2Name, null, proj2BoardDesc, codeDesc,
				proj2CompileDesc, new NullProgressMonitor());

		// Read the data we want to test
		ISloeberConfiguration sloeberConf = ISloeberConfiguration.getActiveConfig(proj1);
		BoardDescription proj1CreatedBoardDesc = sloeberConf.getBoardDescription();
		CompileDescription proj1CreatedCompileDesc = sloeberConf.getCompileDescription();

		ISloeberConfiguration sloeberConf2 = ISloeberConfiguration.getActiveConfig(proj2);
		BoardDescription proj2CreatedBoardDesc = sloeberConf2.getBoardDescription();
		CompileDescription proj2CreatedCompileDesc = sloeberConf2.getCompileDescription();

		// get the filenames to copy
		IFile file = proj1.getFile(AUTOBUILD_CFG);
		File proj1SloeberFile = file.getLocation().toFile();

		file = proj2.getFile(AUTOBUILD_CFG);
		File proj2SloeberFile = file.getLocation().toFile();

		// close and reopen the project
		proj2.close(null);
		// just wait a while
		Thread.sleep(1000);

		// copy from proj1 to proj2
		FileUtils.copyFile(proj1SloeberFile, proj2SloeberFile);

		// reopen the project
		proj2.open(null);
		Thread.sleep(1000);

		// reread project 2
		ISloeberConfiguration sloebercfg2 = ISloeberConfiguration.getActiveConfig(proj2);
		if (sloebercfg2 == null) {
			fail("failed to load the sloeber configuration");
		}
		BoardDescription proj2OpenedBoardDesc = sloebercfg2.getBoardDescription();
		CompileDescription proj2OpenedCompileDesc = sloebercfg2.getCompileDescription();

		// check the setup was done correctly
		if (!proj1BoardDesc.equals(proj1CreatedBoardDesc)) {
			fail("Project 1 not created properly.");
		}
		if (!proj2BoardDesc.equals(proj2CreatedBoardDesc)) {
			fail("Project 2 not created properly.");
		}
		if (!proj1CompileDesc.equals(proj1CreatedCompileDesc)) {
			fail("Project 1 not created properly.");
		}
		if (!proj2CompileDesc.equals(proj2CreatedCompileDesc)) {
			fail("Project 2 not created properly.");
		}

		// check wether the file modification was taken into account
		if (!proj1BoardDesc.equals(proj2OpenedBoardDesc)) {
			fail("Project 2 not created properly.");
		}
		if (!proj1CompileDesc.equals(proj2OpenedCompileDesc)) {
			fail("Project 2 not created properly.");
		}

	}

	@Test
	public void createProjectWithURI() throws Exception {
		CodeDescription codeDesc = new CodeDescription(CodeDescription.CodeTypes.defaultCPP);

		String proj1Name = "projectWithURI";
		String codeFolderName = "locationWithURI";
		BoardDescription proj1BoardDesc = Arduino.uno().getBoardDescriptor();
		OtherDescription otherDesc = new OtherDescription();
		CompileDescription proj1CompileDesc = getBunkersCompileDescription();
		final IWorkspace workspace = ResourcesPlugin.getWorkspace();
		IPath projectFolder = workspace.getRoot().getLocation().removeLastSegments(1).append(codeFolderName);
		URI uri = projectFolder.toFile().toURI();
		// workspace.getRoot().getFolder(Path.fromOSString(codeFolderName)).getLocationURI();
		IProject proj = SloeberProject.createArduinoProject(proj1Name, uri, proj1BoardDesc, codeDesc, proj1CompileDesc,
				otherDesc, new NullProgressMonitor());

		proj.build(IncrementalProjectBuilder.FULL_BUILD, null);
		Assert.assertNull("Failed to compile the project: " + Shared.hasBuildErrors(proj), Shared.hasBuildErrors(proj));
		String fileLocation = projectFolder.append("src").append(proj1Name + ".cpp").toString();

		IFile cppFile = proj.getFolder("src").getFile(proj1Name + ".cpp");
		Assert.assertTrue("File not in correct location", cppFile.exists());
		Assert.assertEquals("File not in correct location", cppFile.getLocation().toString(), fileLocation);

	}

	static CompileDescription getBunkersCompileDescription() {
		CompileDescription inCompileDescription = new CompileDescription();

		inCompileDescription.set_All_CompileOptions("-Deen=1");
		inCompileDescription.set_Archive_CompileOptions("-Dtwee=2");
		inCompileDescription.set_Assembly_CompileOptions("-Drie=3");
		inCompileDescription.set_C_andCPP_CompileOptions("-Dvier=4");
		inCompileDescription.set_C_CompileOptions("-Dvijf=5");
		inCompileDescription.set_Link_CompileOptions("-Dzes=6");
		inCompileDescription.set_CPP_CompileOptions("-Dzeven=7");
		inCompileDescription.setSizeCommand(SizeCommands.ARDUINO_WAY);
		inCompileDescription.setEnableParallelBuild(true);
		inCompileDescription.setWarningLevel(WarningLevels.NONE);
		return inCompileDescription;
	}

	/**
	 * check to see whether upload recipe key is correct for a couple of boards that
	 * have failed in the past
	 *
	 * @throws Exception
	 */
	@Test
	public void uploadPattern() throws Exception {
		BoardDescription boardDescriptor = Arduino.uno().getBoardDescriptor();
		String recipeKey = boardDescriptor.getUploadPatternKey();
		assertEquals("uno upload recipe key is wrong", "tools.avrdude.upload.pattern", recipeKey);
		boardDescriptor = ESP32.esp32().getBoardDescriptor();
		boardDescriptor.setUploadPort("host 10.10.10.10");
		recipeKey = boardDescriptor.getUploadPatternKey();
		assertEquals("ESP OTA upload recipe key is wrong", "tools.esptool_py.upload.network_pattern", recipeKey);

	}

	public static Stream<Arguments> testDifferentSourceFoldersData() throws Exception {
		beforeClass();// This is not always called
		List<Arguments> ret = new LinkedList<>();
		OtherDescription otherDesc = new OtherDescription();
		CompileDescription projCompileDesc = new CompileDescription();

		MCUBoard unoboard = Arduino.uno();
		CodeDescription codeDescriptor1 = CodeDescription.createDefaultCPP();
		codeDescriptor1.setCodeFolder(null);
		String projectName1 = Shared.getProjectName(codeDescriptor1, unoboard);
		ret.add(Arguments.of(projectName1, codeDescriptor1, unoboard, otherDesc, projCompileDesc));

		CodeDescription codeDescriptor2 = CodeDescription.createDefaultCPP();
		codeDescriptor2.setCodeFolder("src");
		String projectName2 = Shared.getProjectName(codeDescriptor2, unoboard);
		ret.add(Arguments.of(projectName2, codeDescriptor2, unoboard, otherDesc, projCompileDesc));

		CodeDescription codeDescriptor3 = CodeDescription.createDefaultCPP();
		codeDescriptor3.setCodeFolder("SRC");
		String projectName3 = Shared.getProjectName(codeDescriptor3, unoboard);
		ret.add(Arguments.of(projectName3, codeDescriptor3, unoboard, otherDesc, projCompileDesc));

		CodeDescription codeDescriptor4 = CodeDescription.createDefaultCPP();
		codeDescriptor4.setCodeFolder("blabla");
		String projectName4 = Shared.getProjectName(codeDescriptor4, unoboard);
		ret.add(Arguments.of(projectName4, codeDescriptor4, unoboard, otherDesc, projCompileDesc));

		return ret.stream();

	}

	@ParameterizedTest
	@MethodSource("testDifferentSourceFoldersData")
	public void testDifferentSourceFolders(String projectName, CodeDescription codeDescriptor, MCUBoard board,
			OtherDescription otherDesc, CompileDescription proj1CompileDesc) throws CoreException {

		BoardDescription proj1BoardDesc = board.getBoardDescriptor();
		IProject project = SloeberProject.createArduinoProject(projectName, null, proj1BoardDesc, codeDescriptor,
				proj1CompileDesc, otherDesc, new NullProgressMonitor());

		project.build(IncrementalProjectBuilder.FULL_BUILD, null);
		assertNull("Failed to compile " + projectName, Shared.hasBuildErrors(project));

		String srcFolder = codeDescriptor.getCodeFolder();
		IFile cppFile = null;
		if (srcFolder == null) {
			cppFile = project.getFile(projectName + ".cpp");
		} else {
			cppFile = project.getFolder(srcFolder).getFile(projectName + ".cpp");
		}
		assertTrue("Source File not in right location " + projectName, cppFile.exists());
	}

	/**
	 * Test wether a platform json redirect is handled properly
	 * https://github.com/jantje/arduino-eclipse-plugin/issues/393
	 *
	 * @throws Exception
	 */
	@Test
	public void redirectedJson() throws Exception {
		// this board references to arduino avr so install that one to
		Arduino.installLatestAVRBoards();
		BoardsManager.installLatestPlatform("package_talk2.wisen.com_index.json", "Talk2", "avr");
		Map<String, String> options = new HashMap<>();
		options.put("mhz", "16MHz");
		BoardDescription boardid = BoardsManager.getBoardDescription("package_talk2.wisen.com_index.json", "Talk2",
				"avr", "whispernode", options);
		if (boardid == null) {
			fail("redirect Json ");
			return;
		}
		Assert.assertNull(Shared.buildAndVerify("redirect_json", boardid, CodeDescription.createDefaultIno(),
				new CompileDescription()));
	}

	static Stream<Arguments> openAndClosePreservesSettingsValueCmd() throws Exception {
		beforeClass();// This is not always called
		CodeDescription codeDescriptordefaultCPPRoot = new CodeDescription(CodeDescription.CodeTypes.defaultCPP);
		CodeDescription codeDescriptordefaultCPPSrc = new CodeDescription(CodeDescription.CodeTypes.defaultCPP);
		CodeDescription codeDescriptordefaultCPXX = new CodeDescription(CodeDescription.CodeTypes.defaultCPP);

		codeDescriptordefaultCPPRoot.setCodeFolder(null);
		codeDescriptordefaultCPPSrc.setCodeFolder("src");
		codeDescriptordefaultCPXX.setCodeFolder("XX");

		List<Arguments> ret = new LinkedList<>();
		ret.add(Arguments.of(Shared.getCounterName("openAndCloseRoot"), codeDescriptordefaultCPPRoot));
		ret.add(Arguments.of(Shared.getCounterName("openAndCloseSrc"), codeDescriptordefaultCPPSrc));
		ret.add(Arguments.of(Shared.getCounterName("openAndCloseXX"), codeDescriptordefaultCPXX));

		return ret.stream();
	}

	public static Stream<Arguments> NightlyBoardPatronTestData() throws Exception {
		beforeClass();// This is not always called
		Preferences.setUseArduinoToolSelection(true);
		CompileDescription compileOptions = new CompileDescription();
		MCUBoard zeroBoard = Arduino.zeroProgrammingPort();

		List<Arguments> ret = new LinkedList<>();
		TreeMap<String, IExample> examples = LibraryManager.getExamplesLibrary(null);
		for (Map.Entry<String, IExample> curexample : examples.entrySet()) {
			String fqn = curexample.getKey().trim();
			IExample example = curexample.getValue();
			IPath examplePath = example.getCodeLocation();
			if (fqn.contains("RTCZero")) {
				Example SloeberExample = new Example(fqn, examplePath);

				ret.add(Arguments.of(Shared.getCounterName(SloeberExample.getLibName() + ":" + example.getName()),
						zeroBoard, SloeberExample, compileOptions));
			}
		}
		return ret.stream();
	}

	@ParameterizedTest
	@MethodSource("NightlyBoardPatronTestData")
	public void NightlyBoardPatron(String name, MCUBoard boardID, Example example, CompileDescription compileOptions)
			throws Exception {

		Set<IExample> examples = new HashSet<>();
		examples.add(example);
		CodeDescription codeDescriptor = CodeDescription.createExample(false, examples);

		BoardDescription boardDescriptor = boardID.getBoardDescriptor();
		boardDescriptor.setOptions(boardID.getBoardOptions(example));
		Assert.assertNull(
				Shared.buildAndVerifyAllBuilders(name, boardID.getBoardDescriptor(), codeDescriptor, compileOptions));

	}

}
