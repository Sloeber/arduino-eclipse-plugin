package it.baeyens.arduino.monitor.views;

import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;

public class ComPortChanged implements ISelectionChangedListener {
    private SerialMonitor TheSerialMonitor;

    public ComPortChanged(SerialMonitor TheSerialMonitor) {
	this.TheSerialMonitor = TheSerialMonitor;
    }

    @Override
    public void selectionChanged(SelectionChangedEvent event) {
	TheSerialMonitor.ComboSerialChanged();
    }

}
