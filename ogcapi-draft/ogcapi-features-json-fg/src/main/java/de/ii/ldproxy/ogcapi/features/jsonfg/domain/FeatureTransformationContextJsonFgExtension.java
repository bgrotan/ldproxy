/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.features.jsonfg.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.ii.xtraplatform.crs.domain.CrsTransformer;
import de.ii.xtraplatform.crs.domain.EpsgCrs;
import org.immutables.value.Value;

import java.util.Optional;

@Value.Immutable
@Value.Style(deepImmutablesDetection = true)
@JsonDeserialize(builder = ImmutableFeatureTransformationContextJsonFgExtension.Builder.class)
public abstract class FeatureTransformationContextJsonFgExtension {

    public abstract Optional<CrsTransformer> getCrsTransformerWhere();

    @Value.Default
    public double getMaxAllowableOffsetWhere() {
        return 0;
    }

    @Value.Default
    public boolean shouldSwapCoordinatesWhere() {
        return false;
    }

    @Value.Default
    public int getGeometryPrecisionWhere() {
        return 0;
    }

    @Value.Default
    public boolean getSuppressWhere() { return false; }

    public abstract EpsgCrs getCrs();

}