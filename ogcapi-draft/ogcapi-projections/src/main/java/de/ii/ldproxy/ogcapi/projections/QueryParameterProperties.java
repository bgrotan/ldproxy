/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.projections;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import de.ii.ldproxy.ogcapi.domain.ApiExtensionCache;
import de.ii.ldproxy.ogcapi.domain.ExtensionConfiguration;
import de.ii.ldproxy.ogcapi.domain.FeatureTypeConfigurationOgcApi;
import de.ii.ldproxy.ogcapi.domain.HttpMethods;
import de.ii.ldproxy.ogcapi.domain.OgcApiDataV2;
import de.ii.ldproxy.ogcapi.domain.OgcApiQueryParameter;
import de.ii.ldproxy.ogcapi.features.core.domain.SchemaInfo;
import de.ii.xtraplatform.features.domain.ImmutableFeatureQuery;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
@Provides
@Instantiate
public class QueryParameterProperties extends ApiExtensionCache implements OgcApiQueryParameter {

    private final SchemaInfo schemaInfo;

    public QueryParameterProperties(@Requires SchemaInfo schemaInfo) {
        this.schemaInfo = schemaInfo;
    }

    @Override
    public String getId(String collectionId) {
        return "properties_"+collectionId;
    }

    @Override
    public String getName() {
        return "properties";
    }

    @Override
    public String getDescription() {
        return "The properties that should be included for each feature. The parameter value is a comma-separated list of property names. By default, all feature properties with a value are returned.";
    }

    @Override
    public boolean isApplicable(OgcApiDataV2 apiData, String definitionPath, HttpMethods method) {
        return computeIfAbsent(this.getClass().getCanonicalName() + apiData.hashCode() + definitionPath + method.name(), () ->
            isEnabledForApi(apiData) &&
                method== HttpMethods.GET &&
                (definitionPath.equals("/collections/{collectionId}/items") ||
                 definitionPath.equals("/collections/{collectionId}/items/{featureId}") ||
                 definitionPath.equals("/tiles/{tileMatrixSetId}/{tileMatrix}/{tileRow}/{tileCol}") ||
                 definitionPath.equals("/collections/{collectionId}/tiles/{tileMatrixSetId}/{tileMatrix}/{tileRow}/{tileCol}")));
    }

    private ConcurrentMap<Integer, ConcurrentMap<String,Schema>> schemaMap = new ConcurrentHashMap<>();

    @Override
    public Schema getSchema(OgcApiDataV2 apiData) {
        int apiHashCode = apiData.hashCode();
        if (!schemaMap.containsKey(apiHashCode))
            schemaMap.put(apiHashCode, new ConcurrentHashMap<>());
        if (!schemaMap.get(apiHashCode).containsKey("*")) {
            schemaMap.get(apiHashCode)
                     .put("*", new ArraySchema().items(new StringSchema()));
        }
        return schemaMap.get(apiHashCode).get("*");
    }

    @Override
    public Schema getSchema(OgcApiDataV2 apiData, String collectionId) {
        int apiHashCode = apiData.hashCode();
        if (!schemaMap.containsKey(apiHashCode))
            schemaMap.put(apiHashCode, new ConcurrentHashMap<>());
        if (!schemaMap.get(apiHashCode).containsKey(collectionId)) {
            schemaMap.get(apiHashCode)
                     .put(collectionId, new ArraySchema().items(new StringSchema()._enum(schemaInfo.getPropertyNames(apiData, collectionId))));
        }
        return schemaMap.get(apiHashCode).get(collectionId);
    }

    @Override
    public Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
        return ProjectionsConfiguration.class;
    }

    @Override
    public ImmutableFeatureQuery.Builder transformQuery(FeatureTypeConfigurationOgcApi featureTypeConfiguration,
                                                        ImmutableFeatureQuery.Builder queryBuilder,
                                                        Map<String, String> parameters, OgcApiDataV2 datasetData) {

        if (!isExtensionEnabled(datasetData.getCollections()
                                           .get(featureTypeConfiguration.getId()), ProjectionsConfiguration.class)) {
            return queryBuilder;
        }
        List<String> propertiesList = getPropertiesList(parameters);

        return queryBuilder.fields(propertiesList);
    }

    private List<String> getPropertiesList(Map<String, String> parameters) {
        if (parameters.containsKey("properties")) {
            return Splitter.on(',')
                    .omitEmptyStrings()
                    .trimResults()
                    .splitToList(parameters.get("properties"));
        } else {
            return ImmutableList.of("*");
        }
    }
}
