/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.features.jsonfg.app;

import de.ii.ldproxy.ogcapi.collections.schema.domain.SchemaConfiguration;
import de.ii.ldproxy.ogcapi.domain.I18n;
import de.ii.ldproxy.ogcapi.domain.ImmutableLink;
import de.ii.ldproxy.ogcapi.features.geojson.domain.EncodingAwareContextGeoJson;
import de.ii.ldproxy.ogcapi.features.geojson.domain.FeatureTransformationContextGeoJson;
import de.ii.ldproxy.ogcapi.features.geojson.domain.GeoJsonWriter;
import de.ii.ldproxy.ogcapi.features.jsonfg.domain.JsonFgConfiguration;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;

import java.io.IOException;
import java.util.Objects;
import java.util.function.Consumer;

@Component
@Provides
@Instantiate
public class JsonFgWriterDescribedby implements GeoJsonWriter {

    private final I18n i18n;
    boolean isEnabled;

    public JsonFgWriterDescribedby(@Requires I18n i18n) {
        this.i18n = i18n;
    }

    @Override
    public JsonFgWriterDescribedby create() {
        return new JsonFgWriterDescribedby(i18n);
    }

    @Override
    public int getSortPriority() {
        // must be after the Links writer
        return 110;
    }

    @Override
    public void onStart(EncodingAwareContextGeoJson context, Consumer<EncodingAwareContextGeoJson> next) throws IOException {
        isEnabled = isEnabled(context.encoding());

        if (isEnabled && context.encoding().isFeatureCollection()) {
            String label = context.encoding().getApiData()
                                                .getCollections()
                                                .get(context.encoding().getCollectionId())
                                                .getLabel();
            context.encoding().getState()
                                 .addCurrentFeatureCollectionLinks(new ImmutableLink.Builder().rel("describedby")
                                                                                              .href(context.encoding().getServiceUrl() + "/collections/" + context.encoding().getCollectionId() + "/schemas/collection")
                                                                                              .type("application/schema+json")
                                                                                              .title(i18n.get("schemaLinkCollection", context.encoding().getLanguage()).replace("{{collection}}", label))
                                                                                              .build(),
                                                                   new ImmutableLink.Builder().rel("describedby")
                                                                                              .href("https://geojson.org/schema/FeatureCollection.json")
                                                                                              .type("application/schema+json")
                                                                                              .title("This document is a GeoJSON FeatureCollection") // TODO add i18n
                                                                                              .build());
        }

        // next chain for extensions
        next.accept(context);
    }

    @Override
    public void onFeatureStart(EncodingAwareContextGeoJson context, Consumer<EncodingAwareContextGeoJson> next) throws IOException {
        if (isEnabled && !context.encoding().isFeatureCollection()) {
            String label = context.encoding().getApiData()
                                                .getCollections()
                                                .get(context.encoding().getCollectionId())
                                                .getLabel();
            context.encoding().getState().addCurrentFeatureLinks(new ImmutableLink.Builder().rel("describedby")
                                                                                               .href(context.encoding().getServiceUrl() + "/collections/" + context.encoding().getCollectionId() + "/schemas/feature")
                                                                                               .type("application/schema+json")
                                                                                               .title(i18n.get("schemaLinkFeature", context.encoding().getLanguage()).replace("{{collection}}", label))
                                                                                               .build(),
                                                                    new ImmutableLink.Builder().rel("describedby")
                                                                                               .href("https://geojson.org/schema/Feature.json")
                                                                                               .type("application/schema+json")
                                                                                               .title("This document is a GeoJSON Feature") // TODO add i18n
                                                                                               .build());
        }

        // next chain for extensions
        next.accept(context);
    }

    private boolean isEnabled(FeatureTransformationContextGeoJson transformationContext) {
        return transformationContext.getApiData()
                                    .getCollections()
                                    .get(transformationContext.getCollectionId())
                                    .getExtension(JsonFgConfiguration.class)
                                    .filter(JsonFgConfiguration::isEnabled)
                                    .filter(cfg -> Objects.requireNonNullElse(cfg.getDescribedby(), false))
                                    .filter(cfg -> cfg.getIncludeInGeoJson().contains(JsonFgConfiguration.OPTION.describedby) ||
                                            transformationContext.getMediaType().equals(FeaturesFormatJsonFg.MEDIA_TYPE))
                                    .isPresent()
                && transformationContext.getApiData()
                                        .getCollections()
                                        .get(transformationContext.getCollectionId())
                                        .getExtension(SchemaConfiguration.class)
                                        .filter(SchemaConfiguration::isEnabled)
                                        .isPresent();

    }
}
