/* SPDX-License-Identifier: Apache-2.0 */
/* Copyright Contributors to the ODPi Egeria project. */
package org.odpi.egeria.connectors.ibm.igc.repositoryconnector.mapping.entities;

import org.odpi.egeria.connectors.ibm.igc.clientlibrary.IGCVersionEnum;
import org.odpi.egeria.connectors.ibm.igc.repositoryconnector.IGCOMRSMetadataCollection;
import org.odpi.egeria.connectors.ibm.igc.repositoryconnector.mapping.relationships.SchemaAttributeTypeMapper_DatabaseColumn;

/**
 * Defines the mapping to the OMRS "RelationalColumnType" entity.
 */
public class RelationalColumnTypeMapper extends ReferenceableMapper {

    public static final String IGC_RID_PREFIX = IGCOMRSMetadataCollection.generateTypePrefix("RCT");

    private static class Singleton {
        private static final RelationalColumnTypeMapper INSTANCE = new RelationalColumnTypeMapper();
    }
    public static RelationalColumnTypeMapper getInstance(IGCVersionEnum version) {
        return Singleton.INSTANCE;
    }

    private RelationalColumnTypeMapper() {

        // Start by calling the superclass's constructor to initialise the Mapper
        super(
                "database_column",
                "Database Column",
                "RelationalColumnType",
                IGC_RID_PREFIX
        );

        // The list of properties that should be mapped
        addSimplePropertyMapping("name", "displayName");
        addSimplePropertyMapping("type", "dataType");

        // The list of relationships that should be mapped
        addRelationshipMapper(SchemaAttributeTypeMapper_DatabaseColumn.getInstance(null));

    }

}
