package it.baeyens.arduino.listeners;

/**
 * his index listener makes ity possible to detect missing libraries
 * if configured to do so libraries are added automatically to the project
 */
import java.util.HashSet;
import java.util.Set;

import org.eclipse.cdt.core.index.IIndexChangeEvent;
import org.eclipse.cdt.core.index.IIndexChangeListener;
import org.eclipse.cdt.core.index.IIndexerStateEvent;
import org.eclipse.cdt.core.index.IIndexerStateListener;
import org.eclipse.core.resources.IProject;

import it.baeyens.arduino.common.InstancePreferences;
import it.baeyens.arduino.tools.Libraries;

public class IndexerListener implements IIndexChangeListener, IIndexerStateListener {
    Set<IProject> ChangedProjects = new HashSet<>();

    @Override
    public void indexChanged(IIndexChangeEvent event) {
	this.ChangedProjects.add(event.getAffectedProject().getProject());

    }

    @Override
    public void indexChanged(IIndexerStateEvent event) {

	if (event.indexerIsIdle()) {
	    if (InstancePreferences.getAutomaticallyIncludeLibraries()) {
		for (IProject curProject : this.ChangedProjects) {
		    Libraries.checkLibraries(curProject);
		}
	    }
	    this.ChangedProjects.clear();
	}
    }

}
