package io.sloeber.autoBuild.Internal;

import org.eclipse.cdt.utils.cdtvariables.ICdtVariableSupplier;
import org.eclipse.cdt.utils.cdtvariables.IVariableContextInfo;

public class DefaultMacroContextInfo implements IMacroContextInfo {

    public DefaultMacroContextInfo(int contextType, Object contextData) {
        // TODO Auto-generated constructor stub
    }

    @Override
    public ICdtVariableSupplier[] getSuppliers() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IVariableContextInfo getNext() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int getContextType() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public Object getContextData() {
        // TODO Auto-generated method stub
        return null;
    }

}
