package jUnit;

import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.eclipse.core.runtime.jobs.IJobManager;
import org.eclipse.core.runtime.jobs.Job;
import org.junit.BeforeClass;
import org.junit.Test;

import io.sloeber.core.api.BoardDescriptor;
import io.sloeber.core.api.BoardsManager;

@SuppressWarnings("nls")
public class Regression {

	/*
	 * In new new installations (of the Sloeber development environment) the
	 * installer job will trigger downloads These mmust have finished before we
	 * can start testing
	 */
	@BeforeClass
	public static void WaitForInstallerToFinish() {
		waitForAllJobsToFinish();
		installAdditionalBoards();
		BoardsManager.installAllLatestPlatforms();
	}

	public static void waitForAllJobsToFinish() {
		try {
			Thread.sleep(10000);

			IJobManager jobMan = Job.getJobManager();

			while (!jobMan.isIdle()) {
				Thread.sleep(5000);
				// If you do not get out of this loop it probably means you are
				// runnning the test in the gui thread
			}
			// As nothing is running now we can start installing

		} catch (InterruptedException e) {
			e.printStackTrace();
			fail("can not find installerjob");
		}
	}

	public static void installAdditionalBoards() {
		String[] packageUrlsToAdd = { "http://talk2arduino.wisen.com.au/master/package_talk2.wisen.com_index.json" };
		BoardsManager.addPackageURLs(new HashSet<>(Arrays.asList(packageUrlsToAdd)), true);
	}

	/**
	 * Test wether a json redirect is handled properly
	 * https://github.com/jantje/arduino-eclipse-plugin/issues/393
	 */
	@SuppressWarnings("static-method")
	@Test
	public void redirectedJson() {

		Map<String, String> options = new HashMap<>();
		options.put("CPU Speed", "16MHz External Crystal (default)");
		BoardDescriptor boardid = BoardsManager.getBoardID("package_talk2.wisen.com_index.json", "Talk2",
				"Talk2 AVR Boards", "whispernode", options);
		if (boardid == null) {
			fail("redirect Json ");
			return;
		}
		CreateAndCompile.BuildAndVerify(boardid);
	}

}
