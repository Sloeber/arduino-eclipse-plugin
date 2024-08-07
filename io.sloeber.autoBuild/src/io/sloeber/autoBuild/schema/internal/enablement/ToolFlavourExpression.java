package io.sloeber.autoBuild.schema.internal.enablement;

import org.eclipse.core.expressions.EvaluationResult;
import org.eclipse.core.expressions.Expression;
import org.eclipse.core.expressions.IEvaluationContext;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;

import io.sloeber.autoBuild.integration.AutoBuildConfigurationDescription;

public class ToolFlavourExpression extends Expression {

    private static final String ATT_PROPERTY = "ToolFlavour"; //$NON-NLS-1$
    private String ExpectedToolFlavour;

    public ToolFlavourExpression(IConfigurationElement element) {
    	ExpectedToolFlavour = element.getAttribute(ATT_PROPERTY);
    }

    @Override
    public EvaluationResult evaluate(IEvaluationContext context) throws CoreException {
        AutoBuildConfigurationDescription autoData = (AutoBuildConfigurationDescription) context.getDefaultVariable();
        if (autoData == null)
            return EvaluationResult.FALSE;
        String str = autoData.getBuildToolsFlavour().toString();
        if (str == null)
            return EvaluationResult.FALSE;
        return EvaluationResult.valueOf(str.equals(ExpectedToolFlavour));
    }

}
