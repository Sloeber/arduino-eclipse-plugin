package io.sloeber.schema.internal.enablement;

import org.eclipse.core.expressions.CompositeExpression;
import org.eclipse.core.expressions.EvaluationResult;
import org.eclipse.core.expressions.IEvaluationContext;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;

public class MBSEnablementExpression extends CompositeExpression {
    public final static int ENABLEMENT_TYPE_VALUE = 1;
    public final static int ENABLEMENT_TYPE_CMD = 2;
    public final static int ENABLEMENT_GUI_ENABLED = 4;
    public final static int ENABLEMENT_GUI_VISIBLE = 8;
    private final static String ATTRIBUTE = "attribute"; //$NON-NLS-1$
    private final static String DEFAULT_VALUE = "defaultValue"; //$NON-NLS-1$
    private final static String CONTAINER_ATTRIBUTE = "CONTAINER_ATTRIBUTE"; //$NON-NLS-1$
    private final static String EXTENSION_ADJUSTMENT = "extensionAdjustment"; //$NON-NLS-1$
    private final static String TYPE = "type"; //$NON-NLS-1$
    private final static String CMD_USAGE = "CMD_USAGE"; //$NON-NLS-1$
    private final static String UI_ENABLEMENT = "UI_ENABLEMENT"; //$NON-NLS-1$
    private final static String UI_VISIBILITY = "UI_VISIBILITY"; //$NON-NLS-1$
    private final static String ALL = "ALL"; //$NON-NLS-1$
    private String myAttribute;
    private String myTypeString;
    private int myType = 0;
    private String myExtensionAdjustment;
    private String myValue;
    private boolean myIsContainerType;
    private boolean myIsDefaultValue;

    public MBSEnablementExpression(IConfigurationElement element) {
        myAttribute = element.getAttribute(ATTRIBUTE);
        myTypeString = element.getAttribute(TYPE);
        myExtensionAdjustment = element.getAttribute(EXTENSION_ADJUSTMENT);
        myValue = element.getAttribute(ATT_VALUE);
        myIsContainerType = CONTAINER_ATTRIBUTE.equals(myTypeString);
        myIsDefaultValue = ATT_VALUE.equals(myAttribute) || DEFAULT_VALUE.equals(myAttribute);
        String[] typeKeys = myTypeString.split("\\|"); //$NON-NLS-1$
        for (String curTypeKey : typeKeys) {
            switch (curTypeKey) {
            case ALL:
                myType = myType | ENABLEMENT_TYPE_CMD | ENABLEMENT_GUI_ENABLED | ENABLEMENT_GUI_VISIBLE;
                break;
            case CONTAINER_ATTRIBUTE:
                myType = myType | ENABLEMENT_TYPE_VALUE;
                break;
            case CMD_USAGE:
                //CMD_USAGE assumes not visible in UI
                myType = myType | ENABLEMENT_TYPE_CMD | ENABLEMENT_GUI_VISIBLE;
                break;
            case UI_ENABLEMENT:
                myType = myType | ENABLEMENT_GUI_ENABLED;
                break;
            case UI_VISIBILITY:
                myType = myType | ENABLEMENT_GUI_VISIBLE;
                break;
            }
        }
    }

    public boolean isOfType(int type) {
        return (myType & type) == type;
    }

    public boolean isContainerType() {
        return myIsContainerType;
    }

    public boolean isDefaultValue() {
        return myIsDefaultValue;
    }

    public String getType() {
        return myTypeString;
    }

    public String getAttribute() {
        return myAttribute;
    }

    public String getExtensionAdjustment() {
        return myExtensionAdjustment;
    }

    public String getValue() {
        return myValue;
    }

    @Override
    public boolean equals(final Object object) {
        if (!(object instanceof MBSEnablementExpression))
            return false;

        final MBSEnablementExpression that = (MBSEnablementExpression) object;
        return equals(this.fExpressions, that.fExpressions);
    }

    @Override
    public EvaluationResult evaluate(IEvaluationContext context) throws CoreException {
        EvaluationResult ret = evaluateAnd(context);
        if (myTypeString.equals(CMD_USAGE)) {
            int type = (int) context.getVariable(CheckOptionExpression.KEY_EXPRESSION_ELEMENT_TYPE);
            if ((type | ENABLEMENT_GUI_VISIBLE) == ENABLEMENT_GUI_VISIBLE) {
                ret = ret.not();
            }
        }
        return ret;
    }
}
