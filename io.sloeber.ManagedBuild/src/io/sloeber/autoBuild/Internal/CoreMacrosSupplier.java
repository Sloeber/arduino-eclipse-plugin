/*******************************************************************************
 * Copyright (c) 2007, 2010 Intel Corporation and others.
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

import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.cdtvariables.ICdtVariable;
import org.eclipse.cdt.core.cdtvariables.ICdtVariableManager;
import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;

import io.sloeber.autoBuild.api.IBuildMacro;


public class CoreMacrosSupplier extends BuildCdtVariablesSupplierBase {
	private ICConfigurationDescription fCfgDes;
	private ICdtVariableManager fMngr;

	CoreMacrosSupplier(ICConfigurationDescription cfgDes) {
		fCfgDes = cfgDes;
		fMngr = CCorePlugin.getDefault().getCdtVariableManager();
	}

	@Override
	public IBuildMacro getMacro(String macroName, int contextType, Object contextData) {
		return BuildMacroProvider.wrap(getVariable(macroName, null));
	}

	@Override
	public IBuildMacro[] getMacros(int contextType, Object contextData) {
		return BuildMacroProvider.wrap(getVariables(null));
	}

	@Override
	public ICdtVariable getVariable(String macroName, IMacroContextInfo context) {
		return fMngr.getVariable(macroName, fCfgDes);
	}

	@Override
	public ICdtVariable[] getVariables(IMacroContextInfo context) {
		return fMngr.getVariables(fCfgDes);
	}
}
