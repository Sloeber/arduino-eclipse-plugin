package io.sloeber.core;

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
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import io.sloeber.core.api.BoardDescriptor;
import io.sloeber.core.api.CodeDescriptor;
import io.sloeber.core.api.CompileOptions;
import io.sloeber.core.api.ConfigurationDescriptor;
import io.sloeber.core.api.LibraryManager;
import io.sloeber.providers.Arduino;
import io.sloeber.providers.MCUBoard;

@SuppressWarnings({"nls","unused"})
@RunWith(Parameterized.class)
public class CreateAndCompileArduinoIDEExamplesOnAVRHardwareTest {
	private static int mCounter = 0;
	private CodeDescriptor myCodeDescriptor;
	private String myName;
	private Examples myExample;
	private static int totalFails = 0;
	private static int maxFails = 500;

	public CreateAndCompileArduinoIDEExamplesOnAVRHardwareTest(String name, CodeDescriptor codeDescriptor,
			Examples example) {

		myCodeDescriptor = codeDescriptor;
		myName = name;
		myExample = example;
	}

	@SuppressWarnings("rawtypes")
	@Parameters(name = "{index}: {0}")
	public static Collection examples() {
		Shared.waitForAllJobsToFinish();
		LinkedList<Object[]> examples = new LinkedList<>();

		TreeMap<String, IPath> exampleFolders = LibraryManager.getAllArduinoIDEExamples();
		for (Map.Entry<String, IPath> curexample : exampleFolders.entrySet()) {
			String fqn = curexample.getKey().trim();
			IPath examplePath = curexample.getValue();
			Examples example = new Examples(fqn, null, examplePath);
			if (!skipExample(example)){
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
		// skip Teensy stuff on Arduino hardware
		// Teensy is so mutch more advanced that most arduino avr hardware can not
		// handle
		// it
		return example.getPath().toString().contains("Teensy");
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
	public void testArduinoIDEExamplesOnUno() {
		testExample(Arduino.uno());
	}

	@Test
	public void testArduinoIDEExamplesOnLeonardo() {
		testExample(Arduino.leonardo());
	}

	@Test
	public void testArduinoIDEExamplesOnEsplora() {
		testExample(Arduino.esplora());
	}

	@Test
	public void testArduinoIDEExamplesOnYun() {
		testExample(Arduino.yun());
	}

	@Test
	public void testArduinoIDEExamplesOnDiecimila() {
		testExample(Arduino.getAvrBoard("diecimila"));
	}

	@Test
	public void testArduinoIDEExamplesOnMega() {
		testExample(Arduino.getMega2560Board());
	}

	@Test
	public void testArduinoIDEExamplesOneMegaADK() {
		testExample(Arduino.MegaADK());

	}

	@Test
	public void testArduinoIDEExamplesOnLeonardoEth() {

		testExample(Arduino.getAvrBoard("leonardoeth"));

	}

	@Test
	public void testArduinoIDEExamplesOneMicro() {

		testExample(Arduino.getAvrBoard("micro"));

	}

	@Test
	public void testArduinoIDEExamplesOneMini() {
		testExample(Arduino.getAvrBoard("mini"));

	}

	@Test
	public void testArduinoIDEExamplesOnEthernet() {
		testExample(Arduino.getAvrBoard("ethernet"));
	}

	@Test
	public void testArduinoIDEExamplesOnFio() {
		testExample(Arduino.getAvrBoard("fio"));
	}

	@Test
	public void testArduinoIDEExamplesOnBt() {
		testExample(Arduino.getAvrBoard("bt"));
	}

	@Test
	public void testArduinoIDEExamplesOnLilyPadUSB() {
		testExample(Arduino.getAvrBoard("LilyPadUSB"));
	}

	@Test
	public void testArduinoIDEExamplesOnlilypad() {
		testExample(Arduino.getAvrBoard("lilypad"));
	}

	@Test
	public void testArduinoIDEExamplesOnPro() {
		testExample(Arduino.getAvrBoard("pro"));
	}

	@Test
	public void testArduinoIDEExamplesOnatmegang() {
		testExample(Arduino.getAvrBoard("atmegang"));
	}

	@Test
	public void testArduinoIDEExamplesOnrobotControl() {
		testExample(Arduino.getAvrBoard("robotControl"));
	}

	@Test
	public void testArduinoIDEExamplesOnrobotMotor() {
		testExample(Arduino.getAvrBoard("robotMotor"));
	}

	@Test
	public void testArduinoIDEExamplesOngemma() {
		testExample(Arduino.getAvrBoard("gemma"));
	}

	@Test
	public void testArduinoIDEExamplesOncircuitplay32u4cat() {
		testExample(Arduino.adafruitnCirquitPlayground());
	}

	@Test
	public void testArduinoIDEExamplesOnyunmini() {
		testExample(Arduino.getAvrBoard("yunmini"));

	}

	@Test
	public void testArduinoIDEExamplesOnchiwawa() {
		testExample(Arduino.getAvrBoard("chiwawa"));
	}

	@Test
	public void testArduinoIDEExamplesOnone() {
		testExample(Arduino.getAvrBoard("one"));
	}

	@Test
	public void testArduinoIDEExamplesOnunowifi() {
		testExample(Arduino.getAvrBoard("unowifi"));
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
