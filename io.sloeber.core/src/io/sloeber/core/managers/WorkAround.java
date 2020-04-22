package io.sloeber.core.managers;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.Charset;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.SystemUtils;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;

import io.sloeber.core.Activator;
import io.sloeber.core.common.Common;
import io.sloeber.core.common.Const;
import io.sloeber.core.tools.FileModifiers;
import io.sloeber.core.tools.Version;

/**
 * A class to apply workarounds to installed packages workaround are done after
 * installation at usage of boards.txt file at usage of platform.txt file
 * 
 * The first line of the worked around files contain a key A newer version of
 * sloeber that has a different workaround shuld change the key in this way the
 * worked around files can be persisted and updated when needed
 * 
 * @author jan
 *
 */
@SuppressWarnings("nls")
public class WorkAround {
	// Each time this class is touched consider changing the String below to enforce
	// updates
	private static final String FIRST_SLOEBER_WORKAROUND_LINE = "#Sloeber created workaound file V1.00.test 3";

	/**
	 * workarounds done at installation time. I try to keep those at a minimum but
	 * none platform.txt and boards.txt workarounds need to be doine during install
	 * time
	 * 
	 * @param platform
	 */
	static synchronized public void applyKnownWorkArounds(ArduinoPlatform platform) {

		/*
		 * for STM32 V1.8 and later #include "SrcWrapper.h" to Arduino.h remove the
		 * prebuild actions remove the build_opt
		 * https://github.com/Sloeber/arduino-eclipse-plugin/issues/1143
		 */
		if (Version.compare("1.8.0", platform.getVersion()) != 1) {
			if ("stm32".equals(platform.getArchitecture())) {
				if ("STM32".equals(platform.getParent().getName())) {
					if (Version.compare("1.8.0", platform.getVersion()) == 0) {
						File arduino_h = platform.getInstallPath().append("cores").append("arduino").append("Arduino.h")
								.toFile();
						if (arduino_h.exists()) {
							FileModifiers.replaceInFile(arduino_h, false, "#include \"pins_arduino.h\"",
									"#include \"pins_arduino.h\"\n#include \"SrcWrapper.h\"");
						}
					}
				}
			}
		}

		MakePlatformSloeberTXT(platform.getPlatformFile());
		MakeBoardsSloeberTxt(platform.getBoardsFile());

	}

	/**
	 * create a workedaround boards.txt and return that filz
	 * 
	 * @param requestedFileToWorkAround
	 * 
	 * @return the worked around file or requestedFileToWorkAround is it does not
	 *         exist or error
	 */
	static synchronized public File MakeBoardsSloeberTxt(File requestedFileToWorkAround) {
		if (!requestedFileToWorkAround.exists()) {
			return requestedFileToWorkAround;
		}
		String inFile = requestedFileToWorkAround.toString();
		String actualFileToLoad = inFile.replace(Const.BOARDS_FILE_NAME, "boards.sloeber.txt");
		if (inFile.equals(actualFileToLoad)) {
			Common.log(new Status(IStatus.ERROR, Activator.getId(),
					"Boards.txt file is not recognized " + requestedFileToWorkAround.toString()));
			return requestedFileToWorkAround;
		}
		File boardsSloeberTXT = new File(actualFileToLoad);
		if (boardsSloeberTXT.exists()) {
			// delete if outdated
			String firstLine = null;
			try (BufferedReader Buff = new BufferedReader(new FileReader(boardsSloeberTXT));) {
				firstLine = Buff.readLine();
			} catch (Exception e) {
				// ignore and delete the file
			}
			if (!FIRST_SLOEBER_WORKAROUND_LINE.equals(firstLine)) {
				boardsSloeberTXT.delete();
			}
		}
		if (!boardsSloeberTXT.exists()) {
			try {
				String boardsTXT = FIRST_SLOEBER_WORKAROUND_LINE + "\n";
				boardsTXT += FileUtils.readFileToString(requestedFileToWorkAround, Charset.defaultCharset());
				boardsTXT = boardsTXT.replace("\r\n", "\n");

				if (SystemUtils.IS_OS_WINDOWS) {
					// replace FI circuitplay32u4cat.build.usb_manufacturer="Adafruit"
					// with circuitplay32u4cat.build.usb_manufacturer=Adafruit
					boardsTXT = boardsTXT.replaceAll("(\\S+\\.build\\.usb\\S+)=\\\"(.+)\\\"", "$1=$2");
				}
				FileUtils.write(boardsSloeberTXT, boardsTXT, Charset.defaultCharset());
			} catch (IOException e) {
				Common.log(new Status(IStatus.WARNING, Activator.getId(),
						"Failed to apply work arounds to " + requestedFileToWorkAround.toString(), e));
				return requestedFileToWorkAround;
			}
		}
		return boardsSloeberTXT;
	}

