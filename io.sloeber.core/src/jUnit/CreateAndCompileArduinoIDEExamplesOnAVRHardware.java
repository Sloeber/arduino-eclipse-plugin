package jUnit;

import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Collection;
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
import jUnit.boards.AdafruitnCirquitPlaygroundBoard;
import jUnit.boards.EsploraBoard;
import jUnit.boards.GenericArduinoAvrBoard;
import jUnit.boards.IBoard;
import jUnit.boards.MegaADKBoard;
import jUnit.boards.UnoBoard;
import jUnit.boards.YunBoard;
import jUnit.boards.leonardoBoard;
import jUnit.boards.megaBoard;

@SuppressWarnings("nls")
@RunWith(Parameterized.class)
public class CreateAndCompileArduinoIDEExamplesOnAVRHardware {
	private static int mCounter = 0;
	private CodeDescriptor myCodeDescriptor;
	private String myName;
	private boolean myUsesSerial1;
	private boolean myUsesKeyboard;
	private boolean myUsesSerial;
	private static int totalFails = 0;

	public CreateAndCompileArduinoIDEExamplesOnAVRHardware(String name, CodeDescriptor codeDescriptor,
			boolean usesSerial, boolean usesSerial1, boolean usesKeyboard) {

		this.myCodeDescriptor = codeDescriptor;
		this.myName = name;
		myUsesSerial = usesSerial;
		myUsesSerial1 = usesSerial1;
		myUsesKeyboard = usesKeyboard;

	}

