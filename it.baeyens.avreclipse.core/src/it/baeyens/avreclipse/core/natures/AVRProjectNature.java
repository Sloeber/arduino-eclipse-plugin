/*******************************************************************************
 * Copyright (c) 2010 Thomas Holland (thomas@innot.de) and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the GNU Public License v3
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 *
 * Contributors:
 *     Thomas Holland - initial API and implementation
 *     
 * $Id: AVRProjectNature.java 851 2010-08-07 19:37:00Z innot $
 *******************************************************************************/
/**
 * 
 */
package it.baeyens.avreclipse.core.natures;

import it.baeyens.arduino.globals.ArduinoConst;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IProjectNature;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;

/**
 * @author Thomas
 * 
 */
public class AVRProjectNature implements IProjectNature {

	private IProject	fProject	= null;

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.core.resources.IProjectNature#configure()
	 */
	public void configure() throws CoreException {

	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.core.resources.IProjectNature#deconfigure()
	 */
	public void deconfigure() throws CoreException {

	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.core.resources.IProjectNature#getProject()
	 */
	public IProject getProject() {
		return fProject;
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.eclipse.core.resources.IProjectNature#setProject(org.eclipse.core.resources.IProject)
	 */
	public void setProject(IProject project) {
		fProject = project;

	}

	public static void addAVRNature(IProject project) throws CoreException {
		

		IProjectDescription description = project.getDescription();
		String[] oldnatures = description.getNatureIds();

		// Check if the project already has an AVR nature
		for (int i = 0; i < oldnatures.length; i++) {
			if (ArduinoConst.AVRnatureid.equals(oldnatures[i]))
				return; // return if AVR nature already set
		}
		String[] newnatures = new String[oldnatures.length + 1];
		System.arraycopy(oldnatures, 0, newnatures, 0, oldnatures.length);
		newnatures[oldnatures.length] = ArduinoConst.AVRnatureid;
		description.setNatureIds(newnatures);
		project.setDescription(description, new NullProgressMonitor());
	}
}
