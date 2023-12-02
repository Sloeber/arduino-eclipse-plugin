/*******************************************************************************
 * Copyright (c) 2005, 2010 Intel Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * Intel Corporation - Initial API and implementation
 *******************************************************************************/
package io.sloeber.autoBuild.api;

import org.eclipse.cdt.core.cdtvariables.CdtVariableException;
import org.eclipse.core.runtime.IStatus;

/**
 * This exception is thrown in the case of some build macros-related operation failure
 * The exception typically contains one or more IBuildMacroStatus statuses
 *
 * @since 3.0
 * @noextend This class is not intended to be subclassed by clients.
 */
public class BuildMacroException extends CdtVariableException {
	/**
	 * All serializable objects should have a stable serialVersionUID
	 */
	private static final long serialVersionUID = 3976741380246681395L;

	/**
	 * Creates a new exception with the given status object.
	 *
	 * @param status the status object to be associated with this exception.
	 * Typically this is either the IBuildMacroStatus or the MultiStatus that holds
	 * the list of the IBuildMacroStatus statuses
	 */
	public BuildMacroException(IStatus status) {
		super(status);
	}

	//	/**
	//	 * Creates an exception containing a single IBuildMacroStatus status with the IStatus.ERROR severity
	//	 *
	//	 * @param code one of the IBuildMacroStatus.TYPE_xxx statusses
	//	 * @param message message, can be null. In this case the default message will
	//	 *  be generated base upon the other status info
	//	 * @param exception a low-level exception, or <code>null</code> if not
	//	 *    applicable
	//	 * @param macroName the name of the build macro whose resolution caused this status creation or null if none
	//	 * @param expression the string whose resolutinon caused caused this status creation or null if none
	//	 * @param referencedName the macro name referenced in the resolution string that caused this this status creation or null if none
	//	 * @param contextType the context type used in the operation
	//	 * @param contextData the context data used in the operation
	//	 */
	//	public BuildMacroException(int code,
	//			String message,
	//			Throwable exception,
	//			String macroName,
	//			String expression,
	//			String referencedName,
	//			int contextType,
	//			Object contextData) {
	//		super(new BuildMacroStatus(code, message, exception, macroName, expression, referencedName, contextType, contextData));
	//	}

	//	/**
	//	 * Creates an exception containing a single IBuildMacroStatus status with the IStatus.ERROR severity and with the default message
	//	 *
	//	 * @param code one of the IBuildMacroStatus.TYPE_xxx statusses
	//	 * @param exception a low-level exception, or <code>null</code> if not
	//	 *    applicable
	//	 * @param macroName the name of the build macro whose resolution caused this status creation or null if none
	//	 * @param expression the string whose resolutinon caused caused this status creation or null if none
	//	 * @param referencedName the macro name referenced in the resolution string that caused this this status creation or null if none
	//	 * @param contextType the context type used in the operation
	//	 * @param contextData the context data used in the operation
	//	 */
	//	public BuildMacroException(int code,
	//			String macroName,
	//			String expression,
	//			String referencedName,
	//			int contextType,
	//			Object contextData) {
	//		super(new BuildMacroStatus(code, macroName, expression, referencedName, contextType, contextData));
	//	}

	public BuildMacroException(CdtVariableException e) {
		super(e.getStatus());
	}

	/**
	 * Returns an array of the IBuildMacroStatus statuses this exception holds
	 *
	 * @return IBuildMacroStatus[]
	 */
	public IBuildMacroStatus[] getMacroStatuses() {
		IStatus status = getStatus();
		if (status instanceof IBuildMacroStatus)
			return new IBuildMacroStatus[] { (IBuildMacroStatus) status };
		else if (status.isMultiStatus()) {
			IStatus children[] = status.getChildren();
			IBuildMacroStatus result[] = new IBuildMacroStatus[children.length];
			int num = 0;
			for (int i = 0; i < children.length; i++) {
				if (children[i] instanceof IBuildMacroStatus)
					result[num++] = (IBuildMacroStatus) children[i];
			}
			if (num != children.length) {
				IBuildMacroStatus tmp[] = new IBuildMacroStatus[num];
				for (int i = 0; i < num; i++)
					tmp[i] = result[i];
				result = tmp;
			}
			return result;
		}
		return new IBuildMacroStatus[0];
	}

}
