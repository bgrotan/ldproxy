/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.tiles.app.mbtiles;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.ii.ldproxy.ogcapi.tiles.domain.VectorLayer;
import org.immutables.value.Value;

import java.util.List;
import java.util.Optional;

/**
 * Mbtiles metadata
 */
@Value.Immutable
@Value.Style(deepImmutablesDetection = true)
@JsonDeserialize(builder = ImmutableMbtilesMetadata.Builder.class)
public abstract class MbtilesMetadata {

    public enum MbtilesFormat { pbf, jpg, png, webp;
        public static MbtilesFormat of(String value) {
            switch (value) {
                case "pbf": return pbf;
                case "jpg": return jpg;
                case "png": return png;
                case "webp": return webp;
            }
            return null;
        }
    }

    public enum MbtilesType { overlay, baselayer;
        public static MbtilesType of(String value) {
            switch (value) {
                case "overlay": return overlay;
                case "baselayer": return baselayer;
            }
            return null;
        }
    }

    public abstract String getName();
    public abstract MbtilesFormat getFormat();

    public abstract List<Double> getBounds();
    public abstract List<Number> getCenter();
    public abstract Optional<Integer> getMinzoom();
    public abstract Optional<Integer> getMaxzoom();

    public abstract Optional<String> getDescription();
    public abstract Optional<String> getAttribution();
    public abstract Optional<MbtilesType> getType();
    public abstract Optional<Number> getVersion();

    public abstract List<VectorLayer> getVectorLayers();

    // TODO add tilestats
}