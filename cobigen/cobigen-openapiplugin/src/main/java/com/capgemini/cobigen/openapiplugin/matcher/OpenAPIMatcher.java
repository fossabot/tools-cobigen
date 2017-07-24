package com.capgemini.cobigen.openapiplugin.matcher;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.capgemini.cobigen.api.exception.CobiGenRuntimeException;
import com.capgemini.cobigen.api.exception.InvalidConfigurationException;
import com.capgemini.cobigen.api.extension.MatcherInterpreter;
import com.capgemini.cobigen.api.to.MatcherTo;
import com.capgemini.cobigen.api.to.VariableAssignmentTo;
import com.capgemini.cobigen.openapiplugin.inputreader.to.OpenAPIFile;

/**
 * Matcher for internal OpenAPI model.
 */
public class OpenAPIMatcher implements MatcherInterpreter {

    /** Logger instance. */
    private static final Logger LOG = LoggerFactory.getLogger(OpenAPIMatcher.class);

    /** Supported matcher types */
    private enum MatcherType {
        /** Element type */
        ELEMENT
    }

    /** Matcher variable types */
    private enum VariableType {
        /** Constant assignment */
        CONSTANT,
        /** Object property extraction */
        PROPERTY
    }

    @Override
    public boolean matches(MatcherTo matcher) {

        if (matcher.getTarget() instanceof OpenAPIFile) {
            OpenAPIFile swaggerDefinition = (OpenAPIFile) matcher.getTarget();

            try {
                MatcherType matcherType = Enum.valueOf(MatcherType.class, matcher.getType().toUpperCase());
                switch (matcherType) {
                case ELEMENT:
                    for (String defName : swaggerDefinition.getSwagger().getDefinitions().keySet()) {
                        if (defName.matches(matcher.getValue())) {
                            return true;
                        }
                    }
                    break;
                default:
                    break;
                }
            } catch (IllegalArgumentException e) {
                LOG.info("Matcher type '{}' not registered --> no match!", matcher.getType());
            }
        }

        return false;
    }

    @Override
    public Map<String, String> resolveVariables(MatcherTo matcher, List<VariableAssignmentTo> variableAssignments)
        throws InvalidConfigurationException {

        Map<String, String> resolvedVariables = new HashMap<>();
        for (VariableAssignmentTo va : variableAssignments) {
            VariableType variableType = Enum.valueOf(VariableType.class, va.getType().toUpperCase());
            switch (variableType) {
            case CONSTANT:
                resolvedVariables.put(va.getVarName(), va.getValue());
                break;
            case PROPERTY:
                Class<?> target = matcher.getTarget().getClass();
                try {
                    Field field = target.getField(matcher.getValue());
                    field.setAccessible(true);
                    Object o = field.get(matcher.getTarget());

                    resolvedVariables.put(va.getVarName(), o.toString());
                } catch (NoSuchFieldException | SecurityException e) {
                    LOG.warn(
                        "The property {} was requested in a variable assignment although the input does not provide this property. Setting it to null",
                        matcher.getValue());
                } catch (IllegalArgumentException | IllegalAccessException e) {
                    throw new CobiGenRuntimeException("This is a programming error, please report an issue on github",
                        e);
                }
                break;
            }
        }
        return resolvedVariables;
    }

}
