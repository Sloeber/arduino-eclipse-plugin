package io.sloeber.arduinoFramework.api;

import java.net.URL;

import org.eclipse.core.runtime.IPath;

public abstract class ArduinoInstallable {

    protected String myArchiveFileName;
    protected URL mydownloadURL;
    protected String myArchiveChecksum;
    protected int myArchiveSize;
    protected String myName;

    abstract public IPath getInstallPath();

    public String getArchiveFileName() {
        return myArchiveFileName;
    }

    public URL getDownloadUrl() {
        return mydownloadURL;
    }

    public String getArchiveChecksum() {
        return myArchiveChecksum;
    }

    public int getArchiveSize() {
        return myArchiveSize;
    }

    public String getName() {
        return myName;
    }
}
