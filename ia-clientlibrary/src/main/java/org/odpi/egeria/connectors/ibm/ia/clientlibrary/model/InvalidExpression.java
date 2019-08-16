/* SPDX-License-Identifier: Apache-2.0 */
/* Copyright Contributors to the ODPi Egeria project. */
package org.odpi.egeria.connectors.ibm.ia.clientlibrary.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class InvalidExpression extends Error {

    @JacksonXmlProperty(isAttribute = true) private String ruleName;
    @JacksonXmlProperty(isAttribute = true) private String ruleExpression;
    @JacksonXmlProperty(localName = "Cause")
    private ErrorList cause;

    public String getRuleName() { return ruleName; }
    public void setRuleName(String ruleName) { this.ruleName = ruleName; }

    public String getRuleExpression() { return ruleExpression; }
    public void setRuleExpression(String ruleExpression) { this.ruleExpression = ruleExpression; }

    public ErrorList getCause() { return cause; }
    public void setCause(ErrorList cause) { this.cause = cause; }

    @Override
    public String toString() {
        String parent = super.toString();
        return parent.substring(0, parent.length() - 2)
                + ", \"ruleName\": \"" + ruleName
                + "\", \"ruleExpression\": \"" + ruleExpression
                + "\", \"Cause\": " + (cause == null ? "{}" : cause.toString())
                + " }";
    }

}
