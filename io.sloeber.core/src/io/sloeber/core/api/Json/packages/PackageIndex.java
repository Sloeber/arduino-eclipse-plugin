/*******************************************************************************
 * Copyright (c) 2015 QNX Software Systems and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package io.sloeber.core.api.Json.packages;

import java.io.File;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.annotations.JsonAdapter;

@JsonAdapter(PackageIndex.class)
public class PackageIndex implements JsonDeserializer<PackageIndex> {

    private List<Package> packages = new ArrayList<>();

    private transient File jsonFile;

    public List<Package> getPackages() {
        return this.packages;
    }

    public Package getPackage(String packageName) {
        for (Package pkg : this.packages) {
            if (pkg.getName().equals(packageName)) {
                return pkg;
            }
        }
        return null;
    }

    public void setPackageFile(File packageFile) {
        this.jsonFile = packageFile;
    }

    public File getJsonFile() {
        return this.jsonFile;
    }

    @SuppressWarnings("nls")
    @Override
    public PackageIndex deserialize(JsonElement json, Type arg1, JsonDeserializationContext arg2)
            throws JsonParseException {
        JsonObject jsonObject = json.getAsJsonObject();

        try {
            for (JsonElement curElement : jsonObject.get("packages").getAsJsonArray()) {
                packages.add(new Package(curElement, this));
            }
        } catch (Exception e) {
            throw new JsonParseException("failed to parse PackageIndex json  " + e.getMessage());
        }

        return this;

    }

}
