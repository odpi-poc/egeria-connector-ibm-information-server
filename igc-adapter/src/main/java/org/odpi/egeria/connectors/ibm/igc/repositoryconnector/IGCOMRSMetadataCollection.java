/* SPDX-License-Identifier: Apache-2.0 */
/* Copyright Contributors to the ODPi Egeria project. */
package org.odpi.egeria.connectors.ibm.igc.repositoryconnector;

import org.odpi.egeria.connectors.ibm.igc.clientlibrary.IGCRestClient;
import org.odpi.egeria.connectors.ibm.igc.clientlibrary.IGCVersionEnum;
import org.odpi.egeria.connectors.ibm.igc.clientlibrary.model.common.Identity;
import org.odpi.egeria.connectors.ibm.igc.clientlibrary.model.common.Reference;
import org.odpi.egeria.connectors.ibm.igc.clientlibrary.model.common.ReferenceList;
import org.odpi.egeria.connectors.ibm.igc.clientlibrary.search.IGCSearch;
import org.odpi.egeria.connectors.ibm.igc.clientlibrary.search.IGCSearchCondition;
import org.odpi.egeria.connectors.ibm.igc.clientlibrary.search.IGCSearchConditionSet;
import org.odpi.egeria.connectors.ibm.igc.clientlibrary.search.IGCSearchSorting;
import org.odpi.egeria.connectors.ibm.igc.repositoryconnector.mapping.EntityMappingInstance;
import org.odpi.egeria.connectors.ibm.igc.repositoryconnector.mapping.entities.EntityMapping;
import org.odpi.egeria.connectors.ibm.igc.repositoryconnector.model.OMRSStub;
import org.odpi.egeria.connectors.ibm.igc.repositoryconnector.stores.*;
import org.odpi.egeria.connectors.ibm.igc.repositoryconnector.mapping.classifications.ClassificationMapping;
import org.odpi.egeria.connectors.ibm.igc.repositoryconnector.mapping.relationships.RelationshipMapping;
import org.odpi.openmetadata.repositoryservices.connectors.stores.metadatacollectionstore.OMRSMetadataCollectionBase;
import org.odpi.openmetadata.repositoryservices.connectors.stores.metadatacollectionstore.properties.MatchCriteria;
import org.odpi.openmetadata.repositoryservices.connectors.stores.metadatacollectionstore.properties.SequencingOrder;
import org.odpi.openmetadata.repositoryservices.connectors.stores.metadatacollectionstore.properties.instances.*;
import org.odpi.openmetadata.repositoryservices.connectors.stores.metadatacollectionstore.properties.typedefs.*;
import org.odpi.openmetadata.repositoryservices.connectors.stores.metadatacollectionstore.repositoryconnector.OMRSRepositoryHelper;
import org.odpi.openmetadata.repositoryservices.connectors.stores.metadatacollectionstore.repositoryconnector.OMRSRepositoryValidator;
import org.odpi.openmetadata.repositoryservices.ffdc.OMRSErrorCode;
import org.odpi.openmetadata.repositoryservices.ffdc.exception.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.StringWriter;
import java.util.*;

/**
 * Provides the OMRSMetadataCollection implementation for IBM InfoSphere Information Governance Catalog ("IGC").
 */
public class IGCOMRSMetadataCollection extends OMRSMetadataCollectionBase {

    private static final Logger log = LoggerFactory.getLogger(IGCOMRSMetadataCollection.class);

    public static final String MAPPING_PKG = "org.odpi.egeria.connectors.ibm.igc.repositoryconnector.mapping.";
    public static final String DEFAULT_IGC_TYPE = "main_object";
    public static final String DEFAULT_IGC_TYPE_DISPLAY_NAME = "Main Object";

    public static final String GENERATED_TYPE_PREFIX = "__|";
    public static final String GENERATED_TYPE_POSTFIX = "|__";

    private IGCRestClient igcRestClient;
    private IGCOMRSRepositoryConnector igcomrsRepositoryConnector;

    private TypeDefStore typeDefStore;
    private EntityMappingStore entityMappingStore;
    private RelationshipMappingStore relationshipMappingStore;
    private ClassificationMappingStore classificationMappingStore;
    private AttributeMappingStore attributeMappingStore;

    private XMLOutputFactory xmlOutputFactory;

    /**
     * @param parentConnector      connector that this metadata collection supports.
     *                             The connector has the information to call the metadata repository.
     * @param repositoryName       name of this repository.
     * @param repositoryHelper     helper that provides methods to repository connectors and repository event mappers
     *                             to build valid type definitions (TypeDefs), entities and relationships.
     * @param repositoryValidator  validator class for checking open metadata repository objects and parameters
     * @param metadataCollectionId unique identifier for the repository
     */
    public IGCOMRSMetadataCollection(IGCOMRSRepositoryConnector parentConnector,
                                     String repositoryName,
                                     OMRSRepositoryHelper repositoryHelper,
                                     OMRSRepositoryValidator repositoryValidator,
                                     String metadataCollectionId) {
        super(parentConnector, repositoryName, repositoryHelper, repositoryValidator, metadataCollectionId);
        if (log.isDebugEnabled()) { log.debug("Constructing IGCOMRSMetadataCollection with name: {}", repositoryName); }
        parentConnector.setRepositoryName(repositoryName);
        this.igcRestClient = parentConnector.getIGCRestClient();
        this.igcomrsRepositoryConnector = parentConnector;
        this.xmlOutputFactory = XMLOutputFactory.newInstance();
        this.typeDefStore = new TypeDefStore();
        this.entityMappingStore = new EntityMappingStore(parentConnector);
        this.relationshipMappingStore = new RelationshipMappingStore(parentConnector);
        this.classificationMappingStore = new ClassificationMappingStore(parentConnector);
        this.attributeMappingStore = new AttributeMappingStore(parentConnector);
    }

    /**
     * Returns the list of different types of metadata organized into two groups.  The first are the
     * attribute type definitions (AttributeTypeDefs).  These provide types for properties in full
     * type definitions.  Full type definitions (TypeDefs) describe types for entities, relationships
     * and classifications.
     *
     * (Currently only full type definitions (TypeDefs) are implemented.)
     *
     * @param userId unique identifier for requesting user.
     * @return TypeDefGalleryResponse List of different categories of type definitions.
     * @throws RepositoryErrorException there is a problem communicating with the metadata repository.
     * @throws UserNotAuthorizedException the userId is not permitted to perform this operation.
     */
    @Override
    public TypeDefGallery getAllTypes(String   userId) throws RepositoryErrorException,
            UserNotAuthorizedException,
            InvalidParameterException
    {
        final String                       methodName = "getAllTypes";

        /*
         * Validate parameters
         */
        this.validateRepositoryConnector(methodName);
        parentConnector.validateRepositoryIsActive(methodName);

        repositoryValidator.validateUserId(repositoryName, userId, methodName);

        /*
         * Perform operation
         */
        TypeDefGallery typeDefGallery = new TypeDefGallery();
        List<TypeDef> typeDefs = typeDefStore.getAllTypeDefs();
        if (log.isDebugEnabled()) { log.debug("Retrieved {} implemented TypeDefs for this repository.", typeDefs.size()); }
        typeDefGallery.setTypeDefs(typeDefs);

        List<AttributeTypeDef> attributeTypeDefs = attributeMappingStore.getAllAttributeTypeDefs();
        if (log.isDebugEnabled()) { log.debug("Retrieved {} implemented AttributeTypeDefs for this repository.", attributeTypeDefs.size()); }
        typeDefGallery.setAttributeTypeDefs(attributeTypeDefs);

        return typeDefGallery;

    }

    /**
     * Returns all of the TypeDefs for a specific category.
     *
     * @param userId unique identifier for requesting user.
     * @param category enum value for the category of TypeDef to return.
     * @return TypeDefs list.
     * @throws InvalidParameterException the TypeDefCategory is null.
     * @throws RepositoryErrorException there is a problem communicating with the metadata repository.
     * @throws UserNotAuthorizedException the userId is not permitted to perform this operation.
     */
    @Override
    public List<TypeDef> findTypeDefsByCategory(String          userId,
                                                TypeDefCategory category) throws InvalidParameterException,
            RepositoryErrorException,
            UserNotAuthorizedException {
        final String methodName            = "findTypeDefsByCategory";
        final String categoryParameterName = "category";

        /*
         * Validate parameters
         */
        this.validateRepositoryConnector(methodName);
        parentConnector.validateRepositoryIsActive(methodName);

        repositoryValidator.validateUserId(repositoryName, userId, methodName);
        repositoryValidator.validateTypeDefCategory(repositoryName, categoryParameterName, category, methodName);

        List<TypeDef> typeDefs = new ArrayList<>();
        switch(category) {
            case ENTITY_DEF:
                typeDefs = entityMappingStore.getTypeDefs();
                break;
            case RELATIONSHIP_DEF:
                typeDefs = relationshipMappingStore.getTypeDefs();
                break;
            case CLASSIFICATION_DEF:
                typeDefs = classificationMappingStore.getTypeDefs();
                break;
        }
        return typeDefs;

    }


    /**
     * Returns all of the AttributeTypeDefs for a specific category.
     *
     * @param userId unique identifier for requesting user.
     * @param category enum value for the category of an AttributeTypeDef to return.
     * @return TypeDefs list.
     * @throws InvalidParameterException the TypeDefCategory is null.
     * @throws RepositoryErrorException there is a problem communicating with the metadata repository.
     * @throws UserNotAuthorizedException the userId is not permitted to perform this operation.
     */
    @Override
    public List<AttributeTypeDef> findAttributeTypeDefsByCategory(String                   userId,
                                                                  AttributeTypeDefCategory category) throws InvalidParameterException,
            RepositoryErrorException,
            UserNotAuthorizedException {
        final String methodName            = "findAttributeTypeDefsByCategory";
        final String categoryParameterName = "category";

        /*
         * Validate parameters
         */
        this.validateRepositoryConnector(methodName);
        parentConnector.validateRepositoryIsActive(methodName);

        repositoryValidator.validateUserId(repositoryName, userId, methodName);
        repositoryValidator.validateAttributeTypeDefCategory(repositoryName, categoryParameterName, category, methodName);

        return attributeMappingStore.getAttributeTypeDefsByCategory(category);

    }

    /**
     * Return the TypeDefs that have the properties matching the supplied match criteria.
     *
     * @param userId unique identifier for requesting user.
     * @param matchCriteria TypeDefProperties containing a list of property names.
     * @return TypeDefs list.
     * @throws InvalidParameterException the matchCriteria is null.
     * @throws RepositoryErrorException there is a problem communicating with the metadata repository.
     * @throws UserNotAuthorizedException the userId is not permitted to perform this operation.
     */
    @Override
    public List<TypeDef> findTypeDefsByProperty(String            userId,
                                                TypeDefProperties matchCriteria) throws InvalidParameterException,
            RepositoryErrorException,
            UserNotAuthorizedException
    {
        final String  methodName                 = "findTypeDefsByProperty";
        final String  matchCriteriaParameterName = "matchCriteria";

        /*
         * Validate parameters
         */
        this.validateRepositoryConnector(methodName);
        parentConnector.validateRepositoryIsActive(methodName);

        repositoryValidator.validateUserId(repositoryName, userId, methodName);
        repositoryValidator.validateMatchCriteria(repositoryName, matchCriteriaParameterName, matchCriteria, methodName);

        /*
         * Perform operation
         */
        List<TypeDef> typeDefs = typeDefStore.getAllTypeDefs();
        List<TypeDef> results = new ArrayList<>();
        if (matchCriteria != null) {
            Map<String, Object> properties = matchCriteria.getTypeDefProperties();
            for (TypeDef candidate : typeDefs) {
                List<TypeDefAttribute> candidateProperties = candidate.getPropertiesDefinition();
                for (TypeDefAttribute candidateAttribute : candidateProperties) {
                    String candidateName = candidateAttribute.getAttributeName();
                    if (properties.containsKey(candidateName)) {
                        results.add(candidate);
                    }
                }
            }
            results = typeDefs;
        }

        return results;

    }

    /**
     * Return the types that are linked to the elements from the specified standard.
     *
     * @param userId unique identifier for requesting user.
     * @param standard name of the standard; null means any.
     * @param organization name of the organization; null means any.
     * @param identifier identifier of the element in the standard; null means any.
     * @return TypeDefs list each entry in the list contains a typedef.  This is is a structure
     * describing the TypeDef's category and properties.
     * @throws InvalidParameterException all attributes of the external id are null.
     * @throws RepositoryErrorException there is a problem communicating with the metadata repository.
     * @throws UserNotAuthorizedException the userId is not permitted to perform this operation.
     */
    @Override
    public List<TypeDef> findTypesByExternalID(String    userId,
                                               String    standard,
                                               String    organization,
                                               String    identifier) throws InvalidParameterException,
            RepositoryErrorException,
            UserNotAuthorizedException
    {
        final String                       methodName = "findTypesByExternalID";

        /*
         * Validate parameters
         */
        this.validateRepositoryConnector(methodName);
        parentConnector.validateRepositoryIsActive(methodName);

        repositoryValidator.validateUserId(repositoryName, userId, methodName);
        repositoryValidator.validateExternalId(repositoryName, standard, organization, identifier, methodName);

        /*
         * Perform operation
         */
        List<TypeDef> typeDefs = typeDefStore.getAllTypeDefs();
        List<TypeDef> results;
        if (standard == null && organization == null && identifier == null) {
            results = typeDefs;
        } else {
            results = new ArrayList<>();
            for (TypeDef typeDef : typeDefs) {
                List<ExternalStandardMapping> externalStandardMappings = typeDef.getExternalStandardMappings();
                for (ExternalStandardMapping externalStandardMapping : externalStandardMappings) {
                    String candidateStandard = externalStandardMapping.getStandardName();
                    String candidateOrg = externalStandardMapping.getStandardOrganization();
                    String candidateId = externalStandardMapping.getStandardTypeName();
                    if ( (standard == null || standard.equals(candidateStandard))
                            && (organization == null || organization.equals(candidateOrg))
                            && (identifier == null || identifier.equals(candidateId))) {
                        results.add(typeDef);
                    }
                }
            }
        }

        return results;

    }

    /**
     * Return the TypeDefs that match the search criteria.
     *
     * @param userId unique identifier for requesting user.
     * @param searchCriteria String search criteria.
     * @return TypeDefs list where each entry in the list contains a typedef.  This is is a structure
     * describing the TypeDef's category and properties.
     * @throws InvalidParameterException the searchCriteria is null.
     * @throws RepositoryErrorException there is a problem communicating with the metadata repository.
     * @throws UserNotAuthorizedException the userId is not permitted to perform this operation.
     */
    @Override
    public List<TypeDef> searchForTypeDefs(String userId,
                                           String searchCriteria) throws InvalidParameterException,
            RepositoryErrorException,
            UserNotAuthorizedException
    {
        final String methodName                  = "searchForTypeDefs";
        final String searchCriteriaParameterName = "searchCriteria";

        /*
         * Validate parameters
         */
        this.validateRepositoryConnector(methodName);
        parentConnector.validateRepositoryIsActive(methodName);

        repositoryValidator.validateUserId(repositoryName, userId, methodName);
        repositoryValidator.validateSearchCriteria(repositoryName, searchCriteriaParameterName, searchCriteria, methodName);

        /*
         * Perform operation
         */
        List<TypeDef> typeDefs = new ArrayList<>();
        for (TypeDef candidate : entityMappingStore.getTypeDefs()) {
            if (candidate.getName().matches(searchCriteria)) {
                typeDefs.add(candidate);
            }
        }
        for (TypeDef candidate : relationshipMappingStore.getTypeDefs()) {
            if (candidate.getName().matches(searchCriteria)) {
                typeDefs.add(candidate);
            }
        }
        for (TypeDef candidate : classificationMappingStore.getTypeDefs()) {
            if (candidate.getName().matches(searchCriteria)) {
                typeDefs.add(candidate);
            }
        }

        return typeDefs;

    }

    /**
     * Return the TypeDef identified by the GUID.
     *
     * @param userId unique identifier for requesting user.
     * @param guid String unique id of the TypeDef.
     * @return TypeDef structure describing its category and properties.
     * @throws InvalidParameterException the guid is null.
     * @throws RepositoryErrorException there is a problem communicating with the metadata repository where
     *                                  the metadata collection is stored.
     * @throws TypeDefNotKnownException The requested TypeDef is not known in the metadata collection.
     * @throws UserNotAuthorizedException the userId is not permitted to perform this operation.
     */
    @Override
    public TypeDef getTypeDefByGUID(String    userId,
                                    String    guid) throws InvalidParameterException,
            RepositoryErrorException,
            TypeDefNotKnownException,
            UserNotAuthorizedException {
        final String methodName        = "getTypeDefByGUID";
        final String guidParameterName = "guid";

        /*
         * Validate parameters
         */
        this.validateRepositoryConnector(methodName);
        parentConnector.validateRepositoryIsActive(methodName);

        repositoryValidator.validateUserId(repositoryName, userId, methodName);
        repositoryValidator.validateGUID(repositoryName, guidParameterName, guid, methodName);

        TypeDef found = typeDefStore.getTypeDefByGUID(guid);

        if (found == null) {
            OMRSErrorCode errorCode = OMRSErrorCode.TYPEDEF_ID_NOT_KNOWN;
            String errorMessage = errorCode.getErrorMessageId() + errorCode.getFormattedErrorMessage(
                    guid,
                    guidParameterName,
                    methodName,
                    repositoryName);
            throw new TypeDefNotKnownException(errorCode.getHTTPErrorCode(),
                    this.getClass().getName(),
                    methodName,
                    errorMessage,
                    errorCode.getSystemAction(),
                    errorCode.getUserAction());
        }

        return found;

    }

