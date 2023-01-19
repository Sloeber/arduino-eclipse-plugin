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
 * Miwako Tokugawa (Intel Corporation) - bug 222817 (OptionCategoryApplicability)
 *******************************************************************************/
package io.sloeber.autoBuild.Internal;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.cdtvariables.CdtVariableException;
import org.eclipse.cdt.core.cdtvariables.ICdtVariable;
import org.eclipse.cdt.core.cdtvariables.ICdtVariableManager;
import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
//import org.eclipse.cdt.managedbuilder.core.IBuildObject;
//import org.eclipse.cdt.managedbuilder.core.IBuilder;
//import org.eclipse.cdt.managedbuilder.core.IConfiguration;
//import org.eclipse.cdt.managedbuilder.core.IResourceConfiguration;
//import org.eclipse.cdt.managedbuilder.core.ITool;
//import org.eclipse.cdt.managedbuilder.core.IToolChain;
//import org.eclipse.cdt.managedbuilder.core.ManagedBuildManager;
//import org.eclipse.cdt.managedbuilder.internal.core.Tool;
//import org.eclipse.cdt.managedbuilder.macros.BuildMacroException;
//import org.eclipse.cdt.managedbuilder.macros.IBuildMacro;
//import org.eclipse.cdt.managedbuilder.macros.IBuildMacroProvider;
//import org.eclipse.cdt.managedbuilder.macros.IBuildMacroSupplier;
import org.eclipse.cdt.utils.cdtvariables.CdtVariableResolver;
import org.eclipse.cdt.utils.cdtvariables.ICdtVariableSupplier;
import org.eclipse.cdt.utils.cdtvariables.IVariableSubstitutor;
import org.eclipse.cdt.utils.cdtvariables.SupplierBasedCdtVariableManager;
import org.eclipse.cdt.utils.cdtvariables.SupplierBasedCdtVariableSubstitutor;
import org.eclipse.core.variables.IDynamicVariable;
import org.eclipse.core.variables.IStringVariable;

import io.sloeber.autoBuild.api.BuildMacroException;
import io.sloeber.autoBuild.api.IBuildMacro;
import io.sloeber.autoBuild.api.IBuildMacroProvider;
import io.sloeber.autoBuild.api.IBuildMacroSupplier;
import io.sloeber.schema.api.IBuilder;
import io.sloeber.schema.api.IConfiguration;
import io.sloeber.schema.api.IResourceConfiguration;
import io.sloeber.schema.api.ITool;
import io.sloeber.schema.api.IToolChain;
import io.sloeber.schema.internal.IBuildObject;

/**
 * The default IBuildMacroProvider implementation
 * 
 * @since 3.0
 */
public class BuildMacroProvider implements IBuildMacroProvider, IMacroContextInfoProvider {
    private static final String PATTERN_MACRO_NAME = "="; //$NON-NLS-1$

    static private BuildMacroProvider fDefault;

    protected BuildMacroProvider() {

    }

