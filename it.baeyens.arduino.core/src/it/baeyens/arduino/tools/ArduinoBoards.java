package it.baeyens.arduino.tools;

import it.baeyens.arduino.common.ArduinoConst;
import it.baeyens.arduino.common.Common;
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

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;

/**ArduinoBoards is that class that hides the Arduino Boards.txt file <br/>
 * The is based on the code of Trump at https://github.com/Trump211/ArduinoEclipsePlugin and later adapted as needed.
 * 
 * @author Jan Baeyens and trump
 *
 */
public class ArduinoBoards {
	private boolean mArduinoPathIsValid = false;
	// Input from arduino file
	private IPath mLastLoadedData = new Path("");
	private Map<String, Map<String, String>> mArduinoSupportedBoards = new LinkedHashMap<String, Map<String, String>>(); // all
																															// the
																															// data
	private Set<String> mBoards = new HashSet<String>(); // only the board names

	// tags to interpret the arduino input files
	private String BoardNameKeyTAG = "name";
	private String FrequencyKeyTAG = "build.f_cpu";
	private String ProcessorTypeKeyTAG = "build.mcu";
	private String UploadSpeedKeyTAG = "upload.speed";
	private String BoardVariantTAG = "build.variant";
	private String BuildCoreFolderTAG ="build.core";
	private String BuildCppOptionsTAG ="build.cpp.options";
	private String BuildCOptionsTAG ="build.c.options";
	private String BuildVIDTAG ="build.vid";
	private String BuildPIDTAG ="build.pid";
	private String BuildLinkOptionsTAG ="build.link.options";
	private String UploadProtocolTAG = "upload.protocol";
	private String disableFlushingKeyTAG = "upload.disable_flushing"; // not yet
																		// used
																		// but
																		// probably
																		// needed


	String LastBoardName="";
	Map<String, String> settings=null;

	/**
	 * Generic method to read a value from the boads file
	 * @param boardName
	 * @param Key
	 * @return
	 */
	private String getBoardSetting(String boardName,String Key,String defaultValue)
		{
			if (!LastBoardName.equals(boardName))
				{
					LastBoardName=boardName;
					String mapName = getBoardNames(boardName);
					settings = mArduinoSupportedBoards.get(mapName);
				}
			
			if (settings != null) {
				String TagContent = settings.get(Key);
				if (TagContent != null)
					return TagContent;
			}
			return defaultValue;
		}
	
	/**getMCUName returns the mcu name for a given board. <br/>
	 * This function assumes (and does not test so will fail) that the board file has been loaded
	 * 
	 * @param boardName the board name for which the mcu name will be returned
	 * @return the MCU name
	 * @author Jan Baeyens
	 */
	public String getMCUName(String boardName) {
		return getBoardSetting( boardName, ProcessorTypeKeyTAG,"");
//		String mapName = getBoardNames(boardName);
//
//		Map<String, String> settings = mArduinoSupportedBoards.get(mapName);
//		if (settings != null) {
//			String TagContent = settings.get(ProcessorTypeKeyTAG);
//			if (TagContent != null)
//				return TagContent;
//		}
//		return "";
	}

	/**getMCUFrequency returns the mcu frequency for a given board. <br/>
	 * This function assumes (and does not test so will fail) that the board file has been loaded
	 * 
	 * @param boardName the board name for which the mcu frequency will be returned
	 * @return the MCU frequency
	 * @author Jan Baeyens
	 */
	public String getMCUFrequency(String boardName) {
		return getBoardSetting( boardName, FrequencyKeyTAG,"").replaceFirst("L", " ").trim();
	}

	/**getUploadBaudRate returns the upload baud rate for a given board. <br/>
	 * This function assumes (and does not test so will fail) that the board file has been loaded
	 * 
	 * @param boardName the board name for which the upload baud rate will be returned
	 * @return the upload baud rate
	 * @author Jan Baeyens
	 */
	public String getUploadBaudRate(String boardName) {
		return getBoardSetting( boardName, UploadSpeedKeyTAG,"");
	}

	
	/** returns the variant of the arduino board<br/>
	 * This function assumes (and does not test so will fail) that the board file has been loaded
	 * 
	 * @param boardName the board name for which the board variant will be returned
	 * @return the Arduino board variant
	 * @author Jan Baeyens
	 */
	public String getBoardVariant(String boardName) {
		return getBoardSetting( boardName, BoardVariantTAG,"");
	}	
	
