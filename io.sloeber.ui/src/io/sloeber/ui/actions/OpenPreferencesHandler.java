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

import static io.sloeber.ui.Activator.*;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.PreferencesUtil;

import io.sloeber.ui.Messages;
import io.sloeber.ui.listeners.ProjectExplorerListener;
import io.sloeber.ui.preferences.PreferenceUtils;

@SuppressWarnings({ "unused" })
public class OpenPreferencesHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
		String pageId = event.getParameter(PreferenceUtils.PREFERENCE_PARAMETER1);
		String[] pages = PreferenceUtils.getPreferencePages("io.sloeber"); //$NON-NLS-1$
		PreferenceDialog dialog = null;
		if (pageId.startsWith("io.sloeber.eclipse.propertypage")) { //$NON-NLS-1$
			IProject projects[] = ProjectExplorerListener.getSelectedProjects();
			switch (projects.length) {
			case 0:
				log(new Status(IStatus.ERROR, PLUGIN_ID, Messages.no_project_found));
				break;
			case 1:
				dialog = PreferencesUtil.createPropertyDialogOn(shell, projects[0], pageId, null, null);
				break;
			default:
				log(new Status(IStatus.ERROR, PLUGIN_ID,
						Messages.arduino_upload_project_handler_multiple_projects_found
								.replace(Messages.NUMBER, Integer.toString(projects.length))
								.replace(Messages.PROJECT_LIST, projects.toString())));
			}
		} else {
			dialog = PreferencesUtil.createPreferenceDialogOn(shell, pageId, pages, null);
		}
		if (dialog == null) {
			return null;
		}
		try {
			dialog.getTreeViewer().getTree().getItems()[1].setExpanded(true);
		} catch (RuntimeException e) {
			/* swallow */}
		dialog.open();
		return null;
	}
}
