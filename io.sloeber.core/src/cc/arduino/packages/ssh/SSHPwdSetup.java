/*
 * This file is part of Arduino.
 *
 * Copyright 2015 Arduino LLC (http://www.arduino.cc/)
 *
 * Arduino is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 *
 * As a special exception, you may use this file as part of a free software
 * library without restriction.  Specifically, if other files instantiate
 * templates or use macros or inline functions from this file, or you compile
 * this file and link it with other files to produce an executable, this
 * file does not by itself cause the resulting executable to be covered by
 * the GNU General Public License.  This exception does not however
 * invalidate any other reasons why the executable file might be covered by
 * the GNU General Public License.
 */

package cc.arduino.packages.ssh;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

import cc.arduino.packages.BoardPort;
import io.sloeber.core.Messages;
import io.sloeber.core.api.PasswordManager;
import io.sloeber.core.common.Common;
import io.sloeber.core.common.Const;

@SuppressWarnings({})
public class SSHPwdSetup implements SSHClientSetupChainRing {

	@Override
	public Session setup(BoardPort port, JSch jSch) throws JSchException {
		String hostLogin = new String();
		String hostPwd = new String();
		String host = port.getBoardName();
		PasswordManager pwdManager = new PasswordManager();
		if (pwdManager.setHost(port.getBoardName())) {
			hostLogin = pwdManager.getLogin();
			hostPwd = pwdManager.getPassword();

		} else {
			// The user should set the password in the project
			// properties->arduino
			Common.log(
					new Status(IStatus.ERROR, Const.CORE_PLUGIN_ID, Messages.Upload_login_credentials_missing.replace(Messages.HOST,  host)));

			return null;
		}

		Session session = jSch.getSession(hostLogin, host, 22);

		session.setUserInfo(new NoInteractionUserInfo(hostPwd));

		return session;
	}

}
