package org.dspace.ref.compliance.rules;

import java.sql.*;
import java.util.*;
import org.apache.commons.collections.*;
import org.apache.commons.lang3.*;
import org.apache.commons.lang3.math.*;
import org.dspace.content.*;
import org.dspace.core.*;
import org.dspace.ref.compliance.definition.model.*;
import org.dspace.ref.compliance.result.*;
import org.dspace.util.*;
import org.joda.time.*;
import org.joda.time.format.*;

/**
 * Abstract implementation of a compliance rule
 */
public abstract class AbstractComplianceRule implements ComplianceRule {

    private final List<ComplianceRule> exceptionRules = new LinkedList<ComplianceRule>();

    private final List<ComplianceRule> preconditionRules = new LinkedList<ComplianceRule>();

    private List<String> violationDescriptions = new LinkedList<String>();

    private String definitionHint = null;

    private String resolutionHint = null;

    public void setDefinitionHint(final String definitionHint) {
        this.definitionHint = definitionHint;
    }

    public void setResolutionHint(final String resolutionHint) {
        this.resolutionHint = resolutionHint;
    }

    public RuleComplianceResult validate(final Context context, final Item item) {

        RuleComplianceResult result = getComplianceResult(context, item);

        if (!result.isCompliant() && CollectionUtils.isNotEmpty(exceptionRules)) {

            RuleComplianceResult exceptionResult = null;
            Iterator<ComplianceRule> it = exceptionRules.iterator();
            do {
                ComplianceRule exceptionRule = it.next();
                exceptionResult = exceptionRule.validate(context, item);
            } while (!(exceptionResult.isCompliant() && exceptionResult.isApplicable()) && it.hasNext());

            if (exceptionResult.isCompliant() && exceptionResult.isApplicable()) {
                result.setCompliant(true);
                result.setExceptionDescription(exceptionResult.getResultDescription());
                result.setExceptionHint(exceptionResult.getDefinitionHint());
            }
        }

        result.setDefinitionHint(definitionHint);
        result.setResolutionHint(resolutionHint);

        return result;
    }

    public void addExceptionRule(final ComplianceRule exceptionRule) {
        exceptionRules.add(exceptionRule);
    }

    public void addPreconditionRule(final ComplianceRule complianceRule) {
        preconditionRules.add(complianceRule);
    }

    protected RuleComplianceResult getComplianceResult(final Context context, final Item item) {
        RuleComplianceResult result = new RuleComplianceResult();
        result.setApplicable(true);

        if (item == null) {
            result.setCompliant(false);
            result.setRuleDescriptionViolation("the item cannot be null");

        } else {
            result.setCompliant(true);

            //Always do the validation so that the rule description can be built if necessary
            boolean isValid = doValidationAndBuildDescription(context, item);
            result.setRuleDescriptionViolation(getRuleDescriptionViolation());
            result.setRuleDescriptionCompliant(getRuleDescriptionCompliant());


            if (preconditionsAreMet(context, result, item)) {
                result.setCompliant(isValid);

                if (!isValid) {
                    result.addViolationDescriptions(violationDescriptions);
                }
            } else {
                result.setApplicable(false);
            }
        }

        return result;
    }

    protected abstract String getRuleDescriptionCompliant();

    protected abstract String getRuleDescriptionViolation();

    private boolean preconditionsAreMet(final Context context, final RuleComplianceResult parentResult, final Item item) {
        boolean conditionsAreMet = true;

        Iterator<ComplianceRule> it = preconditionRules.iterator();
        List<String> preconditionRuleDescriptions = new LinkedList<String>();

        while (it.hasNext()) {
            ComplianceRule rule = it.next();
            RuleComplianceResult complianceResult = rule.validate(context, item);

            conditionsAreMet &= complianceResult.isCompliant();

            if (StringUtils.isNotBlank(complianceResult.getRuleDescriptionCompliant())) {
                preconditionRuleDescriptions.add(complianceResult.getRuleDescriptionCompliant());
            }
        }

        parentResult.setPreconditionDescription(StringUtils.join(preconditionRuleDescriptions, " and "));

        return conditionsAreMet;
    }

    protected void addViolationDescription(final String description) {
        violationDescriptions.add(description);
    }

    protected void addViolationDescription(final String description, final Object... parameters) {
        addViolationDescription(String.format(description, parameters));
    }

    protected String getValueDescription(final Value valueObject) {
        String valueDescription = StringUtils.isBlank(valueObject.getDescription()) ? valueObject.getValue() : valueObject.getDescription();
        if(NumberUtils.isNumber(valueDescription)) {
            return valueDescription;
        } else {
            return "\"" + valueDescription + "\"";
        }
    }

    protected List<Metadatum> getMetadata(final Context context, final Item item, final String metadataField) {
        List<Metadatum> output;

        try {
            CustomField customField = CustomField.findByField(metadataField);
            if (customField == null) {
                output = ItemUtils.getMetadata(item, metadataField);
            } else {
                output = customField.createValueList(context, item);
            }
        } catch (SQLException e) {
            output = null;
        }

        return output;
    }

    protected DateTime getFirstDateValue(final Context context, final Item item, final String metadataField) {

        try {
            List<Metadatum> fieldValueList = getMetadata(context, item, metadataField);
            if (CollectionUtils.isNotEmpty(fieldValueList)) {
                return parseDateTime(fieldValueList.get(0).value);
            }

        } catch (IllegalArgumentException ex) {
            addViolationDescription("the metadata field %s is invalid because it has too few tokens or contains an invalid date", metadataField);
        }

        return null;
    }

    protected DateTime parseDateTime(final String inputString) throws IllegalArgumentException {
        DateTimeFormatter formatter = getDateTimeFormatter();
        try {
        return formatter.parseDateTime(inputString);
        } catch(Exception ex) {
            //We got an invalid date, ignoring the error
            return null;
        }
    }

    public static DateTimeFormatter getDateTimeFormatter() {
        return ISODateTimeFormat.dateTimeParser();
    }

    public static DateTimeFormatter getDateTimePrinter() {
        return ISODateTimeFormat.dateTime();
    }

    protected abstract boolean doValidationAndBuildDescription(final Context context, final Item item);

}
