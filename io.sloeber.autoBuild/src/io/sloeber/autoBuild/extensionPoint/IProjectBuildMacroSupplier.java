/*******************************************************************************
 * Copyright (c) 2005, 2012 Intel Corporation and others.
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

import org.eclipse.cdt.core.cdtvariables.ICdtVariable;
import org.eclipse.core.resources.IProject;

/**
 *
 * this interface is to be implemented by the tool-integrator
 * for supplying the project-specific macros
 *
 * @since 3.0
 */
public interface IProjectBuildMacroSupplier {
    /**
     *
     * @param macroName
     *            the macro name
     * @param project
     *            the instance of the managed project
     * @return the reference to the IBuildMacro interface representing
     *         the build macro of a given name or null if the macro of that name is
     *         not defined
     */
    ICdtVariable getMacro(String macroName, IProject project);

    /**
     *
     * @param project
     *            the instance of the project
     * @return the IBuildMacro[] array representing defined macros
     */
    ICdtVariable[] getMacros(IProject project);
}
