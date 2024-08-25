package io.sloeber.core.api.Json;

import org.eclipse.core.runtime.IPath;

public abstract class ArduinoInstallable {

    protected String myArchiveFileName;
    protected String myURL;
    protected String myChecksum;
    protected String mySize;
    protected String myName;

    abstract public IPath getInstallPath();

    public String getArchiveFileName() {
        return myArchiveFileName;
    }

    public String getUrl() {
        return myURL;
    }

    public String getChecksum() {
        return myChecksum;
    }

    public String getSize() {
        return mySize;
    }

    public String getName() {
        return myName;
    }
}
