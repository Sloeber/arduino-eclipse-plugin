package io.sloeber.core.api;

/*
 * this interface allows none gui core classes to control the gui driven serial communication
 * 
 * You should not use this class directly but use the static methods in SerialManager 
 * as the serial monitor registers on the serialManager and as sutch allowing for gui 
 * actions from the core
 * 
 * 
 */
public interface ISerialUser {

    /**
     * Pause the serial monitor
     * This is used for uploads before the upload is started.
     * After the pause a resumePort call is expected
     * 
     * @param PortName
     * @return true if the pause was executed correctly
     */
    public boolean pausePort(String PortName);

    /**
     * Resume the serial monitor
     * This is used for uploads after the upload is done.
     * A pausePort should have been called before calling this method
     * 
     * @param PortName
     */
    public void resumePort(String PortName);

    /**
     * Remove the serial connection from the serial monitor
     * 
     * @param mComPort
     * @return
     */
    public boolean stopPort(String mComPort);
}