    /**
     * Return the AttributeTypeDef identified by the GUID.
     *
     * @param userId unique identifier for requesting user.
     * @param guid String unique id of the TypeDef
     * @return TypeDef structure describing its category and properties.
     * @throws InvalidParameterException the guid is null.
     * @throws RepositoryErrorException there is a problem communicating with the metadata repository where
     *                                  the metadata collection is stored.
     * @throws TypeDefNotKnownException The requested TypeDef is not known in the metadata collection.
     * @throws UserNotAuthorizedException the userId is not permitted to perform this operation.
     */
    @Override
    public  AttributeTypeDef getAttributeTypeDefByGUID(String    userId,
                                                       String    guid) throws InvalidParameterException,
            RepositoryErrorException,
            TypeDefNotKnownException,
            UserNotAuthorizedException {
        final String methodName        = "getAttributeTypeDefByGUID";
        final String guidParameterName = "guid";

        /*
         * Validate parameters
         */
        this.validateRepositoryConnector(methodName);
        parentConnector.validateRepositoryIsActive(methodName);

        repositoryValidator.validateUserId(repositoryName, userId, methodName);
        repositoryValidator.validateGUID(repositoryName, guidParameterName, guid, methodName);

        AttributeTypeDef found = attributeMappingStore.getAttributeTypeDefByGUID(guid);
        if (found == null) {
            OMRSErrorCode errorCode = OMRSErrorCode.ATTRIBUTE_TYPEDEF_ID_NOT_KNOWN;
            String errorMessage = errorCode.getErrorMessageId() + errorCode.getFormattedErrorMessage(
                    "unknown",
                    guid,
                    methodName,
                    repositoryName);
            throw new TypeDefNotKnownException(errorCode.getHTTPErrorCode(),
                    this.getClass().getName(),
                    methodName,
                    errorMessage,
                    errorCode.getSystemAction(),
                    errorCode.getUserAction());
        }
        return found;

    }

    /**
     * Return the TypeDef identified by the unique name.
     *
     * @param userId unique identifier for requesting user.
     * @param name String name of the TypeDef.
     * @return TypeDef structure describing its category and properties.
     * @throws InvalidParameterException the name is null.
     * @throws RepositoryErrorException there is a problem communicating with the metadata repository where
     *                                  the metadata collection is stored.
     * @throws TypeDefNotKnownException the requested TypeDef is not found in the metadata collection.
     * @throws UserNotAuthorizedException the userId is not permitted to perform this operation.
     */
    @Override
    public TypeDef getTypeDefByName(String    userId,
                                    String    name) throws InvalidParameterException,
            RepositoryErrorException,
            TypeDefNotKnownException,
            UserNotAuthorizedException
    {
        final String  methodName = "getTypeDefByName";
        final String  nameParameterName = "name";

        /*
         * Validate parameters
         */
        this.validateRepositoryConnector(methodName);
        parentConnector.validateRepositoryIsActive(methodName);

        repositoryValidator.validateUserId(repositoryName, userId, methodName);
        repositoryValidator.validateTypeName(repositoryName, nameParameterName, name, methodName);

        /*
         * Perform operation
         */
        TypeDef found = typeDefStore.getTypeDefByName(name);

        if (found == null) {
            OMRSErrorCode errorCode = OMRSErrorCode.TYPEDEF_NAME_NOT_KNOWN;
            String errorMessage = errorCode.getErrorMessageId() + errorCode.getFormattedErrorMessage(
                    name,
                    methodName,
                    repositoryName);
            throw new TypeDefNotKnownException(errorCode.getHTTPErrorCode(),
                    this.getClass().getName(),
                    methodName,
                    errorMessage,
                    errorCode.getSystemAction(),
                    errorCode.getUserAction());
        }

        return found;

    }

    /**
     * Return the AttributeTypeDef identified by the unique name.
     *
     * @param userId unique identifier for requesting user.
     * @param name String name of the TypeDef.
     * @return TypeDef structure describing its category and properties.
     * @throws InvalidParameterException the name is null.
     * @throws RepositoryErrorException there is a problem communicating with the metadata repository where
     *                                  the metadata collection is stored.
     * @throws TypeDefNotKnownException the requested TypeDef is not found in the metadata collection.
     * @throws UserNotAuthorizedException the userId is not permitted to perform this operation.
     */
    @Override
    public  AttributeTypeDef getAttributeTypeDefByName(String    userId,
                                                       String    name) throws InvalidParameterException,
            RepositoryErrorException,
            TypeDefNotKnownException,
            UserNotAuthorizedException {
        final String  methodName = "getAttributeTypeDefByName";
        final String  nameParameterName = "name";

        /*
         * Validate parameters
         */
        this.validateRepositoryConnector(methodName);
        parentConnector.validateRepositoryIsActive(methodName);

        repositoryValidator.validateUserId(repositoryName, userId, methodName);
        repositoryValidator.validateTypeName(repositoryName, nameParameterName, name, methodName);

        AttributeTypeDef found = attributeMappingStore.getAttributeTypeDefByName(name);
        if (found == null) {
            OMRSErrorCode errorCode = OMRSErrorCode.ATTRIBUTE_TYPEDEF_NAME_NOT_KNOWN;
            String errorMessage = errorCode.getErrorMessageId() + errorCode.getFormattedErrorMessage(
                    name,
                    methodName,
                    repositoryName);
            throw new TypeDefNotKnownException(errorCode.getHTTPErrorCode(),
                    this.getClass().getName(),
                    methodName,
                    errorMessage,
                    errorCode.getSystemAction(),
                    errorCode.getUserAction());
        }
        return found;
    }

    /**
     * Create a collection of related types.
     *
     * @param userId unique identifier for requesting user.
     * @param newTypes TypeDefGalleryResponse structure describing the new AttributeTypeDefs and TypeDefs.
     * @throws InvalidParameterException the new TypeDef is null.
     * @throws RepositoryErrorException there is a problem communicating with the metadata repository where
     *                                  the metadata collection is stored.
     * @throws TypeDefNotSupportedException the repository is not able to support this TypeDef.
     * @throws TypeDefKnownException the TypeDef is already stored in the repository.
     * @throws TypeDefConflictException the new TypeDef conflicts with an existing TypeDef.
     * @throws InvalidTypeDefException the new TypeDef has invalid contents.
     * @throws FunctionNotSupportedException the repository does not support this call.
     * @throws UserNotAuthorizedException the userId is not permitted to perform this operation.
     */
    @Override
    public  void addTypeDefGallery(String          userId,
                                   TypeDefGallery  newTypes) throws InvalidParameterException,
            RepositoryErrorException,
            TypeDefNotSupportedException,
            TypeDefKnownException,
            TypeDefConflictException,
            InvalidTypeDefException,
            FunctionNotSupportedException,
            UserNotAuthorizedException
    {
        final String  methodName = "addTypeDefGallery";
        final String  galleryParameterName = "newTypes";

        /*
         * Validate parameters
         */
        this.validateRepositoryConnector(methodName);
        parentConnector.validateRepositoryIsActive(methodName);

        repositoryValidator.validateUserId(repositoryName, userId, methodName);
        repositoryValidator.validateTypeDefGallery(repositoryName, galleryParameterName, newTypes, methodName);

        /*
         * Perform operation
         */
        List<AttributeTypeDef> attributeTypeDefs = newTypes.getAttributeTypeDefs();
        if (attributeTypeDefs != null) {
            for (AttributeTypeDef attributeTypeDef : attributeTypeDefs) {
                addAttributeTypeDef(userId, attributeTypeDef);
            }
        }

        List<TypeDef> typeDefs = newTypes.getTypeDefs();
        if (typeDefs != null) {
            for (TypeDef typeDef : typeDefs) {
                addTypeDef(userId, typeDef);
            }
        }

    }

    /**
     * Create a definition of a new TypeDef.
     *
     * @param userId unique identifier for requesting user.
     * @param newTypeDef TypeDef structure describing the new TypeDef.
     * @throws InvalidParameterException the new TypeDef is null.
     * @throws RepositoryErrorException there is a problem communicating with the metadata repository where
     *                                  the metadata collection is stored.
     * @throws TypeDefNotSupportedException the repository is not able to support this TypeDef.
     * @throws TypeDefKnownException the TypeDef is already stored in the repository.
     * @throws TypeDefConflictException the new TypeDef conflicts with an existing TypeDef.
     * @throws InvalidTypeDefException the new TypeDef has invalid contents.
     * @throws FunctionNotSupportedException the repository does not support this call.
     * @throws UserNotAuthorizedException the userId is not permitted to perform this operation.
     */
    @Override
    public void addTypeDef(String       userId,
                           TypeDef      newTypeDef) throws InvalidParameterException,
            RepositoryErrorException,
            TypeDefNotSupportedException,
            TypeDefKnownException,
            TypeDefConflictException,
            InvalidTypeDefException,
            FunctionNotSupportedException,
            UserNotAuthorizedException {
        final String  methodName = "addTypeDef";
        final String  typeDefParameterName = "newTypeDef";

        /*
         * Validate parameters
         */
        this.validateRepositoryConnector(methodName);
        parentConnector.validateRepositoryIsActive(methodName);

        repositoryValidator.validateUserId(repositoryName, userId, methodName);
        repositoryValidator.validateTypeDef(repositoryName, typeDefParameterName, newTypeDef, methodName);
        repositoryValidator.validateUnknownTypeDef(repositoryName, typeDefParameterName, newTypeDef, methodName);

        TypeDefCategory typeDefCategory = newTypeDef.getCategory();
        String omrsTypeDefName = newTypeDef.getName();
        if (log.isDebugEnabled()) { log.debug("Looking for mapping for {} of type {}", omrsTypeDefName, typeDefCategory.getName()); }

        // See if we have a Mapper defined for the class -- if so, it's implemented
        StringBuilder sbMapperClassname = new StringBuilder();
        sbMapperClassname.append(MAPPING_PKG);
        switch(typeDefCategory) {
            case RELATIONSHIP_DEF:
                sbMapperClassname.append("relationships.");
                break;
            case CLASSIFICATION_DEF:
                sbMapperClassname.append("classifications.");
                break;
            case ENTITY_DEF:
                sbMapperClassname.append("entities.");
                break;
        }
        sbMapperClassname.append(omrsTypeDefName);
        sbMapperClassname.append("Mapper");

        try {

            Class mappingClass = Class.forName(sbMapperClassname.toString());
            if (log.isDebugEnabled()) { log.debug(" ... found mapping class: {}", mappingClass); }

            boolean success = false;

            switch(typeDefCategory) {
                case RELATIONSHIP_DEF:
                    success = relationshipMappingStore.addMapping(newTypeDef, mappingClass);
                    break;
                case CLASSIFICATION_DEF:
                    success = classificationMappingStore.addMapping(newTypeDef, mappingClass);
                    break;
                case ENTITY_DEF:
                    success = entityMappingStore.addMapping(newTypeDef, mappingClass, igcomrsRepositoryConnector);
                    break;
            }

            if (!success) {
                typeDefStore.addUnimplementedTypeDef(newTypeDef);
                throw new TypeDefNotSupportedException(404, IGCOMRSMetadataCollection.class.getName(), methodName, omrsTypeDefName + " is not supported.", "", "Request support through Egeria GitHub issue.");
            } else {
                typeDefStore.addTypeDef(newTypeDef);
            }

        } catch (ClassNotFoundException e) {
            typeDefStore.addUnimplementedTypeDef(newTypeDef);
            throw new TypeDefNotSupportedException(404, IGCOMRSMetadataCollection.class.getName(), methodName, omrsTypeDefName + " is not supported.", "", "Request support through Egeria GitHub issue.");
        }

    }

    /**
     * Create a definition of a new AttributeTypeDef.
     *
     * @param userId unique identifier for requesting user.
     * @param newAttributeTypeDef TypeDef structure describing the new TypeDef.
     * @throws InvalidParameterException the new TypeDef is null.
     * @throws RepositoryErrorException there is a problem communicating with the metadata repository where
     *                                  the metadata collection is stored.
     * @throws TypeDefNotSupportedException the repository is not able to support this TypeDef.
     * @throws TypeDefKnownException the TypeDef is already stored in the repository.
     * @throws TypeDefConflictException the new TypeDef conflicts with an existing TypeDef.
     * @throws InvalidTypeDefException the new TypeDef has invalid contents.
     * @throws FunctionNotSupportedException the repository does not support this call.
     * @throws UserNotAuthorizedException the userId is not permitted to perform this operation.
     */
    @Override
    public  void addAttributeTypeDef(String             userId,
                                     AttributeTypeDef   newAttributeTypeDef) throws InvalidParameterException,
            RepositoryErrorException,
            TypeDefNotSupportedException,
            TypeDefKnownException,
            TypeDefConflictException,
            InvalidTypeDefException,
            FunctionNotSupportedException,
            UserNotAuthorizedException {
        final String  methodName           = "addAttributeTypeDef";
        final String  typeDefParameterName = "newAttributeTypeDef";

        /*
         * Validate parameters
         */
        this.validateRepositoryConnector(methodName);
        parentConnector.validateRepositoryIsActive(methodName);

        repositoryValidator.validateUserId(repositoryName, userId, methodName);
        repositoryValidator.validateAttributeTypeDef(repositoryName, typeDefParameterName, newAttributeTypeDef, methodName);
        repositoryValidator.validateUnknownAttributeTypeDef(repositoryName, typeDefParameterName, newAttributeTypeDef, methodName);

        // Note this is only implemented for Enums, support for other types is indicated directly
        // in the verifyAttributeTypeDef method
        AttributeTypeDefCategory attributeTypeDefCategory = newAttributeTypeDef.getCategory();
        String omrsTypeDefName = newAttributeTypeDef.getName();
        if (log.isDebugEnabled()) { log.debug("Looking for mapping for {} of type {}", omrsTypeDefName, attributeTypeDefCategory.getName()); }

        // See if we have a Mapper defined for the class -- if so, it's implemented
        StringBuilder sbMapperClassname = new StringBuilder();
        sbMapperClassname.append(MAPPING_PKG);
        sbMapperClassname.append("attributes.");
        sbMapperClassname.append(omrsTypeDefName);
        sbMapperClassname.append("Mapper");

        try {
            Class mappingClass = Class.forName(sbMapperClassname.toString());
            if (log.isDebugEnabled()) { log.debug(" ... found mapping class: {}", mappingClass); }
            attributeMappingStore.addMapping(newAttributeTypeDef, mappingClass);
        } catch (ClassNotFoundException e) {
            throw new TypeDefNotSupportedException(404, IGCOMRSMetadataCollection.class.getName(), methodName, omrsTypeDefName + " is not supported.", "", "Request support through Egeria GitHub issue.");
        }

    }

    /**
     * Verify that a definition of a TypeDef is either new or matches the definition already stored.
     *
     * @param userId unique identifier for requesting user.
     * @param typeDef TypeDef structure describing the TypeDef to test.
     * @return boolean true means the TypeDef matches the local definition; false means the TypeDef is not known.
     * @throws InvalidParameterException the TypeDef is null.
     * @throws RepositoryErrorException there is a problem communicating with the metadata repository where
     *                                  the metadata collection is stored.
     * @throws TypeDefNotSupportedException the repository is not able to support this TypeDef.
     * @throws TypeDefConflictException the new TypeDef conflicts with an existing TypeDef.
     * @throws InvalidTypeDefException the new TypeDef has invalid contents.
     * @throws UserNotAuthorizedException the userId is not permitted to perform this operation.
     */
    @Override
    public boolean verifyTypeDef(String  userId,
                                 TypeDef typeDef) throws InvalidParameterException,
            RepositoryErrorException,
            TypeDefNotSupportedException,
            TypeDefConflictException,
            InvalidTypeDefException,
            UserNotAuthorizedException
    {
        final String  methodName           = "verifyTypeDef";
        final String  typeDefParameterName = "typeDef";

        /*
         * Validate parameters
         */
        this.validateRepositoryConnector(methodName);
        parentConnector.validateRepositoryIsActive(methodName);
        repositoryValidator.validateUserId(repositoryName, userId, methodName);
        repositoryValidator.validateTypeDef(repositoryName, typeDefParameterName, typeDef, methodName);

        String guid = typeDef.getGUID();

        // If we know the TypeDef is unimplemented, immediately throw an exception stating as much
        if (typeDefStore.getUnimplementedTypeDefByGUID(guid, false) != null) {
            throw new TypeDefNotSupportedException(
                    404,
                    IGCOMRSMetadataCollection.class.getName(),
                    methodName,
                    typeDef.getName() + " is not supported.",
                    "",
                    "Request support through Egeria GitHub issue.");
        } else if (typeDefStore.getTypeDefByGUID(guid) != null) {

            /* For a read-only connector, we do not need to support all of the instance statuses -- this validation is needed on write operations
            // Otherwise, validate that we support all of the valid InstanceStatus settings before deciding whether we
            // fully-support the TypeDef or not
            HashSet<InstanceStatus> validStatuses = new HashSet<>(typeDef.getValidInstanceStatusList());
             */

            boolean bVerified = true;
            List<String> issues = new ArrayList<>();

            Set<String> mappedProperties = new HashSet<>();
            switch (typeDef.getCategory()) {
                case ENTITY_DEF:
                    EntityMapping entityMapping = entityMappingStore.getMappingByOmrsTypeGUID(guid);
                    if (entityMapping != null) {
                        mappedProperties = entityMapping.getAllMappedOmrsProperties();
                    }
                    break;
                case RELATIONSHIP_DEF:
                    RelationshipMapping relationshipMapping = relationshipMappingStore.getMappingByOmrsTypeGUID(guid);
                    if (relationshipMapping != null) {
                        mappedProperties = relationshipMapping.getMappedOmrsPropertyNames();
                    }
                    break;
                case CLASSIFICATION_DEF:
                    ClassificationMapping classificationMapping = classificationMappingStore.getMappingByOmrsTypeGUID(guid);
                    if (classificationMapping != null) {
                        mappedProperties = classificationMapping.getMappedOmrsPropertyNames();
                    }
                    break;
            }

            // Validate that we support all of the possible properties before deciding whether we
            // fully-support the TypeDef or not
            List<TypeDefAttribute> properties = typeDef.getPropertiesDefinition();
            if (properties != null) {
                for (TypeDefAttribute typeDefAttribute : properties) {
                    String omrsPropertyName = typeDefAttribute.getAttributeName();
                    if (!mappedProperties.contains(omrsPropertyName)) {
                        bVerified = false;
                        issues.add("property '" + omrsPropertyName + "' is not mapped");
                    }
                }
            }

            // If we were unable to verify everything, throw exception indicating it is not a supported TypeDef
            if (!bVerified) {
                throw new TypeDefNotSupportedException(
                        404,
                        IGCOMRSMetadataCollection.class.getName(),
                        methodName,
                        typeDef.getName() + " is not supported: " + String.join(", ", issues),
                        "",
                        "Request support through Egeria GitHub issue.");
            } else {
                // Everything checked out, so return true
                return true;
            }

        } else {
            // It is completely unknown to us, so go ahead and try to addTypeDef
            return false;
        }

    }

