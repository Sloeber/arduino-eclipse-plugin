/*******************************************************************************
 * Copyright (c) 2015 QNX Software Systems and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package io.sloeber.core.managers;

import java.io.File;
import java.util.List;

public class PackageIndex {

    private List<Package> packages;

    private String jsonFileName;

    private File jsonFile;

    public String getJsonFileName() {
	return this.jsonFileName;
    }

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

    public void setOwners() {
	for (Package pkg : this.packages) {
	    pkg.setParent(this);
	}
    }

    public File getJsonFile() {
	return this.jsonFile;
    }

    public void setJsonFile(File packageFile) {
	this.jsonFileName = packageFile.getName();
	this.jsonFile = packageFile;
    }

}