	/**
	 * create a workedaround platform.txt and return that filz
	 * 
	 * @param requestedFileToWorkAround
	 * 
	 * @return the worked around file or requestedFileToWorkAround is it does not
	 *         exist or error
	 */
	public synchronized static File MakePlatformSloeberTXT(File requestedFileToWorkAround) {
		if (!requestedFileToWorkAround.exists()) {
			return requestedFileToWorkAround;
		}
		String inFile = requestedFileToWorkAround.toString();
		String actualFileToLoad = inFile.replace(Const.PLATFORM_FILE_NAME, "platform.sloeber.txt");
		if (inFile.equals(actualFileToLoad)) {
			Common.log(new Status(IStatus.ERROR, Activator.getId(),
					"platform.txt file is not recognized " + requestedFileToWorkAround.toString()));
			return requestedFileToWorkAround;
		}
		File platformSloeberTXT = new File(actualFileToLoad);
		if (platformSloeberTXT.exists()) {
			// delete if outdated
			String firstLine = null;
			try (BufferedReader Buff = new BufferedReader(new FileReader(platformSloeberTXT));) {
				firstLine = Buff.readLine();
			} catch (Exception e) {
				// ignore and delete the file
			}
			if (!FIRST_SLOEBER_WORKAROUND_LINE.equals(firstLine)) {
				platformSloeberTXT.delete();
			}
		}
		if (!platformSloeberTXT.exists()) {
			try {
				String platformTXT = FIRST_SLOEBER_WORKAROUND_LINE + "\n";
				platformTXT += FileUtils.readFileToString(requestedFileToWorkAround, Charset.defaultCharset());
				platformTXT = platformTXT.replace("\r\n", "\n");

				// Arduino treats core differently so we need to change the location of directly
				// referenced files this manifestates only in the combine recipe
				int inCombineStartIndex = platformTXT.indexOf("\nrecipe.c.combine.pattern") + 1;
				if (inCombineStartIndex > 0) {
					int inCombineEndIndex = platformTXT.indexOf("\n", inCombineStartIndex) - 1;
					if (inCombineEndIndex > 0) {
						String inCombineRecipe = platformTXT.substring(inCombineStartIndex, inCombineEndIndex);

						String outCombineRecipe = inCombineRecipe.replaceAll("(\\{build\\.path})(/core)?/sys",
								"$1/core/core/sys");
						platformTXT = platformTXT.replace(inCombineRecipe, outCombineRecipe);
					}
				}

				// workaround for infineon arm v1.4.0 overwriting the default to a wrong value
				platformTXT = platformTXT.replace("\nbuild.core.path", "\n#line removed by Sloeber build.core.path");

				Path platformTXTPath = new Path(requestedFileToWorkAround.toString());
				int totalSegments = platformTXTPath.segmentCount();
				String platformVersion = platformTXTPath.segment(totalSegments - 2);
				String platformArchitecture = platformTXTPath.segment(totalSegments - 3);
				String platformName = platformTXTPath.segment(totalSegments - 5);
				if (Version.compare("1.8.0", platformVersion) != 1) {
					if ("stm32".equals(platformArchitecture)) {
						if ("STM32".equals(platformName)) {
							platformTXT = platformTXT.replace("\"@{build.opt.path}\"", "");
							platformTXT = platformTXT.replaceAll("recipe\\.hooks\\.prebuild\\..*", "");
						}
					}
				}

				// for adafruit nfr
				platformTXT = platformTXT.replace("-DARDUINO_BSP_VERSION=\"{version}\"",
						"\"-DARDUINO_BSP_VERSION=\\\"{version}\\\"\"");

				if (SystemUtils.IS_OS_WINDOWS) {
					// replace FI '-DUSB_PRODUCT={build.usb_product}' with
					// "-DUSB_PRODUCT=\"{build.usb_product}\""
					platformTXT = platformTXT.replaceAll("\\'-D(\\S+)=\\{(\\S+)}\\'", "\"-D$1=\\\\\"{$2}\\\\\"\"");

					// quoting fixes for embedutils
					platformTXT = platformTXT.replaceAll("\"?(-DMBEDTLS_\\S+)=\\\\?\"(mbedtls\\S+)\"\\\\?\"*",
							"\"$1=\\\\\"$2\\\\\"\"");

					// Sometimes "-DUSB_MANUFACTURER={build.usb_manufacturer}"
					// "-DUSB_PRODUCT={build.usb_product}"
					// is used fi LinKit smart
					platformTXT = platformTXT.replace("\"-DUSB_MANUFACTURER={build.usb_manufacturer}\"",
							"\"-DUSB_MANUFACTURER=\\\"{build.usb_manufacturer}\\\"\"");
					platformTXT = platformTXT.replace("\"-DUSB_PRODUCT={build.usb_product}\"",
							"\"-DUSB_PRODUCT=\\\"{build.usb_product}\\\"\"");
					platformTXT = platformTXT.replace(" -DARDUINO_BOARD=\"{build.board}\" ",
							" \"-DARDUINO_BOARD=\\\"{build.board}\\\"\" ");

				}
				FileUtils.write(platformSloeberTXT, platformTXT, Charset.defaultCharset());
			} catch (IOException e) {
				Common.log(new Status(IStatus.WARNING, Activator.getId(),
						"Failed to apply work arounds to " + requestedFileToWorkAround.toString(), e));
				return requestedFileToWorkAround;
			}
		}
		return platformSloeberTXT;
	}

}
