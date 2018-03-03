package io.sloeber.core.toolchain;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import org.eclipse.cdt.managedbuilder.core.IConfiguration;
import org.eclipse.cdt.managedbuilder.envvar.IBuildEnvironmentVariable;
import org.eclipse.cdt.managedbuilder.envvar.IConfigurationEnvironmentVariableSupplier;
import org.eclipse.cdt.managedbuilder.envvar.IEnvironmentVariableProvider;

public class SloeberBuildVariableSupplier implements IConfigurationEnvironmentVariableSupplier {
	private IBuildEnvironmentVariable myValues[] = null;

	@Override
	public IBuildEnvironmentVariable getVariable(String variableName, IConfiguration configuration,
			IEnvironmentVariableProvider provider) {
		switch (variableName) {
		case "A.EXTRA.TIME.UTC": //$NON-NLS-1$
			setValues();
			return myValues[0];
		case "A.EXTRA.TIME.LOCAL": //$NON-NLS-1$
			setValues();
			return myValues[1];
		case "A.EXTRA.TIME.ZONE": //$NON-NLS-1$
			setValues();
			return myValues[2];
		case "A.EXTRA.TIME.DTS": //$NON-NLS-1$
			setValues();
			return myValues[3];
		}
		return null;
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
		myValues[0] = (new BuildEnvironmentVariable("A.EXTRA.TIME.UTC", Long.toString(current))); //$NON-NLS-1$
		myValues[1] = (new BuildEnvironmentVariable("A.EXTRA.TIME.LOCAL", //$NON-NLS-1$
				Long.toString(current + timezone + daylight)));
		myValues[2] = (new BuildEnvironmentVariable("A.EXTRA.TIME.ZONE", Long.toString(timezone))); //$NON-NLS-1$
		myValues[3] = (new BuildEnvironmentVariable("A.EXTRA.TIME.DTS", Long.toString(daylight))); //$NON-NLS-1$

	}

}