    /**
     * Verify that a definition of an AttributeTypeDef is either new or matches the definition already stored.
     *
     * @param userId unique identifier for requesting user.
     * @param attributeTypeDef TypeDef structure describing the TypeDef to test.
     * @return boolean where true means the TypeDef matches the local definition where false means the TypeDef is not known.
     * @throws InvalidParameterException the TypeDef is null.
     * @throws RepositoryErrorException there is a problem communicating with the metadata repository where
     *                                  the metadata collection is stored.
     * @throws TypeDefNotSupportedException the repository is not able to support this TypeDef.
     * @throws TypeDefConflictException the new TypeDef conflicts with an existing TypeDef.
     * @throws InvalidTypeDefException the new TypeDef has invalid contents.
     * @throws UserNotAuthorizedException the userId is not permitted to perform this operation.
     */
    @Override
    public  boolean verifyAttributeTypeDef(String            userId,
                                           AttributeTypeDef  attributeTypeDef) throws InvalidParameterException,
            RepositoryErrorException,
            TypeDefNotSupportedException,
            TypeDefConflictException,
            InvalidTypeDefException,
            UserNotAuthorizedException
    {
        final String  methodName           = "verifyAttributeTypeDef";
        final String  typeDefParameterName = "attributeTypeDef";

        /*
         * Validate parameters
         */
        this.validateRepositoryConnector(methodName);
        parentConnector.validateRepositoryIsActive(methodName);

        repositoryValidator.validateUserId(repositoryName, userId, methodName);
        repositoryValidator.validateAttributeTypeDef(repositoryName, typeDefParameterName, attributeTypeDef, methodName);

        boolean bImplemented;
        switch (attributeTypeDef.getCategory()) {
            case PRIMITIVE:
                bImplemented = true;
                break;
            case UNKNOWN_DEF:
                bImplemented = false;
                break;
            case COLLECTION:
                bImplemented = true;
                break;
            case ENUM_DEF:
                bImplemented = (attributeMappingStore.getAttributeTypeDefByGUID(attributeTypeDef.getGUID()) != null);
                break;
            default:
                bImplemented = false;
                break;
        }

        return bImplemented;

    }

    /**
     * Returns the entity if the entity is stored in the metadata collection, otherwise null.
     *
     * @param userId unique identifier for requesting user.
     * @param guid String unique identifier for the entity
     * @return the entity details if the entity is found in the metadata collection; otherwise return null
     * @throws InvalidParameterException the guid is null.
     * @throws RepositoryErrorException there is a problem communicating with the metadata repository where
     *                                  the metadata collection is stored.
     * @throws UserNotAuthorizedException the userId is not permitted to perform this operation.
     */
    @Override
    public EntityDetail isEntityKnown(String     userId,
                                      String     guid) throws InvalidParameterException,
            RepositoryErrorException,
            UserNotAuthorizedException
    {
        final String  methodName = "isEntityKnown";
        final String  guidParameterName = "guid";

        /*
         * Validate parameters
         */
        this.validateRepositoryConnector(methodName);
        parentConnector.validateRepositoryIsActive(methodName);

        repositoryValidator.validateUserId(repositoryName, userId, methodName);
        repositoryValidator.validateGUID(repositoryName, guidParameterName, guid, methodName);

        /*
         * Perform operation
         */
        EntityDetail detail = null;
        try {
            detail = getEntityDetail(userId, guid);
        } catch (EntityNotKnownException | EntityProxyOnlyException e) {
            if (log.isInfoEnabled()) { log.info("Entity {} not known to the repository, or only a proxy.", guid, e); }
        }
        return detail;
    }


    /**
     * Return the header and classifications for a specific entity.
     *
     * @param userId unique identifier for requesting user.
     * @param guid String unique identifier for the entity.
     * @return EntitySummary structure
     * @throws InvalidParameterException the guid is null.
     * @throws RepositoryErrorException there is a problem communicating with the metadata repository where
     *                                  the metadata collection is stored.
     * @throws EntityNotKnownException the requested entity instance is not known in the metadata collection.
     * @throws UserNotAuthorizedException the userId is not permitted to perform this operation.
     */
    @Override
    public EntitySummary getEntitySummary(String     userId,
                                          String     guid) throws InvalidParameterException,
            RepositoryErrorException,
            EntityNotKnownException,
            UserNotAuthorizedException
    {
        final String  methodName        = "getEntitySummary";
        final String  guidParameterName = "guid";

        /*
         * Validate parameters
         */
        this.validateRepositoryConnector(methodName);
        parentConnector.validateRepositoryIsActive(methodName);

        repositoryValidator.validateUserId(repositoryName, userId, methodName);
        repositoryValidator.validateGUID(repositoryName, guidParameterName, guid, methodName);

        /*
         * Perform operation
         */
        if (log.isDebugEnabled()) { log.debug("getEntitySummary with guid = {}", guid); }

        // Lookup the basic asset based on the RID (strip off prefix (indicating a generated type), if there)
        String rid = getRidFromGeneratedId(guid);
        Reference asset = this.igcRestClient.getAssetRefById(rid);

        EntitySummary summary = null;
        String prefix = getPrefixFromGeneratedId(guid);

        // If we could not find any asset by the provided guid, throw an ENTITY_NOT_KNOWN exception
        if (asset == null) {
            OMRSErrorCode errorCode = OMRSErrorCode.ENTITY_NOT_KNOWN;
            String errorMessage = errorCode.getErrorMessageId() + errorCode.getFormattedErrorMessage(guid,
                    methodName,
                    repositoryName);
            throw new EntityNotKnownException(errorCode.getHTTPErrorCode(),
                    this.getClass().getName(),
                    methodName,
                    errorMessage,
                    errorCode.getSystemAction(),
                    errorCode.getUserAction());
        } else if (asset.getType().equals(DEFAULT_IGC_TYPE)) {
            /* If the asset type returned has an IGC-listed type of 'main_object', it isn't one that the REST API
             * of IGC supports (eg. a data rule detail object, a column analysis master object, etc)...
             * Trying to further process it will result in failed REST API requests; so we should skip these objects */
            OMRSErrorCode errorCode = OMRSErrorCode.INVALID_ENTITY_FROM_STORE;
            String errorMessage = errorCode.getErrorMessageId() + errorCode.getFormattedErrorMessage(guid,
                    repositoryName,
                    methodName,
                    asset.toString());
            throw new RepositoryErrorException(errorCode.getHTTPErrorCode(),
                    this.getClass().getName(),
                    methodName,
                    errorMessage,
                    errorCode.getSystemAction(),
                    errorCode.getUserAction());
        } else {

            // Otherwise, retrieve the mapping dynamically based on the type of asset
            EntityMappingInstance entityMap = getMappingInstanceForParameters(asset, prefix, userId);

            if (entityMap != null) {
                // 2. Apply the mapping to the object, and retrieve the resulting EntityDetail
                summary = EntityMapping.getEntitySummary(entityMap);
            } else {
                OMRSErrorCode errorCode = OMRSErrorCode.TYPEDEF_NOT_KNOWN_FOR_INSTANCE;
                String errorMessage = errorCode.getErrorMessageId() + errorCode.getFormattedErrorMessage(
                        prefix + asset.getType(),
                        "IGC asset",
                        methodName,
                        repositoryName);
                throw new RepositoryErrorException(errorCode.getHTTPErrorCode(),
                        this.getClass().getName(),
                        methodName,
                        errorMessage,
                        errorCode.getSystemAction(),
                        errorCode.getUserAction());
            }

        }

        return summary;

    }

    /**
     * Return the header, classifications and properties of a specific entity.
     *
     * @param userId unique identifier for requesting user.
     * @param guid String unique identifier for the entity.
     * @return EntityDetail structure.
     * @throws InvalidParameterException the guid is null.
     * @throws RepositoryErrorException there is a problem communicating with the metadata repository where
     *                                 the metadata collection is stored.
     * @throws EntityNotKnownException the requested entity instance is not known in the metadata collection.
     * @throws EntityProxyOnlyException the requested entity instance is only a proxy in the metadata collection.
     * @throws UserNotAuthorizedException the userId is not permitted to perform this operation.
     */
    @Override
    public EntityDetail getEntityDetail(String userId,
                                        String guid) throws InvalidParameterException,
            RepositoryErrorException,
            EntityNotKnownException,
            EntityProxyOnlyException,
            UserNotAuthorizedException
    {
        final String  methodName        = "getEntityDetail";
        final String  guidParameterName = "guid";

        /*
         * Validate parameters
         */
        this.validateRepositoryConnector(methodName);
        parentConnector.validateRepositoryIsActive(methodName);

        repositoryValidator.validateUserId(repositoryName, userId, methodName);
        repositoryValidator.validateGUID(repositoryName, guidParameterName, guid, methodName);

        /*
         * Perform operation
         */

        // Lookup the basic asset based on the RID (strip off prefix (indicating a generated type), if there)
        String rid = getRidFromGeneratedId(guid);
        Reference asset = this.igcRestClient.getAssetRefById(rid);

        return getEntityDetail(userId, guid, asset);

    }

    /**
     * Return the relationships for a specific entity. Note that currently this will only work for relationships known
     * to (originated within) IGC, and that not all parameters are (yet) implemented.
     *
     * @param userId unique identifier for requesting user.
     * @param entityGUID String unique identifier for the entity.
     * @param relationshipTypeGUID String GUID of the the type of relationship required (null for all).
     * @param fromRelationshipElement the starting element number of the relationships to return.
     *                                This is used when retrieving elements
     *                                beyond the first page of results. Zero means start from the first element.
     * @param limitResultsByStatus Not implemented for IGC -- will only retrieve ACTIVE entities.
     * @param asOfTime Must be null (history not implemented for IGC).
     * @param sequencingProperty Must be null (there are no properties on IGC relationships).
     * @param sequencingOrder Enum defining how the results should be ordered.
     * @param pageSize -- the maximum number of result classifications that can be returned on this request.  Zero means
     *                 unrestricted return results size.
     * @return Relationships list.  Null means no relationships associated with the entity.
     * @throws InvalidParameterException a parameter is invalid or null.
     * @throws TypeErrorException the type guid passed on the request is not known by the
     *                              metadata collection.
     * @throws RepositoryErrorException there is a problem communicating with the metadata repository where
     *                                  the metadata collection is stored.
     * @throws EntityNotKnownException the requested entity instance is not known in the metadata collection.
     * @throws PropertyErrorException the sequencing property is not valid for the attached classifications.
     * @throws PagingErrorException the paging/sequencing parameters are set up incorrectly.
     * @throws FunctionNotSupportedException the repository does not support the asOfTime parameter.
     * @throws UserNotAuthorizedException the userId is not permitted to perform this operation.
     */
    @Override
    public List<Relationship> getRelationshipsForEntity(String                     userId,
                                                        String                     entityGUID,
                                                        String                     relationshipTypeGUID,
                                                        int                        fromRelationshipElement,
                                                        List<InstanceStatus>       limitResultsByStatus,
                                                        Date                       asOfTime,
                                                        String                     sequencingProperty,
                                                        SequencingOrder            sequencingOrder,
                                                        int                        pageSize) throws InvalidParameterException,
            TypeErrorException,
            RepositoryErrorException,
            EntityNotKnownException,
            PropertyErrorException,
            PagingErrorException,
            FunctionNotSupportedException,
            UserNotAuthorizedException
    {
        final String  methodName = "getRelationshipsForEntity";
        final String  guidParameterName = "entityGUID";
        final String  typeGUIDParameter = "relationshipTypeGUID";
        final String  asOfTimeParameter = "asOfTime";
        final String  pageSizeParameter = "pageSize";

        /*
         * Validate parameters
         */
        this.validateRepositoryConnector(methodName);
        parentConnector.validateRepositoryIsActive(methodName);

        repositoryValidator.validateUserId(repositoryName, userId, methodName);
        repositoryValidator.validateGUID(repositoryName, guidParameterName, entityGUID, methodName);
        repositoryValidator.validateAsOfTime(repositoryName, asOfTimeParameter, asOfTime, methodName);
        repositoryValidator.validateOptionalTypeGUID(repositoryName, typeGUIDParameter, relationshipTypeGUID, methodName);
        repositoryValidator.validatePageSize(repositoryName, pageSizeParameter, pageSize, methodName);

        /*
         * Perform operation
         */
        ArrayList<Relationship> alRelationships = new ArrayList<>();

        // Immediately throw unimplemented exception if trying to retrieve historical view or sequence by property
        if (asOfTime != null) {
            OMRSErrorCode errorCode = OMRSErrorCode.METHOD_NOT_IMPLEMENTED;
            String errorMessage = errorCode.getErrorMessageId() + errorCode.getFormattedErrorMessage(methodName,
                    this.getClass().getName(),
                    repositoryName);
            throw new FunctionNotSupportedException(errorCode.getHTTPErrorCode(),
                    this.getClass().getName(),
                    methodName,
                    errorMessage,
                    errorCode.getSystemAction(),
                    errorCode.getUserAction());
        } else if (sequencingProperty != null
                || (sequencingOrder != null
                &&
                (sequencingOrder.equals(SequencingOrder.PROPERTY_ASCENDING)
                        || sequencingOrder.equals(SequencingOrder.PROPERTY_DESCENDING)))
        ) {
            OMRSErrorCode errorCode = OMRSErrorCode.METHOD_NOT_IMPLEMENTED;
            String errorMessage = errorCode.getErrorMessageId() + errorCode.getFormattedErrorMessage(methodName,
                    this.getClass().getName(),
                    repositoryName);
            throw new RepositoryErrorException(errorCode.getHTTPErrorCode(),
                    this.getClass().getName(),
                    methodName,
                    errorMessage,
                    errorCode.getSystemAction(),
                    errorCode.getUserAction());
        } else if (limitResultsByStatus == null
                || (limitResultsByStatus.size() == 1 && limitResultsByStatus.contains(InstanceStatus.ACTIVE))) {

            // Otherwise, only bother searching if we are after ACTIVE (or "all") entities -- non-ACTIVE means we
            // will just return an empty list

            // 0. see if the entityGUID has a prefix (indicating a generated type)
            String rid = getRidFromGeneratedId(entityGUID);
            String prefix = getPrefixFromGeneratedId(entityGUID);

            // 1. retrieve entity from IGC by GUID (RID)
            Reference asset = this.igcRestClient.getAssetRefById(rid);

            // Ensure the entity actually exists (if not, throw error to that effect)
            if (asset == null) {
                OMRSErrorCode errorCode = OMRSErrorCode.ENTITY_NOT_KNOWN;
                String errorMessage = errorCode.getErrorMessageId() + errorCode.getFormattedErrorMessage(methodName,
                        this.getClass().getName(),
                        repositoryName);
                throw new RepositoryErrorException(errorCode.getHTTPErrorCode(),
                        this.getClass().getName(),
                        methodName,
                        errorMessage,
                        errorCode.getSystemAction(),
                        errorCode.getUserAction());
            } else {

                EntityMappingInstance entityMap = getMappingInstanceForParameters(asset, prefix, userId);

                if (entityMap != null) {
                    // 2. Apply the mapping to the object, and retrieve the resulting relationships
                    alRelationships.addAll(
                            EntityMapping.getMappedRelationships(
                                    entityMap,
                                    relationshipTypeGUID,
                                    fromRelationshipElement,
                                    sequencingOrder,
                                    pageSize)
                    );
                } else {
                    OMRSErrorCode errorCode = OMRSErrorCode.TYPEDEF_NOT_KNOWN_FOR_INSTANCE;
                    String errorMessage = errorCode.getErrorMessageId() + errorCode.getFormattedErrorMessage(
                            prefix + asset.getType(),
                            "IGC asset",
                            methodName,
                            repositoryName);
                    throw new RepositoryErrorException(errorCode.getHTTPErrorCode(),
                            this.getClass().getName(),
                            methodName,
                            errorMessage,
                            errorCode.getSystemAction(),
                            errorCode.getUserAction());
                }

            }

        }

        return alRelationships.isEmpty() ? null : alRelationships;

    }

