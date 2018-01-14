package io.sloeber.core.boards;

import static org.junit.Assert.fail;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import io.sloeber.core.api.BoardsManager;

@SuppressWarnings("nls")
public class ArduinoBoards extends IBoard {

	private static List<String> myDoesNotSupportSerialList = null;
	private static List<String> mySupportSerial1List = null;
	private static List<String> mySupportKeyboardList = null;
	private static final String providerArduino = "arduino";
	private static final String providerIntel = "Intel";
	private static final String AVRPlatformName = "Arduino AVR Boards";
	private static final String SAMDPlatformName = "Arduino SAMD Boards (32-bits ARM Cortex-M0+)";
	private static final String SAMPlatformName = "Arduino SAM Boards (32-bits ARM Cortex-M3)";
	private static final String NFR52PlatformName = "Arduino NRF52 Boards";
	private static final String intelPlatformName = "Intel Curie Boards";

	public static IBoard getGemma() {
		return new ArduinoBoards(providerArduino, AVRPlatformName, "gemma");
	}

	public static IBoard MegaADK() {
		return new ArduinoBoards(providerArduino, AVRPlatformName, "megaADK");
	}

	public static IBoard getEsploraBoard() {
		return new ArduinoBoards(providerArduino, AVRPlatformName, "esplora");
	}

	public static IBoard AdafruitnCirquitPlaygroundBoard() {
		return new ArduinoBoards(providerArduino, AVRPlatformName, "circuitplay32u4cat");
	}

	public static IBoard getAvrBoard(String boardID) {
		return new ArduinoBoards(providerArduino, AVRPlatformName, boardID);
	}

	public static IBoard fried() {
		return new ArduinoBoards(providerArduino, AVRPlatformName, "LilyPadUSB");
	}

	public static IBoard fried(String uploadPort) {
		IBoard fried = fried();
		fried.myBoardDescriptor.setUploadPort(uploadPort);
		return fried;
	}

	public static IBoard getMega2560Board() {
		IBoard mega = new ArduinoBoards(providerArduino, AVRPlatformName, "mega");
		Map<String, String> options = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
		options.put("cpu", "atmega2560");
		mega.myBoardDescriptor.setOptions(options);
		return mega;
	}

	public static IBoard getMega2560Board(String uploadPort) {
		IBoard mega = getMega2560Board();
		mega.myBoardDescriptor.setUploadPort(uploadPort);
		return mega;
	}

	public static IBoard leonardo() {
		IBoard leonardo = new ArduinoBoards(providerArduino, AVRPlatformName, "leonardo");
		return leonardo;
	}

	public static IBoard leonardo(String uploadPort) {
		IBoard leonardo = leonardo();
		leonardo.myBoardDescriptor.setUploadPort(uploadPort);
		return leonardo;
	}

	public static IBoard yun() {
		IBoard yun = new ArduinoBoards(providerArduino, AVRPlatformName, "yun");
		return yun;
	}

	public static IBoard yun(String uploadPort) {
		IBoard yun = yun();
		yun.myBoardDescriptor.setUploadPort(uploadPort);
		return yun;
	}

	public static IBoard zero() {
		IBoard zero = new ArduinoBoards(providerArduino, SAMDPlatformName, "arduino_zero_edbg");
		return zero;
	}

	public static IBoard zero(String uploadPort) {
		IBoard zero = zero();
		zero.myBoardDescriptor.setUploadPort(uploadPort);
		return zero;
	}

	public static IBoard due() {
		return new ArduinoBoards(providerArduino, SAMPlatformName, "arduino_due_x");
	}

	public static IBoard due(String uploadPort) {
		IBoard board = due();
		board.myBoardDescriptor.setUploadPort(uploadPort);
		return board;
	}
	public static IBoard dueprogramming() {
		return new ArduinoBoards(providerArduino, SAMPlatformName, "arduino_due_x_dbg");
	}

	public static IBoard dueprogramming(String uploadPort) {
		IBoard board = dueprogramming();
		board.myBoardDescriptor.setUploadPort(uploadPort);
		return board;
	}

	public static IBoard mkrfox1200() {
		return new ArduinoBoards(providerArduino, SAMDPlatformName, "mkrfox1200");
	}

	public static IBoard primo() {
		return new ArduinoBoards(providerArduino, NFR52PlatformName, "primo");
	}

	public static IBoard uno() {
		IBoard uno = new ArduinoBoards(providerArduino, AVRPlatformName, "uno");
		return uno;
	}

	public static IBoard uno(String uploadPort) {
		IBoard uno = uno();
		uno.myBoardDescriptor.setUploadPort(uploadPort);
		return uno;
	}

	public static IBoard arduino_101() {
		IBoard arduino_101 = new ArduinoBoards(providerIntel, intelPlatformName, "arduino_101");
		return arduino_101;
	}

	public static IBoard arduino_101(String uploadPort) {
		IBoard arduino_101 = arduino_101();
		arduino_101.myBoardDescriptor.setUploadPort(uploadPort);
		return arduino_101;
	}

	private ArduinoBoards(String packageName, String platformName, String boardName) {
		this.myBoardDescriptor = BoardsManager.getBoardDescriptor("package_index.json", packageName, platformName,
				boardName, null);
		if (this.myBoardDescriptor == null) {
			fail(boardName + " Board not found");
		}
		this.myBoardDescriptor.setUploadPort("none");

		setSupportSerial(!doesNotSupportSerialList().contains(boardName));
		setSupportSerial1(supportSerial1List().contains(boardName));
		setSupportKeyboard(supportKeyboardList().contains(boardName));
	}

	static List<String> supportSerial1List() {
		if (mySupportSerial1List == null) {
			mySupportSerial1List = new LinkedList<>();
			mySupportSerial1List.add("circuitplay32u4cat");
			mySupportSerial1List.add("LilyPadUSB");
			mySupportSerial1List.add("Micro");
			mySupportSerial1List.add("yunMini");
			mySupportSerial1List.add("robotControl");
			mySupportSerial1List.add("Esplora");
			mySupportSerial1List.add("mega");
			mySupportSerial1List.add("chiwawa");
			mySupportSerial1List.add("yun");
			mySupportSerial1List.add("one");
			mySupportSerial1List.add("leonardo");
			mySupportSerial1List.add("robotMotor");
			mySupportSerial1List.add("leonardoEth");
			mySupportSerial1List.add("megaADK");
		}
		return mySupportSerial1List;
	}

	static List<String> doesNotSupportSerialList() {
		if (myDoesNotSupportSerialList == null) {
			myDoesNotSupportSerialList = new LinkedList<>();
			myDoesNotSupportSerialList.add("gemma");
		}
		return myDoesNotSupportSerialList;
	}

	static List<String> supportKeyboardList() {
		if (mySupportKeyboardList == null) {
			mySupportKeyboardList = new LinkedList<>();
			mySupportKeyboardList.add("circuitplay32u4cat");
			mySupportKeyboardList.add("LilyPadUSB");
			mySupportKeyboardList.add("Micro");
			mySupportKeyboardList.add("yunMini");
			mySupportKeyboardList.add("robotControl");
			mySupportKeyboardList.add("Esplora");
			mySupportKeyboardList.add("chiwawa");
			mySupportKeyboardList.add("yun");
			// mySupportKeyboardList.add("one");
			// mySupportKeyboardList.add("Leonardo");
			// mySupportKeyboardList.add("robotMotor");
			// mySupportKeyboardList.add("LeonardoEth");
			// mySupportKeyboardList.add("MegaADK");
		}
		return mySupportKeyboardList;
	}

}