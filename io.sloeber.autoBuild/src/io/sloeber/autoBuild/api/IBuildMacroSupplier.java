/*******************************************************************************
 * Copyright (c) 2005, 2010 Intel Corporation and others.
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
package io.sloeber.autoBuild.api;

import org.eclipse.cdt.core.cdtvariables.ICdtVariable;
import org.eclipse.cdt.utils.cdtvariables.ICdtVariableSupplier;
import io.sloeber.autoBuild.Internal.IMacroContextInfo;

/**
 *
 * @since 3.0
 * @noextend This class is not intended to be subclassed by clients.
 * @noimplement This interface is not intended to be implemented by clients.
 */
public interface IBuildMacroSupplier extends ICdtVariableSupplier {

    /**
     *
     * @param macroName
     *            macro name
     * @param contextType
     *            context type
     * @param contextData
     *            context data
     * @return IBuildMacro
     */
    public IBuildMacro getMacro(String macroName, int contextType, Object contextData);

    /**
     *
     * @param contextType
     *            context type
     * @param contextData
     *            context data
     * @return IBuildMacro[]
     */
    public IBuildMacro[] getMacros(int contextType, Object contextData);

    public ICdtVariable getVariable(String macroName, IMacroContextInfo context);

    public ICdtVariable[] getVariables(IMacroContextInfo context);
}
