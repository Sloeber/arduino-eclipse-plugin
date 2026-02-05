package io.sloeber.arduinoFramework.internal;

import java.net.URI;
import java.net.URL;

import org.eclipse.core.runtime.IPath;

import io.sloeber.arduinoFramework.api.ArduinoInstallable;

public class ArduinoInstallableSimple extends ArduinoInstallable {
	private IPath myInstallPath=null;


	static public ArduinoInstallableSimple from(IPath InstallPath, String archiveFileName, String url , String name) {
		URL realUrl=null;
		try {
			realUrl=new URI(url).toURL();
		}catch(@SuppressWarnings("unused") Exception e) {
			//ignore exceptions; go by defaults
		}
		return new ArduinoInstallableSimple( InstallPath,  archiveFileName,  realUrl, null, -1,  name);

	}


	public ArduinoInstallableSimple(IPath InstallPath, String archiveFileName, URL url, String archiveChecksum, int archiveSize, String name) {
		myInstallPath=InstallPath;
		myArchiveFileName = archiveFileName;
		mydownloadURL = url;
		myArchiveChecksum = archiveChecksum;
		myArchiveSize = archiveSize;
		myName = name;

	}

	@Override
	public IPath getInstallPath() {
		return myInstallPath;
	}

}
