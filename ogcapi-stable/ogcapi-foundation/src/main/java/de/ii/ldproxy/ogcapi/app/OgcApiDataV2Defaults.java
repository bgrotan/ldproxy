/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.app;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import de.ii.ldproxy.ogcapi.domain.ApiBuildingBlock;
import de.ii.ldproxy.ogcapi.domain.ExtensionConfiguration;
import de.ii.ldproxy.ogcapi.domain.ExtensionRegistry;
import de.ii.ldproxy.ogcapi.domain.ImmutableCollectionExtent;
import de.ii.ldproxy.ogcapi.domain.ImmutableMetadata;
import de.ii.ldproxy.ogcapi.domain.ImmutableOgcApiDataV2;
import de.ii.ldproxy.ogcapi.domain.Metadata;
import de.ii.ldproxy.ogcapi.domain.OgcApiDataV2;
import de.ii.xtraplatform.services.domain.Service;
import de.ii.xtraplatform.store.domain.KeyPathAlias;
import de.ii.xtraplatform.store.domain.KeyPathAliasUnwrap;
import de.ii.xtraplatform.store.domain.entities.EntityDataBuilder;
import de.ii.xtraplatform.store.domain.entities.EntityDataDefaults;
import de.ii.xtraplatform.store.domain.entities.handler.Entity;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.StaticServiceProperty;

import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@Provides(properties = {
        @StaticServiceProperty(name = Entity.TYPE_KEY, type = "java.lang.String", value = Service.TYPE),
        @StaticServiceProperty(name = Entity.SUB_TYPE_KEY, type = "java.lang.String", value = OgcApiDataV2.SERVICE_TYPE)
})
@Instantiate
public class OgcApiDataV2Defaults implements EntityDataDefaults<OgcApiDataV2> {

    private final ExtensionRegistry extensionRegistry;

    public OgcApiDataV2Defaults(@Requires ExtensionRegistry extensionRegistry) {
        this.extensionRegistry = extensionRegistry;
    }

    @Override
    public int getSortPriority() {
        return 0;
    }

    @Override
    public final EntityDataBuilder<OgcApiDataV2> getBuilderWithDefaults() {
        return new ImmutableOgcApiDataV2.Builder().enabled(true)
                                                     .secured(true)
                                                     .metadata(getMetadata())
                                                  .defaultExtent(new ImmutableCollectionExtent.Builder().spatialComputed(true).temporalComputed(true).build())
                                                     .extensions(getBuildingBlocks());
    }

    @Override
    public Map<String, KeyPathAlias> getAliases() {
        return extensionRegistry.getExtensionsForType(ApiBuildingBlock.class)
                                .stream()
                                .map(ogcApiBuildingBlock -> ogcApiBuildingBlock.getDefaultConfiguration()
                                                                               .getBuildingBlock())
                                .map(buildingBlock -> new AbstractMap.SimpleImmutableEntry<String, KeyPathAlias>(buildingBlock.toLowerCase(), value -> ImmutableMap.of("api", ImmutableList.of(ImmutableMap.builder()
                                                                                                                                                                                                           .put("buildingBlock", buildingBlock)
                                                                                                                                                                                                           .putAll(value)
                                                                                                                                                                                                           .build()))))
                                .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    public Map<String, KeyPathAliasUnwrap> getReverseAliases() {
        return ImmutableMap.of("api", value -> ((List<Map<String, Object>>) value).stream()
                                                                                  .map(buildingBlock -> new AbstractMap.SimpleImmutableEntry<String, Object>(((String) buildingBlock.get("buildingBlock")).toLowerCase(), buildingBlock))
                                                                                  .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue)));
    }

    protected Metadata getMetadata() {
        return new ImmutableMetadata.Builder().build();
    }

    protected List<ExtensionConfiguration> getBuildingBlocks() {
        return extensionRegistry.getExtensionsForType(ApiBuildingBlock.class)
                                .stream()
                                .map(ApiBuildingBlock::getDefaultConfiguration)
                                .collect(Collectors.toList());
    }
}
