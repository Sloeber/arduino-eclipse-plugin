package io.sloeber.core;

import static org.junit.Assert.fail;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import org.eclipse.cdt.core.envvar.EnvironmentVariable;
import org.eclipse.cdt.core.envvar.IEnvironmentVariable;
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
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.osgi.framework.Bundle;
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;

import io.sloeber.core.api.BoardDescriptor;
import io.sloeber.core.api.BoardsManager;
import io.sloeber.core.api.CodeDescriptor;
import io.sloeber.core.api.CompileOptions;
import io.sloeber.core.api.ConfigurationDescriptor;
import io.sloeber.core.common.ConfigurationPreferences;

@SuppressWarnings("nls")
public class Shared {
	public final static String TEST_LIBRARY_INDEX_URL = "https://raw.githubusercontent.com/Sloeber/arduino-eclipse-plugin/master/io.sloeber.core/src/jUnit/library_sloeber_index.json";
	public final static String ADAFRUIT_BOARDS_URL = "https://adafruit.github.io/arduino-board-index/package_adafruit_index.json";
	public final static String ESP8266_BOARDS_URL = "http://arduino.esp8266.com/stable/package_esp8266com_index.json";

	private static int mBuildCounter = 0;

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
			URL resolvedFileURL = FileLocator.toFileURL(fileURL);
			return new Path(resolvedFileURL.toURI().getPath());
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
		String projectName = String.format("%03d_", new Integer(mBuildCounter++)) + boardid.getBoardID();
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

	/*
	 * For some boards that do not run out of the box we know how to fix it. This
	 * code fixes these things
	 */
	public static void applyKnownWorkArounds() {
		/*
		 * for chipkit PONTECH UAV100 board boards.txt contains
		 * usbono_pic32.compiler.c.extra_flags=-G1024 -Danything=1 and platform.txt
		 * contains ... {compiler.define} "{compiler.cpp.extra_flags}"
		 * {build.extra_flags} ... resulting in ... "-G1024 -Danything=1" ... Arduino
		 * IDE magically makes this ... "-G1024" -Danything=1 ... But I refuse to do
		 * this type of smart parsing because modifying text files is lots easier
		 * therefore as a workaround I replace "{compiler.cpp.extra_flags}" in
		 * platform.txt with {compiler.cpp.extra_flags}
		 */

		java.nio.file.Path packageRoot = Paths.get(ConfigurationPreferences.getInstallationPathPackages().toOSString());
		java.nio.file.Path platform_txt = packageRoot.resolve("chipKIT\\hardware\\pic32\\2.0.1\\platform.txt");
		if (platform_txt.toFile().exists()) {
			replaceInFile(platform_txt, "\"{compiler.cpp.extra_flags}\"", "{compiler.cpp.extra_flags}");
		}

		/*
		 * oak does not come with all required libraries and assumes arduino IDE has
		 * them available So I set sloeber_path_extension to the teensy root
		 */
		java.nio.file.Path arduinoIDERoot = Paths.get(MySystem.getTeensyPlatform());
		if (arduinoIDERoot.toFile().exists() && (arduinoIDERoot.toFile().list().length > 3)) {
			arduinoIDERoot = arduinoIDERoot.getParent().getParent();

			IEnvironmentVariable var = new EnvironmentVariable("sloeber_path_extension", arduinoIDERoot.toString());
			IEclipsePreferences myScope = InstanceScope.INSTANCE.getNode("org.eclipse.cdt.core");
			Preferences t = myScope.node("environment").node("workspace").node(var.getName().toUpperCase());
			t.put("delimiter", var.getDelimiter());
			t.put("operation", "append");
			t.put("value", var.getValue());

			try {
				myScope.flush();
			} catch (BackingStoreException e) {

				e.printStackTrace();
			}
		}

		// String CFG_DATA_PROVIDER_ID="";
		// IEnvironmentVariable var = new EnvironmentVariable("sloeber_path_extension",
		// arduinoIDERoot.toString());
		// try {
		// ICConfigurationDescription prjCDesc
		// =CCorePlugin.getDefault().getPreferenceConfiguration(CFG_DATA_PROVIDER_ID,
		// true);
		// IEnvironmentVariableManager envManager =
		// CCorePlugin.getDefault().getBuildEnvironmentManager();
		// envManager.
		// prjCDesc.
		// CCorePlugin.getDefault().setPreferenceConfiguration(CFG_DATA_PROVIDER_ID,
		// prjCDesc);
		// } catch (CoreException e) {
		// // TODO Auto-generated catch block
		// e.printStackTrace();
		// }
		//
		// ICProjectDescription prjCDesc = CoreModel.getDefault().getCModel().
		// .getDefault().getProjectDescription(project);
		// ICConfigurationDescription configurationDescription =
		// prjCDesc.getActiveConfiguration();
		// configurationDescription.

	}

	private static void replaceInFile(java.nio.file.Path file, String find, String replace) {

		try {
			// read all bytes from file (they will include bytes representing used line
			// separtors)
			byte[] bytesFromFile = Files.readAllBytes(file);

			// convert themm to string
			String textFromFile = new String(bytesFromFile, StandardCharsets.UTF_8);// use proper charset

			// replace what you need (line separators will stay the same)
			textFromFile = textFromFile.replace(find, replace);

			// write back data to file

			Files.write(file, textFromFile.getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
