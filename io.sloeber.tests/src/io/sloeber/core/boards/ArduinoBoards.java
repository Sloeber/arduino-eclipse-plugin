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
	private static final String providerArduino="arduino";
	private static final String providerIntel="Intel";
	 private static final String AVRPlatformName= "Arduino AVR Boards";
	private static final String SAMDPlatformName ="Arduino SAMD Boards (32-bits ARM Cortex-M0+)";
	private static final String NFR52PlatformName ="Arduino NRF52 Boards";
	private static final String intelPlatformName ="Intel Curie Boards";
	 public static ArduinoBoards  getGemma() {
		return new ArduinoBoards(providerArduino,AVRPlatformName,"gemma");
	 }
	 public static ArduinoBoards  MegaADK() {
		return new ArduinoBoards(providerArduino,AVRPlatformName,"megaADK");
	 }
	 public static ArduinoBoards  getEsploraBoard() {
		return new ArduinoBoards(providerArduino,AVRPlatformName,"esplora");
	 }
	 public static ArduinoBoards  AdafruitnCirquitPlaygroundBoard () {
		return new ArduinoBoards(providerArduino,AVRPlatformName,"circuitplay32u4cat");
	 }
	 public static ArduinoBoards  getAvrBoard(String boardID) {
		return new ArduinoBoards(providerArduino,AVRPlatformName,boardID);
	 }
	 public static ArduinoBoards  fried() {
		 ArduinoBoards fried= new ArduinoBoards(providerArduino,AVRPlatformName,"LilyPadUSB");
		 fried.myBoardDescriptor.setUploadPort("COM23");
			return fried;
		}


	 public static ArduinoBoards  getMega2560Board() {
		 ArduinoBoards mega= new ArduinoBoards(providerArduino,AVRPlatformName,"mega");
			mega.myBoardDescriptor.setUploadPort("COM11");

			Map<String, String> options = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
			options.put("cpu", "atmega2560");
			mega.myBoardDescriptor.setOptions(options);
			return mega;
		}

	 public static ArduinoBoards  leonardo() {
		 ArduinoBoards leonardo= new ArduinoBoards(providerArduino,AVRPlatformName,"leonardo");
		 leonardo.myBoardDescriptor.setUploadPort("COM13");
			return leonardo;
		}
	 public static ArduinoBoards  yun() {
		 ArduinoBoards yun= new ArduinoBoards(providerArduino,AVRPlatformName,"yun");
		 yun.myBoardDescriptor.setUploadPort("COM24");
			return yun;
		}
	 public static ArduinoBoards  zero() {
		 ArduinoBoards zero= new ArduinoBoards(providerArduino,SAMDPlatformName,"arduino_zero_edbg");
		 zero.myBoardDescriptor.setUploadPort("COM14");
			return zero;
		}
	 public static ArduinoBoards  mkrfox1200() {
		 return new ArduinoBoards(providerArduino,SAMDPlatformName,"mkrfox1200");
		}
	 public static ArduinoBoards  primo() {
		 return new ArduinoBoards(providerArduino,NFR52PlatformName,"primo");
		}


	 public static ArduinoBoards  uno() {
		 ArduinoBoards uno= new ArduinoBoards(providerArduino,AVRPlatformName,"uno");
		 uno.myBoardDescriptor.setUploadPort("COM6");
			return uno;
		}
	 public static ArduinoBoards  arduino_101() {
		 ArduinoBoards arduino_101= new ArduinoBoards(providerIntel,intelPlatformName,"arduino_101");
		 arduino_101.myBoardDescriptor.setUploadPort("COM9");
			return arduino_101;
		}

	public ArduinoBoards(String packageName,String platformName,String boardName) {
		this.myBoardDescriptor = BoardsManager.getBoardDescriptor("package_index.json", packageName,AVRPlatformName,
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