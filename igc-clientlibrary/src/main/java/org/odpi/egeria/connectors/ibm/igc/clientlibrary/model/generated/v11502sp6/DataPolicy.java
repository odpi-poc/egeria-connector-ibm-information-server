/* SPDX-License-Identifier: Apache-2.0 */
/* Copyright Contributors to the ODPi Egeria project. */
package org.odpi.egeria.connectors.ibm.igc.clientlibrary.model.generated.v11502sp6;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeName;
import javax.annotation.Generated;
import org.odpi.egeria.connectors.ibm.igc.clientlibrary.model.common.*;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.ArrayList;

/**
 * POJO for the {@code data_policy} asset type in IGC, displayed as '{@literal Data Policy}' in the IGC UI.
 * <br><br>
 * (this code has been generated based on out-of-the-box IGC metadata types;
 *  if modifications are needed, eg. to handle custom attributes,
 *  extending from this class in your own custom class is the best approach.)
 */
@Generated("org.odpi.egeria.connectors.ibm.igc.clientlibrary.model.IGCRestModelGenerator")
@JsonIgnoreProperties(ignoreUnknown=true)
@JsonTypeName("data_policy")
public class DataPolicy extends Reference {

    public static String getIgcTypeDisplayName() { return "Data Policy"; }

    /**
     * The {@code name} property, displayed as '{@literal Title}' in the IGC UI.
     */
    protected String name;

    /**
     * The {@code policy_number} property, displayed as '{@literal Policy Number}' in the IGC UI.
     */
    protected String policy_number;

    /**
     * The {@code long_description} property, displayed as '{@literal Policy Text}' in the IGC UI.
     */
    protected String long_description;

    /**
     * The {@code effective_date} property, displayed as '{@literal Effective Date}' in the IGC UI.
     */
    protected Date effective_date;

    /**
     * The {@code termination_date} property, displayed as '{@literal Termination Date}' in the IGC UI.
     */
    protected Date termination_date;

    /**
     * The {@code contacts} property, displayed as '{@literal Contacts}' in the IGC UI.
     * <br><br>
     * Will be a {@link ReferenceList} of {@link Steward} objects.
     */
    protected ReferenceList contacts;

    /**
     * The {@code applied_to_assets} property, displayed as '{@literal Applied to Assets}' in the IGC UI.
     * <br><br>
     * Will be a {@link ReferenceList} of {@link MainObject} objects.
     */
    protected ReferenceList applied_to_assets;

    /**
     * The {@code created_by} property, displayed as '{@literal Created By}' in the IGC UI.
     */
    protected String created_by;

    /**
     * The {@code created_on} property, displayed as '{@literal Created On}' in the IGC UI.
     */
    protected Date created_on;

    /**
     * The {@code modified_by} property, displayed as '{@literal Modified By}' in the IGC UI.
     */
    protected String modified_by;

    /**
     * The {@code modified_on} property, displayed as '{@literal Modified On}' in the IGC UI.
     */
    protected Date modified_on;


    /** @see #name */ @JsonProperty("name")  public String getTheName() { return this.name; }
    /** @see #name */ @JsonProperty("name")  public void setTheName(String name) { this.name = name; }

    /** @see #policy_number */ @JsonProperty("policy_number")  public String getPolicyNumber() { return this.policy_number; }
    /** @see #policy_number */ @JsonProperty("policy_number")  public void setPolicyNumber(String policy_number) { this.policy_number = policy_number; }

    /** @see #long_description */ @JsonProperty("long_description")  public String getLongDescription() { return this.long_description; }
    /** @see #long_description */ @JsonProperty("long_description")  public void setLongDescription(String long_description) { this.long_description = long_description; }

    /** @see #effective_date */ @JsonProperty("effective_date")  public Date getEffectiveDate() { return this.effective_date; }
    /** @see #effective_date */ @JsonProperty("effective_date")  public void setEffectiveDate(Date effective_date) { this.effective_date = effective_date; }

    /** @see #termination_date */ @JsonProperty("termination_date")  public Date getTerminationDate() { return this.termination_date; }
    /** @see #termination_date */ @JsonProperty("termination_date")  public void setTerminationDate(Date termination_date) { this.termination_date = termination_date; }

    /** @see #contacts */ @JsonProperty("contacts")  public ReferenceList getContacts() { return this.contacts; }
    /** @see #contacts */ @JsonProperty("contacts")  public void setContacts(ReferenceList contacts) { this.contacts = contacts; }

    /** @see #applied_to_assets */ @JsonProperty("applied_to_assets")  public ReferenceList getAppliedToAssets() { return this.applied_to_assets; }
    /** @see #applied_to_assets */ @JsonProperty("applied_to_assets")  public void setAppliedToAssets(ReferenceList applied_to_assets) { this.applied_to_assets = applied_to_assets; }

    /** @see #created_by */ @JsonProperty("created_by")  public String getCreatedBy() { return this.created_by; }
    /** @see #created_by */ @JsonProperty("created_by")  public void setCreatedBy(String created_by) { this.created_by = created_by; }

    /** @see #created_on */ @JsonProperty("created_on")  public Date getCreatedOn() { return this.created_on; }
    /** @see #created_on */ @JsonProperty("created_on")  public void setCreatedOn(Date created_on) { this.created_on = created_on; }

    /** @see #modified_by */ @JsonProperty("modified_by")  public String getModifiedBy() { return this.modified_by; }
    /** @see #modified_by */ @JsonProperty("modified_by")  public void setModifiedBy(String modified_by) { this.modified_by = modified_by; }

    /** @see #modified_on */ @JsonProperty("modified_on")  public Date getModifiedOn() { return this.modified_on; }
    /** @see #modified_on */ @JsonProperty("modified_on")  public void setModifiedOn(Date modified_on) { this.modified_on = modified_on; }

    public static Boolean canBeCreated() { return false; }
    public static Boolean includesModificationDetails() { return true; }
    private static final List<String> NON_RELATIONAL_PROPERTIES = Arrays.asList(
        "name",
        "policy_number",
        "long_description",
        "effective_date",
        "termination_date",
        "created_by",
        "created_on",
        "modified_by",
        "modified_on"
    );
    private static final List<String> STRING_PROPERTIES = Arrays.asList(
        "name",
        "policy_number",
        "long_description",
        "created_by",
        "modified_by"
    );
    private static final List<String> PAGED_RELATIONAL_PROPERTIES = Arrays.asList(
        "contacts",
        "applied_to_assets"
    );
    private static final List<String> ALL_PROPERTIES = Arrays.asList(
        "name",
        "policy_number",
        "long_description",
        "effective_date",
        "termination_date",
        "contacts",
        "applied_to_assets",
        "created_by",
        "created_on",
        "modified_by",
        "modified_on"
    );
    public static List<String> getNonRelationshipProperties() { return NON_RELATIONAL_PROPERTIES; }
    public static List<String> getStringProperties() { return STRING_PROPERTIES; }
    public static List<String> getPagedRelationshipProperties() { return PAGED_RELATIONAL_PROPERTIES; }
    public static List<String> getAllProperties() { return ALL_PROPERTIES; }
    public static Boolean isDataPolicy(Object obj) { return (obj.getClass() == DataPolicy.class); }

}
