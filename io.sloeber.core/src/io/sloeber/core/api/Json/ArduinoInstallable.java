package io.sloeber.core.api.Json;

import org.eclipse.core.runtime.IPath;

public abstract class ArduinoInstallable {

    protected String archiveFileName;
    protected String url;
    protected String checksum;
    protected String size;
    protected String name;

    abstract public IPath getInstallPath();

    public String getArchiveFileName() {
        return archiveFileName;
    }

    public String getUrl() {
        return url;
    }

    public String getChecksum() {
        return checksum;
    }

    public String getSize() {
        return size;
    }

    public String getName() {
        return name;
    }
}
