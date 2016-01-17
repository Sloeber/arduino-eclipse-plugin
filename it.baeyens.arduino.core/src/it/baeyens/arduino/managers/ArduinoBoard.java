/*******************************************************************************
 * Copyright (c) 2015 QNX Software Systems and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package it.baeyens.arduino.managers;

import java.util.Properties;

public class ArduinoBoard {

    private String name;

    private String id;

    private ArduinoPlatform platform;
    private HierarchicalProperties properties;

    public ArduinoBoard() {
    }

    public ArduinoBoard(String id, HierarchicalProperties properties) {
	this.properties = properties;
	this.id = id;
	this.name = this.properties.getChild("name").getValue(); //$NON-NLS-1$
    }

    public String getName() {
	return this.name;
    }

    public String getId() {
	return this.id;
    }

    public ArduinoPlatform getPlatform() {
	return this.platform;
    }

    ArduinoBoard setOwners(ArduinoPlatform platform) {
	this.platform = platform;
	return this;
    }

    public String getProperty(String key) {
	return this.properties.getProperty(key);
    }

    public Properties getBoardProperties() {
	return this.properties.flatten();
    }

    @Override
    public int hashCode() {
	final int prime = 31;
	int result = 1;
	result = prime * result + ((this.name == null) ? 0 : this.name.hashCode());
	result = prime * result + ((this.platform == null) ? 0 : this.platform.hashCode());
	return result;
    }

    @Override
    public boolean equals(Object obj) {
	if (this == obj)
	    return true;
	if (obj == null)
	    return false;
	if (getClass() != obj.getClass())
	    return false;
	ArduinoBoard other = (ArduinoBoard) obj;
	if (this.name == null) {
	    if (other.name != null)
		return false;
	} else if (!this.name.equals(other.name))
	    return false;
	if (this.platform == null) {
	    if (other.platform != null)
		return false;
	} else if (!this.platform.equals(other.platform))
	    return false;
	return true;
    }

}
