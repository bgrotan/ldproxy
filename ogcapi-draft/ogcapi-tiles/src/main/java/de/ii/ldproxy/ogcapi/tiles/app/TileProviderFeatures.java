/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.tiles.app;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import de.ii.ldproxy.ogcapi.domain.ExtensionConfiguration;
import de.ii.ldproxy.ogcapi.features.core.domain.PropertyTransformation;
import de.ii.ldproxy.ogcapi.tiles.domain.ImmutableTilesConfiguration;
import de.ii.ldproxy.ogcapi.tiles.domain.MinMax;
import de.ii.ldproxy.ogcapi.tiles.domain.PredefinedFilter;
import de.ii.ldproxy.ogcapi.tiles.domain.Rule;
import de.ii.ldproxy.ogcapi.tiles.domain.TileProvider;
import de.ii.ldproxy.ogcapi.tiles.domain.TilesConfiguration;
import org.immutables.value.Value;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Value.Immutable
@Value.Style(deepImmutablesDetection = true)
@JsonDeserialize(builder = ImmutableTileProviderFeatures.Builder.class)
public abstract class TileProviderFeatures extends TileProvider {

    public final String getType() { return "FEATURES"; }

    public abstract List<String> getTileEncodings();

    public abstract Map<String, MinMax> getZoomLevels();

    public abstract Map<String, MinMax> getZoomLevelsCache();

    public abstract Map<String, MinMax> getSeeding();

    public abstract Map<String, List<PredefinedFilter>> getFilters();

    public abstract Map<String, List<Rule>> getRules();

    @Nullable
    public abstract double[] getCenter();

    @Nullable
    public abstract Integer getLimit();

    @Nullable
    public abstract Integer getMaxPointPerTileDefault();

    @Nullable
    public abstract Integer getMaxLineStringPerTileDefault();

    @Nullable
    public abstract Integer getMaxPolygonPerTileDefault();

    @Nullable
    public abstract Boolean getSingleCollectionEnabled();

    @Nullable
    public abstract Boolean getMultiCollectionEnabled();

    @Nullable
    public abstract Boolean getIgnoreInvalidGeometries();

    @Nullable
    public abstract Double getMaxRelativeAreaChangeInPolygonRepair();

    @Nullable
    public abstract Double getMaxAbsoluteAreaChangeInPolygonRepair();

    @Nullable
    public abstract Double getMinimumSizeInPixel();

    @Override
    public TileProvider mergeInto(TileProvider source) {
        if (Objects.isNull(source) || !(source instanceof TileProviderFeatures))
            return this;

        ImmutableTileProviderFeatures.Builder builder = ImmutableTileProviderFeatures.builder()
                                                                                     .from((TileProviderFeatures) source)
                                                                                     .from(this);

        TileProviderFeatures src = (TileProviderFeatures) source;

        List<String> tileEncodings = Objects.nonNull(src.getTileEncodings()) ? Lists.newArrayList(src.getTileEncodings()) : Lists.newArrayList();
        getTileEncodings().forEach(tileEncoding -> {
            if (!tileEncodings.contains(tileEncoding)) {
                tileEncodings.add(tileEncoding);
            }
        });
        builder.tileEncodings(tileEncodings);

        Map<String, MinMax> mergedSeeding = Objects.nonNull(src.getSeeding()) ? Maps.newLinkedHashMap(src.getSeeding()) : Maps.newLinkedHashMap();
        if (Objects.nonNull(getSeeding()))
            getSeeding().forEach(mergedSeeding::put);
        builder.seeding(mergedSeeding);

        Map<String, MinMax> mergedZoomLevels = Objects.nonNull(src.getZoomLevels()) ? Maps.newLinkedHashMap(src.getZoomLevels()) : Maps.newLinkedHashMap();
        if (Objects.nonNull(getZoomLevels()))
            getZoomLevels().forEach(mergedZoomLevels::put);
        builder.zoomLevels(mergedZoomLevels);

        Map<String, MinMax> mergedZoomLevelsCache = Objects.nonNull(src.getZoomLevelsCache()) ? Maps.newLinkedHashMap(src.getZoomLevelsCache()) : Maps.newLinkedHashMap();
        if (Objects.nonNull(getZoomLevelsCache()))
            getZoomLevelsCache().forEach(mergedZoomLevelsCache::put);
        builder.zoomLevelsCache(mergedZoomLevelsCache);

        Map<String, List<Rule>> mergedRules = Objects.nonNull(src.getRules()) ? Maps.newLinkedHashMap(src.getRules()) : Maps.newLinkedHashMap();
        if (Objects.nonNull(getRules()))
            getRules().forEach(mergedRules::put);
        builder.rules(mergedRules);

        Map<String, List<PredefinedFilter>> mergedFilters = Objects.nonNull(src.getFilters()) ? Maps.newLinkedHashMap(src.getFilters()) : Maps.newLinkedHashMap();
        if (Objects.nonNull(getFilters()))
            getFilters().forEach(mergedFilters::put);
        builder.filters(mergedFilters);

        return builder.build();
    }
}