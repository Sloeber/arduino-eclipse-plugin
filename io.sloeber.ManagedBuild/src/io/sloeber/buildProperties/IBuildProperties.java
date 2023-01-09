package io.sloeber.buildProperties;


import org.eclipse.core.runtime.CoreException;

/**
 * @noextend This class is not intended to be subclassed by clients.
 * @noimplement This interface is not intended to be implemented by clients.
 */
public interface IBuildProperties extends Cloneable {
    IBuildProperty[] getProperties();

    IBuildProperty getProperty(String id);

    //  IBuildProperty addProperty(IBuildProperty property);

    IBuildProperty setProperty(String propertyId, String propertyValue) throws CoreException;

    //  IBuildProperty addProperty(IBuildPropertyType type, String propertyValue) throws CoreException;

    IBuildProperty removeProperty(String id);

    boolean containsValue(String propertyId, String valueId);

    //  IBuildProperty removeProperty(IBuildPropertyType propertyType);

    void clear();

    Object clone();
}
