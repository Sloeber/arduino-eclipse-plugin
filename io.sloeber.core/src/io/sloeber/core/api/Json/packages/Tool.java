/*******************************************************************************
 * Copyright (c) 2015 QNX Software Systems and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package io.sloeber.core.api.Json.packages;

import static io.sloeber.core.Gson.GsonConverter.*;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import io.sloeber.core.Activator;
import io.sloeber.core.Messages;
import io.sloeber.core.common.ConfigurationPreferences;

public class Tool {

    private static final String TOOLS = "tools"; //$NON-NLS-1$
    private static final String KEY = Messages.KEY_TAG;
    private String name;
    private String version;
    private List<ToolSystem> systems = new ArrayList<>();

    private transient Package pkg;

    @SuppressWarnings("nls")
    public Tool(JsonElement json, Package pkg) {
        this.pkg = pkg;
        JsonObject jsonObject = json.getAsJsonObject();

        try {
            name = getSafeString(jsonObject, "name");
            version = getSafeString(jsonObject, "version");
            if (jsonObject.get("systems") != null) {
                for (JsonElement curElement : jsonObject.get("systems").getAsJsonArray()) {
                    systems.add(new ToolSystem(curElement, this));
                }
            }
        } catch (Exception e) {
            throw new JsonParseException("failed to parse Tool json  " + e.getMessage());
        }

    }

    public Package getPackage() {
        return this.pkg;
    }

    public String getName() {
        return this.name;
    }

    public String getVersion() {
        return this.version;
    }

    public List<ToolSystem> getSystems() {
        return this.systems;
    }

    public IPath getInstallPath() {
        return ConfigurationPreferences.getInstallationPathPackages().append(this.pkg.getName()).append(TOOLS)
                .append(this.name).append(this.version);

    }

    public boolean isInstalled() {
        return getInstallPath().toFile().exists();
    }

    public IStatus install(IProgressMonitor monitor) {
        if (isInstalled()) {
            return Status.OK_STATUS;
        }

        for (ToolSystem system : this.systems) {
            if (system.isApplicable()) {
                return system.install(monitor);
            }
        }

        // No valid system
        return new Status(IStatus.ERROR, Activator.getId(), Messages.Tool_no_valid_system.replace(KEY, this.name));
    }

}
