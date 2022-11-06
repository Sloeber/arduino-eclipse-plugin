package io.sloeber.autoBuild.Internal;

public interface IMacroContextInfoProvider {

    IMacroContextInfo getMacroContextInfo(int contextType, Object contextData);

}
