/*******************************************************************************
 * Copyright (c) 2015 QNX Software Systems and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package io.sloeber.arduinoFramework.internal;

import java.io.File;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.annotations.JsonAdapter;

import io.sloeber.arduinoFramework.api.IArduinoPackage;
import io.sloeber.arduinoFramework.api.IArduinoPlatformPackageIndex;
import io.sloeber.arduinoFramework.api.Node;

@JsonAdapter(ArduinoPlatformPackageIndex.class)
public class ArduinoPlatformPackageIndex extends Node
        implements Comparable<ArduinoPlatformPackageIndex>, JsonDeserializer<ArduinoPlatformPackageIndex>, IArduinoPlatformPackageIndex {

    private List<ArduinoPackage> myPackages = new ArrayList<>();

    private transient File myJsonFile;

	private String myURL;

    @Override
	public List<IArduinoPackage> getPackages() {
        return new LinkedList<>(myPackages);
    }

    @Override
	public IArduinoPackage getPackage(String packageName) {
        for (ArduinoPackage pkg : myPackages) {
            if (pkg.getNodeName().equals(packageName)) {
                return pkg;
            }
        }
        return null;
    }

    public void setPackageFile(File packageFile) {
        myJsonFile = packageFile;
    }

    @Override
	public File getJsonFile() {
        return myJsonFile;
    }

    /**
     * provide a identifier that uniquely identifies this package
     *
     * @return A ID that you can uses to identify this package
     */
    @Override
	public String getID() {
        return myJsonFile.getPath();
    }

    @SuppressWarnings("nls")
    @Override
    public ArduinoPlatformPackageIndex deserialize(JsonElement json, Type arg1, JsonDeserializationContext arg2)
            throws JsonParseException {
        JsonObject jsonObject = json.getAsJsonObject();

        try {
            for (JsonElement curElement : jsonObject.get("packages").getAsJsonArray()) {
                myPackages.add(new ArduinoPackage(curElement, this));
            }
        } catch (Exception e) {
            throw new JsonParseException("failed to parse PackageIndex json  " + e.getMessage(),e);
        }

        return this;

    }

    @Override
	public boolean isInstalled() {
        for (IArduinoPackage pkg : myPackages) {
            if (pkg.isInstalled()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Node[] getChildren() {
        return myPackages.toArray(new Node[myPackages.size()]);
    }

    @Override
    public Node getParent() {
        return null;
    }

    @Override
    public String getNodeName() {
        return myJsonFile.getName();
    }

    @Override
    public int compareTo(ArduinoPlatformPackageIndex o) {
        return getID().compareTo(o.getID());
    }

	@Override
	public String getName() {
		return getNodeName();
	}

	@Override
	public String getJsonURL() {
		return myURL;
	}

	public void setURL(String url) {
		myURL=url;

	}

}
