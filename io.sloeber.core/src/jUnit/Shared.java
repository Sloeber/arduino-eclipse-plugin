package jUnit;

import static org.junit.Assert.fail;

import org.eclipse.cdt.core.model.ICModelMarker;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.jobs.IJobManager;
import org.eclipse.core.runtime.jobs.Job;

@SuppressWarnings("nls")
public class Shared {
	public static String teensyInstall = "D:/arduino/arduino-1.6.9 - Teensy 1.29/hardware";
	public static String teensyBoards_txt = teensyInstall + "/teensy/avr/boards.txt";

	public static boolean hasBuildErrors(IProject project) throws CoreException {
		IMarker[] markers = project.findMarkers(ICModelMarker.C_MODEL_PROBLEM_MARKER, true, IResource.DEPTH_INFINITE);
		for (IMarker marker : markers) {
			if (marker.getAttribute(IMarker.SEVERITY, IMarker.SEVERITY_INFO) == IMarker.SEVERITY_ERROR) {
				return true;
			}
		}
		return false;
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

	public static IPath getTemplateFolder(String templateName) {
		Path path = new Path("C:/Users/jan/git/arduino-eclipse-plugin/io.sloeber.core/src/jUnit/templates");
		return path.append(templateName);
	}

}
