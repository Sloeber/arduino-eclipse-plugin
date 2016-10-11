package io.sloeber.core;

import java.io.File;
import java.util.Map;

import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;

import io.sloeber.core.api.BoardDescriptor;
import io.sloeber.core.tools.TxtFile;

/**
 * This class exists solely for the purpose of having access to the
 * boardDescriptor API class and have some extra access
 * 
 * @author jan
 *
 */
public class InternalBoardDescriptor extends BoardDescriptor {

	public InternalBoardDescriptor(ICConfigurationDescription confdesc) {
		super(confdesc);

	}

	public InternalBoardDescriptor(File boardsFile, String boardID, Map<String, String> options) {
		super(boardsFile, boardID, options);

	}

	public InternalBoardDescriptor(TxtFile txtFile, String boardID) {
		this.myTxtFile = txtFile;
		setBoardID(boardID);
	}

	public TxtFile getTxtFile() {
		return this.myTxtFile;
	}

}
