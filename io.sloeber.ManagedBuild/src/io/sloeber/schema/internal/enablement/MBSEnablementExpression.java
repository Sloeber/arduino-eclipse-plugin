package io.sloeber.schema.internal.enablement;

import org.eclipse.core.expressions.CompositeExpression;
import org.eclipse.core.expressions.EvaluationResult;
import org.eclipse.core.expressions.IEvaluationContext;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;

public class MBSEnablementExpression extends CompositeExpression {
	public static String ATTRIBUTE="attribute"; //$NON-NLS-1$
	public static String DEFAULT_VALUE="defaultValue"; //$NON-NLS-1$
	public static String CONTAINER_ATTRIBUTE="CONTAINER_ATTRIBUTE"; //$NON-NLS-1$
	public static String EXTENSION_ADJUSTMENT="extensionAdjustment"; //$NON-NLS-1$
	public static String TYPE="type"; //$NON-NLS-1$
	private String myAttribute;
	private String myType;
	private String myExtensionAdjustment;
	private String myValue;
	private boolean myIsContainerType;
	private boolean myIsDefaultValue;
	

	public MBSEnablementExpression(IConfigurationElement element) {
		myAttribute = element.getAttribute(ATTRIBUTE);
		myType = element.getAttribute(TYPE);
		myExtensionAdjustment = element.getAttribute(EXTENSION_ADJUSTMENT);
		myValue = element.getAttribute(ATT_VALUE);
		myIsContainerType=CONTAINER_ATTRIBUTE.equals(myType);
		myIsDefaultValue=ATT_VALUE.equals(myAttribute)||DEFAULT_VALUE.equals(myAttribute);
	}

	public boolean isContainerType() {
		return myIsContainerType;
	}
	public boolean isDefaultValue() {
		return myIsDefaultValue;
	}
	public String getType() {
		return myType;
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
		return evaluateAnd(context);
	}
}
