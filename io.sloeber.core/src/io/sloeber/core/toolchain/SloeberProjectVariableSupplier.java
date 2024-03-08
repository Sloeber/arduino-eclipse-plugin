package io.sloeber.core.toolchain;

import static io.sloeber.core.api.Const.*;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.cdt.core.envvar.EnvironmentVariable;
import org.eclipse.cdt.core.envvar.IEnvironmentVariable;
import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;

import io.sloeber.autoBuild.api.IEnvironmentVariableProvider;
import io.sloeber.core.api.Common;

public class SloeberProjectVariableSupplier implements IEnvironmentVariableProvider {

    private static IEnvironmentVariable get_EXTRA_TIME_UTC() {
        Date d = new Date();
        long current = d.getTime() / 1000;
        return new EnvironmentVariable(EXTRA_TIME_UTC, Long.toString(current));
    }

    private static IEnvironmentVariable get_EXTRA_TIME_LOCAL() {
        Date d = new Date();
        long current = d.getTime() / 1000;
        GregorianCalendar cal = new GregorianCalendar();
        long timezone = cal.get(Calendar.ZONE_OFFSET) / 1000;
        long daylight = cal.get(Calendar.DST_OFFSET) / 1000;
        return new EnvironmentVariable(EXTRA_TIME_LOCAL, Long.toString(current + timezone + daylight));
    }

    private static IEnvironmentVariable get_EXTRA_TIME_ZONE() {
        GregorianCalendar cal = new GregorianCalendar();
        long timezone = cal.get(Calendar.ZONE_OFFSET) / 1000;
        return new EnvironmentVariable(EXTRA_TIME_ZONE, Long.toString(timezone));
    }

    private static IEnvironmentVariable get_EXTRA_TIME_DTS() {
        GregorianCalendar cal = new GregorianCalendar();
        long daylight = cal.get(Calendar.DST_OFFSET) / 1000;
        return new EnvironmentVariable(EXTRA_TIME_DTS, Long.toString(daylight));
    }

    private static IEnvironmentVariable get_SLOEBER_HOME() {
        return new EnvironmentVariable(SLOEBER_HOME, Common.sloeberHome);
    }

    private static IEnvironmentVariable get_RUNTIME_IDE_PATH() {
        return new EnvironmentVariable(RUNTIME_IDE_PATH, Common.sloeberHome);
    }

    @Override
    public IEnvironmentVariable getVariable(String variableName, ICConfigurationDescription cfg,
            boolean resolveMacros) {
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
        case RUNTIME_IDE_PATH:
            return get_RUNTIME_IDE_PATH();
		default:
			break;
        }
        return null;
    }

    @Override
    public IEnvironmentVariable[] getVariables(ICConfigurationDescription cfg, boolean resolveMacros) {
        Map<String, IEnvironmentVariable> retValues = new HashMap<>();
        retValues.put(EXTRA_TIME_UTC, get_EXTRA_TIME_UTC());
        retValues.put(EXTRA_TIME_LOCAL, get_EXTRA_TIME_LOCAL());
        retValues.put(EXTRA_TIME_ZONE, get_EXTRA_TIME_ZONE());
        retValues.put(EXTRA_TIME_DTS, get_EXTRA_TIME_DTS());
        retValues.put(SLOEBER_HOME, get_SLOEBER_HOME());
        retValues.put(RUNTIME_IDE_PATH, get_RUNTIME_IDE_PATH());

        return retValues.values().toArray(new IEnvironmentVariable[retValues.size()]);
    }

}
