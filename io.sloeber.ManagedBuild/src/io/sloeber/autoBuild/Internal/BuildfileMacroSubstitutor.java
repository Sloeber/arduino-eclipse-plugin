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

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.cdtvariables.CdtVariableException;
import org.eclipse.cdt.core.cdtvariables.ICdtVariable;
import org.eclipse.cdt.core.cdtvariables.ICdtVariableManager;
import org.eclipse.cdt.utils.cdtvariables.CdtVariableResolver;
import org.eclipse.cdt.utils.cdtvariables.IVariableContextInfo;
import org.eclipse.cdt.utils.cdtvariables.SupplierBasedCdtVariableSubstitutor;
import org.eclipse.cdt.utils.envvar.EnvVarOperationProcessor;
import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import io.sloeber.autoBuild.extensionPoint.IReservedMacroNameSupplier;
import io.sloeber.autoBuild.integration.AutoBuildConfigurationData;
import io.sloeber.schema.api.IBuilder;
import io.sloeber.schema.api.IConfiguration;
import io.sloeber.schema.api.IInputType;
import io.sloeber.schema.api.IOutputType;
import io.sloeber.schema.api.ITool;

/**
 * This substitutor resolves all macro references except for the environment
 * macro references
 * If a user has chosen to keep those macros in the buildfile, the environment
 * macro references
 * are converted to the buildfile variable references, otherwise those macros
 * are also resolved
 *
 * @see org.eclipse.cdt.managedbuilder.internal.macros#IMacroSubstitutor
 * @since 3.0
 */
public class BuildfileMacroSubstitutor extends SupplierBasedCdtVariableSubstitutor {
    private static final String PATTERN_MACRO_NAME = "="; //$NON-NLS-1$
    private IConfiguration fConfiguration;
    private IBuilder fBuilder;
    private ICdtVariableManager fVarMngr;
    private ICConfigurationDescription fCfgDes;

    private class DefaultReservedMacroNameSupplier implements IReservedMacroNameSupplier {
        String fReservedNames[];

        public DefaultReservedMacroNameSupplier(IConfiguration configuration) {
            IBuilder builder = configuration.getToolChain().getBuilder();
            String reservedNames[] = builder.getReservedMacroNames();
            String buildVars[] = getConfigurationReservedNames(configuration);

            if (reservedNames == null || reservedNames.length == 0)
                fReservedNames = buildVars;
            else if (buildVars == null || buildVars.length == 0)
                fReservedNames = reservedNames;
            else {
                fReservedNames = new String[reservedNames.length + buildVars.length];
                System.arraycopy(reservedNames, 0, fReservedNames, 0, reservedNames.length);
                System.arraycopy(buildVars, 0, fReservedNames, reservedNames.length, buildVars.length);
            }
        }

        /* (non-Javadoc)
         * @see org.eclipse.cdt.managedbuilder.macros.IReservedMacroNameSupplier#isReservedName(java.lang.String, org.eclipse.cdt.managedbuilder.core.IConfiguration)
         */
        @Override
        public boolean isReservedName(String macroName, IConfiguration configuration) {
            if (fReservedNames != null && fReservedNames.length > 0) {
                for (int i = 0; i < fReservedNames.length; i++) {
                    Pattern p = Pattern.compile(fReservedNames[i]);
                    Matcher m = p.matcher(macroName);
                    if (m.matches())
                        return true;
                }
            }
            return false;
        }

        protected String[] getConfigurationReservedNames(IConfiguration configuration) {
            List<ITool> tools = configuration.getToolChain().getTools();
            if (tools != null) {
                Set<String> set = new HashSet<>();
                for (ITool tool : tools) {
                    List<IOutputType> ots = tool.getOutputTypes();
                    for (IOutputType curOutputType : ots) {
                        String varName = curOutputType.getBuildVariable();
                        if (varName != null) {
                            set.add(varName);
                        }
                    }

                    List<IInputType> its = tool.getInputTypes();
                    for (IInputType inputType : its) {
                        String varName = inputType.getBuildVariable();
                        if (varName != null) {
                            set.add(varName);
                        }
                    }

                }

                return set.toArray(new String[set.size()]);
            }
            return null;
        }
    }

    public BuildfileMacroSubstitutor(ICConfigurationDescription CfgDes, IMacroContextInfo contextInfo,
            String inexistentMacroValue, String listDelimiter) {
        super(contextInfo, inexistentMacroValue, listDelimiter);
        init(CfgDes);
    }

    private void init(ICConfigurationDescription CfgDes) {
        fCfgDes = CfgDes;
        fVarMngr = CCorePlugin.getDefault().getCdtVariableManager();
        AutoBuildConfigurationData autoData = (AutoBuildConfigurationData) CfgDes.getConfigurationData();
        fConfiguration = autoData.getConfiguration();
        fBuilder = fConfiguration.getBuilder();
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.managedbuilder.internal.macros.DefaultMacroSubstitutor#resolveMacro(org.eclipse.cdt.managedbuilder.macros.IBuildMacro)
     */
    @Override
    protected ResolvedMacro resolveMacro(ICdtVariable macro) throws CdtVariableException {

        if (fConfiguration != null && fBuilder != null && fVarMngr.isEnvironmentVariable(macro, fCfgDes)
                && (!CdtVariableResolver.isStringListVariable(macro.getValueType())
                        || size(macro.getStringListValue()) < 2)) {
            String ref = getMacroReference(macro);
            if (ref != null)
                return new ResolvedMacro(macro.getName(), ref);

        }
        return super.resolveMacro(macro);
    }

    private static int size(String[] value) {
        return value != null ? value.length : 0;
    }

    public IConfiguration getConfiguration() {
        return fConfiguration;
    }

    protected IReservedMacroNameSupplier getReservedMacroNameSupplier() {
        if (fBuilder == null)
            return null;
        IReservedMacroNameSupplier supplier = fBuilder.getReservedMacroNameSupplier();
        if (supplier == null)
            supplier = new DefaultReservedMacroNameSupplier(fConfiguration);

        return supplier;
    }

    protected String getMacroReference(ICdtVariable macro) {
        String macroName = macro.getName();
        String ref = null;
        IReservedMacroNameSupplier supplier = getReservedMacroNameSupplier();
        macroName = EnvVarOperationProcessor.normalizeName(macroName);
        if (supplier == null || !supplier.isReservedName(macroName, fConfiguration)) {
            String pattern = fBuilder.getBuilderVariablePattern();
            if (pattern != null && pattern.indexOf(PATTERN_MACRO_NAME) != -1) {
                ref = pattern.replaceAll(PATTERN_MACRO_NAME, macroName);
            }
        }
        return ref;
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.managedbuilder.internal.macros.DefaultMacroSubstitutor#setMacroContextInfo(org.eclipse.cdt.managedbuilder.internal.macros.IMacroContextInfo)
     */
    @Override
    public void setMacroContextInfo(IVariableContextInfo info) throws CdtVariableException {
        super.setMacroContextInfo(info);
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.managedbuilder.internal.macros.IMacroSubstitutor#setMacroContextInfo(int, java.lang.Object)
     */
    //	public void setMacroContextInfo(int contextType, Object contextData) throws BuildMacroException{
    //		super.setMacroContextInfo(contextType, contextData);
    //		init();
    //	}

}
