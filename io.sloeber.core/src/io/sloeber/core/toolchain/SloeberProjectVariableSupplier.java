package io.sloeber.core.toolchain;

import static io.sloeber.core.common.Const.*;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.cdt.managedbuilder.core.IManagedProject;
import org.eclipse.cdt.managedbuilder.envvar.IBuildEnvironmentVariable;
import org.eclipse.cdt.managedbuilder.envvar.IEnvironmentVariableProvider;
import org.eclipse.cdt.managedbuilder.envvar.IProjectEnvironmentVariableSupplier;

import io.sloeber.core.common.Common;

//SloeberConfigurationVariableSupplier
public class SloeberProjectVariableSupplier implements IProjectEnvironmentVariableSupplier {



    private static BuildEnvironmentVariable get_EXTRA_TIME_UTC() {
        Date d = new Date();
        long current = d.getTime() / 1000;
        return new BuildEnvironmentVariable(EXTRA_TIME_UTC, Long.toString(current));
    }

    private static BuildEnvironmentVariable get_EXTRA_TIME_LOCAL() {
        Date d = new Date();
        long current = d.getTime() / 1000;
        GregorianCalendar cal = new GregorianCalendar();
        long timezone = cal.get(Calendar.ZONE_OFFSET) / 1000;
        long daylight = cal.get(Calendar.DST_OFFSET) / 1000;
        return new BuildEnvironmentVariable(EXTRA_TIME_LOCAL, Long.toString(current + timezone + daylight));
    }

    private static BuildEnvironmentVariable get_EXTRA_TIME_ZONE() {
        GregorianCalendar cal = new GregorianCalendar();
        long timezone = cal.get(Calendar.ZONE_OFFSET) / 1000;
        return new BuildEnvironmentVariable(EXTRA_TIME_ZONE, Long.toString(timezone));
    }

    private static BuildEnvironmentVariable get_EXTRA_TIME_DTS() {
        GregorianCalendar cal = new GregorianCalendar();
        long daylight = cal.get(Calendar.DST_OFFSET) / 1000;
        return new BuildEnvironmentVariable(EXTRA_TIME_DTS, Long.toString(daylight));
    }

    private static BuildEnvironmentVariable get_SLOEBER_HOME() {
        return new BuildEnvironmentVariable(SLOEBER_HOME, Common.sloeberHome);
    }


    @Override
    public IBuildEnvironmentVariable getVariable(String variableName, IManagedProject project,
            IEnvironmentVariableProvider provider) {
        switch (variableName) {
        case EXTRA_TIME_UTC:
            return get_EXTRA_TIME_UTC();
        case EXTRA_TIME_LOCAL:
            return get_EXTRA_TIME_LOCAL();
        case EXTRA_TIME_ZONE:
            return get_EXTRA_TIME_ZONE();
        case EXTRA_TIME_DTS:
            return get_EXTRA_TIME_DTS();
        case SLOEBER_HOME:
            return get_SLOEBER_HOME();
        }
        return null;
    }

    @Override
    public IBuildEnvironmentVariable[] getVariables(IManagedProject project, IEnvironmentVariableProvider provider) {
        Map<String, BuildEnvironmentVariable> retValues = new HashMap<>();
        retValues.put(EXTRA_TIME_UTC, get_EXTRA_TIME_UTC());
        retValues.put(EXTRA_TIME_LOCAL, get_EXTRA_TIME_LOCAL());
        retValues.put(EXTRA_TIME_ZONE, get_EXTRA_TIME_ZONE());
        retValues.put(EXTRA_TIME_DTS, get_EXTRA_TIME_DTS());
        retValues.put(SLOEBER_HOME, get_SLOEBER_HOME());
        return retValues.values().toArray(new BuildEnvironmentVariable[retValues.size()]);
    }

}
