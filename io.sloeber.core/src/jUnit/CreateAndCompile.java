package jUnit;

import static org.junit.Assert.fail;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import io.sloeber.core.api.BoardDescriptor;
import io.sloeber.core.api.BoardsManager;
import io.sloeber.core.api.CodeDescriptor;
import io.sloeber.core.api.CompileOptions;
import io.sloeber.core.api.ConfigurationDescriptor;

@SuppressWarnings("nls")
@RunWith(Parameterized.class)
public class CreateAndCompile {
	// use the boolean below to avoid downloading and installation
	private static final boolean reinstall_boards_and_libraries = false;
	private BoardDescriptor mBoard;
	private static int mCounter = 0;

	public CreateAndCompile(BoardDescriptor board) {
		this.mBoard = board;
	}

	@SuppressWarnings("rawtypes")
	@Parameters(name = "{index}: {0} ")
	public static Collection boards() {
		installAdditionalBoards();
		List<BoardDescriptor> boards = new ArrayList<>();
		for (String curBoardFile : BoardsManager.getAllBoardsFiles()) {
			boards.addAll(BoardDescriptor.makeBoardDescriptors(new File(curBoardFile)));
		}
		// to avoid warnings set the upload port to some value
		for (BoardDescriptor curBoard : boards) {
			curBoard.setUploadPort("none");
		}
		return boards;
	}

	/*
	 * In new installations (of the Sloeber development environment) the installer
	 * job will trigger downloads These must have finished before we can start
	 * testing This will take a long time
	 */

