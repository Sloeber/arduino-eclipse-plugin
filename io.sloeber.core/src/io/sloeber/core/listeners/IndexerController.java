package io.sloeber.core.listeners;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.index.IndexerSetupParticipant;
import org.eclipse.cdt.core.model.CoreModel;
import org.eclipse.cdt.core.model.ICProject;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.Status;

import io.sloeber.core.Activator;
import io.sloeber.core.api.SloeberProject;
import io.sloeber.core.common.Common;
import io.sloeber.core.common.Const;

public class IndexerController extends IndexerSetupParticipant {
    private static Set<IProject> doNotIndexProjects = new HashSet<>();
    private static Set<IProject> indexingPosponedProjects = new HashSet<>();
    private static IndexerController theController = null;

    @Override
    public boolean postponeIndexerSetup(ICProject cProject) {
        IProject project = cProject.getProject();
        SloeberProject sloeberProject = SloeberProject.getSloeberProject(project);
        if (sloeberProject != null) {
            if (!sloeberProject.isInMemory()) {
                doNotIndexProjects.add(project);
            }
        }
        boolean ret = doNotIndexProjects.contains(project);
        if (ret) {
            Common.log(new Status(Const.SLOEBER_STATUS_DEBUG, Activator.getId(), "pospone index " + project.getName())); //$NON-NLS-1$
            indexingPosponedProjects.add(project);
        } /* else {
             Common.log(new Status(Const.SLOEBER_STATUS_DEBUG, Activator.getId(),
                     "do not pospone index " + project.getName())); //$NON-NLS-1$
          }*/
        return ret;
    }

    static public boolean isPosponed(IProject project) {
        return doNotIndexProjects.contains(project);
    }

    public static void doNotIndex(IProject project) {
        Common.log(new Status(Const.SLOEBER_STATUS_DEBUG, Activator.getId(), "Do not index " + project.getName())); //$NON-NLS-1$
        doNotIndexProjects.add(project);
        getIndexController();
    }

    public static void index(IProject project) {
        if (doNotIndexProjects.remove(project)) {
            Common.log(new Status(Const.SLOEBER_STATUS_DEBUG, Activator.getId(), "index " + project.getName())); //$NON-NLS-1$    
        }
        if (indexingPosponedProjects.contains(project)) {
            indexingPosponedProjects.remove(project);
            ICProject cProject = CoreModel.getDefault().getCModel().getCProject(project.getName());
            getIndexController().notifyIndexerSetup(cProject);
        }
    }

    public static IndexerController getIndexController() {
        if (theController == null) {
            theController = new IndexerController();
            CCorePlugin.getIndexManager().addIndexerSetupParticipant(theController);
        }
        return theController;

    }

}
