/*******************************************************************************
 * Copyright (c) 2017 Remain Software
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      wim.jongman@remainsoftware.com - initial API and implementation
 *******************************************************************************/
package io.sloeber.ui.actions;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.PreferencesUtil;

import io.sloeber.ui.preferences.PreferenceUtils;
@SuppressWarnings({"unused"})
public class OpenPreferencesHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
		String pageId = event.getParameter(PreferenceUtils.PREFERENCE_PARAMETER1);
		String[] pages = PreferenceUtils.getPreferencePages("io.sloeber"); //$NON-NLS-1$
		PreferenceDialog dialog = PreferencesUtil.createPreferenceDialogOn(shell, pageId, pages, null);
		try {
			dialog.getTreeViewer().getTree().getItems()[1].setExpanded(true);
		} catch (RuntimeException e) {
			/* swallow */}
		dialog.open();
		return null;
	}
}
