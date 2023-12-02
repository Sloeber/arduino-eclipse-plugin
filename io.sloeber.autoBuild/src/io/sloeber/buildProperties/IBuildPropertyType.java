package io.sloeber.buildProperties;

import io.sloeber.autoBuild.api.IPropertyBase;

/**
 * @noextend This class is not intended to be subclassed by clients.
 * @noimplement This interface is not intended to be implemented by clients.
 */
public interface IBuildPropertyType extends IPropertyBase {
    IBuildPropertyValue[] getSupportedValues();

    IBuildPropertyValue getSupportedValue(String id);

}
