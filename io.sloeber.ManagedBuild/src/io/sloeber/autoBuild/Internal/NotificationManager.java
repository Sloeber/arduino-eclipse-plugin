/*******************************************************************************
 * Copyright (c) 2007, 2010 Intel Corporation and others.
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
package io.sloeber.autoBuild.Internal;

import io.sloeber.autoBuild.api.IHoldsOptions;
import io.sloeber.autoBuild.api.IOption;
import io.sloeber.autoBuild.api.IResourceInfo;

//import org.eclipse.cdt.managedbuilder.core.IHoldsOptions;
//import org.eclipse.cdt.managedbuilder.core.IOption;
//import org.eclipse.cdt.managedbuilder.core.IResourceInfo;

public class NotificationManager /*implements ISettingsChangeListener */ {
    private static NotificationManager fInstance;
    //private List<ISettingsChangeListener> fListeners;

    private NotificationManager() {
        //  fListeners = new CopyOnWriteArrayList<>();
    }

    public static NotificationManager getInstance() {
        if (fInstance == null)
            fInstance = new NotificationManager();
        return fInstance;
    }

    public void optionRemoved(IResourceInfo rcInfo, IHoldsOptions holder, IOption option) {
        //        SettingsChangeEvent event = createOptionRemovedEvent(rcInfo, holder, option);
        //        notifyListeners(event);
    }

    public void optionChanged(IResourceInfo rcInfo, IHoldsOptions holder, IOption option, Object oldValue) {
        //        SettingsChangeEvent event = createOptionChangedEvent(rcInfo, holder, option, oldValue);
        //        notifyListeners(event);
    }

    //    private void notifyListeners(SettingsChangeEvent event) {
    //        for (ISettingsChangeListener listener : fListeners)
    //            listener.settingsChanged(event);
    //    }
    //
    //    private static SettingsChangeEvent createOptionChangedEvent(IResourceInfo rcInfo, IHoldsOptions holder,
    //            IOption option, Object oldValue) {
    //        return new SettingsChangeEvent(SettingsChangeEvent.CHANGED, rcInfo, holder, option, oldValue);
    //    }
    //
    //    private static SettingsChangeEvent createOptionRemovedEvent(IResourceInfo rcInfo, IHoldsOptions holder,
    //            IOption option) {
    //        return new SettingsChangeEvent(SettingsChangeEvent.REMOVED, rcInfo, holder, option, null);
    //    }
    //
    //    public void subscribe(ISettingsChangeListener listener) {
    //        fListeners.add(listener);
    //    }
    //
    //    public void unsubscribe(ISettingsChangeListener listener) {
    //        fListeners.remove(listener);
    //    }

}
