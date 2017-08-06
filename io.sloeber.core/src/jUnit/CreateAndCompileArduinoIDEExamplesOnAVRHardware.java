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
		this.myUsesSerial = usesSerial;
		this.myUsesSerial1 = usesSerial1;
		this.myUsesKeyboard = usesKeyboard;

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
			String inoName = curexample.getKey().trim();
			Boolean usesSerial = new Boolean(Examples.getUsesSerialExamples().contains(inoName));
			Boolean usesSerial1 = new Boolean(Examples.getUsesSerial1Examples().contains(inoName));
			Boolean usesKeyboard = new Boolean(Examples.getUsesKeyboardExamples().contains(inoName));

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
		if (this.myUsesSerial && !board.supportsSerial()) {
			System.out.println("!TEST SKIPPED due to Serial " + this.myName + " " + board.getName());
			return;
		}
		if (this.myUsesSerial1 && !board.supportsSerial1()) {
			System.out.println("!TEST SKIPPED due to Serial1 " + this.myName + " " + board.getName());
			return;
		}
		if (this.myUsesKeyboard && !board.supportsKeyboard()) {
			System.out.println("!TEST SKIPPED due to keyboard " + this.myName + " " + board.getName());
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