    /**
     * Return a list of entities that match the supplied properties according to the match criteria.  The results
     * can be returned over many pages.
     *
     * @param userId unique identifier for requesting user.
     * @param entityTypeGUID String unique identifier for the entity type of interest (null means any entity type).
     * @param matchProperties Optional list of entity properties to match (where any String property's value should
     *                        be defined as a Java regular expression, even if it should be an exact match).
     * @param matchCriteria Enum defining how the properties should be matched to the entities in the repository.
     * @param fromEntityElement the starting element number of the entities to return.
     *                                This is used when retrieving elements
     *                                beyond the first page of results. Zero means start from the first element.
     * @param limitResultsByStatus Not implemented for IGC, only ACTIVE entities will be returned.
     * @param limitResultsByClassification List of classifications that must be present on all returned entities.
     * @param asOfTime Must be null (history not implemented for IGC).
     * @param sequencingProperty String name of the entity property that is to be used to sequence the results.
     *                           Null means do not sequence on a property name (see SequencingOrder).
     * @param sequencingOrder Enum defining how the results should be ordered.
     * @param pageSize the maximum number of result entities that can be returned on this request.  Zero means
     *                 unrestricted return results size.
     * @return a list of entities matching the supplied criteria where null means no matching entities in the metadata
     * collection.
     *
     * @throws InvalidParameterException a parameter is invalid or null.
     * @throws TypeErrorException the type guid passed on the request is not known by the
     *                              metadata collection.
     * @throws RepositoryErrorException there is a problem communicating with the metadata repository where
     *                                    the metadata collection is stored.
     * @throws PropertyErrorException the properties specified are not valid for any of the requested types of
     *                                  entity.
     * @throws PagingErrorException the paging/sequencing parameters are set up incorrectly.
     * @throws FunctionNotSupportedException the repository does not support one of the provided parameters.
     * @throws UserNotAuthorizedException the userId is not permitted to perform this operation.
     * @see OMRSRepositoryHelper#getExactMatchRegex(String)
     */
    @Override
    public  List<EntityDetail> findEntitiesByProperty(String                    userId,
                                                      String                    entityTypeGUID,
                                                      InstanceProperties        matchProperties,
                                                      MatchCriteria             matchCriteria,
                                                      int                       fromEntityElement,
                                                      List<InstanceStatus>      limitResultsByStatus,
                                                      List<String>              limitResultsByClassification,
                                                      Date                      asOfTime,
                                                      String                    sequencingProperty,
                                                      SequencingOrder           sequencingOrder,
                                                      int                       pageSize) throws InvalidParameterException,
            TypeErrorException,
            RepositoryErrorException,
            PropertyErrorException,
            PagingErrorException,
            FunctionNotSupportedException,
            UserNotAuthorizedException
    {
        final String  methodName                   = "findEntitiesByProperty";
        final String  matchCriteriaParameterName   = "matchCriteria";
        final String  matchPropertiesParameterName = "matchProperties";
        final String  typeGUIDParameterName        = "entityTypeGUID";
        final String  asOfTimeParameter            = "asOfTime";
        final String  pageSizeParameter            = "pageSize";

        /*
         * Validate parameters
         */
        this.validateRepositoryConnector(methodName);
        parentConnector.validateRepositoryIsActive(methodName);

        repositoryValidator.validateUserId(repositoryName, userId, methodName);
        repositoryValidator.validateOptionalTypeGUID(repositoryName, typeGUIDParameterName, entityTypeGUID, methodName);
        repositoryValidator.validateAsOfTime(repositoryName, asOfTimeParameter, asOfTime, methodName);
        repositoryValidator.validatePageSize(repositoryName, pageSizeParameter, pageSize, methodName);
        repositoryValidator.validateMatchCriteria(repositoryName,
                matchCriteriaParameterName,
                matchPropertiesParameterName,
                matchCriteria,
                matchProperties,
                methodName);

        /*
         * Perform operation
         */
        ArrayList<EntityDetail> entityDetails = new ArrayList<>();

        // Immediately throw unimplemented exception if trying to retrieve historical view
        if (asOfTime != null) {
            OMRSErrorCode errorCode = OMRSErrorCode.METHOD_NOT_IMPLEMENTED;
            String errorMessage = errorCode.getErrorMessageId() + errorCode.getFormattedErrorMessage(methodName,
                    this.getClass().getName(),
                    repositoryName);
            throw new FunctionNotSupportedException(errorCode.getHTTPErrorCode(),
                    this.getClass().getName(),
                    methodName,
                    errorMessage,
                    errorCode.getSystemAction(),
                    errorCode.getUserAction());
        } else if (limitResultsByStatus == null
                || (limitResultsByStatus.size() == 1 && limitResultsByStatus.contains(InstanceStatus.ACTIVE))) {

            // Otherwise, only bother searching if we are after ACTIVE (or "all") entities -- non-ACTIVE means we
            // will just return an empty list

            // Short-circuit iterating through mappings if we are searching for something by qualifiedName,
            // in which case we should be able to infer the type we need to search based on the Identity implied
            // by the qualifiedName provided
            if (matchProperties != null
                    && matchProperties.getPropertyCount() == 1
                    && matchProperties.getPropertyNames().next().equals("qualifiedName")) {
                String qualifiedNameToFind = (String) ((PrimitivePropertyValue)matchProperties.getInstanceProperties().get("qualifiedName")).getPrimitiveValue();
                if (repositoryHelper.isExactMatchRegex(qualifiedNameToFind)) {
                    String unqualifiedName = repositoryHelper.getUnqualifiedLiteralString(qualifiedNameToFind);
                    String qualifiedName = unqualifiedName;
                    String prefix = null;
                    if (isGeneratedGUID(unqualifiedName)) {
                        prefix = getPrefixFromGeneratedId(unqualifiedName);
                        qualifiedName = getRidFromGeneratedId(unqualifiedName);
                    }
                    Identity identity = Identity.getFromString(qualifiedName, igcRestClient);
                    if (identity != null) {
                        String igcType = identity.getAssetType();
                        List<EntityMapping> mappers = getMappers(igcType, userId);
                        for (EntityMapping mapper : mappers) {
                            String mapperPrefix = mapper.getIgcRidPrefix();
                            if ( (mapperPrefix == null && prefix == null)
                                    || (mapperPrefix != null && mapperPrefix.equals(prefix)) ) {

                                // validate mapped OMRS type against the provided entityTypeGUID (if non-null), and
                                // only proceed with the search if IGC identity is a (sub)type of the one requested
                                boolean runSearch = true;
                                if (entityTypeGUID != null) {
                                    String mappedOmrsTypeName = mapper.getOmrsTypeDefName();
                                    TypeDef entityTypeDef = repositoryHelper.getTypeDef(repositoryName,
                                            "entityTypeGUID",
                                            entityTypeGUID,
                                            methodName);
                                    runSearch = repositoryHelper.isTypeOf(metadataCollectionId, mappedOmrsTypeName, entityTypeDef.getName());
                                }
                                if (runSearch) {
                                    processResultsForMapping(
                                            mapper,
                                            entityDetails,
                                            userId,
                                            matchProperties,
                                            matchCriteria,
                                            fromEntityElement,
                                            limitResultsByClassification,
                                            sequencingProperty,
                                            sequencingOrder,
                                            pageSize
                                    );
                                }
                            }
                        }
                    }
                }

            } else {

                // If we're searching for anything else, however, we need to iterate through all of the possible mappings
                // to ensure a full set of search results, so construct and run an appropriate search for each one
                List<EntityMapping> mappingsToSearch = getMappingsToSearch(entityTypeGUID, userId);

                for (EntityMapping mapping : mappingsToSearch) {

                    processResultsForMapping(
                            mapping,
                            entityDetails,
                            userId,
                            matchProperties,
                            matchCriteria,
                            fromEntityElement,
                            limitResultsByClassification,
                            sequencingProperty,
                            sequencingOrder,
                            pageSize
                    );

                }

            }

        }

        return entityDetails.isEmpty() ? null : entityDetails;

    }

    /**
     * Return a list of entities that have the requested type of classification attached.
     *
     * @param userId unique identifier for requesting user.
     * @param entityTypeGUID unique identifier for the type of entity requested.  Null mans any type of entity.
     * @param classificationName name of the classification a null is not valid.
     * @param matchClassificationProperties list of classification properties used to narrow the search (where any String
     *                                      property's value should be defined as a Java regular expression, even if it
     *                                      should be an exact match).
     * @param matchCriteria Enum defining how the properties should be matched to the classifications in the repository.
     * @param fromEntityElement the starting element number of the entities to return.
     *                                This is used when retrieving elements
     *                                beyond the first page of results. Zero means start from the first element.
     * @param limitResultsByStatus Not implemented for IGC, only ACTIVE entities will be returned.
     * @param asOfTime Requests a historical query of the entity.  Null means return the present values. (not implemented
     *                 for IGC, must be null)
     * @param sequencingProperty String name of the entity property that is to be used to sequence the results.
     *                           Null means do not sequence on a property name (see SequencingOrder).
     * @param sequencingOrder Enum defining how the results should be ordered.
     * @param pageSize the maximum number of result entities that can be returned on this request.  Zero means
     *                 unrestricted return results size.
     * @return a list of entities matching the supplied criteria where null means no matching entities in the metadata
     * collection.
     * @throws InvalidParameterException a parameter is invalid or null.
     * @throws TypeErrorException the type guid passed on the request is not known by the
     *                              metadata collection.
     * @throws RepositoryErrorException there is a problem communicating with the metadata repository where
     *                                    the metadata collection is stored.
     * @throws ClassificationErrorException the classification request is not known to the metadata collection.
     * @throws PropertyErrorException the properties specified are not valid for the requested type of
     *                                  classification.
     * @throws PagingErrorException the paging/sequencing parameters are set up incorrectly.
     * @throws FunctionNotSupportedException the repository does not support one of the provided parameters.
     * @throws UserNotAuthorizedException the userId is not permitted to perform this operation.
     * @see OMRSRepositoryHelper#getExactMatchRegex(String)
     */
    @Override
    public  List<EntityDetail> findEntitiesByClassification(String                    userId,
                                                            String                    entityTypeGUID,
                                                            String                    classificationName,
                                                            InstanceProperties        matchClassificationProperties,
                                                            MatchCriteria             matchCriteria,
                                                            int                       fromEntityElement,
                                                            List<InstanceStatus>      limitResultsByStatus,
                                                            Date                      asOfTime,
                                                            String                    sequencingProperty,
                                                            SequencingOrder           sequencingOrder,
                                                            int                       pageSize) throws InvalidParameterException,
            TypeErrorException,
            RepositoryErrorException,
            ClassificationErrorException,
            PropertyErrorException,
            PagingErrorException,
            FunctionNotSupportedException,
            UserNotAuthorizedException
    {
        final String  methodName                   = "findEntitiesByClassification";
        final String  classificationParameterName  = "classificationName";
        final String  entityTypeGUIDParameterName  = "entityTypeGUID";

        final String  matchCriteriaParameterName   = "matchCriteria";
        final String  matchPropertiesParameterName = "matchClassificationProperties";
        final String  asOfTimeParameter            = "asOfTime";
        final String  pageSizeParameter            = "pageSize";

        /*
         * Validate parameters
         */
        this.validateRepositoryConnector(methodName);
        parentConnector.validateRepositoryIsActive(methodName);

        repositoryValidator.validateUserId(repositoryName, userId, methodName);
        repositoryValidator.validateOptionalTypeGUID(repositoryName, entityTypeGUIDParameterName, entityTypeGUID, methodName);
        repositoryValidator.validateAsOfTime(repositoryName, asOfTimeParameter, asOfTime, methodName);
        repositoryValidator.validatePageSize(repositoryName, pageSizeParameter, pageSize, methodName);

        /*
         * Validate TypeDef
         */
        if (entityTypeGUID != null)
        {
            TypeDef entityTypeDef = repositoryHelper.getTypeDef(repositoryName,
                    entityTypeGUIDParameterName,
                    entityTypeGUID,
                    methodName);

            repositoryValidator.validateTypeDefForInstance(repositoryName,
                    entityTypeGUIDParameterName,
                    entityTypeDef,
                    methodName);

            repositoryValidator.validateClassification(repositoryName,
                    classificationParameterName,
                    classificationName,
                    entityTypeDef.getName(),
                    methodName);
        }

        repositoryValidator.validateMatchCriteria(repositoryName,
                matchCriteriaParameterName,
                matchPropertiesParameterName,
                matchCriteria,
                matchClassificationProperties,
                methodName);

        ArrayList<EntityDetail> entityDetails = new ArrayList<>();

        // Immediately throw unimplemented exception if trying to retrieve historical view
        if (asOfTime != null) {
            OMRSErrorCode errorCode = OMRSErrorCode.METHOD_NOT_IMPLEMENTED;
            String errorMessage = errorCode.getErrorMessageId() + errorCode.getFormattedErrorMessage(methodName,
                    this.getClass().getName(),
                    repositoryName);
            throw new FunctionNotSupportedException(errorCode.getHTTPErrorCode(),
                    this.getClass().getName(),
                    methodName,
                    errorMessage,
                    errorCode.getSystemAction(),
                    errorCode.getUserAction());
        } else if (limitResultsByStatus == null
                || (limitResultsByStatus.size() == 1 && limitResultsByStatus.contains(InstanceStatus.ACTIVE))) {

            // Otherwise, only bother searching if we are after ACTIVE (or "all") entities -- non-ACTIVE means we
            // will just return an empty list

            List<EntityMapping> mappingsToSearch = getMappingsToSearch(entityTypeGUID, userId);

            // Now iterate through all of the mappings we need to search, construct and run an appropriate search
            // for each one
            for (EntityMapping mapping : mappingsToSearch) {

                ClassificationMapping foundMapping = null;

                // Check which classifications (if any) are implemented for the entity mapping
                List<ClassificationMapping> classificationMappings = mapping.getClassificationMappers();
                for (ClassificationMapping classificationMapping : classificationMappings) {

                    // Check whether the implemented classification matches the one we're searching based on
                    String candidateName = classificationMapping.getOmrsClassificationType();
                    if (candidateName.equals(classificationName)) {
                        foundMapping = classificationMapping;
                        break;
                    }

                }

                // Only proceed if we have found a classification mapping for this entity that matches the search
                // criteria provided
                if (foundMapping != null) {

                    IGCSearch igcSearch = new IGCSearch();
                    igcSearch.addType(mapping.getIgcAssetType());
                    IGCSearchConditionSet igcSearchConditionSet = new IGCSearchConditionSet();

                    // Retrieve the list of property names we need for the classification
                    Set<String> igcClassificationProperties = foundMapping.getMappedIgcPropertyNames();

                    // Compose the search criteria for the classification as a set of nested conditions, so that
                    // matchCriteria does not change the meaning of what we're searching
                    IGCSearchConditionSet baseCriteria = foundMapping.getIGCSearchCriteria(matchClassificationProperties);
                    igcSearchConditionSet.addNestedConditionSet(baseCriteria);

                    IGCSearchSorting igcSearchSorting = null;
                    if (sequencingProperty == null && sequencingOrder != null) {
                        igcSearchSorting = IGCOMRSMetadataCollection.sortFromNonPropertySequencingOrder(sequencingOrder);
                    }

                    if (matchCriteria != null) {
                        switch (matchCriteria) {
                            case ALL:
                                igcSearchConditionSet.setMatchAnyCondition(false);
                                break;
                            case ANY:
                                igcSearchConditionSet.setMatchAnyCondition(true);
                                break;
                            case NONE:
                                igcSearchConditionSet.setMatchAnyCondition(false);
                                igcSearchConditionSet.setNegateAll(true);
                                break;
                        }
                    }

                    igcSearch.addProperties(new ArrayList(igcClassificationProperties));
                    igcSearch.addConditions(igcSearchConditionSet);

                    setPagingForSearch(igcSearch, fromEntityElement, pageSize);

                    if (igcSearchSorting != null) {
                        igcSearch.addSortingCriteria(igcSearchSorting);
                    }

                    processResults(
                            mapping,
                            this.igcRestClient.search(igcSearch),
                            entityDetails,
                            pageSize,
                            userId
                    );

                } else {
                    if (log.isInfoEnabled()) { log.info("No classification mapping has been implemented for {} on entity {} -- skipping from search.", classificationName, mapping.getOmrsTypeDefName()); }
                }

            }

        }

        return entityDetails.isEmpty() ? null : entityDetails;

    }

