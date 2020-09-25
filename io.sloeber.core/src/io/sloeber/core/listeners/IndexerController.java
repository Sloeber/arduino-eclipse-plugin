package io.sloeber.core.listeners;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.index.IndexerSetupParticipant;
import org.eclipse.cdt.core.model.CoreModel;
import org.eclipse.cdt.core.model.ICProject;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import io.sloeber.core.Activator;
import io.sloeber.core.common.Common;

public class IndexerController extends IndexerSetupParticipant  {
	private static Set<IProject> fProjects = new HashSet<>();
	private static IndexerController theController=null;
	@Override
	public boolean postponeIndexerSetup(ICProject cProject) {
		IProject project=cProject.getProject();
		boolean ret= fProjects.contains(project);
		if(ret) {
			Common.log(new Status(IStatus.WARNING, Activator.getId(),"pospone index "+project.getName()));
		}
		else {
			Common.log(new Status(IStatus.WARNING, Activator.getId(),"do not pospone index "+project.getName()));
		}
		return ret;
	}
	public static void doNotIndex(IProject project) {
		Common.log(new Status(IStatus.WARNING, Activator.getId(),"Do not index "+project.getName()));
		fProjects.add(project);
		getIndexController();
	}
	public static void Index(IProject project) {
		Common.log(new Status(IStatus.WARNING, Activator.getId(),"index "+project.getName()));
		fProjects.remove(project);
		ICProject cProject = CoreModel.getDefault().getCModel().getCProject(project.getName());
		getIndexController().notifyIndexerSetup(cProject);
	}

	public static IndexerController getIndexController() {
		if (theController==null) {
			theController=new IndexerController();
			CCorePlugin.getIndexManager().addIndexerSetupParticipant(theController);
		}
		return theController;
		
	}
	
}
