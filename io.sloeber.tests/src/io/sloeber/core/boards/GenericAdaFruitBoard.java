package io.sloeber.core.boards;

import static org.junit.Assert.fail;

import io.sloeber.core.api.BoardsManager;

@SuppressWarnings("nls")
public class GenericAdaFruitBoard extends IBoard {
	private String platformName;


	public static String getJsonFileName() {
		return  "package_adafruit_index.json";
	}
	public static String getPackageName() {
		return "adafruit";
	}
	public String getPlatformName() {
		return this.platformName;
	}
	public static String getnRF52PlatformName() {
		return "Adafruit nRF52";
	}
	public GenericAdaFruitBoard(String platformName,String boardName) {
        this.platformName=platformName;
		this.myBoardDescriptor = BoardsManager.getBoardDescriptor(getJsonFileName(),getPackageName(),platformName ,
				boardName, null);
		if (this.myBoardDescriptor == null) {
			fail(boardName + " Board not found");
		}
		this.myBoardDescriptor.setUploadPort("none");
	}



}
