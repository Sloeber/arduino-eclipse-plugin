package io.sloeber.core;

import static org.junit.Assert.*;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.eclipse.cdt.core.model.CoreModel;
import org.eclipse.cdt.core.model.ICModelMarker;
import org.eclipse.cdt.core.settings.model.ICProjectDescription;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
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
import static io.sloeber.core.api.Const.*;

import io.sloeber.autoBuild.api.AutoBuildProject;
import io.sloeber.autoBuild.api.IAutoBuildConfigurationDescription;
import io.sloeber.core.api.BoardDescription;
import io.sloeber.core.api.CodeDescription;
import io.sloeber.core.api.CompileDescription;
import io.sloeber.core.api.ConfigurationPreferences;
import io.sloeber.core.api.IArduinoLibraryVersion;
import io.sloeber.core.api.IExample;
import io.sloeber.core.api.BoardsManager;
import io.sloeber.core.api.SloeberProject;
import io.sloeber.providers.MCUBoard;

@SuppressWarnings("nls")
public class Shared {
	public static int buildCounter = 0;
	private static boolean deleteProjects = true;

	public static void setDeleteProjects(boolean deleteProjects) {
		Shared.deleteProjects = deleteProjects;
	}

	private static int myTestCounter;
	private static String myLastFailMessage = new String();
	private static boolean closeFailedProjects = false;

	public static boolean isCloseFailedProjects() {
		return closeFailedProjects;
	}

	public static void setCloseFailedProjects(boolean closeFailedProjects) {
		Shared.closeFailedProjects = closeFailedProjects;
	}

