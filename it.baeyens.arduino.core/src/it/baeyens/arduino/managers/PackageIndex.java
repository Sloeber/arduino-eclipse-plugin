/*******************************************************************************
 * Copyright (c) 2015 QNX Software Systems and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package it.baeyens.arduino.managers;

import java.util.List;

public class PackageIndex {

    private List<Package> packages;

    private String jsonFileName;

    public String getJsonFileName() {
	return jsonFileName;
    }

    public void setJsonFileName(String jsonFileName) {
	this.jsonFileName = jsonFileName;
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

    void setOwners(Manager manager) {
	for (Package pkg : this.packages) {
	    pkg.setOwner(manager);
	}
    }

}
