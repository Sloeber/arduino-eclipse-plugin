package io.sloeber.buildProperties;

/**
 * @noextend This class is not intended to be subclassed by clients.
 * @noimplement This interface is not intended to be implemented by clients.
 */
public interface IBuildProperty {
    IBuildPropertyType getPropertyType();

    IBuildPropertyValue getValue();
}