    /**
     * Return a list of entities whose string based property values match the search criteria.  The
     * search criteria may include regex style wild cards.
     *
     * @param userId unique identifier for requesting user.
     * @param entityTypeGUID GUID of the type of entity to search for. Null means all types will
     *                       be searched (could be slow so not recommended).
     * @param searchCriteria String Java regular expression used to match against any of the String property values
     *                       within the entities of the supplied type, even if it should be an exact match.
     *                       (Retrieve all entities of the supplied type if this is either null or an empty string.)
     * @param fromEntityElement the starting element number of the entities to return.
     *                                This is used when retrieving elements
     *                                beyond the first page of results. Zero means start from the first element.
     * @param limitResultsByStatus Not implemented for IGC, only ACTIVE entities will be returned.
     * @param limitResultsByClassification List of classifications that must be present on all returned entities.
     * @param asOfTime Must be null (history not implemented for IGC).
     * @param sequencingProperty String name of the property that is to be used to sequence the results.
     *                           Null means do not sequence on a property name (see SequencingOrder).
     *                           (currently not implemented for IGC)
     * @param sequencingOrder Enum defining how the results should be ordered.
     * @param pageSize the maximum number of result entities that can be returned on this request.  Zero means
     *                 unrestricted return results size.
     * @return a list of entities matching the supplied criteria; null means no matching entities in the metadata
     * collection.
     * @throws InvalidParameterException a parameter is invalid or null.
     * @throws TypeErrorException the type guid passed on the request is not known by the
     *                              metadata collection.
     * @throws RepositoryErrorException there is a problem communicating with the metadata repository where
     *                                    the metadata collection is stored.
     * @throws PropertyErrorException the sequencing property specified is not valid for any of the requested types of
     *                                  entity.
     * @throws PagingErrorException the paging/sequencing parameters are set up incorrectly.
     * @throws FunctionNotSupportedException the repository does not support one of the provided parameters.
     * @throws UserNotAuthorizedException the userId is not permitted to perform this operation.
     * @see OMRSRepositoryHelper#getExactMatchRegex(String)
     * @see OMRSRepositoryHelper#getContainsRegex(String)
     */
    @Override
    public  List<EntityDetail> findEntitiesByPropertyValue(String                userId,
                                                           String                entityTypeGUID,
                                                           String                searchCriteria,
                                                           int                   fromEntityElement,
                                                           List<InstanceStatus>  limitResultsByStatus,
                                                           List<String>          limitResultsByClassification,
                                                           Date                  asOfTime,
                                                           String                sequencingProperty,
                                                           SequencingOrder       sequencingOrder,
                                                           int                   pageSize) throws InvalidParameterException,
            TypeErrorException,
            RepositoryErrorException,
            PropertyErrorException,
            PagingErrorException,
            FunctionNotSupportedException,
            UserNotAuthorizedException
    {
        final String  methodName = "findEntitiesByPropertyValue";
        final String  typeGUIDParameter = "entityTypeGUID";
        final String  asOfTimeParameter = "asOfTime";
        final String  pageSizeParameter = "pageSize";

        /*
         * Validate parameters
         */
        this.validateRepositoryConnector(methodName);
        parentConnector.validateRepositoryIsActive(methodName);

        repositoryValidator.validateUserId(repositoryName, userId, methodName);
        repositoryValidator.validateOptionalTypeGUID(repositoryName, typeGUIDParameter, entityTypeGUID, methodName);
        repositoryValidator.validateAsOfTime(repositoryName, asOfTimeParameter, asOfTime, methodName);
        repositoryValidator.validatePageSize(repositoryName, pageSizeParameter, pageSize, methodName);

        /*
         * Process operation
         */
        ArrayList<EntityDetail> entityDetails = new ArrayList<>();

        // Immediately throw unimplemented exception if trying to retrieve historical view
        if (asOfTime != null) {
            OMRSErrorCode errorCode = OMRSErrorCode.METHOD_NOT_IMPLEMENTED;
            String errorMessage = errorCode.getErrorMessageId() + errorCode.getFormattedErrorMessage(methodName,
                    this.getClass().getName(),
                    repositoryName);
            throw new FunctionNotSupportedException(errorCode.getHTTPErrorCode(),
                    this.getClass().getName(),
                    methodName,
                    errorMessage,
                    errorCode.getSystemAction(),
                    errorCode.getUserAction());
        } else if (limitResultsByStatus == null
                || (limitResultsByStatus.size() == 1 && limitResultsByStatus.contains(InstanceStatus.ACTIVE))) {

            // Otherwise, only bother searching if we are after ACTIVE (or "all") entities -- non-ACTIVE means we
            // will just return an empty list

            List<EntityMapping> mappingsToSearch = getMappingsToSearch(entityTypeGUID, userId);

            // Now iterate through all of the mappings we need to search, construct and run an appropriate search
            // for each one
            for (EntityMapping mapping : mappingsToSearch) {

                IGCSearch igcSearch = new IGCSearch();
                String igcAssetType = addTypeToSearch(mapping, igcSearch);

                // Get POJO from the asset type, and use this to retrieve a listing of all string properties
                // for that asset type -- these are the list of properties we should use for the search
                Class pojo = igcRestClient.getPOJOForType(igcAssetType);

                if (pojo != null) {

                    IGCSearchConditionSet classificationLimiters = getSearchCriteriaForClassifications(
                            igcAssetType,
                            limitResultsByClassification
                    );

                    if (limitResultsByClassification != null && !limitResultsByClassification.isEmpty() && classificationLimiters == null) {
                        if (log.isInfoEnabled()) { log.info("Classification limiters were specified, but none apply to the asset type {}, so excluding this asset type from search.", igcAssetType); }
                    } else {

                        IGCSearchConditionSet outerConditions = new IGCSearchConditionSet();

                        // If the searchCriteria is empty, retrieve all entities of the type (no conditions)
                        if (searchCriteria != null && !searchCriteria.equals("")) {

                            List<String> properties = igcRestClient.getStringPropertiesFromPOJO(igcAssetType);
                            // POST'd search to IGC doesn't work on v11.7.0.2 using long_description
                            // Using "searchText" requires using "searchProperties" (no "where" conditions) -- but does not
                            // work with 'main_object', must be used with a specific asset type
                            // Therefore for v11.7.0.2 we will simply drop long_description from the fields we search
                            if (igcRestClient.getIgcVersion().isEqualTo(IGCVersionEnum.V11702)) {
                                ArrayList<String> propertiesWithoutLongDescription = new ArrayList<>();
                                for (String property : properties) {
                                    if (!property.equals("long_description")) {
                                        propertiesWithoutLongDescription.add(property);
                                    }
                                }
                                properties = propertiesWithoutLongDescription;
                            }

                            IGCSearchConditionSet innerConditions = new IGCSearchConditionSet();
                            innerConditions.setMatchAnyCondition(true);
                            for (String property : properties) {
                                String unqualifiedValue = repositoryHelper.getUnqualifiedLiteralString(searchCriteria);
                                if (repositoryHelper.isContainsRegex(searchCriteria)) {
                                    innerConditions.addCondition(new IGCSearchCondition(
                                            property,
                                            "like %{0}%",
                                            unqualifiedValue
                                    ));
                                } else if (repositoryHelper.isStartsWithRegex(searchCriteria)) {
                                    innerConditions.addCondition(new IGCSearchCondition(
                                            property,
                                            "like {0}%",
                                            unqualifiedValue
                                    ));
                                } else if (repositoryHelper.isEndsWithRegex(searchCriteria)) {
                                    innerConditions.addCondition(new IGCSearchCondition(
                                            property,
                                            "like %{0}",
                                            unqualifiedValue
                                    ));
                                } else if (repositoryHelper.isExactMatchRegex(searchCriteria)) {
                                    innerConditions.addCondition(new IGCSearchCondition(
                                            property,
                                            "=",
                                            unqualifiedValue
                                    ));
                                } else {
                                    IGCOMRSErrorCode errorCode = IGCOMRSErrorCode.REGEX_NOT_IMPLEMENTED;
                                    String errorMessage = errorCode.getErrorMessageId() + errorCode.getFormattedErrorMessage(
                                            repositoryName,
                                            searchCriteria);
                                    throw new FunctionNotSupportedException(errorCode.getHTTPErrorCode(),
                                            this.getClass().getName(),
                                            methodName,
                                            errorMessage,
                                            errorCode.getSystemAction(),
                                            errorCode.getUserAction());
                                }
                            }
                            outerConditions.addNestedConditionSet(innerConditions);

                        }

                        if (classificationLimiters != null) {
                            outerConditions.addNestedConditionSet(classificationLimiters);
                            outerConditions.setMatchAnyCondition(false);
                        }

                        IGCSearchSorting igcSearchSorting = null;
                        if (sequencingProperty == null && sequencingOrder != null) {
                            igcSearchSorting = IGCOMRSMetadataCollection.sortFromNonPropertySequencingOrder(sequencingOrder);
                        }

                        igcSearch.addConditions(outerConditions);

                        setPagingForSearch(igcSearch, fromEntityElement, pageSize);

                        if (igcSearchSorting != null) {
                            igcSearch.addSortingCriteria(igcSearchSorting);
                        }

                        processResults(
                                mapping,
                                this.igcRestClient.search(igcSearch),
                                entityDetails,
                                pageSize,
                                userId
                        );

                    }

                } else {
                    if (log.isWarnEnabled()) { log.warn("Unable to find POJO to handle IGC asset type '{}' -- skipping search against this asset type.", igcAssetType); }
                }

            }

        }

        return entityDetails.isEmpty() ? null : entityDetails;

    }

    /**
     * Returns a boolean indicating if the relationship is stored in the metadata collection.
     *
     * @param userId unique identifier for requesting user.
     * @param guid String unique identifier for the relationship.
     * @return relationship details if the relationship is found in the metadata collection; otherwise return null.
     * @throws InvalidParameterException the guid is null.
     * @throws RepositoryErrorException there is a problem communicating with the metadata repository where
     *                                  the metadata collection is stored.
     * @throws UserNotAuthorizedException the userId is not permitted to perform this operation.
     */
    @Override
    public Relationship  isRelationshipKnown(String     userId,
                                             String     guid) throws InvalidParameterException,
            RepositoryErrorException,
            UserNotAuthorizedException
    {
        final String  methodName = "isRelationshipKnown";
        final String  guidParameterName = "guid";

        /*
         * Validate parameters
         */
        this.validateRepositoryConnector(methodName);
        parentConnector.validateRepositoryIsActive(methodName);

        repositoryValidator.validateUserId(repositoryName, userId, methodName);
        repositoryValidator.validateGUID(repositoryName, guidParameterName, guid, methodName);

        /*
         * Process operation
         */
        Relationship relationship = null;
        try {
            relationship = getRelationship(userId, guid);
        } catch (RelationshipNotKnownException e) {
            if (log.isInfoEnabled()) { log.info("Could not find relationship {} in repository.", guid, e); }
        }
        return relationship;
    }

    /**
     * Return a requested relationship. Note that currently this will only work for relationships known to
     * (originated within) IGC.
     *
     * @param userId unique identifier for requesting user.
     * @param guid String unique identifier for the relationship.
     * @return a relationship structure.
     * @throws InvalidParameterException the guid is null.
     * @throws RepositoryErrorException there is a problem communicating with the metadata repository where
     *                                    the metadata collection is stored.
     * @throws RelationshipNotKnownException the metadata collection does not have a relationship with
     *                                         the requested GUID stored.
     * @throws UserNotAuthorizedException the userId is not permitted to perform this operation.
     */
    @Override
    public Relationship getRelationship(String    userId,
                                        String    guid) throws InvalidParameterException,
            RepositoryErrorException,
            RelationshipNotKnownException,
            UserNotAuthorizedException
    {
        final String  methodName = "getRelationship";
        final String  guidParameterName = "guid";

        /*
         * Validate parameters
         */
        this.validateRepositoryConnector(methodName);
        parentConnector.validateRepositoryIsActive(methodName);

        repositoryValidator.validateUserId(repositoryName, userId, methodName);
        repositoryValidator.validateGUID(repositoryName, guidParameterName, guid, methodName);

        /*
         * Process operation
         */

        // Translate the key properties of the GUID into IGC-retrievables
        String proxyOneRid = RelationshipMapping.getProxyOneGUIDFromRelationshipGUID(guid);
        String proxyTwoRid = RelationshipMapping.getProxyTwoGUIDFromRelationshipGUID(guid);
        String omrsRelationshipName = RelationshipMapping.getRelationshipTypeFromRelationshipGUID(guid);

        String proxyOneIgcRid = proxyOneRid;
        String proxyTwoIgcRid = proxyTwoRid;

        if (isGeneratedGUID(proxyOneRid)) {
            proxyOneIgcRid = getRidFromGeneratedId(proxyOneRid);
        }
        if (isGeneratedGUID(proxyTwoRid)) {
            proxyTwoIgcRid = getRidFromGeneratedId(proxyTwoRid);
        }

        if (log.isDebugEnabled()) { log.debug("Looking up relationship: {}", guid); }

        // Should not need to translate from proxyone / proxytwo to alternative assets, as the RIDs provided
        // in the relationship GUID should already be pointing to the correct assets
        String relationshipLevelRid = (proxyOneRid.equals(proxyTwoRid)) ? proxyOneRid : null;
        Reference proxyOne;
        Reference proxyTwo;
        RelationshipMapping relationshipMapping;
        if (relationshipLevelRid != null) {
            Reference relationshipAsset = igcRestClient.getAssetRefById(proxyOneIgcRid);
            String relationshipAssetType = relationshipAsset.getType();
            relationshipMapping = relationshipMappingStore.getMappingByTypes(
                    omrsRelationshipName,
                    relationshipAssetType,
                    relationshipAssetType
            );
            // Only need to translate if the RIDs are relationship-level RIDs
            proxyOne = relationshipMapping.getProxyOneAssetFromAsset(relationshipAsset, igcRestClient).get(0);
            proxyTwo = relationshipMapping.getProxyTwoAssetFromAsset(relationshipAsset, igcRestClient).get(0);
        } else {
            proxyOne = igcRestClient.getAssetRefById(proxyOneIgcRid);
            proxyTwo = igcRestClient.getAssetRefById(proxyTwoIgcRid);
            relationshipMapping = relationshipMappingStore.getMappingByTypes(
                    omrsRelationshipName,
                    proxyOne.getType(),
                    proxyTwo.getType()
            );
        }

        Relationship found = null;

        if (relationshipMapping != null) {
            try {

                RelationshipDef omrsRelationshipDef = (RelationshipDef) getTypeDefByName(userId, omrsRelationshipName);
                // Since the ordering should be set by the GUID we're lookup up anyway, we'll simply set the property
                // to one of the proxyOne properties
                String igcPropertyName = relationshipMapping.getProxyOneMapping().getIgcRelationshipProperties().get(0);
                if (log.isDebugEnabled()) { log.debug(" ... using property: {}", igcPropertyName); }
                found = RelationshipMapping.getMappedRelationship(
                        igcomrsRepositoryConnector,
                        relationshipMapping,
                        omrsRelationshipDef,
                        proxyOne,
                        proxyTwo,
                        igcPropertyName,
                        userId,
                        relationshipLevelRid
                );

            } catch (TypeDefNotKnownException e) {
                OMRSErrorCode errorCode = OMRSErrorCode.TYPEDEF_NOT_KNOWN;
                String errorMessage = errorCode.getErrorMessageId() + errorCode.getFormattedErrorMessage(
                        omrsRelationshipName,
                        guid,
                        guidParameterName,
                        methodName,
                        repositoryName);
                throw new RepositoryErrorException(errorCode.getHTTPErrorCode(),
                        this.getClass().getName(),
                        methodName,
                        errorMessage,
                        errorCode.getSystemAction(),
                        errorCode.getUserAction());
            }
        } else {
            OMRSErrorCode errorCode = OMRSErrorCode.TYPEDEF_NOT_KNOWN;
            String errorMessage = errorCode.getErrorMessageId() + errorCode.getFormattedErrorMessage(
                    omrsRelationshipName,
                    guid,
                    guidParameterName,
                    methodName,
                    repositoryName);
            throw new RepositoryErrorException(errorCode.getHTTPErrorCode(),
                    this.getClass().getName(),
                    methodName,
                    errorMessage,
                    errorCode.getSystemAction(),
                    errorCode.getUserAction());
        }

        return found;

    }


