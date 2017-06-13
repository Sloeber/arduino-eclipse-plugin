package jUnit.boards;

@SuppressWarnings("nls")
public class megaBoard extends GenericArduinoAvrBoard {
	public megaBoard() {
		super("mega");
		this.myBoardDescriptor.setUploadPort("COM11");
	}

}
