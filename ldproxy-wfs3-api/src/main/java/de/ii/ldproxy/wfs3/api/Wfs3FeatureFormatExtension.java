/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.wfs3.api;

import de.ii.ldproxy.ogcapi.domain.FormatExtension;
import de.ii.xtraplatform.feature.transformer.api.FeatureTransformer;
import de.ii.xtraplatform.feature.transformer.api.GmlConsumer;
import de.ii.xtraplatform.feature.transformer.api.TargetMappingProviderFromGml;

import java.util.Optional;

public interface Wfs3FeatureFormatExtension extends FormatExtension {

    default String getPathPattern() {
        return "^\\/?collections\\/[^\\/]+\\/items(?:\\/[^\\/]+)?$";
    }

    default boolean canPassThroughFeatures() {
        return false;
    }

    default boolean canTransformFeatures() {
        return false;
    }

    default Optional<GmlConsumer> getFeatureConsumer(FeatureTransformationContext transformationContext) {
        return Optional.empty();
    }

    default Optional<FeatureTransformer> getFeatureTransformer(FeatureTransformationContext transformationContext) {
        return Optional.empty();
    }

    default Optional<TargetMappingProviderFromGml> getMappingGenerator() {
        return Optional.empty();
    }

    default Optional<TargetMappingRefiner> getMappingRefiner() {
        return Optional.empty();
    }
}