/* SPDX-License-Identifier: Apache-2.0 */
/* Copyright Contributors to the ODPi Egeria project. */
package org.odpi.egeria.connectors.ibm.ia.clientlibrary.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class DataSourceNotConfigured extends Error {

    @JacksonXmlProperty(isAttribute = true) private String executableRuleName;
    @JacksonXmlProperty(isAttribute = true) private String projectName;
    @JacksonXmlProperty(localName = "Cause")
    private ErrorList cause;

    public String getExecutableRuleName() { return executableRuleName; }
    public void setExecutableRuleName(String executableRuleName) { this.executableRuleName = executableRuleName; }

    public String getProjectName() { return projectName; }
    public void setProjectName(String projectName) { this.projectName = projectName; }

    public ErrorList getCause() { return cause; }
    public void setCause(ErrorList cause) { this.cause = cause; }

    @Override
    public String toString() {
        String parent = super.toString();
        return parent.substring(0, parent.length() - 2)
                + ", \"executableRuleName\": \"" + executableRuleName
                + "\", \"projectName\": \"" + projectName
                + "\", \"Cause\": " + (cause == null ? "{}" : cause.toString())
                + " }";
    }

}
