package io.sloeber.core.managers;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.Charset;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.SystemUtils;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import io.sloeber.core.Activator;
import io.sloeber.core.common.Common;
import io.sloeber.core.common.Const;
import io.sloeber.core.tools.FileModifiers;
import io.sloeber.core.tools.Version;

@SuppressWarnings("nls")
public class WorkAround {
	//Each time this class is touched consider  changing the String below to enforce updates
	private static final String FIRST_SLOEBER_WORKAROUND_LINE = "#Sloeber created workaound file V1.00.test 0";


	static public void applyKnownWorkArounds(ArduinoPlatform platform) {
		boolean applySTM32PlatformFix = false;
		/*
		 * for STM32 V1.8 and later #include "SrcWrapper.h" to Arduino.h remove the
		 * prebuild actions remove the build_opt
		 * https://github.com/Sloeber/arduino-eclipse-plugin/issues/1143
		 */
		if (Version.compare("1.8.0", platform.getVersion()) != 1) {
			if ("stm32".equals(platform.getArchitecture())) {
				if ("STM32".equals(platform.getPackage().getName())) {
					if (Version.compare("1.8.0", platform.getVersion()) == 0) {
						File arduino_h = platform.getInstallPath().append("cores").append("arduino").append("Arduino.h")
								.toFile();
						if (arduino_h.exists()) {
							FileModifiers.replaceInFile(arduino_h, false, "#include \"pins_arduino.h\"",
									"#include \"pins_arduino.h\"\n#include \"SrcWrapper.h\"");
						}
					}
					applySTM32PlatformFix = true;
				}
			}
		}

		File platformTXTFile = platform.getPlatformFile();
		if (platformTXTFile.exists()) {
			try {
				File backupFile = new File(platformTXTFile.getAbsolutePath() + ".org");
				FileUtils.copyFile(platformTXTFile, backupFile);
				String platformTXT = FileUtils.readFileToString(platformTXTFile, Charset.defaultCharset());
				platformTXT = platformTXT.replace("\r\n", "\n");
				
				//Arduino treats core differently so we need to change the location of directly 
				//referenced files this manifestates only in the combine recipe
				int inCombineStartIndex=platformTXT.indexOf("\nrecipe.c.combine.pattern")+1;
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
				platformTXT = platformTXT.replace("\nbuild.core.path", "#line removed by Sloeber build.core.path");

				if (applySTM32PlatformFix) {
					platformTXT = platformTXT.replace("\"@{build.opt.path}\"", "");
					platformTXT = platformTXT.replaceAll("recipe\\.hooks\\.prebuild\\..*", "");
				}
				
				//for adafruit nfr
				platformTXT = platformTXT.replace("-DARDUINO_BSP_VERSION=\"{version}\"", "\"-DARDUINO_BSP_VERSION=\\\"{version}\\\"\"");
				
				if (SystemUtils.IS_OS_WINDOWS) {
					// replace FI '-DUSB_PRODUCT={build.usb_product}' with
					// "-DUSB_PRODUCT=\"{build.usb_product}\""
					platformTXT = platformTXT.replaceAll("\\'-D(\\S+)=\\{(\\S+)}\\'", "\"-D$1=\\\\\"{$2}\\\\\"\"");
					//Sometimes "-DUSB_MANUFACTURER={build.usb_manufacturer}" "-DUSB_PRODUCT={build.usb_product}"
					//is used fi LinKit smart
					platformTXT = platformTXT.replace("\"-DUSB_MANUFACTURER={build.usb_manufacturer}\"", 
							"\"-DUSB_MANUFACTURER=\\\"{build.usb_manufacturer}\\\"\"");
					platformTXT = platformTXT.replace("\"-DUSB_PRODUCT={build.usb_product}\"", 
							"\"-DUSB_PRODUCT=\\\"{build.usb_product}\\\"\"");
					

					
				}
				FileUtils.write(platformTXTFile, platformTXT, Charset.defaultCharset());
			} catch (IOException e) {
				Common.log(new Status(IStatus.WARNING, Activator.getId(),
						"Failed to apply work arounds to " + platformTXTFile.toString(), e));
			}
		}
		File boardsTXTFile = platform.getBoardsFile();
		MakeBoardsSloeberTxt(boardsTXTFile);

	}
	
	static public File MakeBoardsSloeberTxt(File requestedFileToWorkAround) {
		String inFile=requestedFileToWorkAround.toString();
		String actualFileToLoad=inFile.replace(Const.BOARDS_FILE_NAME,"boards.sloeber.txt");
		if(inFile.equals(actualFileToLoad)) {
			Common.log(new Status(IStatus.ERROR, Activator.getId(),
					"Boards.txt file is not recognized " + requestedFileToWorkAround.toString()));
			return requestedFileToWorkAround;
		}
		File boardsSloeberTXT=new File(actualFileToLoad);
		if(boardsSloeberTXT.exists()) {
			//delete if outdated
			String firstLine = null;
			try(BufferedReader Buff = new BufferedReader(new FileReader(boardsSloeberTXT));) {
				firstLine = Buff.readLine();
			} catch (Exception e) {
				//ignore and delete the file
			} 
			if(!FIRST_SLOEBER_WORKAROUND_LINE.equals(firstLine)) {
				boardsSloeberTXT.delete();
			}
		}
		if(!boardsSloeberTXT.exists()) {
			if (requestedFileToWorkAround.exists()) {
				try {
					if (SystemUtils.IS_OS_WINDOWS) {
						String boardsTXT = FIRST_SLOEBER_WORKAROUND_LINE+"\n";
						boardsTXT += FileUtils.readFileToString(requestedFileToWorkAround, Charset.defaultCharset());
						boardsTXT = boardsTXT.replace("\r\n", "\n");
						
						// replace FI '-DUSB_PRODUCT={build.usb_product}' with
						// "-DUSB_PRODUCT=\"{build.usb_product}\""
						//also needed in boards.txt for boards specific settings
						boardsTXT = boardsTXT.replaceAll("\\'-D(\\S+)=\\{(\\S+)}\\'", "\"-D$1=\\\\\"{$2}\\\\\"\"");


						// replace FI circuitplay32u4cat.build.usb_manufacturer="Adafruit"
						// with circuitplay32u4cat.build.usb_manufacturer=Adafruit
						boardsTXT = boardsTXT.replaceAll("(\\S+\\.build\\.usb\\S+)=\\\"(.+)\\\"", "$1=$2");
						
						//quoting fixes for embedutils
						boardsTXT = boardsTXT.replaceAll("\"?(-DMBEDTLS_\\S+)=\\\\?\"(mbedtls\\S+)\"\\\\?\"*", 	"\"$1=\\\\\"$2\\\\\"\"");


						FileUtils.write(boardsSloeberTXT, boardsTXT, Charset.defaultCharset());
					}
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		return boardsSloeberTXT;
	}

}
