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
import java.util.stream.Stream;

import org.eclipse.cdt.core.CProjectNature;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.swt.widgets.Display;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import io.sloeber.core.api.CodeDescription;
import io.sloeber.core.api.CompileDescription;
import io.sloeber.core.api.ConfigurationPreferences;
import io.sloeber.core.api.ISloeberConfiguration;
import io.sloeber.core.api.BoardsManager;
import io.sloeber.core.api.Preferences;
import io.sloeber.core.api.Sketch;
import io.sloeber.core.api.SloeberProject;
import io.sloeber.providers.Arduino;
import io.sloeber.providers.ESP32;
import io.sloeber.providers.ESP8266;
import io.sloeber.providers.MCUBoard;
import io.sloeber.providers.Teensy;
import io.sloeber.ui.monitor.SerialConnection;

@SuppressWarnings({ "nls", "unused" })
public class CompileAndUpload {

	private static final boolean reinstall_boards_and_libraries = false;

	private static String interval = "1500";// change between 1500 and 100


	static Stream<Arguments> uploadBourds() {
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
		List<Arguments> ret = new LinkedList<>();

		for (MCUBoard curBoard : boards) {
			ret.add(Arguments.of(curBoard));
		}

		return ret.stream();

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
		Preferences.setUseBonjour(false);
		String[] packageUrlsToAdd = {
                ESP32.packageURL,
                ESP8266.packageURL };
		BoardsManager.addPackageURLs(
				new HashSet<>(Arrays.asList(packageUrlsToAdd)), true);

        if (reinstall_boards_and_libraries) {
            BoardsManager.removeAllInstalledPlatforms();
        }

        // make sure the needed boards are available
        ESP8266.installLatest();
        Arduino.installLatestAVRBoards();
        Arduino.installLatestSamDBoards();
        Arduino.installLatestIntellCurieBoards();
        Arduino.installLatestSamBoards();
        Teensy.installLatest();
    }

	@ParameterizedTest
	@MethodSource("uploadBourds")
    public void  testExamples(	MCUBoard myBoard)  throws Exception {
		 String myName=myBoard.getID();
        IPath templateFolder = Shared.getTemplateFolder("fastBlink");
        CompileDescription compileOptions = new CompileDescription();
        DateTimeFormatter df = DateTimeFormatter.ofPattern("YYYY/MM/dd-HH-mm-ss");
        String SerialDumpContent = myName + '-' + df.format(LocalDateTime.now());
        compileOptions.set_C_andCPP_CompileOptions("-DINTERVAL=" + interval + " -DSERIAlDUMP=" + SerialDumpContent);
        CodeDescription codeDescriptor = CodeDescription.createCustomTemplate(templateFolder);
        Map<String, String> replacers = new TreeMap<>();
        replacers.put("{SerialMonitorSerial}", myBoard.mySerialPort);

		codeDescriptor.setReplacers(replacers);
		Build_Verify_upload(myBoard,codeDescriptor,	compileOptions, SerialDumpContent);

	}

	public void Build_Verify_upload(MCUBoard myBoard,CodeDescription codeDescriptor,
			CompileDescription compileOptions, String SerialDumpContent) {

		 String myName=myBoard.getID();
		IProject theTestProject = null;
		NullProgressMonitor monitor = new NullProgressMonitor();
		String projectName = String.format("%05d_%s",  Integer.valueOf(Shared.buildCounter++),
				myName);
		try {
            theTestProject = SloeberProject.createArduinoProject(projectName, null,
                    myBoard.getBoardDescriptor(), codeDescriptor, compileOptions, monitor);
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
		ISloeberConfiguration sloeberConfiguration = ISloeberConfiguration.getActiveConfig(theTestProject);
		IStatus uploadStatus = sloeberConfiguration.upload();
		if (!uploadStatus.isOK()) {
			fail("Failed to upload:" + projectName);
		}
		//Wait a while for the board to recover from the upload
		try {
			Thread.sleep(10000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		verifySerialOutput( myBoard,SerialDumpContent);
	}

	boolean serialOutputMismatch;
	public void verifySerialOutput(MCUBoard myBoard,String serialDumpContent) {
		String comPort = myBoard.getBoardDescriptor().getActualUploadPort();
		Display display = SerialConnection.getDisplay();
		display.syncExec(new Runnable() {
			@Override
			public void run() {
				SerialConnection.show();
				SerialConnection.clearMonitor();

                SerialConnection.add(comPort, 9600);
            }
        });

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        serialOutputMismatch = true;
        display.syncExec(new Runnable() {
            @Override
            public void run() {
                SerialConnection.remove(comPort);
                List<String> lines = SerialConnection.getContent();
                serialOutputMismatch = (!lines.contains(serialDumpContent));
                if (serialOutputMismatch) {
                    System.err.println("recieved from: " + comPort);
                    System.err.println(lines);
                    System.err.println("End of serial input");
                }
            }
        });
        if (serialOutputMismatch) {
            fail("Serial output does not match epectation " + serialDumpContent);
        }
    }
}
