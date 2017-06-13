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
import jUnit.boards.GenericArduinoAvrBoard;

@SuppressWarnings("nls")
@RunWith(Parameterized.class)
public class CreateAndCompileArduinoIDEExamplesOnAVRHardware {
	private static int mCounter = 0;
	private CodeDescriptor myCodeDescriptor;
	private String myName;
	private static int totalFails = 0;

	public CreateAndCompileArduinoIDEExamplesOnAVRHardware(String name, CodeDescriptor codeDescriptor) {

		this.myCodeDescriptor = codeDescriptor;
		this.myName = name;

	}

	@SuppressWarnings("rawtypes")
	@Parameters(name = "{index}: {0}")
	public static Collection examples() {
		Shared.waitForAllJobsToFinish();

		LinkedList<Object[]> examples = new LinkedList<>();
		TreeMap<String, IPath> exampleFolders = BoardsManager.getAllArduinoIDEExamples();
		for (Map.Entry<String, IPath> curexample : exampleFolders.entrySet()) {
			ArrayList<Path> paths = new ArrayList<>();

			paths.add(new Path(curexample.getValue().toString()));
			CodeDescriptor codeDescriptor = CodeDescriptor.createExample(false, paths);
			String inoName = curexample.getKey();

			Object[] theData = new Object[] { "Example:" + inoName, codeDescriptor };
			examples.add(theData);
		}

		return examples;

	}

	public void testExample(String boardID) {
		// Stop after X fails because
		// the fails stays open in eclipse and it becomes really slow
		// There are only a number of issues you can handle
		// best is to focus on the first ones and then rerun starting with the
		// failures
		if (totalFails < 20) {
			GenericArduinoAvrBoard board = new GenericArduinoAvrBoard(boardID);
			BuildAndVerify(board.getBoardDescriptor());
		} else {
			fail("To many fails. Stopping test");
		}

	}

	@Test
	public void testArduinoIDEExamplesOnUno() {
		testExample("uno");
	}

	@Test
	public void testArduinoIDEExamplesOnLeonardo() {
		testExample("leonardo");
	}

	@Test
	public void testArduinoIDEExamplesOnEsplora() {
		testExample("esplora");
	}

	@Test
	public void testArduinoIDEExamplesOnYun() {
		testExample("yun");
	}

	@Test
	public void testArduinoIDEExamplesOnDiecimila() {
		testExample("diecimila");
	}

	@Test
	public void testArduinoIDEExamplesOnMega() {
		testExample("mega");
	}

	@Test
	public void testArduinoIDEExamplesOneMegaADK() {
		testExample("megaADK");

	}

	@Test
	public void testArduinoIDEExamplesOnLeonardoEth() {

		testExample("leonardoeth");

	}

	@Test
	public void testArduinoIDEExamplesOneMicro() {

		testExample("micro");

	}

	@Test
	public void testArduinoIDEExamplesOneMini() {
		testExample("mini");

	}

	@Test
	public void testArduinoIDEExamplesOnEthernet() {
		testExample("ethernet");
	}

	@Test
	public void testArduinoIDEExamplesOnFio() {
		testExample("fio");
	}

	@Test
	public void testArduinoIDEExamplesOnBt() {
		testExample("bt");
	}

	@Test
	public void testArduinoIDEExamplesOnLilyPadUSB() {
		testExample("LilyPadUSB");
	}

	@Test
	public void testArduinoIDEExamplesOnlilypad() {
		testExample("lilypad");
	}

	@Test
	public void testArduinoIDEExamplesOnPro() {
		testExample("pro");
	}

	@Test
	public void testArduinoIDEExamplesOnatmegang() {
		testExample("atmegang");
	}

	@Test
	public void testArduinoIDEExamplesOnrobotControl() {
		testExample("robotControl");
	}

	@Test
	public void testArduinoIDEExamplesOnrobotMotor() {
		testExample("robotMotor");
	}
	// Don,'t test gemma as it fails on all examples using serial
	// @Test
	// public void testArduinoIDEExamplesOngemma() {
	// testExample("gemma");
	// }

	@Test
	public void testArduinoIDEExamplesOncircuitplay32u4cat() {
		testExample("circuitplay32u4cat");
	}

	@Test
	public void testArduinoIDEExamplesOnyunmini() {
		testExample("yunmini");

	}

	@Test
	public void testArduinoIDEExamplesOnchiwawa() {
		testExample("chiwawa");
	}

	@Test
	public void testArduinoIDEExamplesOnone() {
		testExample("one");
	}

	@Test
	public void testArduinoIDEExamplesOnunowifi() {
		testExample("unowifi");
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
