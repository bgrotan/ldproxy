/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.tiles.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.hash.Funnel;
import de.ii.ldproxy.ogcapi.domain.Metadata2;
import de.ii.ldproxy.ogcapi.features.geojson.domain.JsonSchema;
import de.ii.ldproxy.ogcapi.features.geojson.domain.JsonSchemaObject;
import de.ii.ldproxy.ogcapi.tiles.domain.tileMatrixSet.TileMatrixSetData;
import de.ii.ldproxy.ogcapi.tiles.domain.tileMatrixSet.TileMatrixSetLimits;
import de.ii.ldproxy.ogcapi.tiles.domain.tileMatrixSet.TilesBoundingBox;
import org.immutables.value.Value;

import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.Optional;

@Value.Immutable
@Value.Style(deepImmutablesDetection = true)
@JsonDeserialize(as = ImmutableTileLayer.class)
public abstract class TileLayer extends Metadata2 {

    public enum GeometryType { points, lines, polygons }

    public abstract String getId();

    public abstract TileSet.DataType getDataType();
    public abstract Optional<String> getFeatureType();
    public abstract Optional<GeometryType> getGeometryType();

    public abstract Optional<String> getTheme();

    public abstract Optional<String> getMinTileMatrix();
    public abstract Optional<String> getMaxTileMatrix();
    public abstract Optional<Double> getMinCellSize();
    public abstract Optional<Double> getMaxCellSize();
    public abstract Optional<Double> getMinScaleDenominator();
    public abstract Optional<Double> getMaxScaleDenominator();

    public abstract Optional<TilesBoundingBox> getBoundingBox();

    public abstract Optional<JsonSchemaObject> getPropertiesSchema();

    // this is for map tiles, so we do not support the following for now:
    // public abstract Optional<StyleEntry> getStyle();

    @SuppressWarnings("UnstableApiUsage")
    public static final Funnel<TileLayer> FUNNEL = (from, into) -> {
        Metadata2.FUNNEL.funnel(from, into);
        into.putString(from.getId(), StandardCharsets.UTF_8);
        into.putString(from.getDataType().toString(), StandardCharsets.UTF_8);
        from.getFeatureType().ifPresent(val -> into.putString(val, StandardCharsets.UTF_8));
        from.getGeometryType().ifPresent(val -> into.putString(val.toString(), StandardCharsets.UTF_8));
        from.getTheme().ifPresent(val -> into.putString(val, StandardCharsets.UTF_8));
        from.getMinTileMatrix().ifPresent(val -> into.putString(val, StandardCharsets.UTF_8));
        from.getMaxTileMatrix().ifPresent(val -> into.putString(val, StandardCharsets.UTF_8));
        from.getMinCellSize().ifPresent(into::putDouble);
        from.getMaxCellSize().ifPresent(into::putDouble);
        from.getMinScaleDenominator().ifPresent(into::putDouble);
        from.getMaxScaleDenominator().ifPresent(into::putDouble);
        from.getBoundingBox().ifPresent(val -> TilesBoundingBox.FUNNEL.funnel(val, into));
        from.getPropertiesSchema().ifPresent(val -> JsonSchema.FUNNEL.funnel(val, into));
    };
}
