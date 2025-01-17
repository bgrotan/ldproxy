/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.filter.api;

import com.google.common.collect.ImmutableList;
import de.ii.ldproxy.ogcapi.domain.ApiExtensionCache;
import de.ii.ldproxy.ogcapi.domain.ExtensionConfiguration;
import de.ii.ldproxy.ogcapi.domain.OgcApiDataV2;
import de.ii.ldproxy.ogcapi.domain.HttpMethods;
import de.ii.ldproxy.ogcapi.domain.OgcApiQueryParameter;
import de.ii.ldproxy.ogcapi.filter.domain.FilterConfiguration;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;

@Component
@Provides
@Instantiate
public class QueryParameterFilterLang extends ApiExtensionCache implements OgcApiQueryParameter {

    private static final String FILTER_LANG_CQL = "cql-text";
    private static final String FILTER_LANG_JSON = "cql-json";

    @Override
    public String getName() {
        return "filter-lang";
    }

    @Override
    public String getDescription() {
        return "Language of the query expression in the 'filter' parameter. Supported are 'cql-text' (default) and 'cql-json', " +
            "specified in the OGC candidate standard 'Common Query Language'. 'cql-text' is an SQL-like text encoding for " +
            "filter expressions that also supports spatial, temporal and array predicates. 'cql-json' is a JSON encoding of " +
            "that grammar, suitable for use as part of a JSON object that represents a query. The use of 'cql-text' is recommended " +
            "for filter expressions in the 'filter' parameter.";
    }

    @Override
    public boolean isApplicable(OgcApiDataV2 apiData, String definitionPath, HttpMethods method) {
        return computeIfAbsent(this.getClass().getCanonicalName() + apiData.hashCode() + definitionPath + method.name(), () ->
            isEnabledForApi(apiData) &&
                method== HttpMethods.GET &&
                (definitionPath.equals("/collections/{collectionId}/items") ||
                 definitionPath.equals("/collections/{collectionId}/tiles/{tileMatrixSetId}/{tileMatrix}/{tileRow}/{tileCol}") ||
                 definitionPath.equals("/tiles/{tileMatrixSetId}/{tileMatrix}/{tileRow}/{tileCol}")));
    }

    private final Schema schema = new StringSchema()._enum(ImmutableList.of(FILTER_LANG_CQL, FILTER_LANG_JSON))._default(FILTER_LANG_CQL);

    @Override
    public Schema getSchema(OgcApiDataV2 apiData) {
        return schema;
    }

    @Override
    public Schema getSchema(OgcApiDataV2 apiData, String collectionId) {
        return schema;
    }

    @Override
    public Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
        return FilterConfiguration.class;
    }
}
