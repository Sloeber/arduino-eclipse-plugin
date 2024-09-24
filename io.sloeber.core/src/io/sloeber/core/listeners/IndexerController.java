package io.sloeber.core.listeners;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.index.IndexerSetupParticipant;
import org.eclipse.cdt.core.model.CoreModel;
import org.eclipse.cdt.core.model.ICProject;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import io.sloeber.core.Activator;
import io.sloeber.core.natures.SloeberNature;

public class IndexerController extends IndexerSetupParticipant {
    private static Set<String> doNotIndexProjects = new HashSet<>();
    private static Set<String> indexingPosponedProjects = new HashSet<>();
    private static IndexerController myIndexController = null;
    private static final String managedBuildNature="org.eclipse.cdt.managedbuilder.core.managedBuildNature"; //$NON-NLS-1$

    @Override
    public boolean postponeIndexerSetup(ICProject cProject) {
        IProject project = cProject.getProject();
        String projectName=project.getName();
        boolean ret=false;
        if( doNotIndexProjects.contains(projectName)) {
        	ret=true;
        }else {
        	try {
				if(project.hasNature(SloeberNature.NATURE_ID) &&project.hasNature(managedBuildNature)) {
					ret=true;
				}
			} catch (CoreException e) {
				e.printStackTrace();
				ret = false;
			}
        }
        if (ret) {
            Activator.log(new Status(IStatus.INFO, Activator.getId(), "pospone index " + projectName)); //$NON-NLS-1$
            indexingPosponedProjects.add(projectName);
        } /* else {
             Common.log(new Status(Const.SLOEBER_STATUS_DEBUG, Activator.getId(),
                     "do not pospone index " + project.getName())); //$NON-NLS-1$
          }*/
        return ret;
    }

    static public boolean isPosponed(String projectName) {
        return doNotIndexProjects.contains(projectName);
    }

    public static void doNotIndex(IProject project) {
    	String projectName=project.getName();
        Activator.log(new Status(IStatus.INFO, Activator.getId(), "Do not index " + projectName)); //$NON-NLS-1$
        doNotIndexProjects.add(projectName);
    }

    public static void index(IProject project) {
    	String projectName=project.getName();
        if (doNotIndexProjects.remove(projectName)) {
            Activator.log(new Status(IStatus.INFO, Activator.getId(), "index " + projectName)); //$NON-NLS-1$
        }
        if (indexingPosponedProjects.contains(projectName)) {
            indexingPosponedProjects.remove(projectName);
            ICProject cProject = CoreModel.getDefault().getCModel().getCProject(projectName);
            myIndexController.notifyIndexerSetup(cProject);
        }
    }

    public static void registerIndexerController() {
        if (myIndexController == null) {
            myIndexController = new IndexerController();
            CCorePlugin.getIndexManager().addIndexerSetupParticipant(myIndexController);
        }
    }

	public static void unRegisterIndexerController() {
        if (myIndexController != null) {
            CCorePlugin.getIndexManager().removeIndexerSetupParticipant(myIndexController);
            myIndexController=null;
        }

	}

}