    /**
     * Add the requested classification to a specific entity.
     *
     * @param userId unique identifier for requesting user.
     * @param entityGUID String unique identifier (guid) for the entity.
     * @param classificationName String name for the classification.
     * @param classificationProperties list of properties to set in the classification.
     * @return EntityDetail showing the resulting entity header, properties and classifications.
     * @throws InvalidParameterException one of the parameters is invalid or null.
     * @throws RepositoryErrorException there is a problem communicating with the metadata repository where
     *                                  the metadata collection is stored.
     * @throws EntityNotKnownException the entity identified by the guid is not found in the metadata collection
     * @throws ClassificationErrorException the requested classification is either not known or not valid
     *                                         for the entity.
     * @throws PropertyErrorException one or more of the requested properties are not defined, or have different
     *                                characteristics in the TypeDef for this classification type
     * @throws UserNotAuthorizedException the userId is not permitted to perform this operation.
     */
    @Override
    public EntityDetail classifyEntity(String               userId,
                                       String               entityGUID,
                                       String               classificationName,
                                       InstanceProperties   classificationProperties) throws InvalidParameterException,
            RepositoryErrorException,
            EntityNotKnownException,
            ClassificationErrorException,
            PropertyErrorException,
            UserNotAuthorizedException
    {
        final String  methodName                  = "classifyEntity";
        final String  entityGUIDParameterName     = "entityGUID";
        final String  classificationParameterName = "classificationName";
        final String  propertiesParameterName     = "classificationProperties";

        /*
         * Validate parameters
         */
        this.validateRepositoryConnector(methodName);
        parentConnector.validateRepositoryIsActive(methodName);

        repositoryValidator.validateUserId(repositoryName, userId, methodName);
        repositoryValidator.validateGUID(repositoryName, entityGUIDParameterName, entityGUID, methodName);

        /*
         * Locate entity
         */
        EntityDetail entityDetail = null;

        try {
            TypeDef classificationTypeDef = getTypeDefByName(userId, classificationName);
            if (classificationTypeDef != null) {

                String rid = getRidFromGeneratedId(entityGUID);
                Reference igcEntity = this.igcRestClient.getAssetRefById(rid);

                if (igcEntity == null) {
                    OMRSErrorCode errorCode = OMRSErrorCode.ENTITY_NOT_KNOWN;
                    String errorMessage = errorCode.getErrorMessageId() + errorCode.getFormattedErrorMessage(
                            entityGUID,
                            methodName,
                            repositoryName
                    );
                    throw new EntityNotKnownException(errorCode.getHTTPErrorCode(),
                            this.getClass().getName(),
                            methodName,
                            errorMessage,
                            errorCode.getSystemAction(),
                            errorCode.getUserAction());
                }

                ClassificationMapping classificationMapping = classificationMappingStore.getMappingByTypes(classificationName, igcEntity.getType());

                if (classificationMapping != null) {

                    entityDetail = classificationMapping.addClassificationToIGCAsset(
                            igcomrsRepositoryConnector,
                            igcEntity,
                            entityGUID,
                            classificationProperties,
                            userId
                    );

                } else {
                    OMRSErrorCode errorCode = OMRSErrorCode.TYPEDEF_NAME_NOT_KNOWN;
                    String errorMessage = errorCode.getErrorMessageId() + errorCode.getFormattedErrorMessage(
                            classificationName,
                            methodName,
                            repositoryName
                    );
                    throw new ClassificationErrorException(
                            errorCode.getHTTPErrorCode(),
                            this.getClass().getName(),
                            methodName,
                            errorMessage,
                            errorCode.getSystemAction(),
                            errorCode.getUserAction()
                    );
                }

            } else {
                OMRSErrorCode errorCode = OMRSErrorCode.TYPEDEF_ID_NOT_KNOWN;
                String errorMessage = errorCode.getErrorMessageId() + errorCode.getFormattedErrorMessage(
                        classificationName,
                        classificationParameterName,
                        methodName,
                        repositoryName
                );
                throw new ClassificationErrorException(
                        errorCode.getHTTPErrorCode(),
                        this.getClass().getName(),
                        methodName,
                        errorMessage,
                        errorCode.getSystemAction(),
                        errorCode.getUserAction()
                );
            }
        } catch (TypeDefNotKnownException e) {
            OMRSErrorCode errorCode = OMRSErrorCode.TYPEDEF_ID_NOT_KNOWN;
            String errorMessage = errorCode.getErrorMessageId() + errorCode.getFormattedErrorMessage(
                    classificationName,
                    classificationParameterName,
                    methodName,
                    repositoryName
            );
            throw new ClassificationErrorException(
                    errorCode.getHTTPErrorCode(),
                    this.getClass().getName(),
                    methodName,
                    errorMessage,
                    errorCode.getSystemAction(),
                    errorCode.getUserAction()
            );
        }

        return entityDetail;

    }


    /**
     * Add a new relationship between two entities to the metadata collection.
     *
     * @param userId unique identifier for requesting user.
     * @param relationshipTypeGUID unique identifier (guid) for the new relationship's type.
     * @param initialProperties initial list of properties for the new entity; null means no properties.
     * @param entityOneGUID the unique identifier of one of the entities that the relationship is connecting together.
     * @param entityTwoGUID the unique identifier of the other entity that the relationship is connecting together.
     * @param initialStatus initial status typically set to DRAFT, PREPARED or ACTIVE.
     * @return Relationship structure with the new header, requested entities and properties.
     * @throws InvalidParameterException one of the parameters is invalid or null.
     * @throws RepositoryErrorException there is a problem communicating with the metadata repository where
     *                                 the metadata collection is stored.
     * @throws TypeErrorException the requested type is not known, or not supported in the metadata repository
     *                            hosting the metadata collection.
     * @throws PropertyErrorException one or more of the requested properties are not defined, or have different
     *                                  characteristics in the TypeDef for this relationship's type.
     * @throws EntityNotKnownException one of the requested entities is not known in the metadata collection.
     * @throws StatusNotSupportedException the metadata repository hosting the metadata collection does not support
     *                                     the requested status.
     * @throws UserNotAuthorizedException the userId is not permitted to perform this operation.
     */
    @Override
    public Relationship addRelationship(String               userId,
                                        String               relationshipTypeGUID,
                                        InstanceProperties   initialProperties,
                                        String               entityOneGUID,
                                        String               entityTwoGUID,
                                        InstanceStatus       initialStatus) throws InvalidParameterException,
            RepositoryErrorException,
            TypeErrorException,
            PropertyErrorException,
            EntityNotKnownException,
            StatusNotSupportedException,
            UserNotAuthorizedException
    {
        final String  methodName = "addRelationship";
        final String  guidParameterName = "relationshipTypeGUID";
        final String  propertiesParameterName       = "initialProperties";
        final String  initialStatusParameterName    = "initialStatus";

        /*
         * Validate parameters
         */
        this.validateRepositoryConnector(methodName);
        parentConnector.validateRepositoryIsActive(methodName);

        repositoryValidator.validateUserId(repositoryName, userId, methodName);
        repositoryValidator.validateTypeGUID(repositoryName, guidParameterName, relationshipTypeGUID, methodName);

        TypeDef  typeDef = repositoryHelper.getTypeDef(repositoryName, guidParameterName, relationshipTypeGUID, methodName);

        repositoryValidator.validateTypeDefForInstance(repositoryName, guidParameterName, typeDef, methodName);


        repositoryValidator.validatePropertiesForType(repositoryName,
                propertiesParameterName,
                typeDef,
                initialProperties,
                methodName);

        repositoryValidator.validateInstanceStatus(repositoryName,
                initialStatusParameterName,
                initialStatus,
                typeDef,
                methodName);

        /*
         * Validation complete, ok to create new instance
         */
        Relationship relationship = null;

        if (initialStatus != null && initialStatus != InstanceStatus.ACTIVE) {
            OMRSErrorCode errorCode = OMRSErrorCode.BAD_INSTANCE_STATUS;
            String errorMessage = errorCode.getErrorMessageId() + errorCode.getFormattedErrorMessage(
                    initialStatus.getName(),
                    initialStatusParameterName,
                    methodName,
                    repositoryName,
                    relationshipTypeGUID);
            throw new StatusNotSupportedException(errorCode.getHTTPErrorCode(),
                    this.getClass().getName(),
                    methodName,
                    errorMessage,
                    errorCode.getSystemAction(),
                    errorCode.getUserAction());
        }

        try {
            TypeDef relationshipTypeDef = getTypeDefByGUID(userId, relationshipTypeGUID);
            if (relationshipTypeDef != null) {

                String relationshipTypeName = relationshipTypeDef.getName();
                Reference entityOne = this.igcRestClient.getAssetRefById(entityOneGUID);
                Reference entityTwo = this.igcRestClient.getAssetRefById(entityTwoGUID);

                if (entityOne == null) {
                    OMRSErrorCode errorCode = OMRSErrorCode.ENTITY_NOT_KNOWN;
                    String errorMessage = errorCode.getErrorMessageId() + errorCode.getFormattedErrorMessage(
                            entityOneGUID,
                            methodName,
                            repositoryName
                    );
                    throw new EntityNotKnownException(errorCode.getHTTPErrorCode(),
                            this.getClass().getName(),
                            methodName,
                            errorMessage,
                            errorCode.getSystemAction(),
                            errorCode.getUserAction());
                }
                if (entityTwo == null) {
                    OMRSErrorCode errorCode = OMRSErrorCode.ENTITY_NOT_KNOWN;
                    String errorMessage = errorCode.getErrorMessageId() + errorCode.getFormattedErrorMessage(
                            entityTwoGUID,
                            methodName,
                            repositoryName
                    );
                    throw new EntityNotKnownException(errorCode.getHTTPErrorCode(),
                            this.getClass().getName(),
                            methodName,
                            errorMessage,
                            errorCode.getSystemAction(),
                            errorCode.getUserAction());
                }

                RelationshipMapping relationshipMapping = relationshipMappingStore.getMappingByTypes(
                        relationshipTypeName,
                        entityOne.getType(),
                        entityTwo.getType()
                );

                if (relationshipMapping != null) {

                    relationship = RelationshipMapping.addIgcRelationship(
                            igcomrsRepositoryConnector,
                            relationshipMapping,
                            initialProperties,
                            entityOne,
                            entityTwo,
                            userId
                    );

                } else {
                    OMRSErrorCode errorCode = OMRSErrorCode.TYPEDEF_NAME_NOT_KNOWN;
                    String errorMessage = errorCode.getErrorMessageId() + errorCode.getFormattedErrorMessage(
                            relationshipTypeName,
                            methodName,
                            repositoryName
                    );
                    throw new TypeErrorException(
                            errorCode.getHTTPErrorCode(),
                            this.getClass().getName(),
                            methodName,
                            errorMessage,
                            errorCode.getSystemAction(),
                            errorCode.getUserAction()
                    );
                }

            } else {
                OMRSErrorCode errorCode = OMRSErrorCode.TYPEDEF_ID_NOT_KNOWN;
                String errorMessage = errorCode.getErrorMessageId() + errorCode.getFormattedErrorMessage(
                        relationshipTypeGUID,
                        guidParameterName,
                        methodName,
                        repositoryName
                );
                throw new TypeErrorException(
                        errorCode.getHTTPErrorCode(),
                        this.getClass().getName(),
                        methodName,
                        errorMessage,
                        errorCode.getSystemAction(),
                        errorCode.getUserAction()
                );
            }
        } catch (TypeDefNotKnownException e) {
            OMRSErrorCode errorCode = OMRSErrorCode.TYPEDEF_ID_NOT_KNOWN;
            String errorMessage = errorCode.getErrorMessageId() + errorCode.getFormattedErrorMessage(
                    relationshipTypeGUID,
                    guidParameterName,
                    methodName,
                    repositoryName
            );
            throw new TypeErrorException(
                    errorCode.getHTTPErrorCode(),
                    this.getClass().getName(),
                    methodName,
                    errorMessage,
                    errorCode.getSystemAction(),
                    errorCode.getUserAction()
            );
        }

        return relationship;

    }


    /**
     * Retrieve a mapping from IGC property name to the OMRS relationship type it represents.
     *
     * @param assetType the IGC asset type for which to find mappings
     * @param userId the userId making the request
     * @return {@code Map<String, RelationshipMapping>} - keyed by IGC asset type with values of the RelationshipMappings
     */
    public Map<String, List<RelationshipMapping>> getIgcPropertiesToRelationshipMappings(String assetType, String userId) {

        HashMap<String, List<RelationshipMapping>> map = new HashMap<>();

        List<EntityMapping> mappers = getMappers(assetType, userId);
        for (EntityMapping mapper : mappers) {
            List<RelationshipMapping> relationshipMappings = mapper.getRelationshipMappers();
            for (RelationshipMapping relationshipMapping : relationshipMappings) {
                if (relationshipMapping.getProxyOneMapping().matchesAssetType(assetType)) {
                    List<String> relationshipNamesOne = relationshipMapping.getProxyOneMapping().getIgcRelationshipProperties();
                    for (String relationshipName : relationshipNamesOne) {
                        if (!map.containsKey(relationshipName)) {
                            map.put(relationshipName, new ArrayList<>());
                        }
                        if (!map.get(relationshipName).contains(relationshipMapping)) {
                            map.get(relationshipName).add(relationshipMapping);
                        }
                    }
                }
                if (relationshipMapping.getProxyTwoMapping().matchesAssetType(assetType)) {
                    List<String> relationshipNamesTwo = relationshipMapping.getProxyTwoMapping().getIgcRelationshipProperties();
                    for (String relationshipName : relationshipNamesTwo) {
                        if (!map.containsKey(relationshipName)) {
                            map.put(relationshipName, new ArrayList<>());
                        }
                        if (!map.get(relationshipName).contains(relationshipMapping)) {
                            map.get(relationshipName).add(relationshipMapping);
                        }
                    }
                }
            }
        }

        return map;

    }

    /**
     * Run a search against IGC and process the results, based on the provided parameters.
     *
     * @param mapping the mapping to use for running the search
     * @param entityDetails the list of results to append into
     * @param userId unique identifier for requesting user
     * @param matchProperties Optional list of entity properties to match (where any String property's value should
     *                        be defined as a Java regular expression, even if it should be an exact match).
     * @param matchCriteria Enum defining how the properties should be matched to the entities in the repository.
     * @param fromEntityElement the starting element number of the entities to return.
     *                                This is used when retrieving elements
     *                                beyond the first page of results. Zero means start from the first element.
     * @param limitResultsByClassification List of classifications that must be present on all returned entities.
     * @param sequencingProperty String name of the entity property that is to be used to sequence the results.
     *                           Null means do not sequence on a property name (see SequencingOrder).
     * @param sequencingOrder Enum defining how the results should be ordered.
     * @param pageSize the maximum number of result entities that can be returned on this request.  Zero means
     *                 unrestricted return results size.
     * @throws FunctionNotSupportedException
     * @throws RepositoryErrorException
     */
    private void processResultsForMapping(EntityMapping mapping,
                                          List<EntityDetail> entityDetails,
                                          String userId,
                                          InstanceProperties matchProperties,
                                          MatchCriteria matchCriteria,
                                          int fromEntityElement,
                                          List<String> limitResultsByClassification,
                                          String sequencingProperty,
                                          SequencingOrder sequencingOrder,
                                          int pageSize)
            throws FunctionNotSupportedException, RepositoryErrorException {

        String igcAssetType = mapping.getIgcAssetType();
        IGCSearchConditionSet classificationLimiters = getSearchCriteriaForClassifications(
                igcAssetType,
                limitResultsByClassification
        );

        if (limitResultsByClassification != null && !limitResultsByClassification.isEmpty() && classificationLimiters == null) {
            if (log.isInfoEnabled()) { log.info("Classification limiters were specified, but none apply to the asset type {}, so excluding this asset type from search.", igcAssetType); }
        } else {

            IGCSearch igcSearch = new IGCSearch();
            igcSearch.addType(igcAssetType);

            /* Provided there is a mapping, build up a list of IGC-specific properties
             * and search criteria, based on the values of the InstanceProperties provided */
            ArrayList<String> properties = new ArrayList<>();
            IGCSearchConditionSet igcSearchConditionSet = new IGCSearchConditionSet();

            String qualifiedNameRegex = null;
            if (matchProperties != null) {
                Iterator iPropertyNames = matchProperties.getPropertyNames();
                Set<String> mappedProperties = mapping.getAllMappedIgcProperties();
                while (mappedProperties != null && !mappedProperties.isEmpty() && iPropertyNames.hasNext()) {
                    String omrsPropertyName = (String) iPropertyNames.next();
                    InstancePropertyValue value = matchProperties.getPropertyValue(omrsPropertyName);
                    if (omrsPropertyName.equals("qualifiedName")) {
                        qualifiedNameRegex = (String) ((PrimitivePropertyValue) value).getPrimitiveValue();
                    }
                    addSearchConditionFromValue(
                            igcSearchConditionSet,
                            omrsPropertyName,
                            properties,
                            mapping,
                            value
                    );
                }
            }

            if (classificationLimiters != null) {
                igcSearchConditionSet.addNestedConditionSet(classificationLimiters);
            }

            IGCSearchSorting igcSearchSorting = null;
            if (sequencingProperty == null && sequencingOrder != null) {
                igcSearchSorting = IGCOMRSMetadataCollection.sortFromNonPropertySequencingOrder(sequencingOrder);
            }

            if (matchCriteria != null) {
                switch (matchCriteria) {
                    case ALL:
                        igcSearchConditionSet.setMatchAnyCondition(false);
                        break;
                    case ANY:
                        igcSearchConditionSet.setMatchAnyCondition(true);
                        break;
                    case NONE:
                        igcSearchConditionSet.setMatchAnyCondition(false);
                        igcSearchConditionSet.setNegateAll(true);
                        break;
                }
            }

            igcSearch.addProperties(properties);
            igcSearch.addConditions(igcSearchConditionSet);

            setPagingForSearch(igcSearch, fromEntityElement, pageSize);

            if (igcSearchSorting != null) {
                igcSearch.addSortingCriteria(igcSearchSorting);
            }

            // If searching by qualifiedName, exact match (or starts with) we need to check results
            // to remove any (non-)generated type based on the qualifiedName (because the search results
            // will contain both from various iterations of this loop, and only one or the other should be
            // returned by the search)
            boolean includeResult = true;
            if (qualifiedNameRegex != null
                    && (repositoryHelper.isStartsWithRegex(qualifiedNameRegex) || repositoryHelper.isExactMatchRegex(qualifiedNameRegex))) {
                String unqualifiedName = repositoryHelper.getUnqualifiedLiteralString(qualifiedNameRegex);
                String prefix = mapping.getIgcRidPrefix();
                boolean generatedQN = isGeneratedGUID(unqualifiedName);
                includeResult = (generatedQN && prefix != null) || (!generatedQN && prefix == null);
            }

            if (includeResult) {
                processResults(
                        mapping,
                        this.igcRestClient.search(igcSearch),
                        entityDetails,
                        pageSize,
                        userId
                );
            }
        }
    }

