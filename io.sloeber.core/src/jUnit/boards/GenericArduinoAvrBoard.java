package jUnit.boards;

import static org.junit.Assert.fail;

import java.util.LinkedList;
import java.util.List;

import io.sloeber.core.api.BoardsManager;

@SuppressWarnings("nls")
public class GenericArduinoAvrBoard extends IBoard {

	private static List<String> myDoesNotSupportSerialList = null;
	private static List<String> mySupportSerial1List = null;
	private static List<String> mySupportKeyboardList = null;

	public GenericArduinoAvrBoard(String boardName) {
		this.myBoardDescriptor = BoardsManager.getBoardDescriptor("package_index.json", "arduino", "Arduino AVR Boards",
				boardName, null);
		if (this.myBoardDescriptor == null) {
			fail(boardName + " Board not found");
		}
		this.myBoardDescriptor.setUploadPort("none");

		setSupportSerial(!doesNotSupportSerialList().contains(boardName));
		setSupportSerial1(supportSerial1List().contains(boardName));
		setSupportKeyboard(supportKeyboardList().contains(boardName));
	}

	private static List<String> supportSerial1List() {
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

	private static List<String> doesNotSupportSerialList() {
		if (myDoesNotSupportSerialList == null) {
			myDoesNotSupportSerialList = new LinkedList<>();
			myDoesNotSupportSerialList.add("gemma");
		}
		return myDoesNotSupportSerialList;
	}

	private static List<String> supportKeyboardList() {
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