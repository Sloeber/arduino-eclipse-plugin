package io.sloeber.ui.monitor.views;

import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;

public class ComPortChanged implements ISelectionChangedListener {
    private SerialMonitor theSerialMonitor;

    public ComPortChanged(SerialMonitor theSerialMonitor) {
	this.theSerialMonitor = theSerialMonitor;
    }

    @Override
    public void selectionChanged(SelectionChangedEvent event) {
	this.theSerialMonitor.ComboSerialChanged();
    }

}
