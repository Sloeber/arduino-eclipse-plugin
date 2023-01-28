/*******************************************************************************
 * Copyright (c) 2005, 2012 Symbian Ltd and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * Symbian Ltd - Initial API and implementation
 *******************************************************************************/

package io.sloeber.autoBuild.Internal;

import java.util.Arrays;
import java.util.List;

import io.sloeber.autoBuild.api.BuildException;
import io.sloeber.autoBuild.extensionPoint.IManagedOptionValueHandler;
import io.sloeber.schema.api.IHoldsOptions;
import io.sloeber.schema.api.IOption;
import io.sloeber.schema.internal.ISchemaObject;

/**
 * This class implements the default managed option value handler for MBS.
 * It is also be intended to be used as a base class for other value handlers.
 */
public class ManagedOptionValueHandler implements IManagedOptionValueHandler {

    /*
     *  E N A B L E   U S E   A S   B A S E   C L A S S   A N D
     *  D E F A U L T   I M P L E M E N T A T I O N
     */

    private static ManagedOptionValueHandler mbsValueHandler;

    protected ManagedOptionValueHandler() {
        mbsValueHandler = null;
    }

    public static ManagedOptionValueHandler getManagedOptionValueHandler() {
        if (mbsValueHandler == null) {
            mbsValueHandler = new ManagedOptionValueHandler();
        }
        return mbsValueHandler;
    }

    /*
     *  D E F A U L T   I M P L E M E N T A T I O N S   O F   I N T E R F A C E
     */

    /* (non-Javadoc)
     * @see org.eclipse.cdt.managedbuilder.core.IManagedOptionValueHandler#handleValue(IConfiguration,IToolChain,IOption,String,int)
     */
    @Override
    public boolean handleValue(ISchemaObject configuration, IHoldsOptions holder, IOption option, String extraArgument,
            int event) {
        /*
        // The following is for debug purposes and thus normally commented out
        String configLabel = "???"; //$NON-NLS-1$
        String holderLabel = "???"; //$NON-NLS-1$
        String eventLabel  = "???"; //$NON-NLS-1$
        
        if (configuration instanceof IConfiguration) {
        	configLabel = "IConfiguration"; //$NON-NLS-1$
        } else if (configuration instanceof IResourceConfiguration) {
        	configLabel = "IResourceConfiguration"; //$NON-NLS-1$
        }
        
        if (holder instanceof IToolChain) {
        	holderLabel = "IToolChain"; //$NON-NLS-1$
        } else if (holder instanceof ITool) {
        	holderLabel = "ITool"; //$NON-NLS-1$
        }
        
        switch (event) {
        case EVENT_OPEN:       eventLabel = "EVENT_OPEN"; break;       //$NON-NLS-1$
        case EVENT_APPLY:      eventLabel = "EVENT_APPLY"; break;      //$NON-NLS-1$
        case EVENT_SETDEFAULT: eventLabel = "EVENT_SETDEFAULT"; break; //$NON-NLS-1$
        case EVENT_CLOSE:      eventLabel = "EVENT_CLOSE"; break;      //$NON-NLS-1$
        }
        
        // Print the event
        System.out.println(eventLabel + "(" +              //$NON-NLS-1$
        		           configLabel + " = " +           //$NON-NLS-1$
        		           configuration.getId() + ", " +  //$NON-NLS-1$
        		           holderLabel + " = " +           //$NON-NLS-1$
        				   holder.getId() + ", " +         //$NON-NLS-1$
        				   "IOption = " +                  //$NON-NLS-1$
        				   option.getId() + ", " +         //$NON-NLS-1$
        				   "String = " +                   //$NON-NLS-1$
        				   extraArgument + ")");           //$NON-NLS-1$
        */
        // The event was not handled, thus return false
        return false;
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.managedbuilder.core.IManagedOptionValueHandler#isDefaultValue(IConfiguration,IToolChain,IOption,String)
     */
    @Override
    public boolean isDefaultValue(ISchemaObject configuration, IHoldsOptions holder, IOption option,
            String extraArgument) {
        // Get the default Value
        Object defaultValue = option.getDefaultValue();
        if (defaultValue instanceof List) {
            @SuppressWarnings("unchecked")
            List<String> list = (List<String>) defaultValue;
            defaultValue = list.toArray(new String[list.size()]);
        }

        try {
            // Figure out which type the option is and implement default behaviour for it.
            switch (option.getValueType()) {
            case IOption.STRING:
                if (option.getStringValue().equals(defaultValue)) {
                    return true;
                }
                break;
            case IOption.BOOLEAN:
                if (option.getBooleanValue() == ((Boolean) defaultValue).booleanValue()) {
                    return true;
                }
                break;
            case IOption.ENUMERATED:
            case IOption.TREE:
                if (option.getValue().toString().equals(defaultValue.toString())) {
                    return true;
                }
                break;
            case IOption.INCLUDE_PATH:
            case IOption.UNDEF_INCLUDE_PATH:
                if (Arrays.equals(option.getBasicStringListValue(), (String[]) defaultValue)) {
                    return true;
                }
                break;
            case IOption.STRING_LIST:
                if (Arrays.equals(option.getStringListValue(), (String[]) defaultValue)) {
                    return true;
                }
                break;
            case IOption.PREPROCESSOR_SYMBOLS:
            case IOption.UNDEF_PREPROCESSOR_SYMBOLS:
                if (Arrays.equals(option.getBasicStringListValue(), (String[]) defaultValue)) {
                    return true;
                }
                break;
            case IOption.LIBRARIES:
                if (Arrays.equals(option.getLibraries(), (String[]) defaultValue)) {
                    return true;
                }
                break;
            case IOption.OBJECTS:
                if (Arrays.equals(option.getUserObjects(), (String[]) defaultValue)) {
                    return true;
                }
                break;
            case IOption.INCLUDE_FILES:
            case IOption.LIBRARY_PATHS:
            case IOption.LIBRARY_FILES:
            case IOption.MACRO_FILES:
            case IOption.UNDEF_INCLUDE_FILES:
            case IOption.UNDEF_LIBRARY_PATHS:
            case IOption.UNDEF_LIBRARY_FILES:
            case IOption.UNDEF_MACRO_FILES:
            default:
                if (Arrays.equals(option.getBasicStringListValue(), (String[]) defaultValue)) {
                    return true;
                }
                break;
            }
        } catch (BuildException e) {
        }
        return false;
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.managedbuilder.core.IManagedOptionValueHandler#isEnumValueAppropriate(IConfiguration,IToolChain,IOption,String,String)
     */
    @Override
    public boolean isEnumValueAppropriate(ISchemaObject configuration, IHoldsOptions holder, IOption option,
            String extraArgument, String enumValue) {
        // By default return true for all the enum values.
        return true;
    }
}
