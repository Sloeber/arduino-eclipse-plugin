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
package io.sloeber.autoBuild.Internal;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.cdt.core.cdtvariables.CdtVariableException;
import org.eclipse.cdt.core.cdtvariables.ICdtVariable;
import org.eclipse.cdt.utils.cdtvariables.SupplierBasedCdtVariableSubstitutor;

import io.sloeber.autoBuild.api.IBuildMacro;

/**
 * This class is used by the MacroResolver to collect and present
 * the explicit file macros referenced in the given expression
 *
 * @since 3.0
 */
public class ExplicitFileMacroCollector extends SupplierBasedCdtVariableSubstitutor {
    private static final String EMPTY_STRING = ""; //$NON-NLS-1$

    private List<ICdtVariable> fMacrosList = new ArrayList<>();

    /*	public ExplicitFileMacroCollector(int contextType, Object contextData){
    		super(contextType, contextData, EMPTY_STRING, EMPTY_STRING);
    	}
    */
    public ExplicitFileMacroCollector(IMacroContextInfo contextInfo) {
        super(contextInfo, EMPTY_STRING, EMPTY_STRING);
    }

    /*
    	public ExplicitFileMacroCollector(ITool tool){
    		super(null, EMPTY_STRING, EMPTY_STRING);
    		IBuildObject bo = tool.getParent();
    		IConfiguration cfg = null;
    		if(bo instanceof IResourceConfiguration)
    			cfg = ((IResourceConfiguration)bo).getParent();
    		else if (bo instanceof IToolChain)
    			cfg = ((IToolChain)bo).getParent();
    		try{
    			setMacroContextInfo(IBuildMacroProvider.CONTEXT_CONFIGURATION,cfg);
    		}catch (BuildMacroException e){
    		}
    	}
    */
    /* (non-Javadoc)
     */
    @Override
    protected ResolvedMacro resolveMacro(ICdtVariable macro) throws CdtVariableException {
        //        if (macro instanceof MbsMacroSupplier.FileContextMacro) {
        //            MbsMacroSupplier.FileContextMacro fileMacro = (MbsMacroSupplier.FileContextMacro) macro;
        //            if (fileMacro.isExplicit())
        //                fMacrosList.add(macro);
        //            return null;
        //        }
        return super.resolveMacro(macro);
    }

    public IBuildMacro[] getExplicisFileMacros() {
        return fMacrosList.toArray(new IBuildMacro[fMacrosList.size()]);
    }

}