	public static void installAdditionalBoards() {
		String[] packageUrlsToAdd = { "https://sandeepmistry.github.io/arduino-nRF5/package_nRF5_boards_index.json",
				"https://github.com/Infineon/Assets/releases/download/current/package_infineon_index.json",
				"http://arduino.esp8266.com/stable/package_esp8266com_index.json",
				"http://clkdiv8.com/download/package_clkdiv8_index.json",
				"http://digistump.com/package_digistump_index.json",
				"http://download.labs.mediatek.com/package_mtk_linkit_index.json",
				"http://download.labs.mediatek.com/package_mtk_linkit_smart_7688_index.json",
				"http://downloads.konekt.io/arduino/package_konekt_index.json",
				"http://downloads.sodaq.net/package_sodaq_index.json",
				"http://drazzy.com/package_drazzy.com_index.json",
				"http://hidnseek.github.io/hidnseek/package_hidnseek_boot_index.json",
				"http://navspark.mybigcommerce.com/content/package_navspark_index.json",
				"http://panstamp.org/arduino/package_panstamp_index.json",
				"http://rfduino.com/package_rfduino_index.json",
				"http://talk2arduino.wisen.com.au/master/package_talk2.wisen.com_index.json",
				"http://www.dwengo.org/sites/default/files/package_dwengo.org_dwenguino_index.json",
				"http://www.leonardomiliani.com/repository/package_leonardomiliani.com_index.json",
				"https://adafruit.github.io/arduino-board-index/package_adafruit_index.json",
				"https://ardhat.github.io/ardhat-board-support/arduino/package_ardhat_index.json",
				"https://arduboy.github.io/board-support/package_arduboy_index.json",
				"https://dl.dropboxusercontent.com/u/2807353/femtoCore/package_femtocow_attiny_index.json",
				"https://github.com/Ameba8195/Arduino/raw/master/release/package_realtek.com_ameba_index.json",
				"https://github.com/chipKIT32/chipKIT-core/raw/master/package_chipkit_index.json",
				"https://github.com/IntoRobot/IntoRobotPackages-ArduinoIDE/releases/download/1.0.0/package_intorobot_index.json",
				"https://github.com/ms-iot/iot-utilities/raw/master/IotCoreAppDeployment/ArduinoIde/package_iotcore_ide-1.6.6_index.json",
				"https://lowpowerlab.github.io/MoteinoCore/package_LowPowerLab_index.json",
				"https://mcudude.github.io/MegaCore/package_MCUdude_MegaCore_index.json",
				"https://mcudude.github.io/MicroCore/package_MCUdude_MicroCore_index.json",
				"https://mcudude.github.io/MightyCore/package_MCUdude_MightyCore_index.json",
				"https://mcudude.github.io/MiniCore/package_MCUdude_MiniCore_index.json",
				"https://per1234.github.io/Ariadne-Bootloader/package_codebendercc_ariadne-bootloader_index.json",
				"https://raw.githubusercontent.com/akafugu/akafugu_core/master/package_akafugu_index.json",
				"https://raw.githubusercontent.com/AloriumTechnology/Arduino_Boards/master/package_aloriumtech_index.json",
				"https://raw.githubusercontent.com/carlosefr/atmega/master/package_carlosefr_atmega_index.json",
				"https://raw.githubusercontent.com/CytronTechnologies/Cytron-Arduino-URL/master/package_cytron_index.json",
				"https://raw.githubusercontent.com/damellis/attiny/ide-1.6.x-boards-manager/package_damellis_attiny_index.json",
				"https://raw.githubusercontent.com/DFRobot/DFRobotDuinoBoard/master/package_dfrobot_index.json",
				// https://raw.githubusercontent.com/DFRobot/DFRobotDuinoBoard/master/package_dfrobot_iot_mainboard.json
				// clearly a shameless copy of esp package Do not support
				"https://raw.githubusercontent.com/eightdog/laika_arduino/master/IDE_Board_Manager/package_project_laika.com_index.json",
				// "https://raw.githubusercontent.com/ElektorLabs/arduino/master/package_elektor-labs.com_ide-1.6.5_index.json",
				"https://raw.githubusercontent.com/ElektorLabs/arduino/master/package_elektor-labs.com_ide-1.6.6_index.json",
				"https://raw.githubusercontent.com/feilipu/feilipu.github.io/master/package_goldilocks_index.json",
				"https://raw.githubusercontent.com/geolink/opentracker-arduino-board/master/package_opentracker_index.json",
				"https://raw.githubusercontent.com/Lauszus/Sanguino/master/package_lauszus_sanguino_index.json",
				"https://raw.githubusercontent.com/mikaelpatel/Cosa/master/package_cosa_index.json",
				"https://raw.githubusercontent.com/NicoHood/HoodLoader2/master/package_NicoHood_HoodLoader2_index.json",
				"https://raw.githubusercontent.com/OLIMEX/Arduino_configurations/master/AVR/package_olimex_avr_index.json",
				"https://raw.githubusercontent.com/OLIMEX/Arduino_configurations/master/PIC/package_olimex_pic_index.json",
				"https://raw.githubusercontent.com/OLIMEX/Arduino_configurations/master/STM/package_olimex_stm_index.json",
				"https://raw.githubusercontent.com/oshlab/Breadboard-Arduino/master/avr/boardsmanager/package_oshlab_breadboard_index.json",
				"https://raw.githubusercontent.com/RiddleAndCode/RnCAtmega256RFR2/master/Board_Manager/package_rnc_index.json",
				"https://raw.githubusercontent.com/Seeed-Studio/Seeeduino-Boards/master/package_seeeduino_index.json",
				"https://raw.githubusercontent.com/sparkfun/Arduino_Boards/master/IDE_Board_Manager/package_sparkfun_index.json",
				"https://raw.githubusercontent.com/stm32duino/BoardManagerFiles/master/STM32/package_stm_index.json",
				"https://raw.githubusercontent.com/ThamesValleyReprapUserGroup/Beta-TVRRUG-Mendel90/master/Added-Documents/OMC/package_omc_index.json",
				"https://raw.githubusercontent.com/TKJElectronics/Balanduino/master/package_tkj_balanduino_index.json",
				"https://rawgit.com/hunianhang/nufront_arduino_json/master/package_tl7788_index.json",
				"https://redbearlab.github.io/arduino/package_redbear_index.json",
				"https://redbearlab.github.io/arduino/package_redbearlab_index.json",
				"https://s3.amazonaws.com/quirkbot-downloads-production/downloads/package_quirkbot.com_index.json",
				"https://raw.githubusercontent.com/avandalen/SAM15x15/master/package_avdweb_nl_index.json",
				"https://raw.githubusercontent.com/eerimoq/simba-releases/master/arduino/avr/package_simba_avr_index.json",
				"https://raw.githubusercontent.com/eerimoq/simba-releases/master/arduino/sam/package_simba_sam_index.json",
				"https://raw.githubusercontent.com/eerimoq/simba-releases/master/arduino/esp/package_simba_esp_index.json",
				"https://thomasonw.github.io/ATmegaxxM1-C1/package_thomasonw_ATmegaxxM1-C1_index.json",
				"https://www.mattairtech.com/software/arduino/package_MattairTech_index.json",
				"https://zevero.github.io/avr_boot/package_zevero_avr_boot_index.json",
				"https://udooboard.github.io/arduino-board-package/package_udoo_index.json",
				"http://downloads.sodaq.net/package_samd_sodaq_index.json",
				"http://fpgalibre.sf.net/Lattuino/package_lattuino_index.json" };
		BoardsManager.addPackageURLs(new HashSet<>(Arrays.asList(packageUrlsToAdd)), true);
		BoardsManager.referenceLocallInstallation(Shared.getTeensyPlatform());
		if (reinstall_boards_and_libraries) {
		BoardsManager.installAllLatestPlatforms();
		BoardsManager.onlyKeepLatestPlatforms();
		}
		Shared.waitForAllJobsToFinish();
	}

	@Test
	public void testBoard() {
		BuildAndVerify(this.mBoard);

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
