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
 * POJO for the {@code logical_foreign_key} asset type in IGC, displayed as '{@literal Logical Foreign Key}' in the IGC UI.
 * <br><br>
 * (this code has been generated based on out-of-the-box IGC metadata types;
 *  if modifications are needed, eg. to handle custom attributes,
 *  extending from this class in your own custom class is the best approach.)
 */
@Generated("org.odpi.egeria.connectors.ibm.igc.clientlibrary.model.IGCRestModelGenerator")
@JsonIgnoreProperties(ignoreUnknown=true)
@JsonTypeName("logical_foreign_key")
public class LogicalForeignKey extends Reference {

    public static String getIgcTypeDisplayName() { return "Logical Foreign Key"; }

    /**
     * The {@code name} property, displayed as '{@literal Name}' in the IGC UI.
     */
    protected String name;

    /**
     * The {@code short_description} property, displayed as '{@literal Short Description}' in the IGC UI.
     */
    protected String short_description;

    /**
     * The {@code long_description} property, displayed as '{@literal Long Description}' in the IGC UI.
     */
    protected String long_description;

    /**
     * The {@code logical_entity} property, displayed as '{@literal Logical Entity}' in the IGC UI.
     * <br><br>
     * Will be a single {@link Reference} to a {@link LogicalEntity} object.
     */
    protected Reference logical_entity;

    /**
     * The {@code physical_name} property, displayed as '{@literal Physical Name}' in the IGC UI.
     */
    protected String physical_name;

    /**
     * The {@code referenced_by_entity_attributes} property, displayed as '{@literal Parent Entity Attribute}' in the IGC UI.
     * <br><br>
     * Will be a {@link ReferenceList} of {@link EntityAttribute} objects.
     */
    protected ReferenceList referenced_by_entity_attributes;

    /**
     * The {@code child_entity_attributes} property, displayed as '{@literal Child Entity Attributes}' in the IGC UI.
     * <br><br>
     * Will be a {@link ReferenceList} of {@link EntityAttribute} objects.
     */
    protected ReferenceList child_entity_attributes;

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

    /** @see #short_description */ @JsonProperty("short_description")  public String getShortDescription() { return this.short_description; }
    /** @see #short_description */ @JsonProperty("short_description")  public void setShortDescription(String short_description) { this.short_description = short_description; }

    /** @see #long_description */ @JsonProperty("long_description")  public String getLongDescription() { return this.long_description; }
    /** @see #long_description */ @JsonProperty("long_description")  public void setLongDescription(String long_description) { this.long_description = long_description; }

    /** @see #logical_entity */ @JsonProperty("logical_entity")  public Reference getLogicalEntity() { return this.logical_entity; }
    /** @see #logical_entity */ @JsonProperty("logical_entity")  public void setLogicalEntity(Reference logical_entity) { this.logical_entity = logical_entity; }

    /** @see #physical_name */ @JsonProperty("physical_name")  public String getPhysicalName() { return this.physical_name; }
    /** @see #physical_name */ @JsonProperty("physical_name")  public void setPhysicalName(String physical_name) { this.physical_name = physical_name; }

    /** @see #referenced_by_entity_attributes */ @JsonProperty("referenced_by_entity_attributes")  public ReferenceList getReferencedByEntityAttributes() { return this.referenced_by_entity_attributes; }
    /** @see #referenced_by_entity_attributes */ @JsonProperty("referenced_by_entity_attributes")  public void setReferencedByEntityAttributes(ReferenceList referenced_by_entity_attributes) { this.referenced_by_entity_attributes = referenced_by_entity_attributes; }

    /** @see #child_entity_attributes */ @JsonProperty("child_entity_attributes")  public ReferenceList getChildEntityAttributes() { return this.child_entity_attributes; }
    /** @see #child_entity_attributes */ @JsonProperty("child_entity_attributes")  public void setChildEntityAttributes(ReferenceList child_entity_attributes) { this.child_entity_attributes = child_entity_attributes; }

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
        "short_description",
        "long_description",
        "physical_name",
        "created_by",
        "created_on",
        "modified_by",
        "modified_on"
    );
    private static final List<String> STRING_PROPERTIES = Arrays.asList(
        "name",
        "short_description",
        "long_description",
        "physical_name",
        "created_by",
        "modified_by"
    );
    private static final List<String> PAGED_RELATIONAL_PROPERTIES = Arrays.asList(
        "referenced_by_entity_attributes",
        "child_entity_attributes"
    );
    private static final List<String> ALL_PROPERTIES = Arrays.asList(
        "name",
        "short_description",
        "long_description",
        "logical_entity",
        "physical_name",
        "referenced_by_entity_attributes",
        "child_entity_attributes",
        "created_by",
        "created_on",
        "modified_by",
        "modified_on"
    );
    public static List<String> getNonRelationshipProperties() { return NON_RELATIONAL_PROPERTIES; }
    public static List<String> getStringProperties() { return STRING_PROPERTIES; }
    public static List<String> getPagedRelationshipProperties() { return PAGED_RELATIONAL_PROPERTIES; }
    public static List<String> getAllProperties() { return ALL_PROPERTIES; }
    public static Boolean isLogicalForeignKey(Object obj) { return (obj.getClass() == LogicalForeignKey.class); }

}
