package io.sloeber.autoBuild.Internal;

import org.eclipse.cdt.utils.cdtvariables.IVariableContextInfo;

public interface IMacroContextInfo extends IVariableContextInfo {
    /**
     * returns the context type
     *
     * @return int
     */
    public int getContextType();

    /**
     * returns the context data
     *
     * @return Object
     */
    public Object getContextData();
}
