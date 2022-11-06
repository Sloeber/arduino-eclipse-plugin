package io.sloeber.autoBuild.Internal;

import org.eclipse.cdt.core.settings.model.ICStorageElement;
import org.eclipse.cdt.core.settings.model.extension.CTargetPlatformData;
import org.osgi.framework.Version;

import io.sloeber.autoBuild.api.IManagedConfigElement;
import io.sloeber.autoBuild.api.ITargetPlatform;
import io.sloeber.autoBuild.api.IToolChain;

public class TargetPlatform implements ITargetPlatform {

    public TargetPlatform(ToolChain toolChain, IManagedConfigElement iManagedConfigElement,
            String managedBuildRevision) {
        // TODO Auto-generated constructor stub
    }

    public TargetPlatform(ToolChain toolChain, ICStorageElement configElement, String managedBuildRevision) {
        // TODO Auto-generated constructor stub
    }

    public TargetPlatform(ToolChain toolChain, ITargetPlatform superClass, String id, String name,
            boolean isExtensionElement) {
        // TODO Auto-generated constructor stub
    }

    public TargetPlatform(IToolChain newChain, String subId, String name, TargetPlatform tpBase) {
        // TODO Auto-generated constructor stub
    }

    @Override
    public String getId() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getName() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getBaseId() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Version getVersion() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void setVersion(Version version) {
        // TODO Auto-generated method stub

    }

    @Override
    public String getManagedBuildRevision() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IToolChain getParent() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ITargetPlatform getSuperClass() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean isAbstract() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void setIsAbstract(boolean b) {
        // TODO Auto-generated method stub

    }

    @Override
    public String getUnusedChildren() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String[] getOSList() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void setOSList(String[] OSs) {
        // TODO Auto-generated method stub

    }

    @Override
    public String[] getArchList() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void setArchList(String[] archs) {
        // TODO Auto-generated method stub

    }

    @Override
    public String getBinaryParserId() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String[] getBinaryParserList() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void setBinaryParserId(String id) {
        // TODO Auto-generated method stub

    }

    @Override
    public void setBinaryParserList(String[] ids) {
        // TODO Auto-generated method stub

    }

    @Override
    public boolean isDirty() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void setDirty(boolean isDirty) {
        // TODO Auto-generated method stub

    }

    @Override
    public boolean isExtensionElement() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public CTargetPlatformData getTargetPlatformData() {
        // TODO Auto-generated method stub
        return null;
    }

    public void serialize(ICStorageElement targetPlatformElement) {
        // TODO Auto-generated method stub

    }

    public void resolveReferences() {
        // TODO Auto-generated method stub

    }

}
