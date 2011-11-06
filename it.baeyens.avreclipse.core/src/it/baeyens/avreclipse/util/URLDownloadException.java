/*******************************************************************************
 * 
 * Copyright (c) 2007, 2010 Thomas Holland (thomas@innot.de) and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the GNU Public License v3
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     Thomas Holland - initial API and implementation
 *     
 * $Id: URLDownloadException.java 851 2010-08-07 19:37:00Z innot $
 *     
 *******************************************************************************/
package it.baeyens.avreclipse.util;

/**
 * Exception indicating a failed download.
 * <p>
 * The Exception message will have a nice readable error description.
 * </p>
 * Used by the URLDownloadManager
 * 
 * @see URLDownloadManager
 * @author Thomas Holland
 * @since 2.2
 * 
 * 
 */
public class URLDownloadException extends Exception {

	/**
     * 
     */
    private static final long serialVersionUID = -5579817130802768958L;

    public URLDownloadException(String message) {
    	super(message);
    }
    
    public URLDownloadException(String message, Throwable cause) {
    	super(message, cause);
    }
}