    /**
     * Add the type to search based on the provided mapping.
     *
     * @param mapping the mapping on which to base the search
     * @param igcSearch the IGC search object to which to add the criteria
     * @return String - the IGC asset type that will be used for the search
     */
    private String addTypeToSearch(EntityMapping mapping, IGCSearch igcSearch) {
        String igcType = DEFAULT_IGC_TYPE;
        if (mapping == null) {
            // If no TypeDef was provided, run against all types
            igcSearch.addType(igcType);
        } else {
            igcType = mapping.getIgcAssetType();
            igcSearch.addType(igcType);
        }
        return igcType;
    }

    /**
     * Setup paging properties of the IGC search.
     *
     * @param igcSearch the IGC search object to which to add the criteria
     * @param beginAt the starting index for results
     * @param pageSize the number of results to include in each page
     */
    private void setPagingForSearch(IGCSearch igcSearch, int beginAt, int pageSize) {
        if (pageSize > 0) {
            /* Only set pageSize if it has been provided; otherwise we'll end up defaulting to IGC's
             * minimal pageSize of 10 (so will need to make many calls to get all pages) */
            igcSearch.setPageSize(pageSize);
        } else {
            /* So if none has been specified, we'll set a large pageSize to be able to more efficiently
             * retrieve all pages of results */
            igcSearch.setPageSize(igcomrsRepositoryConnector.getMaxPageSize());
        }
        igcSearch.setBeginAt(beginAt);
    }

    /**
     * Process the search results into the provided list of EntityDetail objects.
     *
     * @param mapper the EntityMapping that should be used to translate the results
     * @param results the IGC search results
     * @param entityDetails the list of EntityDetails to append
     * @param pageSize the number of results per page (0 for all results)
     * @param userId the user making the request
     */
    private void processResults(EntityMapping mapper,
                                ReferenceList results,
                                List<EntityDetail> entityDetails,
                                int pageSize,
                                String userId) throws RepositoryErrorException {

        if (pageSize == 0) {
            // If the provided pageSize was 0, we need to retrieve ALL pages of results...
            results.getAllPages(this.igcRestClient);
        }

        for (Reference reference : results.getItems()) {
            /* Only proceed with retrieving the EntityDetail if the type from IGC is not explicitly
             * a 'main_object' (as these are non-API-accessible asset types in IGC like column analysis master,
             * etc and will simply result in 400-code Bad Request messages from the API) */
            if (!reference.getType().equals(DEFAULT_IGC_TYPE)) {
                EntityDetail ed = null;

                if (log.isDebugEnabled()) { log.debug("processResults with mapper: {}", mapper.getClass().getCanonicalName()); }
                String idToLookup;
                if (mapper.igcRidNeedsPrefix()) {
                    if (log.isDebugEnabled()) { log.debug(" ... prefix required, getEntityDetail with: {}", mapper.getIgcRidPrefix() + reference.getId()); }
                    idToLookup = mapper.getIgcRidPrefix() + reference.getId();
                } else {
                    if (log.isDebugEnabled()) { log.debug(" ... no prefix required, getEntityDetail with: {}", reference.getId()); }
                    idToLookup = reference.getId();
                }
                try {
                    ed = getEntityDetail(userId, idToLookup, reference);
                } catch (EntityNotKnownException e) {
                    if (log.isErrorEnabled()) { log.error("Unable to find entity: {}", idToLookup); }
                }
                if (ed != null) {
                    entityDetails.add(ed);
                }
            }
        }

        // If we haven't filled a page of results (because we needed to skip some above), recurse...
        if (results.hasMorePages() && entityDetails.size() < pageSize) {
            results.getNextPage(this.igcRestClient);
            processResults(mapper, results, entityDetails, pageSize, userId);
        }

    }

    /**
     * Retrieve the IGC search conditions to limit results by the provided classification. Will return null if the
     * provided classification cannot be applied to the provided IGC asset type.
     *
     * @param igcAssetType name of the IGC asset type for which to limit the search results
     * @param classificationName name of the classification by which to limit results
     * @return IGCSearchConditionSet
     */
    private IGCSearchConditionSet getSearchCriteriaForClassification(String igcAssetType,
                                                                     String classificationName) {

        IGCSearchConditionSet igcSearchConditionSet = new IGCSearchConditionSet();

        ClassificationMapping classificationMapping = classificationMappingStore.getMappingByTypes(classificationName, igcAssetType);
        if (classificationMapping != null) {
            igcSearchConditionSet = classificationMapping.getIGCSearchCriteria(null);
        } else {
            if (log.isWarnEnabled()) { log.warn("Classification {} cannot be applied to IGC asset type {} - excluding from search limitations.", classificationName, igcAssetType); }
        }

        return (igcSearchConditionSet.size() > 0 ? igcSearchConditionSet : null);

    }

    /**
     * Retrieve the IGC search conditions to limit results by the provided list of classifications.
     *
     * @param igcAssetType name of the IGC asset type for which to limit the search results
     * @param classificationNames list of classification names by which to limit results
     * @return IGCSearchConditionSet
     */
    private IGCSearchConditionSet getSearchCriteriaForClassifications(String igcAssetType,
                                                                      List<String> classificationNames) {

        final String methodName = "getSearchCriteriaForClassifications";
        IGCSearchConditionSet igcSearchConditionSet = new IGCSearchConditionSet();

        if (classificationNames != null && !classificationNames.isEmpty()) {
            for (String classificationName : classificationNames) {
                IGCSearchConditionSet classificationLimiter = getSearchCriteriaForClassification(
                        igcAssetType,
                        classificationName
                );
                if (classificationLimiter != null) {
                    igcSearchConditionSet.addNestedConditionSet(classificationLimiter);
                    igcSearchConditionSet.setMatchAnyCondition(false);
                }
            }
        }

        return (igcSearchConditionSet.size() > 0 ? igcSearchConditionSet : null);

    }

    /**
     * Retrieve the listing of implemented mappings that should be used for an entity search, including navigating
     * subtypes when a supertype is the entity type provided.
     *
     * @param entityTypeGUID the GUID of the OMRS entity type for which to search
     * @param userId the userId through which to search
     * @return {@code List<EntityMapping>}
     */
    private List<EntityMapping> getMappingsToSearch(String entityTypeGUID, String userId) throws
            InvalidParameterException,
            RepositoryErrorException,
            UserNotAuthorizedException {

        List<EntityMapping> mappingsToSearch = new ArrayList<>();

        // If no entityType was provided, add all implemented types (except Referenceable, as that could itself
        // include many objects that are not implemented)
        if (entityTypeGUID == null) {
            for (EntityMapping candidate : entityMappingStore.getAllMappings()) {
                if (!candidate.getOmrsTypeDefName().equals("Referenceable")) {
                    mappingsToSearch.add(candidate);
                }
            }
        } else {

            EntityMapping mappingExact = entityMappingStore.getMappingByOmrsTypeGUID(entityTypeGUID);
            String requestedTypeName;
            // If no implemented mapping could be found, at least retrieve the TypeDef for further introspection
            // (so that if it has any implemented subtypes, we can still search for those)
            if (mappingExact == null) {
                TypeDef unimplemented = typeDefStore.getUnimplementedTypeDefByGUID(entityTypeGUID);
                requestedTypeName = unimplemented.getName();
            } else {
                requestedTypeName = mappingExact.getOmrsTypeDefName();
            }

            // Walk the hierarchy of types to ensure we search across all subtypes of the requested TypeDef as well
            List<TypeDef> allEntityTypes = findTypeDefsByCategory(userId, TypeDefCategory.ENTITY_DEF);

            for (TypeDef typeDef : allEntityTypes) {
                EntityMapping implementedMapping = entityMappingStore.getMappingByOmrsTypeGUID(typeDef.getGUID());
                if (implementedMapping != null) {
                    if (repositoryHelper.isTypeOf(metadataCollectionId, typeDef.getName(), requestedTypeName)) {
                        // Add any subtypes of the requested type into the search
                        mappingsToSearch.add(implementedMapping);
                    }
                }
            }

        }

        return mappingsToSearch;

    }

    /**
     * Return the header, classifications and properties of a specific entity, using the provided IGC asset.
     *
     * @param userId unique identifier for requesting user.
     * @param guid String unique identifier for the entity.
     * @param asset the IGC asset for which an EntityDetail should be constructed.
     * @return EntityDetail structure.
     * @throws RepositoryErrorException there is a problem communicating with the metadata repository where
     *                                  the metadata collection is stored.
     */
    public EntityDetail getEntityDetail(String userId, String guid, Reference asset)
            throws RepositoryErrorException, EntityNotKnownException {

        final String  methodName        = "getEntityDetail";

        if (log.isDebugEnabled()) { log.debug("getEntityDetail with guid = {}", guid); }

        EntityDetail detail = null;
        String prefix = getPrefixFromGeneratedId(guid);

        // If we could not find any asset by the provided guid, throw an ENTITY_NOT_KNOWN exception
        if (asset == null) {
            OMRSErrorCode errorCode = OMRSErrorCode.ENTITY_NOT_KNOWN;
            String errorMessage = errorCode.getErrorMessageId() + errorCode.getFormattedErrorMessage(guid,
                    methodName,
                    repositoryName);
            throw new EntityNotKnownException(errorCode.getHTTPErrorCode(),
                    this.getClass().getName(),
                    methodName,
                    errorMessage,
                    errorCode.getSystemAction(),
                    errorCode.getUserAction());
        } else if (asset.getType().equals(DEFAULT_IGC_TYPE)) {
            /* If the asset type returned has an IGC-listed type of 'main_object', it isn't one that the REST API
             * of IGC supports (eg. a data rule detail object, a column analysis master object, etc)...
             * Trying to further process it will result in failed REST API requests; so we should skip these objects */
            OMRSErrorCode errorCode = OMRSErrorCode.INVALID_ENTITY_FROM_STORE;
            String errorMessage = errorCode.getErrorMessageId() + errorCode.getFormattedErrorMessage(guid,
                    repositoryName,
                    methodName,
                    asset.toString());
            throw new RepositoryErrorException(errorCode.getHTTPErrorCode(),
                    this.getClass().getName(),
                    methodName,
                    errorMessage,
                    errorCode.getSystemAction(),
                    errorCode.getUserAction());
        } else {

            // Otherwise, retrieve the mapping dynamically based on the type of asset
            EntityMappingInstance entityMap = getMappingInstanceForParameters(asset, prefix, userId);

            if (entityMap != null) {
                // 2. Apply the mapping to the object, and retrieve the resulting EntityDetail
                detail = EntityMapping.getEntityDetail(entityMap);
            } else {
                OMRSErrorCode errorCode = OMRSErrorCode.TYPEDEF_NOT_KNOWN_FOR_INSTANCE;
                String errorMessage = errorCode.getErrorMessageId() + errorCode.getFormattedErrorMessage(
                        (prefix == null ? "" : prefix) + asset.getType(),
                        "IGC asset",
                        methodName,
                        repositoryName);
                throw new RepositoryErrorException(errorCode.getHTTPErrorCode(),
                        this.getClass().getName(),
                        methodName,
                        errorMessage,
                        errorCode.getSystemAction(),
                        errorCode.getUserAction());
            }

        }

        return detail;

    }

    /**
     * Retrieves an instance of a mapping that can be used for the provided parameters (or null if none exists).
     *
     * @param igcObject the IGC asset to be mapped
     * @param prefix the prefix used for the asset (if any; null otherwise)
     * @param userId the user making the request
     * @return EntityMappingInstance
     */
    public EntityMappingInstance getMappingInstanceForParameters(Reference igcObject, String prefix, String userId) {

        String igcAssetType = igcObject.getType();

        if (log.isDebugEnabled()) { log.debug("Looking for mapper for type {} with prefix {}", igcAssetType, prefix); }

        EntityMappingInstance entityMap = null;
        EntityMapping found = entityMappingStore.getMappingByIgcAssetTypeAndPrefix(igcAssetType, prefix);
        if (found != null) {
            if (log.isDebugEnabled()) { log.debug("Found mapper class: {} ({})", found.getClass().getCanonicalName(), found); }
            // Translate the provided asset to a base asset type for the mapper, if needed
            // (if not needed the 'getBaseIgcAssetFromAlternative' is effectively a NOOP and gives back same object)
            entityMap = new EntityMappingInstance(
                    found,
                    igcomrsRepositoryConnector,
                    found.getBaseIgcAssetFromAlternative(igcObject, igcomrsRepositoryConnector),
                    userId
            );
        } else {
            if (log.isDebugEnabled()) { log.debug("No mapper class found!"); }
        }

        return entityMap;

    }

    /**
     * Retrieves the classes to use for mapping the provided IGC asset type to an OMRS entity.
     *
     * @param igcAssetType the name of the IGC asset type
     * @return {@code List<EntityMapping>}
     */
    public List<EntityMapping> getMappers(String igcAssetType, String userId) {

        List<EntityMapping> mappers = entityMappingStore.getMappingsByIgcAssetType(igcAssetType);

        if (mappers == null) {
            mappers = new ArrayList<>();
        }
        if (mappers.isEmpty()) {
            EntityMapping defaultMapper = entityMappingStore.getDefaultEntityMapper();
            if (defaultMapper != null) {
                mappers.add(defaultMapper);
            }
        }

        return mappers;

    }

    /**
     * Retrieves a mapping from attribute name to TypeDefAttribute for all OMRS attributes defined for the provided
     * OMRS TypeDef name.
     *
     * @param omrsTypeName the name of the OMRS TypeDef for which to retrieve the attributes
     * @return {@code Map<String, TypeDefAttribute>}
     */
    public Map<String, TypeDefAttribute> getTypeDefAttributesForType(String omrsTypeName) {
        return typeDefStore.getAllTypeDefAttributesForName(omrsTypeName);
    }

    /**
     * Retrieves the IGC asset type from the provided IGC asset display name (only for those assets that have
     * a mapping implemented). If none is found, will return null.
     *
     * @param igcAssetName the display name of the IGC asset type
     * @return String
     */
    public String getIgcAssetTypeForAssetName(String igcAssetName) {
        EntityMapping mapping = entityMappingStore.getMappingByIgcAssetDisplayName(igcAssetName);
        if (mapping != null) {
            return mapping.getIgcAssetType();
        } else {
            return null;
        }
    }

    /**
     * Adds the provided value to the search criteria for IGC (when we only know the OMRS property).
     *
     * @param igcSearchConditionSet the search conditions to which to add the criteria
     * @param omrsPropertyName the OMRS property name to search
     * @param igcProperties the list of IGC properties to which to add for inclusion in the IGC search
     * @param mapping the mapping definition for the entity for which we're searching
     * @param value the value for which to search
     * @throws FunctionNotSupportedException when a regular expression is used for the search that is not supported
     */
    public void addSearchConditionFromValue(IGCSearchConditionSet igcSearchConditionSet,
                                            String omrsPropertyName,
                                            List<String> igcProperties,
                                            EntityMapping mapping,
                                            InstancePropertyValue value) throws FunctionNotSupportedException {

        if (omrsPropertyName != null) {
            if (omrsPropertyName.equals(EntityMapping.COMPLEX_MAPPING_SENTINEL)) {

                log.warn("Unhandled search condition: complex OMRS mapping, unknown IGC property.");

            } else {

                String igcPropertyName = mapping.getIgcPropertyName(omrsPropertyName);

                if (igcPropertyName.equals(EntityMapping.COMPLEX_MAPPING_SENTINEL)) {

                    mapping.addComplexPropertySearchCriteria(
                            repositoryHelper,
                            repositoryName,
                            igcRestClient,
                            igcSearchConditionSet,
                            igcPropertyName,
                            omrsPropertyName,
                            igcProperties,
                            value);

                } else {

                    igcProperties.add(igcPropertyName);
                    addIGCSearchConditionFromValue(
                            repositoryHelper,
                            repositoryName,
                            igcSearchConditionSet,
                            igcPropertyName,
                            igcProperties,
                            mapping,
                            value);

                }

            }
        }

    }

