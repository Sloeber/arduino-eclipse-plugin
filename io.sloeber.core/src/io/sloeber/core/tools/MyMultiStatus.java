package io.sloeber.core.tools;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;

import io.sloeber.core.Activator;

public class MyMultiStatus extends MultiStatus {
	public MyMultiStatus(String error) {
		super(Activator.getId(), IStatus.OK, error, null);
	}

	public void addErrors(IStatus addStatus) {

		if (addStatus.isOK()) {
			return;
		}
		int oldsev = this.getSeverity();
		add(addStatus);
		this.setSeverity(oldsev | addStatus.getSeverity());
	}
}
