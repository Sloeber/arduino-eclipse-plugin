/*******************************************************************************
 * Copyright (c) 2004, 2010 TimeSys Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * TimeSys Corporation - Initial API and implementation
 *******************************************************************************/
package io.sloeber.managedBuild.api;

/**
 * This class represents a configuration element for loading the managed build
 * model objects.  They can either be loaded from the ManagedBuildInfo extension
 * point, or from an instance of IManagedConfigProvider.
 *
 * @noextend This class is not intended to be subclassed by clients.
 * @noimplement This interface is not intended to be implemented by clients.
 */
public interface IManagedConfigElement {

	/**
	 * @return the name of this config element (i.e. tag name of the
	 * corresponding xml element)
	 */
	String getName();

	/**
	 * @return the value of the attribute with the given name, or null
	 * if the attribute is unset.
	 */
	String getAttribute(String name);

	/**
	 * @return all child elements of the current config element.
	 */
	IManagedConfigElement[] getChildren();

	/**
	 * @return all child elements of the current config element, such that
	 * <code>child.getName().equals(elementName)</code>.
	 */
	IManagedConfigElement[] getChildren(String elementName);
}
