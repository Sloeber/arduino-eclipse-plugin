package io.sloeber.core.toolchain;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.cdt.managedbuilder.core.IConfiguration;
import org.eclipse.cdt.managedbuilder.envvar.IBuildEnvironmentVariable;
import org.eclipse.cdt.managedbuilder.envvar.IConfigurationEnvironmentVariableSupplier;
import org.eclipse.cdt.managedbuilder.envvar.IEnvironmentVariableProvider;

import io.sloeber.core.common.Const;

public class SloeberBuildVariableSupplier implements IConfigurationEnvironmentVariableSupplier {
    private Map<String, BuildEnvironmentVariable> myValues = new HashMap<>();

    @Override
    public IBuildEnvironmentVariable getVariable(String variableName, IConfiguration configuration,
            IEnvironmentVariableProvider provider) {
        switch (variableName) {
        case Const.EXTRA_TIME_UTC:
            return get_EXTRA_TIME_UTC();
        case Const.EXTRA_TIME_LOCAL:
            return get_EXTRA_TIME_LOCAL();
        case Const.EXTRA_TIME_ZONE:
            return get_EXTRA_TIME_ZONE();
        case Const.EXTRA_TIME_DTS:
            return get_EXTRA_TIME_DTS();
        default:
            return myValues.get(variableName);
        }

    }

    @Override
    public IBuildEnvironmentVariable[] getVariables(IConfiguration configuration,
            IEnvironmentVariableProvider provider) {
        myValues.put(Const.EXTRA_TIME_UTC, get_EXTRA_TIME_UTC());
        myValues.put(Const.EXTRA_TIME_LOCAL, get_EXTRA_TIME_LOCAL());
        myValues.put(Const.EXTRA_TIME_ZONE, get_EXTRA_TIME_ZONE());
        myValues.put(Const.EXTRA_TIME_DTS, get_EXTRA_TIME_DTS());
        return myValues.values().toArray(new BuildEnvironmentVariable[myValues.size()]);
    }

    private static BuildEnvironmentVariable get_EXTRA_TIME_UTC() {
        Date d = new Date();
        long current = d.getTime() / 1000;
        return new BuildEnvironmentVariable(Const.EXTRA_TIME_UTC, Long.toString(current));
    }

    private static BuildEnvironmentVariable get_EXTRA_TIME_LOCAL() {
        Date d = new Date();
        long current = d.getTime() / 1000;
        GregorianCalendar cal = new GregorianCalendar();
        long timezone = cal.get(Calendar.ZONE_OFFSET) / 1000;
        long daylight = cal.get(Calendar.DST_OFFSET) / 1000;
        return new BuildEnvironmentVariable(Const.EXTRA_TIME_LOCAL, Long.toString(current + timezone + daylight));
    }

    private static BuildEnvironmentVariable get_EXTRA_TIME_ZONE() {
        GregorianCalendar cal = new GregorianCalendar();
        long timezone = cal.get(Calendar.ZONE_OFFSET) / 1000;
        return new BuildEnvironmentVariable(Const.EXTRA_TIME_ZONE, Long.toString(timezone));
    }

    private static BuildEnvironmentVariable get_EXTRA_TIME_DTS() {
        GregorianCalendar cal = new GregorianCalendar();
        long daylight = cal.get(Calendar.DST_OFFSET) / 1000;
        return new BuildEnvironmentVariable(Const.EXTRA_TIME_DTS, Long.toString(daylight));
    }

}
