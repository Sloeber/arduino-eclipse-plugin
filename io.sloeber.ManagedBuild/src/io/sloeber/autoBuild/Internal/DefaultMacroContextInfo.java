/*******************************************************************************
 * Copyright (c) 2005, 2011 Intel Corporation and others.
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

import org.eclipse.cdt.utils.cdtvariables.ICdtVariableSupplier;
import org.eclipse.cdt.utils.cdtvariables.IVariableContextInfo;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;

import io.sloeber.autoBuild.api.IBuildMacroProvider;
import io.sloeber.schema.api.IBuilder;
import io.sloeber.schema.api.IConfiguration;
import io.sloeber.schema.api.IHoldsOptions;
import io.sloeber.schema.api.IManagedProject;
import io.sloeber.schema.api.IResourceConfiguration;
import io.sloeber.schema.api.ITool;
import io.sloeber.schema.api.IToolChain;
import io.sloeber.schema.internal.IBuildObject;

/**
 * This is the default implementation of the IMacroContextInfo
 *
 * @since 3.0
 */
public class DefaultMacroContextInfo implements IMacroContextInfo {
	private ICdtVariableSupplier fSuppliers[];
	private int fType;
	private Object fData;

	public DefaultMacroContextInfo(int type, Object data) {
		fType = type;
		fData = data;
	}

	public DefaultMacroContextInfo(int type, Object data, ICdtVariableSupplier suppliers[]) {
		fType = type;
		fData = data;
		fSuppliers = suppliers;
	}

	protected ICdtVariableSupplier[] getSuppliers(int type, Object data) {
		switch (type) {

		case IBuildMacroProvider.CONTEXT_CONFIGURATION:
			IConfiguration cfg = null;

			if (data instanceof IConfiguration) {
				cfg = (IConfiguration) data;
			} else if (data instanceof IBuilder) {
				cfg = ((IBuilder) data).getParent().getParent();
			}

			if (cfg != null) {
				CoreMacrosSupplier supplier = BuildMacroProvider.createCoreSupplier(cfg);
				if (supplier != null) {
					return new ICdtVariableSupplier[] { supplier };
				}
			}
			break;
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.cdt.managedbuilder.internal.macros.IMacroContextInfo#getContextType()
	 */
	@Override
	public int getContextType() {
		return fType;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.cdt.managedbuilder.internal.macros.IMacroContextInfo#getContextData()
	 */
	@Override
	public Object getContextData() {
		return fData;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.cdt.managedbuilder.internal.macros.IMacroContextInfo#getSuppliers()
	 */
	@Override
	public ICdtVariableSupplier[] getSuppliers() {
		if (fSuppliers == null)
			fSuppliers = getSuppliers(fType, fData);
		return fSuppliers;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.cdt.managedbuilder.internal.macros.IMacroContextInfo#getNext()
	 */
	@Override
	public IVariableContextInfo getNext() {
		switch (fType) {
		case IBuildMacroProvider.CONTEXT_FILE:
			if (fData instanceof IFileContextData) {
				IFileContextData fileContext = (IFileContextData) fData;
				IOptionContextData optionContext = fileContext.getOptionContextData();
				if (optionContext != null)
					return new DefaultMacroContextInfo(IBuildMacroProvider.CONTEXT_OPTION, optionContext);
			}
			break;
		case IBuildMacroProvider.CONTEXT_OPTION:
			if (fData instanceof IOptionContextData) {
				IOptionContextData optionContext = (IOptionContextData) fData;
				IHoldsOptions ho = OptionContextData.getHolder(optionContext);
				if (ho instanceof ITool)
					return new DefaultMacroContextInfo(IBuildMacroProvider.CONTEXT_TOOL, ho);
				else if (ho instanceof IToolChain)
					return new DefaultMacroContextInfo(IBuildMacroProvider.CONTEXT_CONFIGURATION,
							((IToolChain) ho).getParent());
				else {
					IBuildObject buildObj = optionContext.getParent();
					IConfiguration cfg = null;
					if (buildObj instanceof ITool)
						buildObj = ((ITool) buildObj).getParent();
					if (buildObj instanceof IToolChain)
						cfg = ((IToolChain) buildObj).getParent();
					else if (buildObj instanceof IResourceConfiguration)
						cfg = ((IResourceConfiguration) buildObj).getParent();
					else if (buildObj instanceof IConfiguration)
						cfg = (IConfiguration) buildObj;

					if (cfg != null) {
						return new DefaultMacroContextInfo(IBuildMacroProvider.CONTEXT_CONFIGURATION, cfg);
					}
				}
			}
			break;
		case IBuildMacroProvider.CONTEXT_TOOL:
			if (fData instanceof ITool) {
				IBuildObject parent = ((ITool) fData).getParent();
				IConfiguration cfg = null;
				if (parent instanceof IToolChain)
					cfg = ((IToolChain) parent).getParent();
				else if (parent instanceof IResourceConfiguration)
					cfg = ((IResourceConfiguration) parent).getParent();

				if (cfg != null)
					return new DefaultMacroContextInfo(IBuildMacroProvider.CONTEXT_CONFIGURATION, cfg);
			}
			break;
		case IBuildMacroProvider.CONTEXT_CONFIGURATION:
			IConfiguration configuration = null;
			if (fData instanceof IConfiguration) {
				configuration = (IConfiguration) fData;
			} else if (fData instanceof IBuilder) {
				configuration = ((IBuilder) fData).getParent().getParent();
			}

			if (configuration != null) {
				IManagedProject managedProject = configuration.getManagedProject();
				if (managedProject != null)
					return new DefaultMacroContextInfo(IBuildMacroProvider.CONTEXT_PROJECT, managedProject);
			}
			break;
		case IBuildMacroProvider.CONTEXT_PROJECT:
			if (fData instanceof IManagedProject) {
				IWorkspace wsp = ResourcesPlugin.getWorkspace();
				if (wsp != null)
					return new DefaultMacroContextInfo(IBuildMacroProvider.CONTEXT_WORKSPACE, wsp);
			}
			break;
		case IBuildMacroProvider.CONTEXT_WORKSPACE:
			if (fData instanceof IWorkspace) {
				return new DefaultMacroContextInfo(IBuildMacroProvider.CONTEXT_INSTALLATIONS, null);
			}
			break;
		case IBuildMacroProvider.CONTEXT_INSTALLATIONS:
			if (fData == null) {
				return new DefaultMacroContextInfo(IBuildMacroProvider.CONTEXT_ECLIPSEENV, null);
			}
			break;
		case IBuildMacroProvider.CONTEXT_ECLIPSEENV:
			if (fData == null) {
				return null;
			}
			break;
		}
		return null;
	}
}
