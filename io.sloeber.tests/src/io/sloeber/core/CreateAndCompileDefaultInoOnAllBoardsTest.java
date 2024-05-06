package io.sloeber.core;

import static org.junit.Assert.*;
import static io.sloeber.core.api.Const.*;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Stream;

import org.eclipse.core.runtime.IPath;
import org.junit.Assume;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import io.sloeber.core.api.BoardDescription;
import io.sloeber.core.api.BoardsManager;
import io.sloeber.core.api.CodeDescription;
import io.sloeber.core.api.LibraryManager;
import io.sloeber.core.api.Preferences;

@SuppressWarnings("nls")
public class CreateAndCompileDefaultInoOnAllBoardsTest {

    // use the boolean below to avoid downloading and installation
    private static final boolean removeAllinstallationInfoAtStartup = false;
    private static final boolean skipPlatformInstallation = false;
    private static final boolean apply_known_work_Arounds = true;
    private static final boolean closeFailedProjects = false;

    private int myTotalFails = 0;
    private static int maxFails = 50;
    private static int mySkipTestsAtStart = 0;

	private static final String[] packageUrlsToIgnoreonAllOSes = {
            // There is a newer version
            "https://raw.githubusercontent.com/ElektorLabs/arduino/master/package_elektor-labs.com_ide-1.6.5_index.json",
            // Third party url implies this is outdated (it also doesn't work)
            "http://downloads.arduino.cc/packages/package_mkr1000_index.json",
            // http 403 download error
            "https://git.oschina.net/dfrobot/FireBeetle-ESP32/raw/master/package_esp32_index.json",
            "http://www.arducam.com/downloads/ESP8266_UNO/package_ArduCAM_index.json",
            "http://www.arducam.com/downloads/ESP32_UNO/package_ArduCAM_ESP32S_UNO_index.json",
            // moved their stuff but didn't bother to update arduino site
            "http://www.dwengo.org/sites/default/files/package_dwengo.org_dwenguino_index.json",
            // uses extra windows in the tolpath and even after that fix it still fails the
            // build
            "https://github.com/sonydevworld/spresense-arduino-compatible/releases/download/generic/package_spresense_index.json",
            // web site not responding
            "http://zoubworld.com/~zoubworld_Arduino/files/Release/package_Zoubworld_index.json",
            // discontinued
            "http://rfduino.com/package_rfduino_index.json",
            "https://redbearlab.github.io/arduino/package_redbear_index.json",
            "https://redbearlab.github.io/arduino/package_redbearlab_index.json",
            "http://drazzy.com/package_drazzy.com_index.json",
            "http://download.labs.mediatek.com/package_mtk_linkit_smart_7688_index.json",
            "http://download.labs.mediatek.com/package_mtk_linkit_7697_index.json",
            "http://download.labs.mediatek.com/package_mtk_linkit_index.json",
            "http://panstamp.org/arduino/package_panstamp_index.json",
            "https://raw.githubusercontent.com/Seeed-Studio/Seeeduino-Boards/master/package_seeeduino_index.json",
            "http://rig.reka.com.my/package_rig_index.json",
            "http://adelino.cc/package_adelino_index.json",
            "https://raw.githubusercontent.com/ioteamit/smarteverything-core/master/package_arrow_index.json",
            "http://digistump.com/package_digistump_index.json",

            // confirmed 2020 03 09 version 25 12 17
            "https://raw.githubusercontent.com/avandalen/SAM15x15/master/package_avdweb_nl_index.json",

            // uses busybox on windows so command line issues and on Linux the all in one archive build fails
            "https://github.com/tenbaht/sduino/raw/master/package_sduino_stm8_index.json", };
    private static final String[] packageUrlsToIgnoreonWindows = {
            // following packages did not work in the arduino ide on windows at last test
            // confirmed 220 03 09 was version 1.0
            "https://ardhat.github.io/ardhat-board-support/arduino/package_ardhat_index.json",

    };
    private static final String[] packageUrlsToIgnoreOnLinux = {
            // following packages did not work in the arduino ide on windows at last test
            "https://ardhat.github.io/ardhat-board-support/arduino/package_ardhat_index.json",
            // A ( is used in a define in the compile command and that seems to be a issue
            "https://raw.githubusercontent.com/NicoHood/HoodLoader2/master/package_NicoHood_HoodLoader2_index.json",
            // Arduinoide says npt supported on this os
            // Sloeber misses a tool
            "http://download.labs.mediatek.com/package_mtk_linkit_index.json",
            // command contains ( and compiler complains; works in arduino IDE
            "https://raw.githubusercontent.com/VSChina/azureiotdevkit_tools/master/package_azureboard_index.json"

    };
    private static final String[] packageUrlsToIgnoreOnMac = {

    };
    private static final String[] boardsToIgnoreOnAllOses = {
            // Variant folder non existing but using core that references variant.h
            // confirmed 2020 03 09 version 4.0.0
            "SmartEverything Bee (Native USB Port)",

            // issue #1152 (confirmed 2020 03 07 )
            "Engimusing EFM32WG840", "Engimusing EFM32WG842", "Engimusing EFM32WG842F64",

            "D-duino-32", // confirmed 2020 03 09
            "SparkFun Blynk Board", // (confirmed 2020 03 07 )
            "ATtiny167 @ 8 MHz  (internal oscillator; BOD enabled)", // (confirmed 2020 03 07 )
            "Optiboot ATtiny167 @ 20 MHz  (external oscillator; BOD enabled)", // (confirmed 2020 03 07 )
            "Rock Solid XMega 128A", // (confirmed 2020 03 07 )

            "ATXmega128A1U", // (confirmed 2020 03 07 )
            "ATXMega128A1", // (confirmed 2020 03 07 )
            // this board does not use gcc so there is no added value in using Sloeber
            "Windows 10 IoT Core",

            "256RFR2ZBITXPRO", // confirmed 2020 03 09
            "256RFR2ZBIT", // confirmed 2020 03 09

            "Maple (RET6)", // confirmed failing in arduino IDE 2020 05 30
            "Generic STM32F103Z series",// confirmed failing in arduino IDE 2020 05 30

    };
    private static final String[] boardsToIgnoreOnWindows = {

            // does not work in, arduino ide on windows

    };