	public static boolean hasBuildErrors(IProject project) throws CoreException {
		IMarker[] markers = project.findMarkers(ICModelMarker.C_MODEL_PROBLEM_MARKER, true, IResource.DEPTH_INFINITE);
		for (IMarker marker : markers) {
			if (marker.getAttribute(IMarker.SEVERITY, IMarker.SEVERITY_INFO) == IMarker.SEVERITY_ERROR) {
				return true;
			}
		}
		IAutoBuildConfigurationDescription activeConfig = IAutoBuildConfigurationDescription.getActiveConfig(project,
				false);

		IFolder buildFolder = activeConfig.getBuildFolder();
		String projName = project.getName();
		String[] validOutputss = { projName + ".elf", projName + ".bin", projName + ".hex", projName + ".exe",
				"application.axf" };
		for (String validOutput : validOutputss) {
			IFile validFile = buildFolder.getFile(validOutput);
			if (validFile.exists()) {
				return false;
			}
		}

		return true;
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

	public static IPath getTemplateFolder(String templateName) throws Exception {
		Bundle bundle = Platform.getBundle("io.sloeber.tests");
		Path path = new Path("src/templates/" + templateName);
		URL fileURL = FileLocator.find(bundle, path, null);
		URL resolvedFileURL = FileLocator.toFileURL(fileURL);
		return new Path(resolvedFileURL.toURI().getPath());
	}

	public static IPath getprojectZip(String zipFileName) throws Exception {
		Bundle bundle = Platform.getBundle("io.sloeber.tests");
		Path path = new Path("src/projects/" + zipFileName);
		URL fileURL = FileLocator.find(bundle, path, null);
		URL resolvedFileURL = FileLocator.toFileURL(fileURL);
		return new Path(resolvedFileURL.toURI().getPath());
	}

	/**
	 * Convenience method to call BuildAndVerify with default project name and
	 * default compile options
	 *
	 * @param boardDescriptor
	 * @param codeDescriptor
	 * @return true if build is successful otherwise false
	 * @throws Exception
	 */
	public static boolean buildAndVerify(BoardDescription boardDescriptor, CodeDescription codeDescriptor) throws Exception {
		return buildAndVerify(boardDescriptor, codeDescriptor, null);
	}

	/**
	 * Convenience method to call BuildAndVerify with default project name and null
	 * as compile options
	 *
	 * @param boardDescriptor
	 * @param codeDescriptor
	 * @param compileOptions  can be null
	 * @return true if build is successful otherwise false
	 * @throws Exception
	 */
	public static boolean buildAndVerify(BoardDescription boardDescriptor, CodeDescription codeDescriptor,
			CompileDescription compileOptions) throws Exception {

		String projectName = getCounterName(boardDescriptor.getBoardID());
		IExample example = codeDescriptor.getLinkedExample();
		if (example != null) {
			IArduinoLibraryVersion lib = example.getArduinoLibrary();
			if (lib != null) {
				projectName = getCounterName("%05d_Library_%s_%s", codeDescriptor.getExampleName());
			} else {
				projectName = getCounterName(codeDescriptor.getExampleName());
			}
		}

		CompileDescription localCompileOptions = compileOptions;
		if (compileOptions == null) {
			localCompileOptions = new CompileDescription();
		}
		return buildAndVerify(projectName, boardDescriptor, codeDescriptor, localCompileOptions);
	}

	public static boolean buildAndVerify(String projectName, BoardDescription boardDescriptor,
			CodeDescription codeDescriptor, CompileDescription compileOptions) throws Exception {
		Set<String> builders = new HashSet<>();
		builders.add(AutoBuildProject.INTERNAL_BUILDER_ID);
		return buildAndVerifyGivenBuilders(projectName, boardDescriptor, codeDescriptor, compileOptions, builders);
	}

	public static boolean buildAndVerifyAllBuilders(String projectName, BoardDescription boardDescriptor,
			CodeDescription codeDescriptor, CompileDescription compileOptions) throws Exception {
		Set<String> builders = new HashSet<>();
		builders.add(AutoBuildProject.INTERNAL_BUILDER_ID);
		builders.add(AutoBuildProject.MAKE_BUILDER_ID);
		return buildAndVerifyGivenBuilders(projectName, boardDescriptor, codeDescriptor, compileOptions, builders);
	}

	public static boolean buildAndVerifyGivenBuilders(String projectName, BoardDescription boardDescriptor,
			CodeDescription codeDescriptor, CompileDescription compileOptions, Set<String> builders)
			throws Exception {
		IProject theTestProject = null;
		NullProgressMonitor monitor = new NullProgressMonitor();
		Shared.buildCounter++;
		boolean projectFreshlyCreated = true;

		for (String curBuilder : builders) {
			if (theTestProject == null) {
				compileOptions.setEnableParallelBuild(true);
				theTestProject = SloeberProject.createArduinoProject(projectName, null, boardDescriptor, codeDescriptor,
						compileOptions, curBuilder, monitor);
				waitForAllJobsToFinish(); // for the indexer
			}
			// do not set the build tool when the project is freshly created because then
			// the default case would never be tested
			if (!projectFreshlyCreated) {
				// set the builder
				CoreModel coreModel = CoreModel.getDefault();
				ICProjectDescription projectDescription = coreModel.getProjectDescription(theTestProject, true);
				IAutoBuildConfigurationDescription autoDesc = IAutoBuildConfigurationDescription
						.getActiveConfig(projectDescription);
				autoDesc.setBuilder(curBuilder);
				coreModel.setProjectDescription(theTestProject, projectDescription);
			}
			projectFreshlyCreated = false;
			theTestProject.build(IncrementalProjectBuilder.FULL_BUILD, monitor);
			if (hasBuildErrors(theTestProject)) {
				waitForAllJobsToFinish(); // for the indexer
				Thread.sleep(2000);
				theTestProject.build(IncrementalProjectBuilder.FULL_BUILD, monitor);
				if (hasBuildErrors(theTestProject)) {
					waitForAllJobsToFinish(); // for the indexer
					Thread.sleep(2000);
					theTestProject.build(IncrementalProjectBuilder.FULL_BUILD, monitor);
					if (hasBuildErrors(theTestProject)) {
						myLastFailMessage = myLastFailMessage + NEWLINE + "Failed to compile the project:" + projectName
								+ " with builder " + curBuilder;
					}
				}
			}
			// clean so we will get a full build at the next build
			theTestProject.build(IncrementalProjectBuilder.CLEAN_BUILD, monitor);
		}
		if (!myLastFailMessage.isBlank()) {
			if (closeFailedProjects && theTestProject!=null) {
				theTestProject.close(null);
			}
			return false;
		}

		if (theTestProject != null) {
			if (deleteProjects) {
				theTestProject.delete(true, true, null);
			} else {
				theTestProject.close(null);
			}
		}
		return true;
	}

	/*
	 * For some boards that do not run out of the box we know how to fix it. This
	 * code fixes these things
	 */
	public static void applyKnownWorkArounds() {

		java.nio.file.Path packageRoot = Paths.get(ConfigurationPreferences.getInstallationPathPackages().toString());

		/*
		 * oak on linux comes with a esptool2 in a wrong folder. As it is only 1 file I
		 * move the file
		 *
		 */
		if (isLinux) {
			java.nio.file.Path esptool2root = packageRoot.resolve("digistump").resolve("tools").resolve("esptool2")
					.resolve("0.9.1");
			java.nio.file.Path esptool2wrong = esptool2root.resolve("0.9.1").resolve("esptool2");
			java.nio.file.Path esptool2right = esptool2root.resolve("esptool2");
			if (esptool2wrong.toFile().exists()) {
				try {
					Files.move(esptool2wrong, esptool2right);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		/*
		 * Elector heeft core Platino maar de directory noemt platino. In windows geen
		 * probleem maar in case sensitive linux dus wel
		 */
		if (isLinux) {
			java.nio.file.Path cores = packageRoot.resolve("Elektor").resolve("hardware").resolve("avr")
					.resolve("1.0.0").resolve("cores");
			java.nio.file.Path coreWrong = cores.resolve("platino");
			java.nio.file.Path coreGood = cores.resolve("Platino");
			if (coreWrong.toFile().exists()) {
				coreWrong.toFile().renameTo(coreGood.toFile());
			}
		}
	}

	public static String getCounterName(String name) {
		return getCounterName("%05d_%s", name);
	}

	public static String getCounterName(String format, String name) {
		return String.format(format, Integer.valueOf(myTestCounter++), name);
	}

	public static String getProjectName(CodeDescription codeDescriptor, MCUBoard board) {
		String codeString = codeDescriptor.getExampleName();
		if (codeString == null) {
			codeString = codeDescriptor.getCodeType().toString();
		}
		return String.format("%05d_%s_%s", Integer.valueOf(myTestCounter++), codeString,
				board.getBoardDescriptor().getBoardID());
	}

	public static String getLastFailMessage() {
		return myLastFailMessage;
	}

	// copied from
	// https://www.codejava.net/java-se/file-io/programmatically-extract-a-zip-file-using-java

	// /**
	// * This utility extracts files and directories of a standard zip file to
	// * a destination directory.
	// * @author www.codejava.net
	// *
	// */
	// public class UnzipUtility {
	/**
	 * Size of the buffer to read/write data
	 */
	private static final int BUFFER_SIZE = 4096;

	/**
	 * Extracts a zip file specified by the zipFilePath to a directory specified by
	 * destDirectory (will be created if does not exists)
	 *
	 * @param zipFilePath
	 * @param destDirectory
	 * @throws IOException
	 */
	public static void unzip(String zipFilePath, String destDirectory) throws IOException {
		File destDir = new File(destDirectory);
		if (!destDir.exists()) {
			destDir.mkdir();
		}
		try (ZipInputStream zipIn = new ZipInputStream(new FileInputStream(zipFilePath))) {
			ZipEntry entry = zipIn.getNextEntry();
			// iterates over entries in the zip file
			while (entry != null) {
				String filePath = destDirectory + File.separator + entry.getName();
				if (!entry.isDirectory()) {
					// if the entry is a file, extracts it
					extractFile(zipIn, filePath);
				} else {
					// if the entry is a directory, make the directory
					File dir = new File(filePath);
					dir.mkdirs();
				}
				zipIn.closeEntry();
				entry = zipIn.getNextEntry();
			}
			zipIn.close();
		}
	}

	/**
	 * Extracts a zip entry (file entry)
	 *
	 * @param zipIn
	 * @param filePath
	 * @throws IOException
	 */
	private static void extractFile(ZipInputStream zipIn, String filePath) throws IOException {
		try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(filePath))) {
			byte[] bytesIn = new byte[BUFFER_SIZE];
			int read = 0;
			while ((read = zipIn.read(bytesIn)) != -1) {
				bos.write(bytesIn, 0, read);
			}
			bos.close();
		}
	}
	// }
	// end copy from
	// https://www.codejava.net/java-se/file-io/programmatically-extract-a-zip-file-using-java

	// copied from
	// https://stackoverflow.com/questions/18344721/extract-the-difference-between-two-strings-in-java
	/**
	 * Returns an array of size 2. The entries contain a minimal set of characters
	 * that have to be removed from the corresponding input strings in order to make
	 * the strings equal.
	 */
	public static String[] difference(String a, String b) {
		return diffHelper(a, b, new HashMap<>());
	}

	@SuppressWarnings("boxing")
	private static String[] diffHelper(String a, String b, Map<Long, String[]> lookup) {
		return lookup.computeIfAbsent(((long) a.length()) << 32 | b.length(), k -> {
			if (a.isEmpty() || b.isEmpty()) {
				return new String[] { a, b };
			} else if (a.charAt(0) == b.charAt(0)) {
				return diffHelper(a.substring(1), b.substring(1), lookup);
			} else {
				String[] aa = diffHelper(a.substring(1), b, lookup);
				String[] bb = diffHelper(a, b.substring(1), lookup);
				if (aa[0].length() + aa[1].length() < bb[0].length() + bb[1].length()) {
					return new String[] { a.charAt(0) + aa[0], aa[1] };
				}
				return new String[] { bb[0], b.charAt(0) + bb[1] };
			}
		});
	}
	// end of copied from
	// https://stackoverflow.com/questions/18344721/extract-the-difference-between-two-strings-in-java
}
