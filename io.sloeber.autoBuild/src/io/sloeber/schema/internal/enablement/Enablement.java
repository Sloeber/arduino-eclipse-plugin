package io.sloeber.schema.internal.enablement;

import static io.sloeber.autoBuild.integration.AutoBuildConstants.*;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.eclipse.core.expressions.ElementHandler;
import org.eclipse.core.expressions.EvaluationContext;
import org.eclipse.core.expressions.EvaluationResult;
import org.eclipse.core.expressions.Expression;
import org.eclipse.core.expressions.ExpressionConverter;
import org.eclipse.core.expressions.ExpressionTagNames;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;

import io.sloeber.autoBuild.api.IAutoBuildConfigurationDescription;
import io.sloeber.autoBuild.core.Activator;
import io.sloeber.schema.api.ITool;
import io.sloeber.schema.internal.SchemaObject;

public class Enablement {
    private List<Expression> myExpressions = new LinkedList<>();

    private Set<Expression> findExpressionsOfType(int enablementType) {
        Set<Expression> ret = new HashSet<>();
        for (Expression curExpression : myExpressions) {
            if (curExpression instanceof MBSEnablementExpression) {
                MBSEnablementExpression curMBSExpression = (MBSEnablementExpression) curExpression;
                if (curMBSExpression.isOfType(enablementType)) {
                    ret.add(curExpression);
                }

            } else {
                ret.add(curExpression);
            }
        }
        return ret;
    }

    private static Expression findMatchingExpression(int enablementType, Set<Expression> relevantExpressions,
            IResource resource, ITool tool, IAutoBuildConfigurationDescription autoData) {
        try {
            EvaluationContext evalContext = new EvaluationContext(null, autoData);
            if (resource != null) {
                evalContext.addVariable(CheckOptionExpression.KEY_RESOURCE, resource);
            }
            if (tool != null) {
                evalContext.addVariable(CheckOptionExpression.KEY_TOOL, tool);
            }
            evalContext.addVariable(CheckOptionExpression.KEY_EXPRESSION_ELEMENT_TYPE, Integer.valueOf(enablementType));
            for (Expression curExpression : relevantExpressions) {
                EvaluationResult result = curExpression.evaluate(evalContext);
                if (result == EvaluationResult.TRUE) {
                    return curExpression;
                }
            }
        } catch (CoreException e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean isEnabled(int enablementType, IResource resource, ITool tool,
            IAutoBuildConfigurationDescription autoData) {
        Set<Expression> relevantExpressions = findExpressionsOfType(enablementType);
        if (relevantExpressions.size() == 0) {
            //No expressions are relevant so this is enabled
            return true;
        }
        Expression foundExpression = findMatchingExpression(enablementType, relevantExpressions, resource, tool,
                autoData);
        if (foundExpression == null) {
            return false;
        }
        return true;
    }

    public String getDefaultValue(IResource resource, ITool tool, IAutoBuildConfigurationDescription autoData) {
        int enablementType = MBSEnablementExpression.ENABLEMENT_TYPE_VALUE;
        Set<Expression> relevantExpressions = findExpressionsOfType(enablementType);
        if (relevantExpressions.size() == 0) {
            //No expressions are relevant so this is enabled
            return EMPTY_STRING;
        }
        Expression foundExpression = findMatchingExpression(enablementType, relevantExpressions, resource, tool,
                autoData);
        if (foundExpression == null) {
            return EMPTY_STRING;
        }
        if (!(foundExpression instanceof MBSEnablementExpression)) {
            System.err.println("Enablement should be of type MBSEnablementExpression"); //$NON-NLS-1$
            return EMPTY_STRING;
        }
        MBSEnablementExpression exp = (MBSEnablementExpression) foundExpression;
        return exp.getValue();
    }

    public Enablement(IConfigurationElement element, SchemaObject schemaObject) {
        // ExpressionConverter myExpressionConverter = ExpressionConverter.getDefault();
        ElementHandler[] handlers = new ElementHandler[2];
        handlers[0] = new ElementHandler() {

            @Override
            public Expression create(ExpressionConverter converter, IConfigurationElement configElement)
                    throws CoreException {
                String name = configElement.getName();
                if (name == null) {
                    return null;
                }

                switch (name) {
                case "checkBuildProperty": //$NON-NLS-1$
                    return new BuildPropertyExpression(configElement);
                case "checkToolFlavour": //$NON-NLS-1$
                    return new ToolFlavourExpression(configElement);
                case "checkOption": //$NON-NLS-1$
                    return new CheckOptionExpression(configElement, schemaObject);
                case ExpressionTagNames.ENABLEMENT: {
                    MBSEnablementExpression result = new MBSEnablementExpression(configElement);
                    processChildren(converter, configElement, result);
                    return result;
                }
                default:
                    break;
                }
                return null;
            }

        };
        handlers[1] = ElementHandler.getDefault();
        ExpressionConverter myExpressionConverter = new ExpressionConverter(handlers);

        try {
            IConfigurationElement[] enablementElements = element.getChildren("enablement"); //$NON-NLS-1$
            for (IConfigurationElement curEnablementElement : enablementElements) {
                myExpressions.add(myExpressionConverter.perform(curEnablementElement));
            }
        } catch (CoreException e) {
            Activator.log(e);
        }
    }

    public boolean isBlank() {
        return myExpressions.size() == 0;
    }

}