    private static final String[] boardsToIgnoreOnLinux = {
            // The installation script fail in Arduino IDE and so does
            // the verify action.
            // Sloeber does not support the install stuff and the verify fails as well
            "Intel® Galileo", "Intel® Galileo Gen2", "Intel® Edison",
            // uses cmd /c in recipes
            "STM32 Discovery F407", "Blackpill STM32F401CCU6", "STM32 Discovery F411E", "Generic STM32F407V series",
            "Generic STM32F407V mini series", "Seeed Arch Max 1.1",

            // folder casing problem
            "LilyPad LilyMini",

            //linkerscript file name casing problem; github code is old
            "DFRduino M0 MainBoard",

    };
    private static final String[] packageUrlsFromThirthPartyWebPage = {
            /*
             * the list below is made as follows extract all url's containing .json from
             * https://github.com/arduino/Arduino/wiki/Unofficial-list-of-3rd-party-boards-
             * support-urls replace http with "http replace .json with .json",
             *
             * remove the error line
             * "https://github.com/arduino/Arduino/wiki/Arduino-IDE-1.6.x-package_index.json"
             * ,-format-specification
             */
            "http://clkdiv8.com/download/package_clkdiv8_index.json",
            "http://dan.drown.org/stm32duino/package_STM32duino_index.json",
            "http://digistump.com/package_digistump_index.json",
            "http://dl.sipeed.com/MAIX/Maixduino/package_Maixduino_k210_dl_cdn_index.json",
            "http://dl.sipeed.com/MAIX/Maixduino/package_Maixduino_k210_index.json",
            "http://download.labs.mediatek.com/package_mtk_linkit_7697_index.json",
            "http://download.labs.mediatek.com/package_mtk_linkit_index.json",
            "http://download.labs.mediatek.com/package_mtk_linkit_smart_7688_index.json",
            "http://downloads.arduino.cc/packages/package_mkr1000_index.json",
            "http://downloads.konekt.io/arduino/package_konekt_index.json",
            "http://downloads.sodaq.net/package_sodaq_index.json",
            "http://downloads.sodaq.net/package_sodaq_samd_index.json",
            "http://drazzy.com/package_drazzy.com_index.json",
            "http://fpgalibre.sf.net/Lattuino/package_lattuino_index.json",
            "http://hidnseek.github.io/hidnseek/package_hidnseek_boot_index.json",
            "http://library.radino.cc/Arduino_1_8/package_radino_radino32_index.json",
            "http://navspark.mybigcommerce.com/content/package_navspark_index.json",
            "http://panstamp.org/arduino/package_panstamp_index.json", "http://rfduino.com/package_rfduino_index.json",
            "http://rig.reka.com.my/package_rig_index.json",
            "http://talk2arduino.wisen.com.au/master/package_talk2.wisen.com_index.json",
            "http://www.arducam.com/downloads/ESP32_UNO/package_ArduCAM_ESP32S_UNO_index.json",
            "http://www.arducam.com/downloads/ESP8266_UNO/package_ArduCAM_index.json",
            "http://www.dwengo.org/sites/default/files/package_dwengo.org_dwenguino_index.json",
            "http://www.leonardomiliani.com/repository/package_leonardomiliani.com_index.json",
            "http://zoubworld.com/~zoubworld_Arduino/files/Release/package_Zoubworld_index.json",
            "https://adafruit.github.io/arduino-board-index/package_adafruit_index.json",
            "https://ambasat.com/boards/package_ambasat-1.com_index.json",
            "https://ardhat.github.io/ardhat-board-support/arduino/package_ardhat_index.json",
            "https://arduboy.github.io/board-support/package_arduboy_index.json",
            "https://arduino.esp8266.com/stable/package_esp8266com_index.json",
            "https://dl.espressif.com/dl/package_esp32_index.json",
            "https://engimusing.github.io/arduinoIDE/package_engimusing_modules_index.json",
            "https://git.oschina.net/dfrobot/FireBeetle-ESP32/raw/master/package_esp32_index.json",
            "https://github.com/Ameba8195/Arduino/raw/master/release/package_realtek.com_ameba_index.json",
            "https://github.com/Infineon/Assets/releases/download/current/package_infineon_index.json",
            "https://github.com/IntoRobot/IntoRobotPackages-ArduinoIDE/releases/download/1.0.0/package_intorobot_index.json",
            "https://github.com/XMegaForArduino/IDE/raw/master/package_XMegaForArduino_index.json",
            "https://github.com/chipKIT32/chipKIT-core/raw/master/package_chipkit_index.json",
            "https://github.com/ms-iot/iot-utilities/raw/master/IotCoreAppDeployment/ArduinoIde/package_iotcore_ide-1.6.6_index.json",
            "https://github.com/sonydevworld/spresense-arduino-compatible/releases/download/generic/package_spresense_index.json",
            "https://github.com/tenbaht/sduino/raw/master/package_sduino_stm8_index.json",
            "https://lowpowerlab.github.io/MoteinoCore/package_LowPowerLab_index.json",
            "https://macchina.cc/package_macchina_index.json",
            "https://mcudude.github.io/MegaCore/package_MCUdude_MegaCore_index.json",
            "https://mcudude.github.io/MicroCore/package_MCUdude_MicroCore_index.json",
            "https://mcudude.github.io/MightyCore/package_MCUdude_MightyCore_index.json",
            "https://mcudude.github.io/MiniCore/package_MCUdude_MiniCore_index.json",
            "https://mesom.de/atflash/package_atflash_index.json",
            "https://nekuneko.github.io/arduino-board-index/package_nekuneko_index.json",
            "https://openpanzerproject.github.io/OpenPanzerBoards/package_openpanzer_index.json",
            "https://per1234.github.io/Ariadne-Bootloader/package_codebendercc_ariadne-bootloader_index.json",
            "https://per1234.github.io/wirino/package_per1234_wirino_index.json",
            "https://raw.githubusercontent.com/AloriumTechnology/Arduino_Boards/master/package_aloriumtech_index.json",
            "https://raw.githubusercontent.com/CytronTechnologies/Cytron-Arduino-URL/master/package_cytron_index.json",
            "https://raw.githubusercontent.com/DFRobot/DFRobotDuinoBoard/master/package_dfrobot_index.json",
            "https://raw.githubusercontent.com/DFRobot/DFRobotDuinoBoard/master/package_dfrobot_iot_mainboard.json",
            "https://raw.githubusercontent.com/ElektorLabs/arduino/master/package_elektor-labs.com_ide-1.6.5_index.json",
            "https://raw.githubusercontent.com/ElektorLabs/arduino/master/package_elektor-labs.com_ide-1.6.6_index.json",
            "https://raw.githubusercontent.com/FemtoCow/ATTinyCore/master/Downloads/package_femtocow_attiny_index.json",
            "https://raw.githubusercontent.com/Lauszus/Sanguino/master/package_lauszus_sanguino_index.json",
            "https://raw.githubusercontent.com/MauricioJancic/Elemon/master/package_Elemon_index.json",
            "https://raw.githubusercontent.com/MaximIntegratedMicros/arduino-collateral/master/package_maxim_index.json",
            "https://raw.githubusercontent.com/NicoHood/HoodLoader2/master/package_NicoHood_HoodLoader2_index.json",
            "https://raw.githubusercontent.com/OLIMEX/Arduino_configurations/master/AVR/package_olimex_avr_index.json",
            "https://raw.githubusercontent.com/OLIMEX/Arduino_configurations/master/PIC/package_olimex_pic_index.json",
            "https://raw.githubusercontent.com/OLIMEX/Arduino_configurations/master/STM/package_olimex_stm_index.json",
            "https://raw.githubusercontent.com/Quirkbot/QuirkbotArduinoHardware/master/package_quirkbot.com_index.json",
            "https://raw.githubusercontent.com/ROBOTIS-GIT/OpenCR/master/arduino/opencr_release/package_opencr_index.json",
            "https://raw.githubusercontent.com/RiddleAndCode/RnCAtmega256RFR2/master/Board_Manager/package_rnc_index.json",
            "https://raw.githubusercontent.com/RobotCing/Cing/master/Software/Packages/package_RobotCing_index.json",
            "https://raw.githubusercontent.com/Seeed-Studio/Seeeduino-Boards/master/package_seeeduino_index.json",
            "https://raw.githubusercontent.com/TKJElectronics/Balanduino/master/package_tkj_balanduino_index.json",
            "https://raw.githubusercontent.com/ThamesValleyReprapUserGroup/Beta-TVRRUG-Mendel90/master/Added-Documents/OMC/package_omc_index.json",
            "https://raw.githubusercontent.com/VSChina/azureiotdevkit_tools/master/package_azureboard_index.json",
            "https://raw.githubusercontent.com/akafugu/akafugu_core/master/package_akafugu_index.json",
            "https://raw.githubusercontent.com/arachnidlabs/arachnidlabs-boards/master/package_arachnidlabs.com_boards_index.json",
            "https://raw.githubusercontent.com/avandalen/SAM15x15/master/package_avdweb_nl_index.json",
            "https://raw.githubusercontent.com/carlosefr/atmega/master/package_carlosefr_atmega_index.json",
            "https://raw.githubusercontent.com/damellis/attiny/ide-1.6.x-boards-manager/package_damellis_attiny_index.json",
            "https://raw.githubusercontent.com/eerimoq/simba-releases/master/arduino/avr/package_simba_avr_index.json",
            "https://raw.githubusercontent.com/eerimoq/simba-releases/master/arduino/esp/package_simba_esp_index.json",
            "https://raw.githubusercontent.com/eerimoq/simba-releases/master/arduino/sam/package_simba_sam_index.json",
            "https://raw.githubusercontent.com/eightdog/laika_arduino/master/IDE_Board_Manager/package_project_laika.com_index.json",
            "https://raw.githubusercontent.com/facts-engineering/facts-engineering.github.io/master/package_productivity-P1AM-boardmanagermodule_index.json",
            "https://raw.githubusercontent.com/feilipu/feilipu.github.io/master/package_goldilocks_index.json",
            "https://raw.githubusercontent.com/geolink/opentracker-arduino-board/master/package_opentracker_index.json",
            "https://raw.githubusercontent.com/harbaum/ftduino/master/package_ftduino_index.json",
            "https://raw.githubusercontent.com/ioteamit/ioteam-arduino-core/master/package_ioteam_index.json",
            "https://raw.githubusercontent.com/ioteamit/smarteverything-core/master/package_arrow_index.json",
            "https://raw.githubusercontent.com/mikaelpatel/Cosa/master/package_cosa_index.json",
            "https://raw.githubusercontent.com/oshlab/Breadboard-Arduino/master/avr/boardsmanager/package_oshlab_breadboard_index.json",
            "https://raw.githubusercontent.com/sanu-krishnan/ebot-arduino-core/master/package_ebots.cc_index.json",
            "https://raw.githubusercontent.com/sparkfun/Arduino_Boards/master/IDE_Board_Manager/package_sparkfun_index.json",
            "https://raw.githubusercontent.com/stm32duino/BoardManagerFiles/master/STM32/package_stm_index.json",
            "https://raw.githubusercontent.com/udif/ITEADSW_Iteaduino-Lite-HSP/master/package/package_iteaduino_lite_index.json",
            "https://rawgit.com/hunianhang/nufront_arduino_json/master/package_tl7788_index.json",
            "https://redbearlab.github.io/arduino/package_redbear_index.json",
            "https://redbearlab.github.io/arduino/package_redbearlab_index.json",
            "https://resources.canique.com/ide/package_canique_index.json",
            "https://sandeepmistry.github.io/arduino-nRF5/package_nRF5_boards_index.json",
            "https://thomasonw.github.io/ATmegaxxM1-C1/package_thomasonw_ATmegaxxM1-C1_index.json",
            "https://udooboard.github.io/arduino-board-package/package_udoo_index.json",
            "https://www.mattairtech.com/software/arduino/package_MattairTech_index.json",
            "https://zevero.github.io/avr_boot/package_zevero_avr_boot_index.json",
            "http://adelino.cc/package_adelino_index.json",

    };

