package jUnit;

import static org.junit.Assert.fail;

import org.eclipse.cdt.core.model.ICModelMarker;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.jobs.IJobManager;
import org.eclipse.core.runtime.jobs.Job;

@SuppressWarnings("nls")
public class Shared {
	private static String teensyInstall = "D:\\arduino\\arduino-1.8.2Teensy1.38beta2\\hardware\\teensy";
	private static String teensyInstallLinux = "/home/jantje/programs/arduino-1.8.0/hardware/teensy";

	public static String getTeensyPlatform() {
		switch (Platform.getOS()) {
		case Platform.OS_WIN32:
			return teensyInstall;
		case Platform.OS_LINUX:
			return teensyInstallLinux;
		}
		return null;
	}

	public static String getTeensyBoard_txt() {
		return getTeensyPlatform() + "/avr/boards.txt";
	}

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
			Thread.sleep(1000);

			IJobManager jobMan = Job.getJobManager();

			while (!jobMan.isIdle()) {
				Thread.sleep(500);
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

		String gitHome = System.getenv("HOME");

		if (Platform.getOS().equals(Platform.OS_WIN32)) {
			if (gitHome == null) {
				System.err.println("Git HOME envvar is not define. Using default value");
				gitHome = System.getenv("HOMEDRIVE") + System.getenv("HOMEPATH");
			}
			gitHome += "/git";
		} else {
			if (gitHome == null) {
				System.err.println("Git HOME envvar is not define. Using default value");
				gitHome = "~";
			}
			gitHome += "/.git";
		}
		Path path = new Path(gitHome + "/arduino-eclipse-plugin/io.sloeber.core/src/jUnit/templates/" + templateName);
		return path;
	}

}
