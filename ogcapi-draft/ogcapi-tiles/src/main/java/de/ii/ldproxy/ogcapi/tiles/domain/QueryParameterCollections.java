/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.tiles.domain;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import de.ii.ldproxy.ogcapi.domain.*;
import de.ii.xtraplatform.feature.transformer.api.FeatureTypeConfiguration;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Component
@Provides
@Instantiate
public class QueryParameterCollections extends ApiExtensionCache implements OgcApiQueryParameter {

    @Override
    public String getName() {
        return "collections";
    }

    @Override
    public String getDescription() {
        return "The collections that should be included. The parameter value is a comma-separated list of collection names.";
    }

    @Override
    public boolean isApplicable(OgcApiDataV2 apiData, String definitionPath, HttpMethods method) {
        return computeIfAbsent(this.getClass().getCanonicalName() + apiData.hashCode() + definitionPath + method.name(), () ->
            isEnabledForApi(apiData) &&
               method== HttpMethods.GET &&
               definitionPath.equals("/tiles/{tileMatrixSetId}/{tileMatrix}/{tileRow}/{tileCol}"));
    }

    private final Map<Integer,Schema> schemaMap = new ConcurrentHashMap<>();
    private final Map<Integer,List<String>> collectionsMap = new ConcurrentHashMap<>();

    private List<String> getCollectionIds(OgcApiDataV2 apiData) {
        int apiHashCode = apiData.hashCode();
        if (!collectionsMap.containsKey(apiHashCode)) {
            collectionsMap.put(apiHashCode, apiData.getCollections()
                    .values()
                    .stream()
                    .filter(collection -> apiData.isCollectionEnabled(collection.getId()))
                    .filter(collection -> collection.getExtension(TilesConfiguration.class).filter(ExtensionConfiguration::isEnabled).isPresent())
                    .map(FeatureTypeConfiguration::getId)
                    .collect(Collectors.toList()));
        }
        return collectionsMap.get(apiHashCode);
    }

    @Override
    public Schema getSchema(OgcApiDataV2 apiData) {
        int apiHashCode = apiData.hashCode();
        if (!schemaMap.containsKey(apiHashCode)) {
            schemaMap.put(apiHashCode, new ArraySchema().items(new StringSchema()._enum(getCollectionIds(apiData))));
        }
        return schemaMap.get(apiHashCode);
    }

    @Override
    public boolean isEnabledForApi(OgcApiDataV2 apiData) {
        Optional<TilesConfiguration> config = apiData.getExtension(TilesConfiguration.class);
        return config.isPresent() && config.get().getMultiCollectionEnabledDerived() && config.get().getTileProvider().requiresQuerySupport();
    }

    @Override
    public Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
        return TilesConfiguration.class;
    }

    @Override
    public Map<String, Object> transformContext(FeatureTypeConfigurationOgcApi featureType,
                                                Map<String, Object> context,
                                                Map<String, String> parameters,
                                                OgcApiDataV2 apiData) {
        if (!isEnabledForApi(apiData))
            return context;

        List<String> availableCollections = getCollectionIds(apiData);
        List<String> requestedCollections = getCollectionsList(parameters);
        context.put("collections", requestedCollections.isEmpty() ? availableCollections :
                                                                    requestedCollections.stream()
                                                                                        .filter(availableCollections::contains)
                                                                                        .collect(Collectors.toList()));

        return context;
    }

    private List<String> getCollectionsList(Map<String, String> parameters) {
        if (parameters.containsKey("collections")) {
            return Splitter.on(',')
                    .omitEmptyStrings()
                    .trimResults()
                    .splitToList(parameters.get("collections"));
        } else {
            return ImmutableList.of();
        }
    }
}