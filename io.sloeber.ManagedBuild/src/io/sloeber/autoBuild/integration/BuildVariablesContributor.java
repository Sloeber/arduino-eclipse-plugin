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
package io.sloeber.autoBuild.integration;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.cdt.core.cdtvariables.ICdtVariable;
import org.eclipse.cdt.core.cdtvariables.ICdtVariableManager;
import org.eclipse.cdt.core.cdtvariables.ICdtVariablesContributor;
import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.cdt.utils.cdtvariables.ICdtVariableSupplier;
import org.eclipse.cdt.utils.cdtvariables.IVariableContextInfo;
import org.eclipse.cdt.utils.cdtvariables.SupplierBasedCdtVariableManager;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;

import io.sloeber.autoBuild.Internal.BuildMacroProvider;
import io.sloeber.autoBuild.Internal.DefaultMacroContextInfo;
import io.sloeber.autoBuild.Internal.ManagedBuildManager;
import io.sloeber.autoBuild.api.IBuildMacroProvider;
import io.sloeber.schema.api.IBuilder;
import io.sloeber.schema.api.IConfiguration;
import io.sloeber.schema.api.IManagedProject;
import io.sloeber.schema.internal.Configuration;

public class BuildVariablesContributor implements ICdtVariablesContributor {
	private BuildConfigurationData fCfgData;

	private class ContributorMacroContextInfo extends DefaultMacroContextInfo {
		ICdtVariableManager fMngr;
		private ICConfigurationDescription fCfgDes;

		public ContributorMacroContextInfo(ICdtVariableManager mngr, ICConfigurationDescription cfgDes, int type,
				Object data) {
			super(type, data);
			fMngr = mngr;
			fCfgDes = cfgDes;
		}


		@Override
		public IVariableContextInfo getNext() {
			switch (getContextType()) {
			case IBuildMacroProvider.CONTEXT_CONFIGURATION: {
				if (fCfgData != null) {
					IManagedProject managedProject = fCfgData.getManagedProject();
					if (managedProject != null)
						return new ContributorMacroContextInfo(fMngr, fCfgDes, IBuildMacroProvider.CONTEXT_PROJECT,
								managedProject);
				}
			}
				break;
			case IBuildMacroProvider.CONTEXT_PROJECT: {
				Object data = getContextData();
				if (data instanceof IManagedProject) {
					IWorkspace wsp = ResourcesPlugin.getWorkspace();
					if (wsp != null)
						return new ContributorMacroContextInfo(fMngr, fCfgDes, IBuildMacroProvider.CONTEXT_WORKSPACE,
								wsp);
				}
			}
				break;
			case IBuildMacroProvider.CONTEXT_WORKSPACE:
				if (getContextData() instanceof IWorkspace) {
					return new ContributorMacroContextInfo(fMngr, fCfgDes, IBuildMacroProvider.CONTEXT_INSTALLATIONS,
							null);
				}
				break;
			}
			return null;
		}
	}

	BuildVariablesContributor(BuildConfigurationData data) {
		fCfgData = data;
	}

	@Override
	public ICdtVariable getVariable(String name, ICdtVariableManager provider) {
		ContributorMacroContextInfo info = createContextInfo(provider);
		if (info != null)
			return SupplierBasedCdtVariableManager.getVariable(name, info, true);
		return null;
	}

	private ContributorMacroContextInfo createContextInfo(ICdtVariableManager mngr) {
		IConfiguration cfg = fCfgData.getConfiguration();
		if (((Configuration) cfg).isPreference())
			return null;
		ICConfigurationDescription cfgDes = ManagedBuildManager.getDescriptionForConfiguration(cfg);
		if (cfgDes != null) {
			return new ContributorMacroContextInfo(mngr, cfgDes, BuildMacroProvider.CONTEXT_CONFIGURATION, cfg);
		}
		return null;
	}

	@Override
	public ICdtVariable[] getVariables(ICdtVariableManager provider) {
		ContributorMacroContextInfo info = createContextInfo(provider);
		if (info == null) {
			return null;
		}
		
			ICdtVariableSupplier suppliers[] = info.getSuppliers();
			if (suppliers == null) {
				return null;
			}
			Map<String, ICdtVariable> map = new HashMap<>();
				for (ICdtVariableSupplier supplier : suppliers ) {
					ICdtVariable macros[] = supplier.getVariables(info);
					if (macros != null) {
						for (ICdtVariable macro : macros) {
							map.put(macro.getName(), macro);
						}
					}
				}
				Collection<ICdtVariable> values = map.values();
				return values.toArray(new ICdtVariable[values.size()]);
	}

}
