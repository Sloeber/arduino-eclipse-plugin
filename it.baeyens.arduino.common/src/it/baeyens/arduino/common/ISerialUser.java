package it.baeyens.arduino.common;

public interface ISerialUser {
    public boolean PauzePort(String PortName);

    public void ResumePort(String PortName);
}
