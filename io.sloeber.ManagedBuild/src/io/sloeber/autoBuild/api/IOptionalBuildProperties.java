/*******************************************************************************
 * Copyright (c) 2017 Red Hat Inc. and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * Red Hat Inc. - Initial API and implementation
 *******************************************************************************/
package io.sloeber.autoBuild.api;

/**
 * @noextend This class is not intended to be subclassed by clients.
 * @noimplement This interface is not intended to be implemented by clients.
 * @since 8.6
 */
public interface IOptionalBuildProperties extends Cloneable {
	String[] getProperties();

	String getProperty(String id);

	void setProperty(String propertyId, String propertyValue);

	void removeProperty(String id);

	void clear();

	Object clone();
}