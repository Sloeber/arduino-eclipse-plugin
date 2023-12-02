package io.sloeber.schema.internal.enablement;

import org.eclipse.core.expressions.EvaluationResult;
import org.eclipse.core.expressions.Expression;
import org.eclipse.core.expressions.IEvaluationContext;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;

import io.sloeber.autoBuild.integration.AutoBuildConfigurationDescription;
import io.sloeber.schema.api.IOption;
import io.sloeber.schema.api.ITool;
import io.sloeber.schema.internal.SchemaObject;

public class CheckOptionExpression extends Expression {

    private static final String KEY_OPTION_ID = "optionId"; //$NON-NLS-1$
    private static final String KEY_HOLDER_ID = "holderId"; //$NON-NLS-1$
    private static final String KEY_IS_REG_EX = "isRegex"; //$NON-NLS-1$
    private static final String KEY_OTHER_HOLDER_ID = "OtherHolderId"; //$NON-NLS-1$
    private static final String KEY_OTHER_OPTION_ID = "otherOptionId"; //$NON-NLS-1$
    public static final String KEY_RESOURCE = "resource"; //$NON-NLS-1$
    public static final String KEY_TOOL = "tool"; //$NON-NLS-1$
    public static final String KEY_EXPRESSION_ELEMENT_TYPE = "element_type"; //$NON-NLS-1$

    private String myOptionID;
    private String myHolderID;
    private String myOtherOptionID;
    private String myOtherHolderID;
    private String myIsRegex;
    private String myExpectedValue;
    private SchemaObject mySchemaObject;

    public CheckOptionExpression(IConfigurationElement element, SchemaObject schemaObject) {
        mySchemaObject = schemaObject;
        myOptionID = element.getAttribute(KEY_OPTION_ID);
        myHolderID = element.getAttribute(KEY_HOLDER_ID);
        myOtherHolderID = element.getAttribute(KEY_OTHER_HOLDER_ID);
        myOtherOptionID = element.getAttribute(KEY_OTHER_OPTION_ID);
        myIsRegex = element.getAttribute(KEY_IS_REG_EX);
        myExpectedValue = element.getAttribute(ATT_VALUE);
        if (myHolderID != null || myOtherHolderID != null || myOtherOptionID != null | myIsRegex != null) {
            System.err.println("otherOption uses value that is not yet implemented"); //$NON-NLS-1$
        }
        if (myOptionID == null || myExpectedValue == null) {
            System.err.println("otherOption needs to specify " + KEY_OPTION_ID + " and " + ATT_VALUE); //$NON-NLS-1$ //$NON-NLS-2$
        }
    }

    @Override
    public EvaluationResult evaluate(IEvaluationContext context) throws CoreException {
        AutoBuildConfigurationDescription autoData = (AutoBuildConfigurationDescription) context.getDefaultVariable();
        IResource resource = (IResource) context.getVariable(KEY_RESOURCE);
        ITool tool = (ITool) context.getVariable(KEY_TOOL);
        String selectedOption = autoData.getSelectedOptions(resource, tool).get(myOptionID);
        boolean selectedBoolean = "true".equals(selectedOption); //$NON-NLS-1$
        boolean expectedBoolean = "true".equals(myExpectedValue); //$NON-NLS-1$
        if (selectedBoolean == expectedBoolean) {
            return EvaluationResult.TRUE;
        }
        return EvaluationResult.FALSE;
    }

}