    public static Stream<Arguments> allBoards() {
        Shared.setCloseFailedProjects(closeFailedProjects);
        // make sure all plugin installation is done
        Shared.waitForAllJobsToFinish();
        // build the Arduino way
        Preferences.setUseArduinoToolSelection(true);
        Preferences.setUseBonjour(false);
        installAdditionalBoards();

        List<BoardDescription> boards = new ArrayList<>();
        for (File curBoardFile : BoardsManager.getAllBoardsFiles()) {
            System.out.println("Adding boards of " + curBoardFile.toString());
            boards.addAll(BoardDescription.makeBoardDescriptors(curBoardFile));
        }


        HashSet<String> boardsToIgnoreList = new HashSet<>(Arrays.asList(boardsToIgnoreOnAllOses));

        if (isLinux) {
            boardsToIgnoreList.addAll(Arrays.asList(boardsToIgnoreOnLinux));
        }
        if (isWindows) {
            boardsToIgnoreList.addAll(Arrays.asList(boardsToIgnoreOnWindows));
        }
        List<Arguments> ret = new LinkedList<>();
        for (BoardDescription curBoard : boards) {
            if (!boardsToIgnoreList.contains(curBoard.getBoardName())) {
            	// to avoid warnings set the upload port to some value
            	curBoard.setUploadPort("none");
            	ret.add(Arguments.of(  curBoard));
            }
        }
		return ret.stream();
    }

