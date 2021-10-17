package io.sloeber.core.api;

public interface ISerialUser {
    public boolean PauzePort(String PortName);

    public void ResumePort(String PortName);

    public boolean stopPort(String mComPort);
}
