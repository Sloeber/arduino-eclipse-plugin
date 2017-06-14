package jUnit;

import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import io.sloeber.core.api.BoardsManager;
import io.sloeber.core.api.CodeDescriptor;
import io.sloeber.core.api.CompileOptions;
import io.sloeber.core.api.ConfigurationDescriptor;
import io.sloeber.core.api.Sketch;
import jUnit.boards.Due;
import jUnit.boards.IBoard;
import jUnit.boards.NodeMCUBoard;
import jUnit.boards.UnoBoard;
import jUnit.boards.Zero;
import jUnit.boards.megaBoard;

@SuppressWarnings("nls")
@RunWith(Parameterized.class)
public class CompileAndUpload {
	private static final boolean reinstall_boards_and_libraries = false;
	private static int mCounter = 0;
	private IBoard myBoard;
	private String myName;

	public CompileAndUpload(String name, IBoard board) {
		this.myBoard = board;
		this.myName = name;

	}

	@SuppressWarnings("rawtypes")
	@Parameters(name = "{index}: {0}")
	public static Collection examples() {
		WaitForInstallerToFinish();

		IBoard unoBoard = new UnoBoard();
		IBoard nodeMCUBoard = new NodeMCUBoard();
		IBoard megaBoard = new megaBoard();
		IBoard zeroBoard = new Zero();
		IBoard dueBoard = new Due();
		LinkedList<Object[]> examples = new LinkedList<>();

		examples.add(new Object[] { "uno", unoBoard });
		examples.add(new Object[] { "mega", megaBoard });
		examples.add(new Object[] { "zero", zeroBoard });
		examples.add(new Object[] { "due", dueBoard });

		return examples;
		// new leonardoBoard(), new EsploraBoard(), new AdafruitnRF52idBoard(),
		// new AdafruitnCirquitPlaygroundBoard(), new Primo(),
	}

	/*
	 * In new new installations (of the Sloeber development environment) the
	 * installer job will trigger downloads These mmust have finished before we
	 * can start testing
	 */

	public static void WaitForInstallerToFinish() {

		installAdditionalBoards();

		Shared.waitForAllJobsToFinish();
	}

	public static void installAdditionalBoards() {
		String[] packageUrlsToAdd = { "http://arduino.esp8266.com/stable/package_esp8266com_index.json",
				"https://raw.githubusercontent.com/stm32duino/BoardManagerFiles/master/STM32/package_stm_index.json" };
		BoardsManager.addPackageURLs(new HashSet<>(Arrays.asList(packageUrlsToAdd)), true);
		if (reinstall_boards_and_libraries) {
			BoardsManager.installAllLatestPlatforms();
		}

	}

	@Test
	public void testExamples() {
		IPath templateFolder = Shared.getTemplateFolder("fastBlink");
		Build_Verify_upload(CodeDescriptor.createCustomTemplate(templateFolder));

	}

	public void Build_Verify_upload(CodeDescriptor codeDescriptor) {

		IProject theTestProject = null;

		NullProgressMonitor monitor = new NullProgressMonitor();
		String projectName = String.format("%05d_%s", new Integer(mCounter++), this.myName);
		try {

			theTestProject = this.myBoard.getBoardDescriptor().createProject(projectName, null,
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
					fail("Failed to compile the project:" + projectName + " build errors");
				}
			}

		} catch (CoreException e) {
			e.printStackTrace();
			fail("Failed to compile the project:" + projectName + " exception");
		}
		Sketch.upload(theTestProject);
	}
}