    /**
     * Adds the provided value to search criteria for IGC (once we know the IGC property).
     *
     * @param repositoryHelper helper for the OMRS repository
     * @param igcSearchConditionSet the search conditions to which to add the criteria
     * @param igcPropertyName the IGC property name to search
     * @param igcProperties the list of IGC properties to which to add for inclusion in the IGC search
     * @param mapping the mapping definition for the entity for which we're searching
     * @param value the value for which to search
     * @throws FunctionNotSupportedException when a regular expression is used for the search that is not supported
     */
    public static void addIGCSearchConditionFromValue(OMRSRepositoryHelper repositoryHelper,
                                                      String repositoryName,
                                                      IGCSearchConditionSet igcSearchConditionSet,
                                                      String igcPropertyName,
                                                      List<String> igcProperties,
                                                      EntityMapping mapping,
                                                      InstancePropertyValue value) throws FunctionNotSupportedException {

        final String methodName = "addIGCSearchConditionFromValue";

        InstancePropertyCategory category = value.getInstancePropertyCategory();
        switch (category) {
            case PRIMITIVE:
                PrimitivePropertyValue actualValue = (PrimitivePropertyValue) value;
                PrimitiveDefCategory primitiveType = actualValue.getPrimitiveDefCategory();
                switch (primitiveType) {
                    case OM_PRIMITIVE_TYPE_BOOLEAN:
                    case OM_PRIMITIVE_TYPE_BYTE:
                    case OM_PRIMITIVE_TYPE_CHAR:
                    case OM_PRIMITIVE_TYPE_SHORT:
                    case OM_PRIMITIVE_TYPE_INT:
                    case OM_PRIMITIVE_TYPE_LONG:
                    case OM_PRIMITIVE_TYPE_FLOAT:
                    case OM_PRIMITIVE_TYPE_DOUBLE:
                    case OM_PRIMITIVE_TYPE_BIGINTEGER:
                    case OM_PRIMITIVE_TYPE_BIGDECIMAL:
                        igcSearchConditionSet.addCondition(new IGCSearchCondition(
                                igcPropertyName,
                                "=",
                                actualValue.getPrimitiveValue().toString()
                        ));
                        break;
                    case OM_PRIMITIVE_TYPE_DATE:
                        Date date = (Date) actualValue.getPrimitiveValue();
                        igcSearchConditionSet.addCondition(new IGCSearchCondition(
                                igcPropertyName,
                                "=",
                                "" + date.getTime()
                        ));
                        break;
                    case OM_PRIMITIVE_TYPE_STRING:
                    default:
                        String candidateValue = actualValue.getPrimitiveValue().toString();
                        String unqualifiedValue = repositoryHelper.getUnqualifiedLiteralString(candidateValue);
                        if (repositoryHelper.isContainsRegex(candidateValue)) {
                            igcSearchConditionSet.addCondition(new IGCSearchCondition(
                                    igcPropertyName,
                                    "like %{0}%",
                                    unqualifiedValue
                            ));
                        } else if (repositoryHelper.isStartsWithRegex(candidateValue)) {
                            igcSearchConditionSet.addCondition(new IGCSearchCondition(
                                    igcPropertyName,
                                    "like {0}%",
                                    unqualifiedValue
                            ));
                        } else if (repositoryHelper.isEndsWithRegex(candidateValue)) {
                            igcSearchConditionSet.addCondition(new IGCSearchCondition(
                                    igcPropertyName,
                                    "like %{0}",
                                    unqualifiedValue
                            ));
                        } else if (repositoryHelper.isExactMatchRegex(candidateValue)) {
                            igcSearchConditionSet.addCondition(new IGCSearchCondition(
                                    igcPropertyName,
                                    "=",
                                    unqualifiedValue
                            ));
                        } else {
                            IGCOMRSErrorCode errorCode = IGCOMRSErrorCode.REGEX_NOT_IMPLEMENTED;
                            String errorMessage = errorCode.getErrorMessageId() + errorCode.getFormattedErrorMessage(
                                    repositoryName,
                                    candidateValue);
                            throw new FunctionNotSupportedException(errorCode.getHTTPErrorCode(),
                                    IGCOMRSMetadataCollection.class.getName(),
                                    methodName,
                                    errorMessage,
                                    errorCode.getSystemAction(),
                                    errorCode.getUserAction());
                        }
                        break;
                }
                break;
            case ENUM:
                igcSearchConditionSet.addCondition(new IGCSearchCondition(
                        igcPropertyName,
                        "=",
                        ((EnumPropertyValue) value).getSymbolicName()
                ));
                break;
            /*case STRUCT:
                Map<String, InstancePropertyValue> structValues = ((StructPropertyValue) value).getAttributes().getInstanceProperties();
                for (Map.Entry<String, InstancePropertyValue> nextEntry : structValues.entrySet()) {
                    addSearchConditionFromValue(
                            igcSearchConditionSet,
                            nextEntry.getKey(),
                            igcProperties,
                            mapping,
                            nextEntry.getValue()
                    );
                }
                break;*/
            case MAP:
                Map<String, InstancePropertyValue> mapValues = ((MapPropertyValue) value).getMapValues().getInstanceProperties();
                for (Map.Entry<String, InstancePropertyValue> nextEntry : mapValues.entrySet()) {
                    addIGCSearchConditionFromValue(
                            repositoryHelper,
                            repositoryName,
                            igcSearchConditionSet,
                            nextEntry.getKey(),
                            igcProperties,
                            mapping,
                            nextEntry.getValue()
                    );
                }
                break;
            case ARRAY:
                Map<String, InstancePropertyValue> arrayValues = ((ArrayPropertyValue) value).getArrayValues().getInstanceProperties();
                for (Map.Entry<String, InstancePropertyValue> nextEntry : arrayValues.entrySet()) {
                    addIGCSearchConditionFromValue(
                            repositoryHelper,
                            repositoryName,
                            igcSearchConditionSet,
                            igcPropertyName,
                            igcProperties,
                            mapping,
                            nextEntry.getValue()
                    );
                }
                break;
            default:
                // Do nothing
                if (log.isWarnEnabled()) { log.warn("Unable to handle search criteria for value type: {}", category); }
                break;
        }

    }

    /**
     * Retrieves the RID from a generated GUID (or the GUID if it is not generated).
     *
     * @param guid the guid to translate
     * @return String
     */
    public static final String getRidFromGeneratedId(String guid) {
        if (isGeneratedGUID(guid)) {
            return guid
                    .substring(guid.indexOf(GENERATED_TYPE_POSTFIX) + GENERATED_TYPE_POSTFIX.length());
        } else {
            return guid;
        }
    }

    /**
     * Retrieves the generated prefix from a generated GUID (or null if the GUID is not generated).
     *
     * @param guid the guid from which to retrieve the prefix
     * @return String
     */
    public static final String getPrefixFromGeneratedId(String guid) {
        if (isGeneratedGUID(guid)) {
            return guid
                    .substring(0, guid.indexOf(GENERATED_TYPE_POSTFIX) + GENERATED_TYPE_POSTFIX.length());
        } else {
            return null;
        }
    }

    /**
     * Indicates whether the provided GUID was generated (true) or not (false).
     *
     * @param guid the guid to test
     * @return boolean
     */
    public static final boolean isGeneratedGUID(String guid) {
        return guid.startsWith(GENERATED_TYPE_PREFIX);
    }

    /**
     * Generates a unique type prefix for RIDs based on the provided moniker.
     *
     * @param moniker a repeatable way by which to refer to the type
     * @return String
     */
    public static final String generateTypePrefix(String moniker) {
        return GENERATED_TYPE_PREFIX + moniker + GENERATED_TYPE_POSTFIX;
    }

    /**
     * Retrieve an OMRS asset stub (shadow copy of last version of an asset) for the provided asset details.
     * If there is no existing stub, will return null.
     *
     * @param rid the Repository ID (RID) of the asset for which to retrieve the OMRS stub
     * @param type the IGC asset type of the asset for which to retrieve the OMRS stub
     * @return OMRSStub
     */
    public OMRSStub getOMRSStubForAsset(String rid, String type) {

        // We need to translate the provided asset into a unique name for the stub
        String stubName = getStubNameForAsset(rid, type);
        IGCSearchCondition condition = new IGCSearchCondition(
                "name",
                "=",
                stubName
        );
        String[] properties = new String[]{ "$sourceRID", "$sourceType", "$payload" };
        IGCSearchConditionSet conditionSet = new IGCSearchConditionSet(condition);
        IGCSearch igcSearch = new IGCSearch("$OMRS-Stub", properties, conditionSet);
        ReferenceList results = igcRestClient.search(igcSearch);
        OMRSStub stub = null;
        if (results.getPaging().getNumTotal() > 0) {
            if (results.getPaging().getNumTotal() > 1) {
                if (log.isWarnEnabled()) { log.warn("Found multiple stubs for asset, taking only the first: {}", stubName); }
            }
            stub = (OMRSStub) results.getItems().get(0);
        } else {
            if (log.isInfoEnabled()) { log.info("No stub found for asset: {}", stubName); }
        }
        return stub;

    }

    /**
     * Retrieve an OMRS asset stub (shadow copy of last version of an asset) for the provided asset.
     * If there is no existing stub, will return null.
     *
     * @param asset the asset for which to retrieve the OMRS stub
     * @return OMRSStub
     */
    public OMRSStub getOMRSStubForAsset(Reference asset) {
        // We need to translate the provided asset into a unique name for the stub
        return getOMRSStubForAsset(asset.getId(), asset.getType());
    }

    /**
     * Update (or create if it does not already exist) the OMRS asset stub for the provided asset.
     * (Note that this method assumes you have already retrieved the full asset being provided.)
     *
     * @param asset the asset for which to upsert the OMRS stub
     * @return String the Repository ID (RID) of the OMRS stub
     */
    public String upsertOMRSStubForAsset(Reference asset) {

        String stubName = getStubNameFromAsset(asset);

        // Get the full asset details as a singular JSON payload
        String payload = igcRestClient.getValueAsJSON(asset);

        // Construct the asset XML document, including the full asset payload
        StringWriter stringWriter = new StringWriter();
        try {

            XMLStreamWriter xmlStreamWriter = xmlOutputFactory.createXMLStreamWriter(stringWriter);
            xmlStreamWriter.writeStartDocument("UTF-8", "1.0");

            xmlStreamWriter.writeStartElement("doc");
            xmlStreamWriter.writeNamespace("xmlns", "http://www.ibm.com/iis/flow-doc");

            xmlStreamWriter.writeStartElement("assets");
            xmlStreamWriter.writeStartElement("asset");

            xmlStreamWriter.writeAttribute("class", "$OMRS-Stub");
            xmlStreamWriter.writeAttribute("repr", stubName);
            xmlStreamWriter.writeAttribute("ID", "stub1");

            addAttributeToAssetXML(xmlStreamWriter, "name", stubName);
            addAttributeToAssetXML(xmlStreamWriter, "$sourceType", asset.getType());
            addAttributeToAssetXML(xmlStreamWriter, "$sourceRID", asset.getId());
            addAttributeToAssetXML(xmlStreamWriter, "$payload", payload);

            xmlStreamWriter.writeEndElement(); // </asset>
            xmlStreamWriter.writeEndElement(); // </assets>

            xmlStreamWriter.writeStartElement("importAction");
            xmlStreamWriter.writeAttribute("completeAssetIDs", "stub1");
            xmlStreamWriter.writeEndElement(); // </importAction>

            xmlStreamWriter.writeEndElement(); // </doc>

            xmlStreamWriter.writeEndDocument();
            xmlStreamWriter.flush();
            xmlStreamWriter.close();

        } catch (XMLStreamException e) {
            if (log.isErrorEnabled()) { log.error("Unable to write XML stream: {}", asset, e); }
        }

        String stubXML = stringWriter.getBuffer().toString();
        if (log.isDebugEnabled()) { log.debug("Constructed XML for stub: {}", stubXML); }

        // Upsert using the constructed asset XML
        String results = igcRestClient.upsertOpenIgcAsset(stubXML);

        return results.substring("stub1".length() + 5, results.length() - 2);

    }

    /**
     * Adds the provided attribute to the asset XML being constructed.
     *
     * @param xmlStreamWriter the asset XML being constructed
     * @param attrName the name of the attribute to add
     * @param attrValue the value of the attribute
     * @throws XMLStreamException
     */
    private void addAttributeToAssetXML(XMLStreamWriter xmlStreamWriter, String attrName, String attrValue) throws XMLStreamException {
        xmlStreamWriter.writeStartElement("attribute");
        xmlStreamWriter.writeAttribute("name", attrName);
        xmlStreamWriter.writeAttribute("value", attrValue);
        xmlStreamWriter.writeEndElement(); // </attribute>
    }

    /**
     * Delete the OMRS asset stub for the provided asset details (cannot require the asset itself since it has
     * already been removed).
     *
     * @param rid the Repository ID (RID) of the asset for which to delete the OMRS stub
     * @param assetType the IGC asset type of the asset for which to delete the OMRS stub
     * @return boolean - true on successful deletion, false otherwise
     */
    public boolean deleteOMRSStubForAsset(String rid, String assetType) {

        String stubName = getStubNameForAsset(rid, assetType);

        // Construct the asset XML document, including the full asset payload
        StringWriter stringWriter = new StringWriter();
        try {

            XMLStreamWriter xmlStreamWriter = xmlOutputFactory.createXMLStreamWriter(stringWriter);
            xmlStreamWriter.writeStartDocument("UTF-8", "1.0");

            xmlStreamWriter.writeStartElement("doc");
            xmlStreamWriter.writeNamespace("xmlns", "http://www.ibm.com/iis/flow-doc");

            xmlStreamWriter.writeStartElement("assets");
            xmlStreamWriter.writeStartElement("asset");

            xmlStreamWriter.writeAttribute("class", "$OMRS-Stub");
            xmlStreamWriter.writeAttribute("repr", stubName);
            xmlStreamWriter.writeAttribute("ID", "stub1");

            addAttributeToAssetXML(xmlStreamWriter, "name", stubName);

            xmlStreamWriter.writeEndElement(); // </asset>
            xmlStreamWriter.writeEndElement(); // </assets>

            xmlStreamWriter.writeStartElement("assetsToDelete");
            xmlStreamWriter.writeCharacters("stub1");
            xmlStreamWriter.writeEndElement(); // </assetsToDelete>

            xmlStreamWriter.writeEndElement(); // </doc>

            xmlStreamWriter.writeEndDocument();
            xmlStreamWriter.flush();
            xmlStreamWriter.close();

        } catch (XMLStreamException e) {
            log.error("Unable to write XML stream.", e);
        }

        String stubXML = stringWriter.getBuffer().toString();
        if (log.isDebugEnabled()) { log.debug("Constructed XML for stub deletion: {}", stubXML); }

        // Delete using the constructed asset XML
        return igcRestClient.deleteOpenIgcAsset(stubXML);

    }

    /**
     * Construct the unique name for the OMRS stub based on the provided asset.
     *
     * @param asset the asset for which to construct the unique OMRS stub name
     * @return String
     */
    public static String getStubNameFromAsset(Reference asset) {
        return getStubNameForAsset(asset.getId(), asset.getType());
    }

    /**
     * Construct the unique name for the OMRS stub based on the provided asset information.
     *
     * @param rid the Repository ID (RID) of the asset for which to construct the unique OMRS stub name
     * @param assetType the asset type (REST form) of the asset for which to construct the unique OMRS stub name
     * @return String
     */
    public static String getStubNameForAsset(String rid, String assetType) {
        return assetType + "_" + rid;
    }

    /**
     * Retrieve all of the asset details, including all relationships, from the RID.
     * <br><br>
     * Note that this is quite a heavy operation, relying on multiple REST calls, to build up what could be a very
     * large object; to simply retrieve the details without all relationships, see getAssetDetails.
     *
     * @param rid the Repository ID (RID) of the asset for which to retrieve all details
     * @return Reference - the object including all of its details and relationships
     */
    public Reference getFullAssetDetails(String rid) {

        // Start by retrieving the asset header, so we can introspect the class itself
        Reference assetRef = igcRestClient.getAssetRefById(rid);
        Reference fullAsset = null;

        if (assetRef != null) {

            // Introspect the full list of properties from the POJO of the asset
            String assetType = assetRef.getType();
            Class pojoClass = igcRestClient.getPOJOForType(assetType);
            if (pojoClass != null) {
                List<String> allProps = igcRestClient.getAllPropertiesFromPOJO(assetType);

                // Retrieve all asset properties, via search, as this will allow larger page
                // retrievals (and therefore be overall more efficient) than going by the GET of the asset
                fullAsset = assetRef.getAssetWithSubsetOfProperties(
                        igcRestClient,
                        allProps.toArray(new String[0]),
                        igcRestClient.getDefaultPageSize()
                );

                if (fullAsset != null) {

                    // Iterate through all the paged properties and retrieve all pages for each
                    List<String> allPaged = igcRestClient.getPagedRelationalPropertiesFromPOJO(assetType);
                    for (String pagedProperty : allPaged) {
                        ReferenceList pagedValue = (ReferenceList) igcRestClient.getPropertyByName(fullAsset, pagedProperty);
                        if (pagedValue != null) {
                            pagedValue.getAllPages(igcRestClient);
                        }
                    }

                    // Set the asset as fully retrieved, so we do not attempt to retrieve parts of it again
                    fullAsset.setFullyRetrieved();

                }
            } else {
                if (log.isDebugEnabled()) { log.debug("No registered POJO for asset type {} -- returning basic reference.", assetRef.getType()); }
                fullAsset = assetRef;
            }

        } else {
            if (log.isInfoEnabled()) { log.info("Unable to retrieve any asset with RID {} -- assume it was deleted.", rid); }
        }

        return fullAsset;

    }

    /**
     * Returns an IGCSearchSorting equivalent to the provided SequencingOrder, so long as the provided
     * sequencingOrder is not one of [ PROPERTY_ASCENDING, PROPERTY_DESCENDING ] (because these must
     * be explicitly mapped on a type-by-type basis).
     *
     * @param sequencingOrder the non-property SequencingOrder to create an IGC sort order from
     * @return IGCSearchSorting
     */
    public static IGCSearchSorting sortFromNonPropertySequencingOrder(SequencingOrder sequencingOrder) {
        IGCSearchSorting sort = null;
        if (sequencingOrder != null) {
            switch(sequencingOrder) {
                case GUID:
                    sort = new IGCSearchSorting("_id");
                    break;
                case CREATION_DATE_RECENT:
                    sort = new IGCSearchSorting("created_on", false);
                    break;
                case CREATION_DATE_OLDEST:
                    sort = new IGCSearchSorting("created_on", true);
                    break;
                case LAST_UPDATE_RECENT:
                    sort = new IGCSearchSorting("modified_on", false);
                    break;
                case LAST_UPDATE_OLDEST:
                    sort = new IGCSearchSorting("modified_on", true);
                    break;
                default:
                    sort = null;
                    break;
            }
        }
        return sort;
    }

}
