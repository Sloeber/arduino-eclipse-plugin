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
import io.sloeber.providers.Jantje;
import io.sloeber.providers.MCUBoard;

@SuppressWarnings({"nls","unused"})
@RunWith(Parameterized.class)
public class CreateAndCompileJantjesBoardsTest {
	private static int mCounter = 0;
	private CodeDescriptor myCodeDescriptor;
	private String myName;
	private Examples myExample;
	private static int totalFails = 0;
	private static int maxFails = 500;

	public CreateAndCompileJantjesBoardsTest(String name, CodeDescriptor codeDescriptor, Examples example) {

		myCodeDescriptor = codeDescriptor;
		myName = name;
		myExample = example;

	}

	@SuppressWarnings("rawtypes")
	@Parameters(name = "{index}: {0}")
	public static Collection examples() {
		String[] packageUrlsToAdd = {
				"https://raw.githubusercontent.com/jantje/hardware/master/package_jantje_index.json" };
		PackageManager.addPackageURLs(new HashSet<>(Arrays.asList(packageUrlsToAdd)), true);
		PackageManager.installLatestPlatform(Jantje.getJsonFileName(), Jantje.getPackageName(),
				Jantje.getPlatformName());

		Shared.waitForAllJobsToFinish();
		LinkedList<Object[]> examples = new LinkedList<>();

		TreeMap<String, IPath> exampleFolders = LibraryManager.getAllArduinoIDEExamples();
		for (Map.Entry<String, IPath> curexample : exampleFolders.entrySet()) {
			String fqn = curexample.getKey().trim();
			IPath examplePath = curexample.getValue();
			Examples example = new Examples(fqn, null, examplePath);
			if (!skipExample(example)) {
				ArrayList<IPath> paths = new ArrayList<>();

				paths.add(examplePath);
				CodeDescriptor codeDescriptor = CodeDescriptor.createExample(false, paths);

				Object[] theData = new Object[] { "Example:" + fqn, codeDescriptor, example };
				examples.add(theData);
			}
		}

		return examples;

	}

	private static boolean skipExample(Examples example) {
		switch (example.getFQN()) {
		case "example/10.StarterKit/BasicKit/p13_TouchSensorLamp":
			return true;
		case "example/09.USB/KeyboardAndMouseControl":
			return true;
		case "example/09.USB/Mouse/JoystickMouseControl":
			return true;
		case "example/09.USB/Mouse/ButtonMouseControl":
			return true;
		case "example/09.USB/Keyboard/KeyboardSerial":
			return true;
		case "example/09.USB/Keyboard/KeyboardReprogram":
			return true;
		case "example/09.USB/Keyboard/KeyboardMessage":
			return true;
		case "example/09.USB/Keyboard/KeyboardLogout":
			return true;
		}
		return false;
	}

	public void testExample(MCUBoard board) {
		// Stop after X fails because
		// the fails stays open in eclipse and it becomes really slow
		// There are only a number of issues you can handle
		// best is to focus on the first ones and then rerun starting with the
		// failures
		if (!board.isExampleSupported(myExample)) {
			return;
		}

		if (totalFails < maxFails) {
			BuildAndVerify(board.getBoardDescriptor());
		} else {
			fail("To many fails. Stopping test");
		}

	}

	@Test
	public void testJantjeYun() {
		testExample(new Jantje("yun"));
	}

	@Test
	public void testJantjeUno() {
		testExample(new Jantje("uno"));
	}

	@Test
	public void testJantjeDiecimila() {
		testExample(new Jantje("diecimila"));
	}

	@Test
	public void testJantjeNano() {
		testExample(new Jantje("nano"));
	}

	@Test
	public void testJantjeMega() {
		testExample(new Jantje("mega"));
	}

	@Test
	public void testJantjeMegaADK() {
		testExample(new Jantje("megaADK"));
	}

	@Test
	public void testJantjeLeonardo() {
		testExample(new Jantje("leonardo"));
	}

	@Test
	public void testJantjeMicro() {
		testExample(new Jantje("micro"));
	}

	@Test
	public void testJantjeEsplora() {
		testExample(new Jantje("esplora"));
	}

	@Test
	public void testJantjeMini() {
		testExample(new Jantje("mini"));
	}

	@Test
	public void testJantjeEthernet() {
		testExample(new Jantje("ethernet"));
	}

	@Test
	public void testJantje_fio() {
		testExample(new Jantje("fio"));
	}

	@Test
	public void testJantje_bt() {
		testExample(new Jantje("bt"));
	}

	@Test
	public void testJantje_LilyPadUSB() {
		testExample(new Jantje("LilyPadUSB"));
	}

	@Test
	public void testJantje_lilypad() {
		testExample(new Jantje("lilypad"));
	}

	@Test
	public void testJantje_pro() {
		testExample(new Jantje("pro"));
	}

	@Test
	public void testJantje_atmegang() {
		testExample(new Jantje("atmegang"));
	}

	@Test
	public void testJantje_robotControl() {
		testExample(new Jantje("robotControl"));
	}

	public void BuildAndVerify(BoardDescriptor boardDescriptor) {

		IProject theTestProject = null;

		NullProgressMonitor monitor = new NullProgressMonitor();
		String projectName = String.format("%05d_:%s_%s", new Integer(mCounter++), this.myName,
				boardDescriptor.getBoardID());
		try {

			theTestProject = boardDescriptor.createProject(projectName, null,
					ConfigurationDescriptor.getDefaultDescriptors(), this.myCodeDescriptor, new CompileOptions(null),
					monitor);
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