	/** returns the upload protocol to use with AVR DUDE<br/>
	 * This function assumes (and does not test so will fail) that the board file has been loaded
	 * If nothing is found in the file ArduinoConst.ProgrammerName is returned
	 * 
	 * @param boardName the board name for which the upload protocol will be returned
	 * @return the Arduino upload protocol if found else "" is returned
	 * @author Jan Baeyens
	 */
	public String getUploadProtocol(String boardName) {
		return getBoardSetting( boardName, UploadProtocolTAG,"");
	}		

	public String getBuildVID(String boardName) {
		return getBoardSetting( boardName, BuildVIDTAG,"");
	}		

	public String getBuildPID(String boardName) {
		return getBoardSetting( boardName, BuildPIDTAG,"");
	}		
	

	/**getDisableFlushing returns the disable flushing flag for a given board. <br/>
	 * This function assumes (and does not test so will fail) that the board file has been loaded.
	 * This function is currently unused
	 * 
	 * @param boardName the board name for which the disable flushing flag will be returned
	 * @return disable flushing flag
	 * @author Jan Baeyens
	 */	
	public boolean getDisableFlushing(String boardName) {
		return getBoardSetting( boardName, disableFlushingKeyTAG,"").equalsIgnoreCase("TRUE");
	}
	
	/**getBuildCoreFolder returns the folder that con,tains the arduino library code for a given board. <br/>
	 * This function assumes (and does not test so will fail) that the board file has been loaded.
	 * This function is currently unused
	 * 
	 * @param boardName the board name for which the disable flushing flag will be returned
	 * @return the subfolder of arduino/cores to be used as the library souce
	 * @author Jan Baeyens
	 */	
	public String getBuildCoreFolder(String boardName) {
		return getBoardSetting( boardName, BuildCoreFolderTAG, "arduino");
	}
	

	/**
	 * GetArduinoBoards returns all the boards that are in the currently loaded board.txt file.
	 * 
	 * @return an empty list if no board file is loaded. In all other cases it returns the list of oards found in the file
	 * @author Trump
	 * 
	 */
	public String[] GetArduinoBoards() {
		if (mLastLoadedData.equals("")) {
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

	/**Load loads the board.txt file based on the arduino path.
	 * 
	 * @param NewArduinoPath the path where Arduino is installed. Not the board.txt location
	 * @return true when the action was successful. else false.
	 * @author Trump
	 */
	public boolean Load(IPath NewArduinoPath) {

		if (mLastLoadedData.equals(NewArduinoPath))
			return mArduinoPathIsValid; // do nothing when value didn't change
		mLastLoadedData = NewArduinoPath;
		mArduinoSupportedBoards.clear();

		File boardsFile =NewArduinoPath.append(ArduinoConst.BOARDS_FILE_SUFFIX).toFile();
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
			IStatus status = new Status(Status.WARNING, ArduinoConst.CORE_PLUGIN_ID,	"Failed to read arduino boards at "  + boardsFile.getPath(), e);
			Common.log(status);
		}
		return mArduinoPathIsValid;
	};
	
	/**
	 * @author Trump
	 */
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
			IStatus status = new Status(Status.WARNING, ArduinoConst.CORE_PLUGIN_ID,	"Failed to read stream "  , e);
			Common.log(status);
		}
		return null;
	}

	public String getCppCompileOptions(String boardName)
		{
			return getBoardSetting( boardName, BuildCppOptionsTAG, "");
		}

	public String getCCompileOptions(String boardName)
		{
			return getBoardSetting( boardName, BuildCOptionsTAG, "");
		}

	public String getLinkOptions(String boardName)
		{
			return getBoardSetting( boardName, BuildLinkOptionsTAG, "");
		}

}
