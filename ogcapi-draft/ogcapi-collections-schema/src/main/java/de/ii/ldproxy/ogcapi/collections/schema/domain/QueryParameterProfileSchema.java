/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.collections.schema.domain;

import com.google.common.collect.ImmutableList;
import de.ii.ldproxy.ogcapi.common.domain.QueryParameterProfile;
import de.ii.ldproxy.ogcapi.domain.ExtensionConfiguration;
import de.ii.ldproxy.ogcapi.domain.ExtensionRegistry;
import de.ii.ldproxy.ogcapi.domain.OgcApiDataV2;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;

import java.util.List;

@Component
@Provides
@Instantiate
public class QueryParameterProfileSchema extends QueryParameterProfile {

    final static List<String> PROFILES = ImmutableList.of("2019-09", "07");

    public QueryParameterProfileSchema(@Requires ExtensionRegistry extensionRegistry) {
        super(extensionRegistry);
    }

    @Override
    public String getId() {
        return "profileSchema";
    }

    @Override
    protected boolean isApplicable(OgcApiDataV2 apiData, String definitionPath) {
        return definitionPath.equals("/collections/{collectionId}/schemas/{type}");
    }

    @Override
    protected List<String> getProfiles(OgcApiDataV2 apiData) {
        return PROFILES;
    }

    @Override
    protected String getDefault(OgcApiDataV2 apiData) {
        return "2019-09";
    }

    @Override
    public Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
        return SchemaConfiguration.class;
    }

}
