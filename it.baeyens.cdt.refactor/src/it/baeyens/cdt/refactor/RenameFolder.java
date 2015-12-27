package it.baeyens.cdt.refactor;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.CheckConditionsContext;
import org.eclipse.ltk.core.refactoring.participants.RenameArguments;
import org.eclipse.ltk.core.refactoring.participants.RenameParticipant;

public class RenameFolder extends RenameParticipant {
    private String orgName;
    private IProject myProject;

    /**
     * helper function that test whether a project is a cdt project or not. Because the handler needs the same functionality this method is public and
     * static.
     * 
     * @return true if it is a cdt project; false in all other cases.
     */
    static public boolean isCDTProject(IProject project) {
	try {
	    if (project.getNature("org.eclipse.cdt.core.cnature") != null) { //$NON-NLS-1$
		return true;// this is a cdt project
	    }
	} catch (CoreException e) {
	    e.printStackTrace();

	}
	return false;// this is not a cdt project
    }

    /**
     * We only handle folder renames of folders in cdt projects and cdt project renames. All other renames are ignored by this participant.
     */
    @Override
    protected boolean initialize(Object element) {
	if (element instanceof IFolder) { // it is a folder rename
	    IFolder theFolder = (IFolder) element;
	    this.myProject = theFolder.getProject();
	    this.orgName = theFolder.getFullPath().toString();
	    return isCDTProject(this.myProject);
	} else if (element instanceof IProject) {
	    this.myProject = (IProject) element;
	    this.orgName = "/" + this.myProject.getName(); //$NON-NLS-1$
	    return isCDTProject(this.myProject);
	}
	return false;
    }

    @Override
    public String getName() {
	return "Arduino environment adaptor"; //$NON-NLS-1$
    }

    @Override
    public RefactoringStatus checkConditions(IProgressMonitor pm, CheckConditionsContext context) throws OperationCanceledException {
	// We only do some kind of cleanup so we are OK with the change
	return null;
    }

    @Override
    public Change createChange(IProgressMonitor pm) throws CoreException, OperationCanceledException {
	RenameArguments args = getArguments();
	return new RenameFolderChangeHandler(this.orgName, args.getNewName());
    }
}
