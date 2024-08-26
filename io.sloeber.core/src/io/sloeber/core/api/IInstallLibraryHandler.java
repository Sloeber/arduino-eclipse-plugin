package io.sloeber.core.api;

import java.util.Map;

import io.sloeber.arduinoFramework.api.IArduinoLibraryVersion;

/**
 * this interface is to allow the ui to handle the automatic installation
 * of libraries.
 * If you do not register your implementation of this interface there will be
 * no automatic install of libraries
 *
 * registering your inmplementation is done in the library manager
 *
 * @author jan
 *
 */

public interface IInstallLibraryHandler {
    /**
     * The core will call this method to find out if you want to install
     * the libraries automatically or not
     *
     * @return true if you want libraries to beinstalled automatically
     */
    abstract boolean autoInstall();

    /**
     * given the set of proposed libraries to install
     * let the user decide on what to install
     *
     * @param proposedLibsToInstall
     *            the libraries Sloeber proposes to install
     *
     * @return The libraries the user wants to install
     */
    abstract Map<String, IArduinoLibraryVersion> selectLibrariesToInstall(Map<String, IArduinoLibraryVersion> proposedLibsToInstall);
}
