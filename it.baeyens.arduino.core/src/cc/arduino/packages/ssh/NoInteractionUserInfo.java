package cc.arduino.packages.ssh;

import com.jcraft.jsch.UserInfo;

public class NoInteractionUserInfo implements UserInfo {

    private final String password;

    public NoInteractionUserInfo(String password) {
	this.password = password;
    }

    @Override
    public String getPassword() {
	return this.password;
    }

    @Override
    public boolean promptYesNo(String str) {
	return true;
    }

    @Override
    public String getPassphrase() {
	return this.password;
    }

    @Override
    public boolean promptPassphrase(String message) {
	return true;
    }

    @Override
    public boolean promptPassword(String message) {
	return true;
    }

    @Override
    public void showMessage(String message) {
	// no code needed here
    }

}
