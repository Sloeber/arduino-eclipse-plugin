package io.sloeber.schema.internal.enablement;

import static io.sloeber.autoBuild.integration.AutoBuildConstants.*;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.core.expressions.ElementHandler;
import org.eclipse.core.expressions.EvaluationContext;
import org.eclipse.core.expressions.EvaluationResult;
import org.eclipse.core.expressions.Expression;
import org.eclipse.core.expressions.ExpressionConverter;
import org.eclipse.core.expressions.ExpressionTagNames;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;

import io.sloeber.autoBuild.core.Activator;
import io.sloeber.autoBuild.integration.AutoBuildConfigurationData;
import io.sloeber.schema.internal.SchemaObject;

public class Enablement {
    private List<Expression> myExpressions = new LinkedList<>();

    private Expression findMatchingExpression(IResource resource, AutoBuildConfigurationData autoData) {
        try {
            EvaluationContext evalContext = new EvaluationContext(null, autoData);
            evalContext.addVariable(CheckOptionExpression.KEY_RESOURCE, resource);
            for (Expression curExpression : myExpressions) {

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

    public boolean isEnabled(IResource resource, AutoBuildConfigurationData autoData) {
        Expression foundExpression = findMatchingExpression(resource, autoData);
        if (foundExpression == null) {
            return myExpressions.size() == 0;
        }
        int a = 0;
        return true;
    }

    public String getDefaultValue(IResource resource, AutoBuildConfigurationData autoData) {
        Expression foundExpression = findMatchingExpression(resource, autoData);
        if (foundExpression == null) {
            return EMPTY_STRING;
        }
        if (!(foundExpression instanceof MBSEnablementExpression)) {
            System.err.println("Enablement should be of type MBSEnablementExpression"); //$NON-NLS-1$
            return EMPTY_STRING;
        }
        MBSEnablementExpression exp = (MBSEnablementExpression) foundExpression;
        if (exp.isContainerType() && exp.isDefaultValue()) {
            return exp.getValue();
        }
        return EMPTY_STRING;
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
                case "checkOption": //$NON-NLS-1$
                    return new CheckOptionExpression(configElement, schemaObject);
                case ExpressionTagNames.ENABLEMENT: {
                    MBSEnablementExpression result = new MBSEnablementExpression(configElement);
                    processChildren(converter, configElement, result);
                    return result;
                }
                // case ExpressionTagNames.TEST:
                // return new TestExpression(element);
                // case ExpressionTagNames.OR:
                // {
                // OrExpression result= new OrExpression();
                // processChildren(converter, element, result);
                // return result;
                // }
                // case ExpressionTagNames.AND:
                // {
                // AndExpression result= new AndExpression();
                // processChildren(converter, element, result);
                // return result;
                // }
                // case ExpressionTagNames.NOT:
                // return new NotExpression(converter.perform(element.getChildren()[0]));
                // case ExpressionTagNames.WITH:
                // {
                // WithExpression result= new WithExpression(element);
                // processChildren(converter, element, result);
                // return result;
                // }
                // case ExpressionTagNames.ADAPT:
                // {
                // AdaptExpression result= new AdaptExpression(element);
                // processChildren(converter, element, result);
                // return result;
                // }
                // case ExpressionTagNames.ITERATE:
                // {
                // IterateExpression result= new IterateExpression(element);
                // processChildren(converter, element, result);
                // return result;
                // }
                // case ExpressionTagNames.COUNT:
                // return new CountExpression(element);
                // case ExpressionTagNames.SYSTEM_TEST:
                // return new SystemTestExpression(element);
                // case ExpressionTagNames.RESOLVE:
                // {
                // ResolveExpression result= new ResolveExpression(element);
                // processChildren(converter, element, result);
                // return result;
                // }
                // case ExpressionTagNames.ENABLEMENT:
                // {
                // EnablementExpression result= new EnablementExpression(element);
                // processChildren(converter, element, result);
                // return result;
                // }
                // case ExpressionTagNames.EQUALS:
                // return new EqualsExpression(element);
                // case ExpressionTagNames.REFERENCE:
                // return new ReferenceExpression(element);
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
