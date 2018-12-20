package io.sloeber.core;

import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.swt.widgets.Display;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import io.sloeber.core.api.CodeDescriptor;
import io.sloeber.core.api.CompileOptions;
import io.sloeber.core.api.ConfigurationDescriptor;
import io.sloeber.core.api.PackageManager;
import io.sloeber.core.api.Sketch;
import io.sloeber.core.common.ConfigurationPreferences;
import io.sloeber.providers.MCUBoard;
import io.sloeber.ui.monitor.SerialConnection;

@SuppressWarnings({"nls","unused"})
@RunWith(Parameterized.class)
public class CompileAndUpload {
	private static final boolean reinstall_boards_and_libraries = false;
	private static int mCounter = 0;
	private MCUBoard myBoard;
	private String myName;
	private static String interval = "1500";// change between 1500 and 100

	public CompileAndUpload(String name, MCUBoard board) {
		this.myBoard = board;
		this.myName = name;

	}

	@SuppressWarnings("rawtypes")
	@Parameters(name = "{index}: {0}")
	public static Collection examples() {
		WaitForInstallerToFinish();

		try {
			File file = ConfigurationPreferences.getInstallationPath()
					.append("test.properties").toFile();
			if (!file.exists()) {
				file.createNewFile();
			}
			Properties properties = new Properties();
			try (FileInputStream fileInput = new FileInputStream(file)) {
				properties.load(fileInput);
				fileInput.close();
			}

			String key = "Last Used Blink Interval";
			interval = properties.getProperty(key);

			if ("100".equals(interval)) {
				interval = "1500";
			} else {
				interval = "100";
			}
			properties.put(key, interval);
			try (FileOutputStream fileOutput = new FileOutputStream(file);) {
				properties.store(fileOutput,
						"This is a file with values for unit testing");
				fileOutput.close();
			}

		} catch (IOException e1) {
			e1.printStackTrace();
		}

		MCUBoard[] boards = MySystem.getUploadBoards();
		// , new NodeMCUBoard()
		LinkedList<Object[]> examples = new LinkedList<>();

		for (MCUBoard curBoard : boards) {
			examples.add(new Object[]{curBoard.getID(), curBoard});
		}

		return examples;
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
		String[] packageUrlsToAdd = {
				"http://arduino.esp8266.com/stable/package_esp8266com_index.json",
				"https://raw.githubusercontent.com/stm32duino/BoardManagerFiles/master/STM32/package_stm_index.json"};
		PackageManager.addPackageURLs(
				new HashSet<>(Arrays.asList(packageUrlsToAdd)), true);
		if (reinstall_boards_and_libraries) {
			PackageManager.installAllLatestPlatforms();
		}
		PackageManager.addPrivateHardwarePath(MySystem.getTeensyPlatform());

	}

	@Test
	public void testExamples() {
		IPath templateFolder = Shared.getTemplateFolder("fastBlink");
		CompileOptions compileOptions = new CompileOptions(null);
		DateTimeFormatter df =  DateTimeFormatter
				.ofPattern("YYYY/MM/dd/MM-HH-mm-ss");
		String SerialDumpContent = myName+'-'+ df.format(LocalDateTime.now());
		compileOptions.set_C_andCPP_CompileOptions("-DINTERVAL=" + interval
				+ " -DSERIAlDUMP=" + SerialDumpContent);
		CodeDescriptor codeDescriptor=CodeDescriptor.createCustomTemplate(templateFolder);
		Map<String, String> replacers=new TreeMap<>();
		replacers.put("\\{SerialMonitorSerial\\}", myBoard.mySerialPort);
		codeDescriptor.setReplacers(replacers);
		Build_Verify_upload(codeDescriptor,	compileOptions, SerialDumpContent);

	}

	public void Build_Verify_upload(CodeDescriptor codeDescriptor,
			CompileOptions compileOptions, String SerialDumpContent) {

		IProject theTestProject = null;
		NullProgressMonitor monitor = new NullProgressMonitor();
		String projectName = String.format("%05d_%s", new Integer(mCounter++),
				this.myName);
		try {

			theTestProject = this.myBoard.getBoardDescriptor().createProject(
					projectName, null,
					ConfigurationDescriptor.getDefaultDescriptors(),
					codeDescriptor, compileOptions, monitor);
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
				theTestProject.build(IncrementalProjectBuilder.FULL_BUILD,
						monitor);
				if (Shared.hasBuildErrors(theTestProject)) {
					// give up
					fail("Failed to compile the project:" + projectName
							+ " build errors");
				}
			}

		} catch (CoreException e) {
			e.printStackTrace();
			fail("Failed to compile the project:" + projectName + " exception");
		}
		IStatus uploadStatus = Sketch.syncUpload(theTestProject);
		if (!uploadStatus.isOK()) {
			fail("Failed to upload:" + projectName);
		}
		//Wait a while for the board to recover from the upload
		try {
			Thread.sleep(10000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		verifySerialOutput(SerialDumpContent);
	}

	public boolean serialOutputMismatch;
	public void verifySerialOutput(String serialDumpContent) {
		String comPort = myBoard.getBoardDescriptor().getActualUploadPort();
		Display display = SerialConnection.getDisplay();
		display.asyncExec(new Runnable() {
			@Override
			public void run() {
				SerialConnection.show();
				SerialConnection.clearMonitor();
				SerialConnection.add(comPort, 115200);
			}
		});

		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		serialOutputMismatch=true;
		display.syncExec(new Runnable() {
			@Override
			public void run() {
				SerialConnection.remove(comPort);
				List<String> lines = SerialConnection.getContent();
				serialOutputMismatch=(! lines.contains(serialDumpContent));
				if(serialOutputMismatch) {
					System.err.println("recieved from: "+comPort);
					System.err.println(lines);
					System.err.println("End of serial input");
				}
				}	
		});
		if(serialOutputMismatch) {
			fail("Serial output does not match epectation "+serialDumpContent);
		}
	}
}
