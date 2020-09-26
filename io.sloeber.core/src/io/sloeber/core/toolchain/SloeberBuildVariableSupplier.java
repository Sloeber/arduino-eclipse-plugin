package io.sloeber.core.toolchain;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import org.eclipse.cdt.managedbuilder.core.IConfiguration;
import org.eclipse.cdt.managedbuilder.envvar.IBuildEnvironmentVariable;
import org.eclipse.cdt.managedbuilder.envvar.IConfigurationEnvironmentVariableSupplier;
import org.eclipse.cdt.managedbuilder.envvar.IEnvironmentVariableProvider;

import io.sloeber.core.common.Const;

public class SloeberBuildVariableSupplier implements IConfigurationEnvironmentVariableSupplier {
	private IBuildEnvironmentVariable myValues[] = null;

	@Override
	public IBuildEnvironmentVariable getVariable(String variableName, IConfiguration configuration,
			IEnvironmentVariableProvider provider) {
		switch (variableName) {
		case Const.EXTRA_TIME_UTC : 
			setValues();
			return myValues[0];
		case Const.EXTRA_TIME_LOCAL :
			setValues();
			return myValues[1];
		case Const.EXTRA_TIME_ZONE :
			setValues();
			return myValues[2];
		case Const.EXTRA_TIME_DTS:
			setValues();
			return myValues[3];
		default:
			return null;
		}
		
	}

	@Override
	public IBuildEnvironmentVariable[] getVariables(IConfiguration configuration,
			IEnvironmentVariableProvider provider) {
		setValues();
		return myValues;
	}

	private void setValues() {
		// Build Time to set clock based on computer time
		Date d = new Date();
		GregorianCalendar cal = new GregorianCalendar();
		long current = d.getTime() / 1000;
		long timezone = cal.get(Calendar.ZONE_OFFSET) / 1000;
		long daylight = cal.get(Calendar.DST_OFFSET) / 1000;

		myValues = new IBuildEnvironmentVariable[4];
		myValues[0] = (new BuildEnvironmentVariable(Const.EXTRA_TIME_UTC , Long.toString(current))); 
		myValues[1] = (new BuildEnvironmentVariable(Const.EXTRA_TIME_LOCAL,
				Long.toString(current + timezone + daylight)));
		myValues[2] = (new BuildEnvironmentVariable(Const.EXTRA_TIME_ZONE, Long.toString(timezone))); 
		myValues[3] = (new BuildEnvironmentVariable(Const.EXTRA_TIME_DTS, Long.toString(daylight))); 

	}

}
