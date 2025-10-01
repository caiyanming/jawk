package org.metricshub.jawk;

import java.util.Collection;
import java.util.Collections;
import org.metricshub.jawk.ext.AbstractExtension;
import org.metricshub.jawk.ext.JawkExtension;
import org.metricshub.jawk.jrt.AssocArray;
import org.metricshub.jawk.jrt.JRT;
import org.metricshub.jawk.jrt.VariableManager;
import org.metricshub.jawk.util.AwkSettings;

public class TestExtension extends AbstractExtension implements JawkExtension {

	private static final String MY_EXTENSION_FUNCTION = "myExtensionFunction";
	private static final Collection<String> KEYWORDS = Collections.singletonList(MY_EXTENSION_FUNCTION);

	@Override
	public void init(VariableManager vm, JRT jrt, AwkSettings settings) {}

	@Override
	public int[] getAssocArrayParameterPositions(String extensionKeyword, int numArgs) {
		if (MY_EXTENSION_FUNCTION.equals(extensionKeyword)) {
			return new int[] { 1 };
		} else {
			return new int[] {};
		}
	}

	@Override
	public String getExtensionName() {
		return "TestExtension";
	}

	@Override
	public Collection<String> extensionKeywords() {
		return KEYWORDS;
	}

	@Override
	public Object invoke(String keyword, Object[] args) {
		if (MY_EXTENSION_FUNCTION.equals(keyword)) {
			StringBuilder result = new StringBuilder();
			int count = ((Long) args[0]).intValue();
			AssocArray array = (AssocArray) args[1];
			for (int i = 0; i < count; i++) {
				for (Object item : array.keySet()) {
					result.append((String) array.get(item));
				}
			}
			return result.toString();
		} else {
			throw new NotImplementedError(keyword + " is not implemented by " + getExtensionName());
		}
	}
}
