package io.sloeber.arduinoFramework.internal;

import static io.sloeber.arduinoFramework.internal.GsonConverter.*;
import static io.sloeber.core.api.Const.*;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import io.sloeber.arduinoFramework.api.IArduinoLibrary;
import io.sloeber.arduinoFramework.api.IArduinoLibraryVersion;
import io.sloeber.arduinoFramework.api.Node;
import io.sloeber.core.api.VersionNumber;

/**
 * This class represents an entry in a library json file
 *
 * @author jan
 *
 */

public class ArduinoLibraryVersion extends Node implements IArduinoLibraryVersion {

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
	private IPath myFQN;

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
			myFQN=calculateFQN(getName());
		} catch (Exception e) {
			throw new JsonParseException("failed to parse json  " + e.getMessage(),e);
		}

	}

	@Override
	public VersionNumber getVersion() {
		return version;
	}

	@Override
	public String getAuthor() {
		return author;
	}

	@Override
	public String getMaintainer() {
		return maintainer;
	}

	@Override
	public String getSentence() {
		return sentence;
	}

	@Override
	public String getParagraph() {
		return paragraph;
	}

	public String getWebsite() {
		return website;
	}

	public String getCategory() {
		return category;
	}

	@Override
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

	@Override
	public boolean isInstalled() {
		return getInstallPath().toFile().exists();
	}

	@Override
	public int compareTo(IArduinoLibraryVersion other) {
		if (other == null) {
			return 1;
		}
		int ret = getLibrary().compareTo(other.getLibrary());
		if (ret != 0) {
			// seems we are comparing 2 different libraries.
			// No need to look at the versions
			return ret;
		}
		return version.toString().compareTo(other.getVersion().toString());
	}

	@Override
	public IArduinoLibrary getLibrary() {
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
		return getInstallPath().append(EXAMPLES_FOLDER);
	}

	static public IPath calculateFQN(String libName) {
		return  Path.fromPortableString(SLOEBER_LIBRARY_FQN).append(MANAGED).append(libName);
	}

	@Override
	public String[] getBreadCrumbs() {
		return myFQN.segments();
	}

	@Override
	public IPath getFQN() {
		return myFQN;
	}

	@Override
	public boolean equals(IArduinoLibraryVersion other) {
		return myFQN.equals(other.getFQN());
	}
}
