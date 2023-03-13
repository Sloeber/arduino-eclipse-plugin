package io.sloeber.schema.internal.enablement;

import org.eclipse.core.expressions.EvaluationResult;
import org.eclipse.core.expressions.Expression;
import org.eclipse.core.expressions.IEvaluationContext;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;

import io.sloeber.autoBuild.integration.AutoBuildConfigurationDescription;

public class BuildPropertyExpression extends Expression {

    private static final String ATT_PROPERTY = "property"; //$NON-NLS-1$
    private String fProperty;
    private String fExpectedValue;

    public BuildPropertyExpression(IConfigurationElement element) {
        fProperty = element.getAttribute(ATT_PROPERTY);
        fExpectedValue = element.getAttribute(ATT_VALUE);
    }

    @Override
    public EvaluationResult evaluate(IEvaluationContext context) throws CoreException {
        AutoBuildConfigurationDescription autoData = (AutoBuildConfigurationDescription) context.getDefaultVariable();
        String str = autoData.getProperty(fProperty);
        if (str == null)
            return EvaluationResult.FALSE;
        return EvaluationResult.valueOf(str.equals(fExpectedValue));
    }

}
