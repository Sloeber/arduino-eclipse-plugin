package io.sloeber.providers;

import static org.junit.Assert.fail;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import io.sloeber.core.api.PackageManager;

@SuppressWarnings("nls")
public class Arduino extends MCUBoard {

	private static final String providerArduino = "arduino";
	private static final String providerIntel = "Intel";
	private static final String AVRPlatformName = "Arduino AVR Boards";
	private static final String SAMDPlatformName = "Arduino SAMD Boards (32-bits ARM Cortex-M0+)";
	private static final String SAMPlatformName = "Arduino SAM Boards (32-bits ARM Cortex-M3)";
	private static final String NFR52PlatformName = "Arduino nRF52 Boards";
	private static final String intelPlatformName = "Intel Curie Boards";
    private static final String jsonFileName ="package_index.json";
    
    public static final String circuitplay32ID="circuitplay32u4cat";
    public static final String unoID="uno";
    public static final String ethernetID="ethernet";

	public static MCUBoard gemma() {
		MCUBoard ret = new Arduino(providerArduino, AVRPlatformName, "gemma");
		ret.mySlangName="gemma";
		return ret;
	}

	public static MCUBoard MegaADK() {
		return new Arduino(providerArduino, AVRPlatformName, "megaADK");
	}

	public static MCUBoard esplora() {
		return new Arduino(providerArduino, AVRPlatformName, "esplora");
	}

	public static MCUBoard adafruitnCirquitPlayground() {
		return new Arduino(providerArduino, AVRPlatformName, circuitplay32ID);
	}
	public static MCUBoard cirquitPlaygroundExpress() {
		return new Arduino(providerArduino, SAMDPlatformName, "adafruit_circuitplayground_m0");
	}

	public static MCUBoard getAvrBoard(String boardID) {
		return new Arduino(providerArduino, AVRPlatformName, boardID);
	}

	public static MCUBoard fried2016() {
		return new Arduino(providerArduino, AVRPlatformName, "LilyPadUSB");
	}

	public static MCUBoard fried2016(String uploadPort) {
		MCUBoard fried = fried2016();
		fried.myBoardDescriptor.setUploadPort(uploadPort);
		return fried;
	}

	public static MCUBoard getMega2560Board() {
		MCUBoard mega = new Arduino(providerArduino, AVRPlatformName, "mega");
		Map<String, String> options = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
		options.put("cpu", "atmega2560");
		mega.myBoardDescriptor.setOptions(options);
		return mega;
	}

	public static MCUBoard getMega2560Board(String uploadPort) {
		MCUBoard mega = getMega2560Board();
		mega.myBoardDescriptor.setUploadPort(uploadPort);
		return mega;
	}

	public static MCUBoard leonardo() {
		MCUBoard leonardo = new Arduino(providerArduino, AVRPlatformName, "leonardo");
		return leonardo;
	}

	public static MCUBoard leonardo(String uploadPort) {
		MCUBoard leonardo = leonardo();
		leonardo.myBoardDescriptor.setUploadPort(uploadPort);
		return leonardo;
	}

	public static MCUBoard yun() {
		MCUBoard yun = new Arduino(providerArduino, AVRPlatformName, "yun");
		return yun;
	}

	public static MCUBoard yun(String uploadPort) {
		MCUBoard yun = yun();
		yun.myBoardDescriptor.setUploadPort(uploadPort);
		return yun;
	}

	public static MCUBoard zeroProgrammingPort() {
		MCUBoard zero = new Arduino(providerArduino, SAMDPlatformName, "arduino_zero_edbg");
		zero.mySlangName="zero";
		return zero;
	}

	public static MCUBoard zeroProgrammingPort(String uploadPort) {
		MCUBoard zero = zeroProgrammingPort();
		zero.myBoardDescriptor.setUploadPort(uploadPort);
		return zero;
	}

	public static MCUBoard due() {
		return new Arduino(providerArduino, SAMPlatformName, "arduino_due_x");
	}

	public static MCUBoard due(String uploadPort) {
		MCUBoard board = due();
		board.myBoardDescriptor.setUploadPort(uploadPort);
		return board;
	}

	public static MCUBoard dueprogramming() {
		return new Arduino(providerArduino, SAMPlatformName, "arduino_due_x_dbg");
	}

	public static MCUBoard dueprogramming(String uploadPort) {
		MCUBoard board = dueprogramming();
		board.myBoardDescriptor.setUploadPort(uploadPort);
		return board;
	}

	public static MCUBoard mkrfox1200() {
		return new Arduino(providerArduino, SAMDPlatformName, "mkrfox1200");
	}

	public static MCUBoard primo() {
		return new Arduino(providerArduino, NFR52PlatformName, "primo");
	}

	public static MCUBoard uno() {
		MCUBoard uno = new Arduino(providerArduino, AVRPlatformName, unoID);
		uno.mySlangName="uno";
		return uno;
	}
	public static MCUBoard ethernet() {
		MCUBoard uno = new Arduino(providerArduino, AVRPlatformName, ethernetID);
		return uno;
	}