	@SuppressWarnings("rawtypes")
	@Parameters(name = "{index}: {0}")
	public static Collection examples() {
		LinkedList<String> usesSerialExampleList = new LinkedList<String>();
		LinkedList<String> usesSerial1ExampleList = new LinkedList<String>();
		LinkedList<String> usesKeyboardExampleList = new LinkedList<String>();
		usesSerial1ExampleList.add("examples? 04.Communication?MultiSerial");
		usesKeyboardExampleList.add("examples? 09.USB? Keyboard?KeyboardLogout");
		usesKeyboardExampleList.add("examples? 09.USB? Keyboard?KeyboardMessage");
		usesKeyboardExampleList.add("examples? 09.USB? Keyboard?KeyboardReprogram");
		usesKeyboardExampleList.add("examples? 09.USB? Keyboard?KeyboardSerial");
		usesKeyboardExampleList.add("examples? 09.USB? Mouse?ButtonMouseControl");
		usesKeyboardExampleList.add("examples? 09.USB? Mouse?JoystickMouseControl");
		usesKeyboardExampleList.add("examples? 09.USB?KeyboardAndMouseControl");
		usesSerialExampleList.add("examples? 01.Basics?AnalogReadSerial");
		usesSerialExampleList.add("examples? 01.Basics?DigitalReadSerial");
		usesSerialExampleList.add("examples? 01.Basics?ReadAnalogVoltage");
		usesSerialExampleList.add("examples? 02.Digital?DigitalInputPullup");
		usesSerialExampleList.add("examples? 02.Digital?StateChangeDetection");
		usesSerialExampleList.add("examples? 02.Digital?tonePitchFollower");
		usesSerialExampleList.add("examples? 03.Analog?AnalogInOutSerial");
		usesSerialExampleList.add("examples? 03.Analog?Smoothing");
		usesSerialExampleList.add("examples? 04.Communication?ASCIITable");
		usesSerialExampleList.add("examples? 04.Communication?Dimmer");
		usesSerialExampleList.add("examples? 04.Communication?Graph");
		usesSerialExampleList.add("examples? 04.Communication?Midi");
		usesSerialExampleList.add("examples? 04.Communication?PhysicalPixel");
		usesSerialExampleList.add("examples? 04.Communication?ReadASCIIString");
		usesSerialExampleList.add("examples? 04.Communication?SerialCallResponse");
		usesSerialExampleList.add("examples? 04.Communication?SerialCallResponseASCII");
		usesSerialExampleList.add("examples? 04.Communication?SerialEvent");
		usesSerialExampleList.add("examples? 04.Communication?VirtualColorMixer");
		usesSerialExampleList.add("examples? 05.Control?IfStatementConditional");
		usesSerialExampleList.add("examples? 05.Control?switchCase");
		usesSerialExampleList.add("examples? 05.Control?switchCase2");
		usesSerialExampleList.add("examples? 06.Sensors?ADXL3xx");
		usesSerialExampleList.add("examples? 06.Sensors?Knock");
		usesSerialExampleList.add("examples? 06.Sensors?Memsic2125");
		usesSerialExampleList.add("examples? 06.Sensors?Ping");
		usesSerialExampleList.add("examples? 08.Strings?CharacterAnalysis");
		usesSerialExampleList.add("examples? 08.Strings?StringAdditionOperator");
		usesSerialExampleList.add("examples? 08.Strings?StringAppendOperator");
		usesSerialExampleList.add("examples? 08.Strings?StringCaseChanges");
		usesSerialExampleList.add("examples? 08.Strings?StringCharacters");
		usesSerialExampleList.add("examples? 08.Strings?StringComparisonOperators");
		usesSerialExampleList.add("examples? 08.Strings?StringConstructors");
		usesSerialExampleList.add("examples? 08.Strings?StringIndexOf");
		usesSerialExampleList.add("examples? 08.Strings?StringLength");
		usesSerialExampleList.add("examples? 08.Strings?StringLengthTrim");
		usesSerialExampleList.add("examples? 08.Strings?StringReplace");
		usesSerialExampleList.add("examples? 08.Strings?StringStartsWithEndsWith");
		usesSerialExampleList.add("examples? 08.Strings?StringSubstring");
		usesSerialExampleList.add("examples? 08.Strings?StringToInt");
		usesSerialExampleList.add("examples? 10.StarterKit_BasicKit?p03_LoveOMeter");
		usesSerialExampleList.add("examples? 10.StarterKit_BasicKit?p04_ColorMixingLamp");
		usesSerialExampleList.add("examples? 10.StarterKit_BasicKit?p05_ServoMoodIndicator");
		usesSerialExampleList.add("examples? 10.StarterKit_BasicKit?p07_Keyboard");
		usesSerialExampleList.add("examples? 10.StarterKit_BasicKit?p12_KnockLock");
		usesSerialExampleList.add("examples? 10.StarterKit_BasicKit?p13_TouchSensorLamp");
		usesSerialExampleList.add("examples? 10.StarterKit_BasicKit?p14_TweakTheArduinoLogo");
		usesSerialExampleList.add("examples? 11.ArduinoISP?ArduinoISP");

		Shared.waitForAllJobsToFinish();

		LinkedList<Object[]> examples = new LinkedList<>();
		TreeMap<String, IPath> exampleFolders = BoardsManager.getAllArduinoIDEExamples();
		for (Map.Entry<String, IPath> curexample : exampleFolders.entrySet()) {
			ArrayList<Path> paths = new ArrayList<>();

			paths.add(new Path(curexample.getValue().toString()));
			CodeDescriptor codeDescriptor = CodeDescriptor.createExample(false, paths);
			String inoName = curexample.getKey().trim();
			boolean usesSerial = usesSerialExampleList.contains(inoName);
			boolean usesSerial1 = usesSerial1ExampleList.contains(inoName);
			boolean usesKeyboard = usesKeyboardExampleList.contains(inoName);

			Object[] theData = new Object[] { "Example:" + inoName, codeDescriptor, usesSerial, usesSerial1,
					usesKeyboard };
			examples.add(theData);
		}

		return examples;

	}

