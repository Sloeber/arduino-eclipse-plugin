package it.baeyens.arduino.tools.uploaders;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.UserInfo;

import it.baeyens.arduino.common.ArduinoConst;

public class SSHPrompt {
    String host = ArduinoConst.EMPTY_STRING;
    String user = ArduinoConst.EMPTY_STRING;
    String passwd = ArduinoConst.EMPTY_STRING;

    public void test() {

	try {
	    JSch jsch = new JSch();

	    Session session = jsch.getSession(this.user, this.host, 22);
	    session.setPassword(this.passwd);

	    MyUserInfo ui = new MyUserInfo(this.user, this.passwd);

	    session.setUserInfo(ui);

	    // It must not be recommended, but if you want to skip host-key check,
	    // invoke following,
	    // session.setConfig("StrictHostKeyChecking", "no");

	    // session.connect();
	    session.connect(30000); // making a connection with timeout.

	    Channel channel = session.openChannel("shell"); //$NON-NLS-1$

	    // Enable agent-forwarding.
	    // ((ChannelShell)channel).setAgentForwarding(true);

	    channel.setInputStream(System.in);
	    /*
	     * // a hack for MS-DOS prompt on Windows. channel.setInputStream(new FilterInputStream(System.in){ public int read(byte[] b, int off, int
	     * len)throws IOException{ return in.read(b, off, (len>1024?1024:len)); } });
	     */

	    channel.setOutputStream(System.out);

	    /*
	     * // Choose the pty-type "vt102". ((ChannelShell)channel).setPtyType("vt102");
	     */

	    /*
	     * // Set environment variable "LANG" as "ja_JP.eucJP". ((ChannelShell)channel).setEnv("LANG", "ja_JP.eucJP");
	     */

	    // channel.connect();
	    channel.connect(3 * 1000);
	} catch (Exception e) {
	    System.out.println(e);
	}
    }

    public class MyUserInfo implements UserInfo {

	String myUser;
	String myPasswd;

	public MyUserInfo(String user, String passwd) {
	    this.myUser = user;
	    this.myPasswd = passwd;
	}

	@Override
	public String getPassphrase() {
	    // TODO Auto-generated method stub
	    return null;
	}

	@Override
	public String getPassword() {
	    // TODO Auto-generated method stub
	    return this.myPasswd;
	}

	@Override
	public boolean promptPassphrase(String arg0) {
	    // TODO Auto-generated method stub
	    return false;
	}

	@Override
	public boolean promptPassword(String arg0) {
	    // TODO Auto-generated method stub
	    return false;
	}

	@Override
	public boolean promptYesNo(String arg0) {
	    // TODO Auto-generated method stub
	    return false;
	}

	@Override
	public void showMessage(String arg0) {
	    // TODO Auto-generated method stub

	}
    }

    // public String getPassword(){ return null; }
    // public boolean promptYesNo(String str){ return false; }
    // public String getPassphrase(){ return null; }
    // public boolean promptPassphrase(String message){ return false; }
    // public boolean promptPassword(String message){ return false; }
    // public void showMessage(String message){ }
    // public String[] promptKeyboardInteractive(String destination,
    // String name,
    // String instruction,
    // String[] prompt,
    // boolean[] echo){
    // return null;
    // }
    // }
}