	public static MCUBoard uno(String uploadPort) {
		MCUBoard uno = uno();
		uno.myBoardDescriptor.setUploadPort(uploadPort);
		return uno;
	}

	public static MCUBoard arduino_101() {
		MCUBoard arduino_101 = new Arduino(providerIntel, intelPlatformName, "arduino_101");
		arduino_101.mySlangName="101";
		return arduino_101;
	}

	public static MCUBoard arduino_101(String uploadPort) {
		MCUBoard arduino_101 = arduino_101();
		arduino_101.myBoardDescriptor.setUploadPort(uploadPort);
		return arduino_101;
	}


	private Arduino(String providerName, String platformName, String boardName) {
		this.myBoardDescriptor = PackageManager.getBoardDescriptor(jsonFileName, providerName, platformName,
				boardName, null);
		if (this.myBoardDescriptor == null) {
			fail(boardName + " Board not found");
		}
		this.myBoardDescriptor.setUploadPort("none");

		myAttributes.serial = !doesNotSupportSerialList().contains(boardName);
		myAttributes.serial1 = supportSerial1List().contains(boardName);
		myAttributes.keyboard = supportKeyboardList().contains(boardName);
		myAttributes.wire1 = supportWire1List().contains(boardName);

	}

	static List<String> supportWire1List() {
		List<String> ret = new LinkedList<>();
		ret.add("zero");
		return ret;
	}

	static List<String> supportSerial1List() {
		List<String> ret = new LinkedList<>();
		ret.add("circuitplay32u4cat");
		ret.add("LilyPadUSB");
		ret.add("Micro");
		ret.add("yunMini");
		ret.add("robotControl");
		ret.add("Esplora");
		ret.add("mega");
		ret.add("chiwawa");
		ret.add("yun");
		ret.add("one");
		ret.add("leonardo");
		ret.add("robotMotor");
		ret.add("leonardoEth");
		ret.add("megaADK");

		return ret;
	}

	static List<String> doesNotSupportSerialList() {
		List<String> ret = new LinkedList<>();
		ret.add("gemma");

		return ret;
	}

	static List<String> supportKeyboardList() {
		List<String> ret = new LinkedList<>();
		ret.add("circuitplay32u4cat");
		ret.add("LilyPadUSB");
		ret.add("Micro");
		ret.add("yunMini");
		ret.add("robotControl");
		ret.add("Esplora");
		ret.add("chiwawa");
		ret.add("yun");
		// mySupportKeyboardList.add("one");
		// mySupportKeyboardList.add("Leonardo");
		// mySupportKeyboardList.add("robotMotor");
		// mySupportKeyboardList.add("LeonardoEth");
		// mySupportKeyboardList.add("MegaADK");

		return ret;
	}
	public static void installLatestAVRBoards() {
	    PackageManager.installLatestPlatform(jsonFileName,providerArduino, AVRPlatformName);
	}

    public static void installLatestSamDBoards() {
        PackageManager.installLatestPlatform(jsonFileName,providerArduino, SAMDPlatformName);   
    }

    public static MCUBoard[] getAllBoards() {
    	//hardcode this stuff now because I want to release 4.3.1
    	//shoulds be something like 
        //return PackageManager.getAllBoardDescriptors(getJsonFileName(),getPackageName(),getPlatformName() , options);
    	MCUBoard[] boards = {         Arduino.uno(),
    	        Arduino.leonardo(),
    	        Arduino.esplora(),
    	        Arduino.yun(),
    	        Arduino.getAvrBoard("diecimila"),
    	        Arduino.getMega2560Board(),
    	        Arduino.MegaADK(),
    	        Arduino.getAvrBoard("leonardoeth"),
    	        Arduino.getAvrBoard("micro"),
    	        Arduino.getAvrBoard("mini"),
    	        Arduino.getAvrBoard("ethernet"),
    	        Arduino.getAvrBoard("fio"),
    	        Arduino.getAvrBoard("bt"),
    	        Arduino.getAvrBoard("LilyPadUSB"),
    	        Arduino.getAvrBoard("lilypad"),
    	        Arduino.getAvrBoard("pro"),
    	        Arduino.getAvrBoard("atmegang"),
    	        Arduino.getAvrBoard("robotControl"),
    	        Arduino.getAvrBoard("robotMotor"),
    	        Arduino.getAvrBoard("gemma"),
    	        Arduino.adafruitnCirquitPlayground(),
    	        Arduino.getAvrBoard("yunmini"),
    	        Arduino.getAvrBoard("chiwawa"),
    	        Arduino.getAvrBoard("one"),
    	        Arduino.getAvrBoard("unowifi"), };
		return boards;
   			
    }
	public static MCUBoard zeroNatviePort() {
		MCUBoard zero = new Arduino(providerArduino, SAMDPlatformName, "arduino_zero_native");
		zero.mySlangName="zero Native";
		zero.mySerialPort="SerialUSB";
		return zero;
	}


	public static MCUBoard zeroNatviePort(String uploadPort) {
		MCUBoard zero = zeroNatviePort();
		zero.myBoardDescriptor.setUploadPort(uploadPort);
		return zero;
	}

    
    
    
}