/*******************************************************************************
 * Copyright (c) 2007, 2011 Intel Corporation and others.
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
package io.sloeber.autoBuild.Internal;

import org.eclipse.cdt.core.cdtvariables.ICdtVariable;
import org.eclipse.cdt.utils.cdtvariables.IVariableContextInfo;

import io.sloeber.autoBuild.api.IBuildMacro;
import io.sloeber.autoBuild.api.IBuildMacroSupplier;

public abstract class BuildCdtVariablesSupplierBase implements IBuildMacroSupplier {

	@Override
	public abstract IBuildMacro getMacro(String macroName, int contextType, Object contextData);

	@Override
	public abstract IBuildMacro[] getMacros(int contextType, Object contextData);

	@Override
	public ICdtVariable getVariable(String macroName, IMacroContextInfo context) {
		return getMacro(macroName, context.getContextType(), context.getContextData());
	}

	@Override
	public ICdtVariable[] getVariables(IMacroContextInfo context) {
		return getMacros(context.getContextType(), context.getContextData());
	}

	@Override
	public ICdtVariable getVariable(String macroName, IVariableContextInfo context) {
		if (context instanceof IMacroContextInfo) {
			IMacroContextInfo info = (IMacroContextInfo) context;
			return getMacro(macroName, info.getContextType(), info.getContextData());
		}
		return null;
	}

	@Override
	public ICdtVariable[] getVariables(IVariableContextInfo context) {
		if (context instanceof IMacroContextInfo) {
			IMacroContextInfo info = (IMacroContextInfo) context;
			return getMacros(info.getContextType(), info.getContextData());
		}
		return null;
	}

}
