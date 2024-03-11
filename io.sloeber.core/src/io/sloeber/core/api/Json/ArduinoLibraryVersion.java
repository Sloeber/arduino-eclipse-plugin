package io.sloeber.core.api.Json;

import static io.sloeber.core.Gson.GsonConverter.*;
import static io.sloeber.core.api.Const.ARDUINO_LIBRARY_FOLDER_NAME;
import static io.sloeber.core.api.Const.EXAMPLES_FODER;
import static io.sloeber.core.api.Const.MANAGED;
import static io.sloeber.core.api.Const.PRIVATE;
import static io.sloeber.core.api.Const.SPACE;
import static io.sloeber.core.api.Const.eXAMPLES_FODER;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IPath;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import io.sloeber.core.api.IArduinoLibraryVersion;
import io.sloeber.core.api.VersionNumber;

/**
 * This class represents an entry in a library json file
 *
 * @author jan
 *
 */

public class ArduinoLibraryVersion extends Node implements IArduinoLibraryVersion, Comparable<ArduinoLibraryVersion> {

	private String name;
	private VersionNumber version;
	private String author;
	private String maintainer;
	private String sentence;
	private String paragraph;
	private String website;
	private String category;
	private List<String> architectures = new ArrayList<>();
	private List<String> types = new ArrayList<>();
	private String url;
	private String archiveFileName;
	private int size;
	private String checksum;
	private ArduinoLibrary myParent;

	@SuppressWarnings("nls")
	public ArduinoLibraryVersion(JsonElement json, ArduinoLibrary arduinoLibrary) {
		JsonObject jsonObject = json.getAsJsonObject();
		try {
			myParent = arduinoLibrary;
			name = getSafeString(jsonObject, "name");
			version = getSafeVersion(jsonObject, "version");
			author = getSafeString(jsonObject, "author");
			maintainer = getSafeString(jsonObject, "maintainer");
			sentence = getSafeString(jsonObject, "sentence");
			paragraph = getSafeString(jsonObject, "paragraph");
			website = getSafeString(jsonObject, "website");
			category = getSafeString(jsonObject, "category");
			for (JsonElement curType : jsonObject.get("architectures").getAsJsonArray()) {
				architectures.add(curType.getAsString());
			}
			for (JsonElement curType : jsonObject.get("types").getAsJsonArray()) {
				types.add(curType.getAsString());
			}
			url = getSafeString(jsonObject, "url");
			archiveFileName = getSafeString(jsonObject, "archiveFileName");
			size = jsonObject.get("size").getAsInt();
			checksum = getSafeString(jsonObject, "checksum");
		} catch (Exception e) {
			throw new JsonParseException("failed to parse json  " + e.getMessage());
		}

	}

	public VersionNumber getVersion() {
		return version;
	}

	public String getAuthor() {
		return author;
	}

	public String getMaintainer() {
		return maintainer;
	}

	public String getSentence() {
		return sentence;
	}

	public String getParagraph() {
		return paragraph;
	}

	public String getWebsite() {
		return website;
	}

	public String getCategory() {
		return category;
	}

	public List<String> getArchitectures() {
		return architectures;
	}

	public List<String> getTypes() {
		return types;
	}

	public String getUrl() {
		return url;
	}

	public String getArchiveFileName() {
		return archiveFileName;
	}

	public int getSize() {
		return size;
	}

	public String getChecksum() {
		return checksum;
	}

	public boolean isInstalled() {
		return getInstallPath().toFile().exists();
	}

	@Override
	public int compareTo(ArduinoLibraryVersion other) {
		if (other == null) {
			return 1;
		}
		int ret = getLibrary().compareTo(other.getLibrary());
		if (ret != 0) {
			// seems we are comparing 2 different libraries.
			// No need to look at the versions
			return ret;
		}
		return getID().compareTo(other.getID());
	}

	public ArduinoLibrary getLibrary() {
		return myParent;
	}

	@Override
	public String getNodeName() {
		return name;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public Node getParent() {
		return myParent;
	}

	@Override
	public Node[] getChildren() {
		return null;
	}

	@Override
	public String getID() {
		return version.toString();
	}

	@Override
	public IPath getInstallPath() {
		return myParent.getInstallPath().append(version.toString());
	}

	@Override
	public boolean isHardwareLib() {
		return false;
	}

	@Override
	public boolean isPrivateLib() {
		return false;
	}

	@Override
	public IPath getExamplePath() {
		IPath Lib_examples = getInstallPath().append(eXAMPLES_FODER);
		if (Lib_examples.toFile().exists()) {
			return Lib_examples;
		}
		return getInstallPath().append(EXAMPLES_FODER);
	}

	@Override
	public String[] getBreadCrumbs() {
		ArrayList<String> ret = new ArrayList<>();
		if (isHardwareLib()) {
			ret.add("Board"); //$NON-NLS-1$
		} else {
			ret.add(isPrivateLib() ? PRIVATE + SPACE + ARDUINO_LIBRARY_FOLDER_NAME
					: MANAGED + SPACE + ARDUINO_LIBRARY_FOLDER_NAME);
		}
		ret.add(getName());
		return ret.toArray(new String[ret.size()]);
	}
}
