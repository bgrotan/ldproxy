/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.styles.domain;


import com.google.common.collect.ImmutableList;
import de.ii.ldproxy.ogcapi.domain.ExtensionConfiguration;
import de.ii.ldproxy.ogcapi.domain.OgcApiDataV2;
import de.ii.ldproxy.ogcapi.domain.OgcApiPathParameter;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;


@Component
@Provides
@Instantiate
public class PathParameterStyleId implements OgcApiPathParameter {

    private static final Logger LOGGER = LoggerFactory.getLogger(PathParameterStyleId.class);

    public static final String STYLE_ID_PATTERN = "[^/]+";

    @Override
    public String getPattern() {
        return STYLE_ID_PATTERN;
    }

    @Override
    public List<String> getValues(OgcApiDataV2 apiData) {
        return ImmutableList.of();
    }

    @Override
    public Schema getSchema(OgcApiDataV2 apiData) {
        return new StringSchema().pattern(getPattern());
    }

    @Override
    public String getName() {
        return "styleId";
    }

    @Override
    public String getDescription() {
        return "The local identifier of a style, unique within the API.";
    }

    @Override
    public boolean isApplicable(OgcApiDataV2 apiData, String definitionPath) {
        return isEnabledForApi(apiData) &&
               (definitionPath.endsWith("/styles/{styleId}") ||
                definitionPath.endsWith("/styles/{styleId}/metadata"));
    }

    @Override
    public Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
        return StylesConfiguration.class;
    }
}
