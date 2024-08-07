/*******************************************************************************
 * Copyright (c) 2005 Intel Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * Intel Corporation - Initial API and implementation
 *******************************************************************************/
package io.sloeber.autoBuild.extensionPoint;

import io.sloeber.autoBuild.schema.api.IConfiguration;

//import org.eclipse.cdt.managedbuilder.core.IConfiguration;

/**
 * This interface is to be implemented by the tool-integrator to specify to the
 * MBS
 * the reserved builder variable names
 *
 * @since 3.0
 */
public interface IReservedMacroNameSupplier {

    /**
     * @return true if the given macro name is reserved by the builder or the
     *         makefile generator
     */
    boolean isReservedName(String macroName, IConfiguration configuration);
}