    /*
     * In new installations (of the Sloeber development environment) the installer
     * job will trigger downloads and uncompression jobs These must have finished
     * before we can start testing
     *
     * This method will take a long time "Don't panic before 60 minutes are over".
     * You can check the [eclipseInstall]/arduinoPlugin/packages folder for progress
     */
    public static void installAdditionalBoards() {
        if (removeAllinstallationInfoAtStartup) {
            BoardsManager.removeAllInstalledPlatforms();
            LibraryManager.unInstallAllLibs();
        }

        HashSet<String> toAddList = new HashSet<>(Arrays.asList(packageUrlsFromThirthPartyWebPage));
        toAddList.addAll(Arrays.asList(BoardsManager.getJsonURLList()));
        toAddList.removeAll(Arrays.asList(packageUrlsToIgnoreonAllOSes));
        if (isWindows) {
            toAddList.removeAll(Arrays.asList(packageUrlsToIgnoreonWindows));
        }
        if (isLinux) {
            toAddList.removeAll(Arrays.asList(packageUrlsToIgnoreOnLinux));
        }
        if (isMac) {
            toAddList.removeAll(Arrays.asList(packageUrlsToIgnoreOnMac));
        }
        BoardsManager.setPackageURLs(toAddList, true);

        if (!skipPlatformInstallation) {
            BoardsManager.installAllLatestPlatforms();
            // PackageManager.installsubsetOfLatestPlatforms(0,5);
            // PackageManager.onlyKeepLatestPlatforms();
        }

        if (apply_known_work_Arounds) {
            Shared.applyKnownWorkArounds();
        }
        Shared.waitForAllJobsToFinish();
    }

	@ParameterizedTest
	@MethodSource("allBoards")
    public void testBoard(BoardDescription board) throws Exception {
        Shared.buildCounter++;
        Assume.assumeTrue("Skipping first " + mySkipTestsAtStart + " tests", Shared.buildCounter >= mySkipTestsAtStart);
        Assume.assumeTrue("To many fails. Stopping test", myTotalFails < maxFails);

        IPath templateFolder = Shared.getTemplateFolder("CreateAndCompileTest");
        if (!Shared.BuildAndVerify(board, CodeDescription.createCustomTemplate(templateFolder), null,
        		Shared.buildCounter)) {
            myTotalFails++;
            fail(Shared.getLastFailMessage());
        }

    }

}
