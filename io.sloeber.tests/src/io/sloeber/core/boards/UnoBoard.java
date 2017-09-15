package io.sloeber.core.boards;

@SuppressWarnings("nls")
public class UnoBoard extends GenericArduinoAvrBoard {
	public UnoBoard() {
		super("uno");
		this.myBoardDescriptor.setUploadPort("COM6");

	}

}