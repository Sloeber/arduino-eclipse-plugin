/*******************************************************************************
 * 
 * Copyright (c) 2008, 2010 Thomas Holland (thomas@innot.de) and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the GNU Public License v3
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     Thomas Holland - initial API and implementation
 *     
 * $Id: AVRDudeException.java 851 2010-08-07 19:37:00Z innot $
 *     
 *******************************************************************************/
package it.baeyens.avreclipse.core.avrdude;

import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * Wrapper for all Exceptions that may be thrown when accessing avrdude.
 * <p>
 * This Exceptions contains a reason, set when creating the Exception and readable with
 * {@link #getReason()}. This is used by the {@link AVRDudeErrorDialog} to display a human readable
 * detailed description of the error.
 * </p>
 * 
 * @author Thomas Holland
 * @since 2.2
 * 
 */
public class AVRDudeException extends Exception {

	private static final long	serialVersionUID	= 1L;

	public enum Reason {
		UNKNOWN, NO_AVRDUDE_FOUND, CANT_ACCESS_AVRDUDE, CONFIG_NOT_FOUND, UNKNOWN_MCU, UNKNOWN_PROGRAMMER, NO_PROGRAMMER, PORT_BLOCKED, NO_USB, TIMEOUT, PARSE_ERROR, INVALID_CWD, USER_CANCEL, SYNC_FAIL, INIT_FAIL, NO_TARGET_POWER, INVALID_PORT, USB_RECEIVE_ERROR;
	}

	/** The Reason for the exception */
	private Reason	fReason;

	/**
	 * Instantiate a new AVRDudeException with the given reason.
	 * 
	 * @param reason
	 *            Enum <code>Reason</code> for the reason of this Exception.
	 * @param message
	 *            the detail message (which is saved for later retrieval by the
	 *            <code>getMessage()</code> method).
	 */
	public AVRDudeException(Reason reason, String message) {
		this(reason, message, null);
	}

	/**
	 * Instantiate a new AVRDudeException with the given reason and the root Exception.
	 * 
	 * @param reason
	 *            Enum <code>Reason</code> for the reason of this Exception.
	 * @param message
	 *            the detail message (which is saved for later retrieval by the
	 *            <code>getMessage()</code> method).
	 * @param cause
	 *            the cause (which is saved for later retrieval by the <code>getCause()</code>
	 *            method). (A <code>null</code> value is permitted, and indicates that the cause is
	 *            nonexistent or unknown.)
	 */
	public AVRDudeException(Reason reason, String message, Throwable cause) {
		super(message, cause);
		fReason = reason;
	}

	/**
	 * Instantiate a new AVRDudeException with the given root Exception.
	 * <p>
	 * If the given Exception matches some predefined Exceptions, a reason will be set. Otherwise
	 * <code>Reason.UNKNOWN</code> will be used.
	 * </p>
	 * 
	 * @param exc
	 *            Root <code>Exception</code>
	 */
	public AVRDudeException(Exception exc) {
		super(exc);
		if (exc instanceof FileNotFoundException) {
			fReason = Reason.NO_AVRDUDE_FOUND;
		} else if (exc instanceof IOException) {
			fReason = Reason.CANT_ACCESS_AVRDUDE;
		} else {
			fReason = Reason.UNKNOWN;
		}
	}

	/**
	 * Get the reason for this Exception.
	 * 
	 * @return Enum <code>Reason</code>
	 */
	public Reason getReason() {
		return fReason;
	}
}
