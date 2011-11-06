package it.baeyens.arduino.eclipse;

import it.baeyens.arduino.ArduinoConst;
import it.baeyens.avreclipse.AVRPlugin;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;


public class ArduinoBoards {
	private boolean mArduinoPathIsValid = false;
	// Input from arduino file
	private String mLastLoadedData = "";
	private Map<String, Map<String, String>> mArduinoSupportedBoards = new LinkedHashMap<String, Map<String, String>>(); // all
																															// the
																															// data
	private Set<String> mBoards = new HashSet<String>(); // only the board names

	// tags to interpret the arduino input files
	private String BoardNameKeyTAG = "name";
	private String FrequencyKeyTAG = "build.f_cpu";
	private String ProcessorTypeKeyTAG = "build.mcu";
	private String UploadSpeedKeyTAG = "upload.speed";
	private String disableFlushingKeyTAG = "upload.disable_flushing"; // not yet
																		// used
																		// but
																		// probably
																		// needed

	public String getMCUName(String boardName) {
		String mapName = getBoardNames(boardName);

		Map<String, String> settings = mArduinoSupportedBoards.get(mapName);
		if (settings != null) {
			String TagContent = settings.get(ProcessorTypeKeyTAG);
			if (TagContent != null)
				return TagContent;
		}
		return "";
	}

	public String getMCUFrequency(String boardName) {
		String mapName = getBoardNames(boardName);

		Map<String, String> settings = mArduinoSupportedBoards.get(mapName);
		if (settings != null) {
			String TagContent = settings.get(FrequencyKeyTAG);
			if (TagContent != null) {
				return TagContent.replaceFirst("L", " ").trim();
			}
		}
		return "";

	}

	public String getUploadBaudRate(String boardName) {
		String mapName = getBoardNames(boardName);

		Map<String, String> settings = mArduinoSupportedBoards.get(mapName);
		if (settings != null) {
			String TagContent = settings.get(UploadSpeedKeyTAG);
			if (TagContent != null)
				return TagContent;
		}
		return "";
	}

	public boolean getDisableFlushing(String BoardName) {
		String mapName = getBoardNames(BoardName);

		Map<String, String> settings = mArduinoSupportedBoards.get(mapName);
		if (settings != null) {
			String TagContent = settings.get(disableFlushingKeyTAG);
			if (TagContent != null)
				return TagContent.equalsIgnoreCase("TRUE");
		}
		return false;
	}

	public String[] GetArduinoBoards() {
		if (mLastLoadedData == "") {
			String[] sBoards = new String[0];
			return sBoards;
		}
		for (String s : mArduinoSupportedBoards.keySet()) {
			if (s != null)
				mBoards.add(mArduinoSupportedBoards.get(s).get(BoardNameKeyTAG));
		}
		String[] sBoards = new String[mBoards.size()];
		mBoards.toArray(sBoards);
		return sBoards;
	}

	public boolean Load(String NewArduinoPath) {

		if (mLastLoadedData.equals(NewArduinoPath))
			return mArduinoPathIsValid; // do nothing when value didn't change
		mLastLoadedData = NewArduinoPath;
		mArduinoSupportedBoards.clear();
		String boardtxtPath = NewArduinoPath + File.separator + "hardware" + File.separator + "arduino";
		File boardsFile = new File(boardtxtPath, "boards.txt");
		mArduinoPathIsValid = boardsFile.exists();
		try {
			if (mArduinoPathIsValid) {
				Map<String, String> boardPreferences = new LinkedHashMap<String, String>();
				load(new FileInputStream(boardsFile), boardPreferences);
				for (Object k : boardPreferences.keySet()) {
					String key = (String) k;
					String board = key.substring(0, key.indexOf('.'));
					if (!mArduinoSupportedBoards.containsKey(board))
						mArduinoSupportedBoards.put(board, new HashMap<String, String>());
					((Map<String, String>) mArduinoSupportedBoards.get(board)).put(key.substring(key.indexOf('.') + 1), boardPreferences.get(key));

				}
			}

		} catch (Exception e) {
			IStatus status = new Status(Status.ERROR, ArduinoConst.CORE_PLUGIN_ID,	"Failed to read arduino boards at "  + boardtxtPath, e);
			AVRPlugin.getDefault().log(status);
		}
		return mArduinoPathIsValid;
	};

	private String getBoardNames(String boardName) {
		for (Entry<String, Map<String, String>> entry : mArduinoSupportedBoards.entrySet()) {
			for (Entry<String, String> e2 : entry.getValue().entrySet()) {
				if (e2.getValue().equals(boardName))
					return entry.getKey();
			}

		}
		return null;
	}

	/**
	 * Loads the input stream to a Map, ignoring any lines that start with a #
	 * <p>
	 * Taken from preferences.java in the arduino source
	 * 
	 * @param input
	 *            the input stream to load
	 * @param table
	 *            the Map to load the values to
	 * @throws IOException
	 *             when something goes wrong??
	 */
	static public void load(InputStream input, Map<String, String> table) throws IOException {
		String[] lines = loadStrings(input); // Reads as UTF-8
		for (String line : lines) {
			if ((line.length() == 0) || (line.charAt(0) == '#'))
				continue;

			// this won't properly handle = signs being in the text
			int equals = line.indexOf('=');
			if (equals != -1) {
				String key = line.substring(0, equals).trim();
				String value = line.substring(equals + 1).trim();
				table.put(key, value);
			}
		}
	}

	// Taken from PApplet.java
	/**
	 * Loads an input stream into an array of strings representing each line of
	 * the input stream
	 * 
	 * @param input
	 *            the input stream to load
	 * @return the array of strings representing the inputStream
	 */
	static public String[] loadStrings(InputStream input) {
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(input, "UTF-8"));

			String lines[] = new String[100];
			int lineCount = 0;
			String line = null;
			while ((line = reader.readLine()) != null) {
				if (lineCount == lines.length) {
					String temp[] = new String[lineCount << 1];
					System.arraycopy(lines, 0, temp, 0, lineCount);
					lines = temp;
				}
				lines[lineCount++] = line;
			}
			reader.close();

			if (lineCount == lines.length) {
				return lines;
			}

			// resize array to appropriate amount for these lines
			String output[] = new String[lineCount];
			System.arraycopy(lines, 0, output, 0, lineCount);
			return output;

		} catch (IOException e) {
			IStatus status = new Status(Status.ERROR, ArduinoConst.CORE_PLUGIN_ID,	"Failed to read stream "  , e);
			AVRPlugin.getDefault().log(status);
		}
		return null;
	}

}
