package io.sloeber.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.TreeMap;

import org.eclipse.core.runtime.IPath;
import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import io.sloeber.core.api.CodeDescriptor;
import io.sloeber.core.api.LibraryManager;
import io.sloeber.core.api.PackageManager;
import io.sloeber.providers.Jantje;
import io.sloeber.providers.MCUBoard;

@SuppressWarnings({"nls"})
@RunWith(Parameterized.class)
public class CreateAndCompileJantjesBoardsTest {
	private CodeDescriptor myCodeDescriptor;

	private Examples myExample;
    private static int myBuildCounter = 0;
    private static int myTotalFails = 0;
    private static int maxFails = 200;
    private static int mySkipAtStart = 0;

	public CreateAndCompileJantjesBoardsTest( CodeDescriptor codeDescriptor, Examples example) {

		myCodeDescriptor = codeDescriptor;

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

				Object[] theData = new Object[] { codeDescriptor, example };
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
        Assume.assumeTrue("Skipping first " + mySkipAtStart + " tests", myBuildCounter++ >= mySkipAtStart);
        Assume.assumeTrue("To many fails. Stopping test", myTotalFails < maxFails);
        //because we run all examples on all boards we need to filter incompatible combinations
        //like serial examples on gemma
        if (!board.isExampleSupported(myExample)) {
            return;
        }
        if (!Shared.BuildAndVerify(myBuildCounter, board.getBoardDescriptor(), myCodeDescriptor, null)) {
            myTotalFails++;
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


}
