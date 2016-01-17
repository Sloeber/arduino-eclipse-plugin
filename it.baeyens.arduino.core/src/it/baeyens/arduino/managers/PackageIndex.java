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

    private List<ArduinoPackage> packages;

    public List<ArduinoPackage> getPackages() {
	return this.packages;
    }

    public ArduinoPackage getPackage(String packageName) {
	for (ArduinoPackage pkg : this.packages) {
	    if (pkg.getName().equals(packageName)) {
		return pkg;
	    }
	}
	return null;
    }

    void setOwners(ArduinoManager manager) {
	for (ArduinoPackage pkg : this.packages) {
	    pkg.setOwner(manager);
	}
    }

}
