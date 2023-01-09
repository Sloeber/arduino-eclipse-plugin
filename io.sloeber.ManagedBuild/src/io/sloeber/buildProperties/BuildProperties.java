package io.sloeber.buildProperties;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.StringTokenizer;

import org.eclipse.cdt.core.settings.model.util.CDataUtil;
import org.eclipse.core.runtime.CoreException;

import io.sloeber.buildProperties.BuildProperty;
import io.sloeber.buildProperties.BuildPropertyManager;

public class BuildProperties implements IBuildProperties {
    private HashMap<String, IBuildProperty> fPropertiesMap = new HashMap<>();
    private ArrayList<String> fInexistentProperties;

    public BuildProperties() {

    }

    public BuildProperties(String properties) {
        StringTokenizer t = new StringTokenizer(properties, BuildPropertyManager.PROPERTIES_SEPARATOR);
        while (t.hasMoreTokens()) {
            String property = t.nextToken();
            try {
                BuildProperty prop = new BuildProperty(property);
                addProperty(prop);
            } catch (CoreException e) {
                if (fInexistentProperties == null)
                    fInexistentProperties = new ArrayList<>();

                fInexistentProperties.add(property);
            }
        }

        if (fInexistentProperties != null)
            fInexistentProperties.trimToSize();
    }

    @SuppressWarnings("unchecked")
    public BuildProperties(BuildProperties properties) {
        fPropertiesMap.putAll(properties.fPropertiesMap);
        if (properties.fInexistentProperties != null)
            fInexistentProperties = (ArrayList<String>) properties.fInexistentProperties.clone();
    }

    @Override
    public IBuildProperty[] getProperties() {
        return fPropertiesMap.values().toArray(new BuildProperty[fPropertiesMap.size()]);
    }

    @Override
    public IBuildProperty getProperty(String id) {
        return fPropertiesMap.get(id);
    }

    void addProperty(IBuildProperty property) {
        fPropertiesMap.put(property.getPropertyType().getId(), property);
    }

    @Override
    public IBuildProperty setProperty(String propertyId, String propertyValue) throws CoreException {
        return setProperty(propertyId, propertyValue, false);
    }

    public IBuildProperty setProperty(String propertyId, String propertyValue, boolean force) throws CoreException {
        try {
            IBuildProperty property = BuildPropertyManager.getInstance().createProperty(propertyId, propertyValue);

            addProperty(property);

            return property;
        } catch (CoreException e) {
            if (force) {
                if (fInexistentProperties == null)
                    fInexistentProperties = new ArrayList<>(1);

                fInexistentProperties.add(BuildProperty.toString(propertyId, propertyValue));
                fInexistentProperties.trimToSize();
            }
            throw e;
        }
    }

    @Override
    public IBuildProperty removeProperty(String id) {
        return fPropertiesMap.remove(id);
    }

    void removeProperty(BuildProperty property) {
        fPropertiesMap.remove(property.getPropertyType().getId());
    }

    @Override
    public String toString() {
        String props = toStringExistingProperties();
        if (fInexistentProperties != null) {
            String inexistentProps = CDataUtil.arrayToString(
                    fInexistentProperties.toArray(new String[fInexistentProperties.size()]),
                    BuildPropertyManager.PROPERTIES_SEPARATOR);
            if (props.length() != 0) {
                StringBuilder buf = new StringBuilder();
                buf.append(props).append(BuildPropertyManager.PROPERTIES_SEPARATOR).append(inexistentProps);
            } else {
                props = inexistentProps;
            }
        }
        return props;
    }

    public String toStringExistingProperties() {
        int size = fPropertiesMap.size();
        if (size == 0)
            return ""; //$NON-NLS-1$
        else if (size == 1)
            return fPropertiesMap.values().iterator().next().toString();

        StringBuilder buf = new StringBuilder();
        Iterator<IBuildProperty> iter = fPropertiesMap.values().iterator();
        buf.append(iter.next().toString());
        for (; iter.hasNext();) {
            buf.append(BuildPropertyManager.PROPERTIES_SEPARATOR);
            buf.append(iter.next().toString());
        }
        return buf.toString();
    }

    @SuppressWarnings("unchecked")
    @Override
    public Object clone() {
        try {
            BuildProperties clone = (BuildProperties) super.clone();

            if (fInexistentProperties != null)
                clone.fInexistentProperties = (ArrayList<String>) fInexistentProperties.clone();

            clone.fPropertiesMap = (HashMap<String, IBuildProperty>) fPropertiesMap.clone();
            /*          for(Iterator iter = clone.fPropertiesMap.entrySet().iterator(); iter.hasNext();){
                            Map.Entry entry = (Map.Entry)iter.next();
                            BuildProperty prop = (BuildProperty)entry.getValue();
                            entry.setValue(prop.clone());
                        }
            */
            return clone;
        } catch (CloneNotSupportedException e) {
        }
        return null;
    }

    @Override
    public void clear() {
        fPropertiesMap.clear();
        fInexistentProperties.clear();
    }

    @Override
    public boolean containsValue(String propertyId, String valueId) {
        IBuildProperty prop = getProperty(propertyId);
        if (prop != null) {
            return valueId.equals(prop.getValue().getId());
        }
        return false;
    }

}