	public void testExample(IBoard board) {
		// Stop after X fails because
		// the fails stays open in eclipse and it becomes really slow
		// There are only a number of issues you can handle
		// best is to focus on the first ones and then rerun starting with the
		// failures
		if (myUsesSerial && !board.supportsSerial()) {
			System.out.println("!TEST SKIPPED due to Serial " + myName + " " + board.getName());
			return;
		}
		if (myUsesSerial1 && !board.supportsSerial1()) {
			System.out.println("!TEST SKIPPED due to Serial1 " + myName + " " + board.getName());
			return;
		}
		if (myUsesKeyboard && !board.supportsKeyboard()) {
			System.out.println("!TEST SKIPPED due to keyboard " + myName + " " + board.getName());
			return;
		}
		if (totalFails < 40) {
			BuildAndVerify(board.getBoardDescriptor());
		} else {
			fail("To many fails. Stopping test");
		}

	}

	@Test
	public void testArduinoIDEExamplesOnUno() {
		testExample(new UnoBoard());
	}

	@Test
	public void testArduinoIDEExamplesOnLeonardo() {
		testExample(new leonardoBoard());
	}

	@Test
	public void testArduinoIDEExamplesOnEsplora() {
		testExample(new EsploraBoard());
	}

	@Test
	public void testArduinoIDEExamplesOnYun() {
		testExample(new YunBoard());
	}

	@Test
	public void testArduinoIDEExamplesOnDiecimila() {
		testExample(new GenericArduinoAvrBoard("diecimila"));
	}

	@Test
	public void testArduinoIDEExamplesOnMega() {
		testExample(new megaBoard());
	}

	@Test
	public void testArduinoIDEExamplesOneMegaADK() {
		testExample(new MegaADKBoard());

	}

	@Test
	public void testArduinoIDEExamplesOnLeonardoEth() {

		testExample(new GenericArduinoAvrBoard("leonardoeth"));

	}

	@Test
	public void testArduinoIDEExamplesOneMicro() {

		testExample(new GenericArduinoAvrBoard("micro"));

	}

	@Test
	public void testArduinoIDEExamplesOneMini() {
		testExample(new GenericArduinoAvrBoard("mini"));

	}

	@Test
	public void testArduinoIDEExamplesOnEthernet() {
		testExample(new GenericArduinoAvrBoard("ethernet"));
	}

	@Test
	public void testArduinoIDEExamplesOnFio() {
		testExample(new GenericArduinoAvrBoard("fio"));
	}

	@Test
	public void testArduinoIDEExamplesOnBt() {
		testExample(new GenericArduinoAvrBoard("bt"));
	}

	@Test
	public void testArduinoIDEExamplesOnLilyPadUSB() {
		testExample(new GenericArduinoAvrBoard("LilyPadUSB"));
	}

	@Test
	public void testArduinoIDEExamplesOnlilypad() {
		testExample(new GenericArduinoAvrBoard("lilypad"));
	}

	@Test
	public void testArduinoIDEExamplesOnPro() {
		testExample(new GenericArduinoAvrBoard("pro"));
	}

	@Test
	public void testArduinoIDEExamplesOnatmegang() {
		testExample(new GenericArduinoAvrBoard("atmegang"));
	}

	@Test
	public void testArduinoIDEExamplesOnrobotControl() {
		testExample(new GenericArduinoAvrBoard("robotControl"));
	}

	@Test
	public void testArduinoIDEExamplesOnrobotMotor() {
		testExample(new GenericArduinoAvrBoard("robotMotor"));
	}

	@Test
	public void testArduinoIDEExamplesOngemma() {
		testExample(new GenericArduinoAvrBoard("gemma"));
	}

	@Test
	public void testArduinoIDEExamplesOncircuitplay32u4cat() {
		testExample(new AdafruitnCirquitPlaygroundBoard());
	}

	@Test
	public void testArduinoIDEExamplesOnyunmini() {
		testExample(new GenericArduinoAvrBoard("yunmini"));

	}

	@Test
	public void testArduinoIDEExamplesOnchiwawa() {
		testExample(new GenericArduinoAvrBoard("chiwawa"));
	}

	@Test
	public void testArduinoIDEExamplesOnone() {
		testExample(new GenericArduinoAvrBoard("one"));
	}

	@Test
	public void testArduinoIDEExamplesOnunowifi() {
		testExample(new GenericArduinoAvrBoard("unowifi"));
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
