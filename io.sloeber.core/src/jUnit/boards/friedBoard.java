package jUnit.boards;

@SuppressWarnings("nls")
public class friedBoard extends GenericArduinoAvrBoard {
	public friedBoard() {
		super("LilyPadUSB");
		this.myBoardDescriptor.setUploadPort("COM23");

	}



}
