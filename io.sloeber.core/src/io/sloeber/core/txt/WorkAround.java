package io.sloeber.core.txt;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.SystemUtils;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;

import io.sloeber.core.Activator;
import io.sloeber.core.common.Common;
import io.sloeber.core.common.Const;
import io.sloeber.core.managers.ArduinoPlatform;
import io.sloeber.core.tools.FileModifiers;
import io.sloeber.core.tools.Version;

/**
 * A class to apply workarounds to installed packages. Workaround are done after
 * installation and at usage of boards.txt or platform.txt file currently there
 * are noworkarounds for programmers.trx
 * 
 * The first line of the worked around files contain a key. A newer version of
 * sloeber that has a different workaround should change the key. This way the
 * worked around files can be persisted and updated when needed
 * 
 * @author jan
 *
 */
@SuppressWarnings("nls")
public class WorkAround extends Const {
    // Each time this class is touched consider changing the String below to enforce
    // updates
    // for debugging I added the system time so the files get refresed at each run
    private static final String FIRST_SLOEBER_WORKAROUND_LINE = "#Sloeber created workaound file V1.02.test 20 ";
    // + String.valueOf(System.currentTimeMillis());

    /**
     * workarounds done at installation time. I try to keep those at a minimum but
     * none platform.txt and boards.txt workarounds need to be done during install
     * time
     * 
     * @param platform
     */
    static public void applyKnownWorkArounds(ArduinoPlatform platform) {

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
     * Get the a workaround boards.txt and if needed create/update it This method
     * takes a boards.txt file and returns a worked around file. The worked around
     * file is persisted on disk for easy debugging/ reduce code impact and
     * performance.
     * 
     * @param requestedFileToWorkAround
     *            the board.txt that you want to process
     * 
     * @return the worked around file or requestedFileToWorkAround if it does not
     *         exist or an error occurred
     */
    static File MakeBoardsSloeberTxt(File requestedFileToWorkAround) {
        if (!requestedFileToWorkAround.exists()) {
            return requestedFileToWorkAround;
        }
        String inFile = requestedFileToWorkAround.toString();
        String actualFileToLoad = inFile.replace(BOARDS_FILE_NAME, "boards.sloeber.txt");
        if (inFile.equals(actualFileToLoad)) {
            Common.log(new Status(IStatus.ERROR, Activator.getId(),
                    "Boards.txt file is not recognized " + requestedFileToWorkAround.toString()));
            return requestedFileToWorkAround;
        }
        File boardsSloeberTXT = new File(actualFileToLoad);
        deleteIfOutdated(boardsSloeberTXT);

        if (boardsSloeberTXT.exists()) {
            // if boardsSloeberTXT still exists it is up to date
            return boardsSloeberTXT;
        }

        // generate the workaround file
        try {
            String boardsTXT = FileUtils.readFileToString(requestedFileToWorkAround, Charset.defaultCharset());

            boardsTXT = boardsApplyWorkArounds(boardsTXT);

            boardsTXT = FIRST_SLOEBER_WORKAROUND_LINE + "\n" + boardsTXT;
            FileUtils.write(boardsSloeberTXT, boardsTXT, Charset.defaultCharset());
        } catch (IOException e) {
            Common.log(new Status(IStatus.WARNING, Activator.getId(),
                    "Failed to apply work arounds to " + requestedFileToWorkAround.toString(), e));
            return requestedFileToWorkAround;
        }

        return boardsSloeberTXT;
    }

    public static String boardsApplyWorkArounds(String inBoardsTXT) {
        String boardsTXT = inBoardsTXT.replace("\r\n", "\n");
        // because I search for spaces around string as delimiters I add a space at the
        // end of the line
        boardsTXT = boardsTXT.replace("\n", " \n");
        boardsTXT = solveOSStuff(boardsTXT);

        String correctMAN = " \"-DUSB_MANUFACTURER=\\\"{build.usb_manufacturer}\\\"\" ";
        String correctPROD = " \"-DUSB_PRODUCT=\\\"{build.usb_product}\\\"\" ";
        String correctBOARD = " \"-DARDUINO_BOARD=\\\"{build.board}\\\"\" ";
        String correctUSBSERIAL = " \"-DUSB_SERIAL=\\\"{build.usb_serial}\\\"\" ";

        // replace FI circuitplay32u4cat.build.usb_manufacturer="Adafruit"
        // with circuitplay32u4cat.build.usb_manufacturer=Adafruit
        boardsTXT = boardsTXT.replaceAll("(\\S+\\.build\\.usb\\S+)=\\\"(.+)\\\"", "$1=$2");

        // quoting fixes for embedutils
        // ['\"]?(-DMBEDTLS_\S+)=\\?"(mbedtls\S+?)\\?\"["']? \"$1=\\\"$2\\\"\"
        boardsTXT = boardsTXT.replaceAll(" ['\\\"]?(-DMBEDTLS_\\S+)=\\\\?\"(mbedtls\\S+?)\\\\?\\\"[\"']? ",
                " \\\"$1=\\\\\\\"$2\\\\\\\"\\\" ");

        // some providers put -DUSB_PRODUCT={build.usb_product} in boards.txt
        boardsTXT = boardsTXT.replace(" \"-DUSB_MANUFACTURER={build.usb_manufacturer}\" ", correctMAN);
        boardsTXT = boardsTXT.replace(" \"-DUSB_PRODUCT={build.usb_product}\" ", correctPROD);
        boardsTXT = boardsTXT.replace(" -DARDUINO_BOARD=\"{build.board}\" ", correctBOARD);

        boardsTXT = boardsTXT.replace(" '-DUSB_MANUFACTURER={build.usb_manufacturer}' ", correctMAN);
        boardsTXT = boardsTXT.replace(" '-DUSB_PRODUCT={build.usb_product}' ", correctPROD);
        boardsTXT = boardsTXT.replace(" '-DARDUINO_BOARD=\"{build.board}' ", correctBOARD);
        boardsTXT = boardsTXT.replace(" '-DUSB_SERIAL={build.usb_serial}' ", correctUSBSERIAL);
        boardsTXT = boardsTXT.replace("{", "${");
        return boardsTXT;
    }

    /**
     * * Get the a workaround platform.txt and if needed create/update it This
     * method takes a platform.txt file and returns a worked around file. The worked
     * around file is persisted on disk for easy debugging/ reduce code impact and
     * performance.
     * 
     * 
     * @param requestedFileToWorkAround
     *            the platform.txt you want to process
     * 
     * @return the worked around file or requestedFileToWorkAround if it does not
     *         exist or an error occurred
     */
    static File MakePlatformSloeberTXT(File requestedFileToWorkAround) {

        if (!requestedFileToWorkAround.exists()) {
            return requestedFileToWorkAround;
        }
        String inFile = requestedFileToWorkAround.toString();
        String actualFileToLoad = inFile.replace(PLATFORM_FILE_NAME, "platform.sloeber.txt");
        if (inFile.equals(actualFileToLoad)) {
            Common.log(new Status(IStatus.ERROR, Activator.getId(),
                    "platform.txt file is not recognized " + requestedFileToWorkAround.toString()));
            return requestedFileToWorkAround;
        }
        File platformSloeberTXT = new File(actualFileToLoad);
        deleteIfOutdated(platformSloeberTXT);

        if (platformSloeberTXT.exists()) {
            // if the worked around file still exists it is up to date
            return platformSloeberTXT;
        }

        // generate the workaround file
        try {

            String platformTXT = FileUtils.readFileToString(requestedFileToWorkAround, Charset.defaultCharset());

            platformTXT = platformApplyWorkArounds(platformTXT, requestedFileToWorkAround);

            platformTXT = FIRST_SLOEBER_WORKAROUND_LINE + "\n" + platformTXT;
            FileUtils.write(platformSloeberTXT, platformTXT, Charset.defaultCharset());
        } catch (IOException e) {
            Common.log(new Status(IStatus.WARNING, Activator.getId(),
                    "Failed to apply work arounds to " + requestedFileToWorkAround.toString(), e));
            return requestedFileToWorkAround;
        }

        return platformSloeberTXT;
    }

    /**
     * Method that does the actual conversion of the provided platform.txt to the
     * platform.sloeber.txt without eh house keeping. Basically this produces the
     * content of platform.sloeber.txt after the header
     * 
     * @param inPlatformTxt
     *            the content of the platform.txt
     * @param requestedFileToWorkAround
     *            the fqn filename of the platform.txt
     * @return the "worked around" content of platform.txtx
     */
    public static String platformApplyWorkArounds(String inPlatformTxt, File requestedFileToWorkAround) {
        String platformTXT = inPlatformTxt.replace("\r\n", "\n");
        // remove spaces before =
        platformTXT = platformTXT.replaceAll("(?m)^(\\S*)\\s*=", "$1=");

        platformTXT = solveOSStuff(platformTXT);

        platformTXT = platformApplyReleaseWorkArounds(platformTXT, requestedFileToWorkAround);

        platformTXT = platformApplyCustomWorkArounds(platformTXT);

        platformTXT = platformApplyStandardWorkArounds(platformTXT);

        platformTXT = platformTXT.replace("{", "${");
        // Arduino zero openocd script uses { as parameter delimiter for program
        platformTXT = platformTXT.replace("program ${${", "program {${");
        return platformTXT;
    }

    /**
     * This method applies workarounds for specific platforms
     * 
     * @param inPlatformTxt
     * @return
     */
    private static String platformApplyCustomWorkArounds(String inPlatformTxt) {
        String platformTXT = inPlatformTxt;
        // workaround for infineon arm v1.4.0 overwriting the default to a wrong value
        platformTXT = platformTXT.replace("\nbuild.core.path", "\n#line removed by Sloeber build.core.path");

        // workaround for jantje PC
        platformTXT = platformTXT.replace("{runtime.tools.mingw.path}/bin/", "{runtime.tools.MinGW.path}/bin/");

        // for adafruit nfr
        platformTXT = platformTXT.replace(" -DARDUINO_BSP_VERSION=\"{version}\" ",
                " \"-DARDUINO_BSP_VERSION=\\\"{version}\\\"\" ");
        platformTXT = platformTXT.replace(" '-DARDUINO_BSP_VERSION=\"{version}\"' ",
                " \"-DARDUINO_BSP_VERSION=\\\"{version}\\\"\" ");

        // for STM32
        platformTXT = platformTXT.replace(" -DBOARD_NAME=\"{build.board}\"", " \"-DBOARD_NAME=\\\"{build.board}\\\"\"");

        return platformTXT;
    }

    /**
     * This method applies workaround to specific releases of specific platforms
     * 
     * @param inPlatformTxt
     *            The content of the platform.txt
     * 
     * @param requestedFileToWorkAround
     *            The path of the platform.txt so we can validate for
     *            provider/architecture and versions
     * @return the worked around platform.txt
     */
    private static String platformApplyReleaseWorkArounds(String inPlatformTxt, File requestedFileToWorkAround) {
        String platformTXT = inPlatformTxt;
        try { // https://github.com/Sloeber/arduino-eclipse-plugin/issues/1182#
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
        } catch (Exception e) {
            // ignore
        }
        return platformTXT;
    }

    /**
     * This method applies the default workarounds
     * 
     * @param inPlatformTxt
     * @return
     */
    private static String platformApplyStandardWorkArounds(String inPlatformTxt) {
        String platformTXT = inPlatformTxt;
        String searchPlatformTXT = "\n" + inPlatformTxt;

        // the fix below seems no longer needed but is still on august 2021
        // Arduino treats core differently so we need to change the location of directly
        // referenced files this manifests only in the combine recipe
        String inCombineRecipe = findLineStartingWith(platformTXT, "recipe.c.combine.pattern");
        if (null != inCombineRecipe) {
            String outCombineRecipe = inCombineRecipe.replaceAll("(\\{build\\.path})(/core)?/sys", "$1/core/core/sys");
            platformTXT = platformTXT.replace(inCombineRecipe, outCombineRecipe);
        }

        // replace tools.x.y* {path}
        // by
        // tools.x.y* {tools.x.path}
        List<String> knownShortHands = List.of("cmd", "path", "cmd.path", "config.path");
        List<String> keepAsShortHands = List.of("upload.verify", "program.verify");
        Pattern tools_x_y_pattern = Pattern.compile("(?m)^tools\\.[^\\.]*.*=.*$");
        Matcher tools_x_y_macher = tools_x_y_pattern.matcher(platformTXT);

        Map<String, String> replaceInfo = new TreeMap<>();
        while (tools_x_y_macher.find()) {
            String origLine = platformTXT.substring(tools_x_y_macher.start(), tools_x_y_macher.end());
            String prefix = origLine.substring(0, origLine.indexOf('.', 7) + 1);
            String workedAroundLine = origLine;
            Pattern variablePattern = Pattern.compile("\\{[^}]*}");
            Matcher variableMacher = variablePattern.matcher(origLine);
            while (variableMacher.find()) {
                String origVar = origLine.substring(variableMacher.start() + 1, variableMacher.end() - 1);
                String newVar = prefix + origVar;
                if (keepAsShortHands.contains(origVar)) {
                    // ignore these
                } else {
                    if (knownShortHands.contains(origVar)) {
                        workedAroundLine = workedAroundLine.replace("{" + origVar + "}", "{" + newVar + "}");
                    } else if (searchPlatformTXT.contains("\n" + newVar + "=")) {
                        workedAroundLine = workedAroundLine.replace("{" + origVar + "}", "{" + newVar + "}");
                    }
                }
            }
            if (!origLine.equals(workedAroundLine)) {
                replaceInfo.put(origLine, workedAroundLine);
            }
        }
        for (Entry<String, String> replaceSet : replaceInfo.entrySet()) {
            platformTXT = platformTXT.replace(replaceSet.getKey(), replaceSet.getValue());
        }

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

        return platformTXT;

    }

    private static String findLineStartingWith(String text, String startOfLine) {
        int lineStartIndex = text.indexOf("\n" + startOfLine) + 1;
        if (lineStartIndex > 0) {
            int lineEndIndex = text.indexOf("\n", lineStartIndex) - 1;
            if (lineEndIndex > 0) {
                return text.substring(lineStartIndex, lineEndIndex);
            }
        }
        return null;
    }

    private static String findLineContaining(String text, String searchString) {
        int SearchStringIndex = text.indexOf(searchString) + 1;
        if (SearchStringIndex > 0) {
            int lineStartIndex = text.lastIndexOf("\n", SearchStringIndex) + 1;
            if (lineStartIndex > 0) {
                int lineEndIndex = text.indexOf("\n", lineStartIndex);
                if (lineEndIndex > 0) {
                    return text.substring(lineStartIndex, lineEndIndex);
                }
            }
        }
        return null;
    }

    private static String solveOSStuff(String inpuText) {
        String WINDOWSKEY = ".windows="; //$NON-NLS-1$
        String LINUXKEY = ".linux="; //$NON-NLS-1$
        String MACKEY = ".macosx="; //$NON-NLS-1$
        LinkedList<String> Otherosses = new LinkedList<>();

        String thisOSKey = null;
        // do not use platform as I run this in plain junit tests
        if (SystemUtils.IS_OS_WINDOWS) {
            thisOSKey = WINDOWSKEY;
            Otherosses.add(LINUXKEY);
            Otherosses.add(MACKEY);
        } else if (SystemUtils.IS_OS_LINUX) {
            thisOSKey = LINUXKEY;
            Otherosses.add(WINDOWSKEY);
            Otherosses.add(MACKEY);
        } else if (SystemUtils.IS_OS_MAC_OSX) {
            thisOSKey = MACKEY;
            Otherosses.add(WINDOWSKEY);
            Otherosses.add(LINUXKEY);
        } else {
            Common.log(new Status(IStatus.ERROR, Activator.getId(), "Failed to recognize the os you are using"));
            return inpuText;
        }

        String ret = inpuText;
        // remove the os keys
        for (String searchKey : Otherosses) {
            String notNeededosLine = findLineContaining(ret, searchKey);
            while (null != notNeededosLine) {
                ret = ret.replace(notNeededosLine, EMPTY);
                notNeededosLine = findLineContaining(ret, searchKey);
            }
        }
        String neededOSLine = findLineContaining(ret, thisOSKey);
        while (null != neededOSLine) {
            int keyIndex = neededOSLine.indexOf(thisOSKey);
            if (keyIndex < 0) {
                Common.log(new Status(IStatus.ERROR, Activator.getId(), "Error processing txt file: " + neededOSLine));
                neededOSLine = null;
            } else {
                String genericKey = neededOSLine.substring(0, keyIndex) + EQUAL;
                String foundKey = neededOSLine.substring(0, keyIndex + thisOSKey.length());
                String matchingGenericLine = findLineContaining(ret, genericKey);
                if (null != matchingGenericLine) {
                    ret = ret.replace(matchingGenericLine, EMPTY);
                }
                ret = ret.replace(foundKey, genericKey);
                neededOSLine = findLineContaining(ret, thisOSKey);
            }
        }

        return ret;
    }

    /**
     * * Get the a workaround programmers.txt and if needed create/update it This
     * method takes a programmers.txt file and returns a worked around file. The
     * worked around file is persisted on disk for easy debugging/ reduce code
     * impact and performance.
     * 
     * 
     * @param requestedFileToWorkAround
     *            the programmers.txt you want to process
     * 
     * @return the worked around file or requestedFileToWorkAround if it does not
     *         exist or an error occurred
     */
    public static File MakeProgrammersSloeberTXT(File requestedFileToWorkAround) {

        if (!requestedFileToWorkAround.exists()) {
            return requestedFileToWorkAround;
        }

        String inFile = requestedFileToWorkAround.toString();
        String actualFileToLoad = inFile.replace("programmers.txt", "programmers.sloeber.txt");
        if (inFile.equals(actualFileToLoad)) {
            Common.log(new Status(IStatus.ERROR, Activator.getId(),
                    "programmers.txt file is not recognized " + requestedFileToWorkAround.toString()));
            return requestedFileToWorkAround;
        }
        File actualProgrammersTXT = new File(actualFileToLoad);
        deleteIfOutdated(actualProgrammersTXT);

        if (actualProgrammersTXT.exists()) {
            // if the worked around file still exists it is up tp date
            return actualProgrammersTXT;
        }

        // generate the workaround file
        try {

            String programmersTXT = FileUtils.readFileToString(requestedFileToWorkAround, Charset.defaultCharset());
            programmersTXT = programmersApplyWorkArounds(programmersTXT);

            programmersTXT = FIRST_SLOEBER_WORKAROUND_LINE + "\n" + programmersTXT;
            FileUtils.write(actualProgrammersTXT, programmersTXT, Charset.defaultCharset());
        } catch (IOException e) {
            Common.log(new Status(IStatus.WARNING, Activator.getId(),
                    "Failed to apply work arounds to " + requestedFileToWorkAround.toString(), e));
            return requestedFileToWorkAround;
        }
        return actualProgrammersTXT;

    }

    public static String programmersApplyWorkArounds(String inProgrammersTXT) {
        String programmersTXT = inProgrammersTXT.replace("\r\n", "\n");

        programmersTXT = solveOSStuff(programmersTXT);

        programmersTXT = programmersTXT.replace("{", "${");

        return programmersTXT;
    }

    /**
     * If the sloeber.txt variant exists delete it if it is outdated
     * 
     * @param actualProgrammersTXT
     */
    private static void deleteIfOutdated(File actualProgrammersTXT) {
        if (actualProgrammersTXT.exists()) {
            // delete if outdated
            String firstLine = null;
            try (BufferedReader Buff = new BufferedReader(new FileReader(actualProgrammersTXT));) {
                firstLine = Buff.readLine();
            } catch (Exception e) {
                // ignore and delete the file
            }
            if (!FIRST_SLOEBER_WORKAROUND_LINE.trim().equals(firstLine.trim())) {
                actualProgrammersTXT.delete();
            }
        }
    }
}
