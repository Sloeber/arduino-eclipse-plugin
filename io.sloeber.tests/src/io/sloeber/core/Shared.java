package io.sloeber.core;

import static org.junit.Assert.fail;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;

import org.eclipse.cdt.core.model.ICModelMarker;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.jobs.IJobManager;
import org.eclipse.core.runtime.jobs.Job;
import org.osgi.framework.Bundle;

import io.sloeber.core.api.BoardDescriptor;
import io.sloeber.core.api.BoardsManager;
import io.sloeber.core.api.CodeDescriptor;
import io.sloeber.core.api.CompileOptions;
import io.sloeber.core.api.ConfigurationDescriptor;
import io.sloeber.core.api.Other;

@SuppressWarnings("nls")
public class Shared {
	public final static String TEST_LIBRARY_INDEX_URL = "https://raw.githubusercontent.com/Sloeber/arduino-eclipse-plugin/master/io.sloeber.core/src/jUnit/library_sloeber_index.json";
	public final static String ADAFRUIT_BOARDS_URL = "https://adafruit.github.io/arduino-board-index/package_adafruit_index.json";
	public final static String ESP8266_BOARDS_URL = "http://arduino.esp8266.com/stable/package_esp8266com_index.json";

	private static String jantjesWindowsMachine = "D:\\arduino\\arduino-1.8.2Teensy1.38beta2\\hardware\\teensy";
	private static String jantjesVirtualLinuxMachine = "/home/jantje/programs/arduino-1.8.0/hardware/teensy";
	private static int mCounter = 0;

	public static String getTeensyPlatform() {
		switch (Other.getSystemHash()) {
		case "1248215851":
			return jantjesWindowsMachine;
		case "still need to gett the key":
			return jantjesVirtualLinuxMachine;
		}
		return new String();
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

			while (!(jobMan.isIdle() && BoardsManager.isReady())) {
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

		try {
			   Bundle bundle = Platform.getBundle("io.sloeber.tests");
			   Path path = new Path("src/templates/" + templateName);
			   URL fileURL = FileLocator.find(bundle, path, null);
			   URL resolvedFileURL=FileLocator.toFileURL(fileURL);
			return new  Path(resolvedFileURL.toURI().getPath());
		} catch (URISyntaxException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}


		System.err.println("Failed to find templates in io.sloeber.tests plugin.");
		return new Path(new String());
	}

	public static void BuildAndVerify(BoardDescriptor boardid) {

		IProject theTestProject = null;
		CodeDescriptor codeDescriptor = CodeDescriptor.createDefaultIno();
		NullProgressMonitor monitor = new NullProgressMonitor();
		String projectName = String.format("%03d_", new Integer(mCounter++)) + boardid.getBoardID();
		try {

			theTestProject = boardid.createProject(projectName, null, ConfigurationDescriptor.getDefaultDescriptors(),
					codeDescriptor, new CompileOptions(null), monitor);
			Shared.waitForAllJobsToFinish(); // for the indexer
		} catch (Exception e) {
			e.printStackTrace();
			fail("Failed to create the project:" + projectName);
			return;
		}
		try {
			theTestProject.build(IncrementalProjectBuilder.FULL_BUILD, monitor);
			if (Shared.hasBuildErrors(theTestProject)) {
				fail("Failed to compile the project:" + projectName + " build errors");
			}
		} catch (CoreException e) {
			e.printStackTrace();
			fail("Failed to compile the project:" + boardid.getBoardName() + " exception");
		}
		try {
			theTestProject.delete(false, true, null);
		} catch (CoreException e) {
			e.printStackTrace();
		}
	}

}