    public static BuildMacroProvider getDefault() {
        if (fDefault == null)
            fDefault = new BuildMacroProvider();
        return fDefault;
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.managedbuilder.macros.IBuildMacroProvider#getMacro(java.lang.String, int, java.lang.Object, boolean)
     */
    @Override
    public IBuildMacro getMacro(String macroName, int contextType, Object contextData, boolean includeParentContexts) {
        ICdtVariable var = getVariable(macroName, contextType, contextData, includeParentContexts);
        if (var != null)
            return wrap(var);
        return null;
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.managedbuilder.macros.IBuildMacroProvider#getMacros(int, java.lang.Object, boolean)
     */
    @Override
    public IBuildMacro[] getMacros(int contextType, Object contextData, boolean includeParentContexts) {
        ICdtVariable[] vars = getVariables(contextType, contextData, includeParentContexts);
        if (vars != null)
            return wrap(vars);
        return null;
    }

    public static CoreMacrosSupplier createCoreSupplier(IConfiguration cfg) {
        ICConfigurationDescription cfgDes = ManagedBuildManager.getDescriptionForConfiguration(cfg);
        if (cfgDes != null) {
            return new CoreMacrosSupplier(cfgDes);
        }
        return null;
    }

    @Override
    public ICdtVariable getVariable(String macroName, int contextType, Object contextData,
            boolean includeParentContexts) {
        return SupplierBasedCdtVariableManager.getVariable(macroName, getMacroContextInfo(contextType, contextData),
                includeParentContexts);
    }

    @Override
    public ICdtVariable[] getVariables(int contextType, Object contextData, boolean includeParentContexts) {
        return SupplierBasedCdtVariableManager.getVariables(getMacroContextInfo(contextType, contextData),
                includeParentContexts);
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.managedbuilder.macros.IBuildMacroProvider#getSuppliers(int, java.lang.Object)
     */
    @Override
    public IBuildMacroSupplier[] getSuppliers(int contextType, Object contextData) {
        IMacroContextInfo info = getMacroContextInfo(contextType, contextData);
        if (info != null) {
            ICdtVariableSupplier suppliers[] = info.getSuppliers();
            if (suppliers != null)
                return filterMacroSuppliers(suppliers);
        }
        return null;
    }

    private static IBuildMacroSupplier[] filterMacroSuppliers(ICdtVariableSupplier suppliers[]) {
        List<ICdtVariableSupplier> list = new ArrayList<>(suppliers.length);
        for (int i = 0; i < suppliers.length; i++) {
            if (suppliers[i] instanceof IBuildMacroSupplier)
                list.add(suppliers[i]);
        }
        return list.toArray(new IBuildMacroSupplier[list.size()]);
    }

    @Override
    public IMacroContextInfo getMacroContextInfo(int contextType, Object contextData) {
        DefaultMacroContextInfo info = new DefaultMacroContextInfo(contextType, contextData);
        if (info.getSuppliers() != null)
            return info;
        return null;
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.managedbuilder.macros.IBuildMacroProvider#convertStringListToString(java.lang.String[], java.lang.String)
     */
    @Override
    public String convertStringListToString(String[] value, String listDelimiter) {
        return CdtVariableResolver.convertStringListToString(value, listDelimiter);
    }

    private static class VariableWrapper implements IBuildMacro {
        private ICdtVariable fVariable;

        public VariableWrapper(ICdtVariable var) {
            if (var == null)
                throw new NullPointerException();

            fVariable = var;
        }

        //		public ICdtVariable getVariable(){
        //			return fVariable;
        //		}

        @Override
        public int getMacroValueType() {
            return fVariable.getValueType();
        }

        @Override
        public String[] getStringListValue() throws BuildMacroException {
            try {
                return fVariable.getStringListValue();
            } catch (CdtVariableException e) {
                throw new BuildMacroException(e);
            }
        }

        @Override
        public String getStringValue() throws BuildMacroException {
            try {
                return fVariable.getStringValue();
            } catch (CdtVariableException e) {
                throw new BuildMacroException(e);
            }
        }

        @Override
        public String getName() {
            return fVariable.getName();
        }

        @Override
        public int getValueType() {
            return fVariable.getValueType();
        }
    }

    public static IBuildMacro wrap(ICdtVariable var) {
        if (var == null)
            return null;
        if (var instanceof IBuildMacro)
            return (IBuildMacro) var;
        return new VariableWrapper(var);
    }

    public static IBuildMacro[] wrap(ICdtVariable vars[]) {
        if (vars instanceof IBuildMacro[])
            return (IBuildMacro[]) vars;

        IBuildMacro macros[] = new IBuildMacro[vars.length];
        for (int i = 0; i < macros.length; i++) {
            macros[i] = wrap(vars[i]);
        }
        return macros;
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.managedbuilder.macros.IBuildMacroProvider#resolveValue(java.lang.String, java.lang.String, java.lang.String, int, java.lang.Object)
     */
    @Override
    public String resolveValue(String value, String nonexistentMacrosValue, String listDelimiter, int contextType,
            Object contextData) throws BuildMacroException {
        IMacroContextInfo info = getMacroContextInfo(contextType, contextData);

        if (info != null) {
            try {
                return CdtVariableResolver.resolveToString(value,
                        getMacroSubstitutor(info, nonexistentMacrosValue, listDelimiter));
            } catch (CdtVariableException e) {
                throw new BuildMacroException(e);
            }
        }
        return value;
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.managedbuilder.macros.IBuildMacroProvider#resolveStringListValue(java.lang.String, java.lang.String, int, java.lang.Object)
     */
    @Override
    public String[] resolveStringListValue(String value, String nonexistentMacrosValue, String listDelimiter,
            int contextType, Object contextData) throws BuildMacroException {

        IMacroContextInfo info = getMacroContextInfo(contextType, contextData);
        if (info != null) {
            try {
                return CdtVariableResolver.resolveToStringList(value,
                        getMacroSubstitutor(info, nonexistentMacrosValue, listDelimiter));
            } catch (CdtVariableException e) {
                throw new BuildMacroException(e);
            }
        }
        return null;
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.managedbuilder.macros.IBuildMacroProvider#resolveValueToMakefileFormat(java.lang.String, java.lang.String, java.lang.String, int, java.lang.Object)
     */
    @Override
    public String resolveValueToMakefileFormat(String value, String nonexistentMacrosValue, String listDelimiter,
            int contextType, Object contextData) throws BuildMacroException {

        IMacroContextInfo info = getMacroContextInfo(contextType, contextData);
        if (info != null) {
            try {
                return CdtVariableResolver.resolveToString(value,
                        getBuildfileMacroSubstitutor(info, nonexistentMacrosValue, listDelimiter));
            } catch (CdtVariableException e) {
                throw new BuildMacroException(e);
            }
        }
        return nonexistentMacrosValue;
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.managedbuilder.macros.IBuildMacroProvider#resolveStringListValueToMakefileFormat(java.lang.String, java.lang.String, int, java.lang.Object)
     */
    @Override
    public String[] resolveStringListValueToMakefileFormat(String value, String nonexistentMacrosValue,
            String listDelimiter, int contextType, Object contextData) throws BuildMacroException {

        IMacroContextInfo info = getMacroContextInfo(contextType, contextData);
        if (info != null) {
            try {
                return CdtVariableResolver.resolveToStringList(value,
                        getBuildfileMacroSubstitutor(info, nonexistentMacrosValue, listDelimiter));
            } catch (CdtVariableException e) {
                throw new BuildMacroException(e);
            }
        }
        return null;
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.managedbuilder.macros.IBuildMacroProvider#isStringListValue(java.lang.String)
     */
    @Override
    public boolean isStringListValue(String value, int contextType, Object contextData) throws BuildMacroException {
        try {
            CdtVariableResolver.resolveToStringList(value,
                    getMacroSubstitutor(getMacroContextInfo(contextType, contextData), " ", null)); //$NON-NLS-1$
        } catch (CdtVariableException e) {
            return false;
        }
        return true;

    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.managedbuilder.macros.IBuildMacroProvider#checkIntegrity(int, java.lang.Object)
     */
    @Override
    public void checkIntegrity(int contextType, Object contextData) throws BuildMacroException {

        final ICdtVariableManager mngr = CCorePlugin.getDefault().getCdtVariableManager();
        IMacroContextInfo info = getMacroContextInfo(contextType, contextData);
        IVariableSubstitutor subst = new SupplierBasedCdtVariableSubstitutor(info, null, "") { //$NON-NLS-1$
            @Override
            protected ResolvedMacro resolveMacro(ICdtVariable macro) throws CdtVariableException {
                IStringVariable var = mngr.toEclipseVariable(macro, null);
                if (var instanceof IDynamicVariable)
                    return new ResolvedMacro(macro.getName(), ""); //$NON-NLS-1$
                return super.resolveMacro(macro);
            }
        };
        if (info != null) {
            try {
                CdtVariableResolver.checkIntegrity(info, subst);
            } catch (CdtVariableException e) {
                throw new BuildMacroException(e);
            }
        }
    }

    public SupplierBasedCdtVariableSubstitutor getMacroSubstitutor(IMacroContextInfo info, String inexistentMacroValue,
            String listDelimiter) {
        return new SupplierBasedCdtVariableSubstitutor(info, inexistentMacroValue, listDelimiter);
    }

    public IVariableSubstitutor getBuildfileMacroSubstitutor(IMacroContextInfo info, String inexistentMacroValue,
            String listDelimiter) {
        return new BuildfileMacroSubstitutor(info, inexistentMacroValue, listDelimiter);
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.managedbuilder.macros.IBuildMacroProvider#resolveStringListValues(java.lang.String[], java.lang.String, java.lang.String, int, java.lang.Object)
     */
    @Override
    public String[] resolveStringListValues(String[] value, String nonexistentMacrosValue, String listDelimiter,
            int contextType, Object contextData) throws BuildMacroException {
        IMacroContextInfo info = getMacroContextInfo(contextType, contextData);
        if (info != null) {
            try {
                return CdtVariableResolver.resolveStringListValues(value,
                        getMacroSubstitutor(info, nonexistentMacrosValue, listDelimiter), true);
            } catch (CdtVariableException e) {
                throw new BuildMacroException(e);
            }
        }
        return null;
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.managedbuilder.macros.IBuildMacroProvider#resolveStringListValuesToMakefileFormat(java.lang.String[], java.lang.String, java.lang.String, int, java.lang.Object)
     */
    @Override
    public String[] resolveStringListValuesToMakefileFormat(String[] value, String nonexistentMacrosValue,
            String listDelimiter, int contextType, Object contextData) throws BuildMacroException {
        IMacroContextInfo info = getMacroContextInfo(contextType, contextData);
        if (info != null) {
            try {
                return CdtVariableResolver.resolveStringListValues(value,
                        getBuildfileMacroSubstitutor(info, nonexistentMacrosValue, listDelimiter), true);
            } catch (CdtVariableException e) {
                throw new BuildMacroException(e);
            }
        }
        return null;
    }

    /**
     * @return whether the builder used for the given configuration is capable
     *         of handling macros in the buildfile
     */
    public static boolean canKeepMacrosInBuildfile(IConfiguration cfg) {
        if (cfg != null) {
            IToolChain toolChain = cfg.getToolChain();
            if (toolChain != null)
                return canKeepMacrosInBuildfile(toolChain.getBuilder());
        }
        return false;
    }

    /**
     * @return whether the given builder is capable
     *         of handling macros in the buildfile
     */
    public static boolean canKeepMacrosInBuildfile(IBuilder builder) {
        if (builder != null) {
            String pattern = builder.getBuilderVariablePattern();
            if (pattern != null && pattern.indexOf(PATTERN_MACRO_NAME) != -1)
                return true;
        }
        return false;
    }

    /**
     * creates a macro reference in the buildfile format for the given builder.
     * If the builder can not treat macros, returns null
     */
    public static String createBuildfileMacroReference(String name, IBuilder builder) {
        String ref = null;
        if (builder != null) {
            String pattern = builder.getBuilderVariablePattern();
            if (pattern != null && pattern.indexOf(PATTERN_MACRO_NAME) != -1)
                ref = pattern.replaceAll(PATTERN_MACRO_NAME, name);
        }
        return ref;
    }

    /**
     * creates a macro reference in the buildfile format for the builder used for
     * the given configuration.
     * If the builder can not treat macros, returns null
     */
    public static String createBuildfileMacroReference(String name, IConfiguration cfg) {
        String ref = null;
        if (cfg != null) {
            IToolChain toolChain = cfg.getToolChain();
            if (toolChain != null)
                ref = createBuildfileMacroReference(name, toolChain.getBuilder());
        }
        return ref;
    }

    /**
     * @return the array of the explicit file macros, referenced in the tool's
     *         options
     *         (Explicit file macros are the file-specific macros, whose values are
     *         not provided
     *         by the tool-integrator. As a result these macros contain explicit
     *         values, but not the values
     *         specified in the format of the builder automatic variables and text
     *         functions)
     */
    //    public static IBuildMacro[] getReferencedExplitFileMacros(ITool tool) {
    //        if (tool instanceof Tool) {
    //            Tool t = (Tool) tool;
    //            ExplicitFileMacroCollector collector = new ExplicitFileMacroCollector(null);
    //            t.getToolCommandFlags(null, null, collector, getDefault());
    //            return collector.getExplicisFileMacros();
    //        }
    //        return new IBuildMacro[0];
    //    }

    static IConfiguration getConfiguration(ITool tool) {
        IBuildObject bo = tool.getParent();
        if (bo instanceof IResourceConfiguration)
            return ((IResourceConfiguration) bo).getParent();
        else if (bo instanceof IToolChain)
            return ((IToolChain) bo).getParent();
        return null;
    }

    /**
     * @return the array of the explicit file macros, referenced in the given string
     *         (Explicit file macros are the file-specific macros, whose values are
     *         not provided
     *         by the tool-integrator. As a result these macros contain explicit
     *         values, but not the values
     *         specified in the format of the builder automatic variables and text
     *         functions)
     */
    public static IBuildMacro[] getReferencedExplitFileMacros(String expression, int contextType, Object contextData) {
        ExplicitFileMacroCollector collector = new ExplicitFileMacroCollector(
                getDefault().getMacroContextInfo(contextType, contextData));
        try {
            CdtVariableResolver.resolveToString(expression, collector);
        } catch (CdtVariableException e) {
        }
        return collector.getExplicisFileMacros();
    }

}
