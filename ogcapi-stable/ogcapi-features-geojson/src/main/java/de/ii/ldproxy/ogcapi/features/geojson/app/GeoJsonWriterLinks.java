/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.features.geojson.app;

import com.fasterxml.jackson.core.JsonGenerator;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import de.ii.ldproxy.ogcapi.domain.Link;
import de.ii.ldproxy.ogcapi.features.core.domain.FeatureLinksGenerator;
import de.ii.ldproxy.ogcapi.features.core.domain.FeaturesCollectionQueryables;
import de.ii.ldproxy.ogcapi.features.core.domain.FeaturesCoreConfiguration;
import de.ii.ldproxy.ogcapi.features.geojson.domain.EncodingAwareContextGeoJson;
import de.ii.ldproxy.ogcapi.features.geojson.domain.FeatureTransformationContextGeoJson;
import de.ii.ldproxy.ogcapi.features.geojson.domain.GeoJsonConfiguration;
import de.ii.ldproxy.ogcapi.features.geojson.domain.GeoJsonWriter;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * @author zahnen
 */
@Component
@Provides
@Instantiate
public class GeoJsonWriterLinks implements GeoJsonWriter {

    @Override
    public GeoJsonWriterLinks create() {
        return new GeoJsonWriterLinks();
    }

    private Set<String> featureRels;

    @Override
    public int getSortPriority() {
        return 25;
    }

    private void reset() {
        this.featureRels = ImmutableSet.of();
    }

    @Override
    public void onStart(EncodingAwareContextGeoJson context, Consumer<EncodingAwareContextGeoJson> next) throws IOException {
        reset();

        if (context.encoding().isFeatureCollection()) {
            OptionalLong numberReturned = context.metadata().getNumberReturned();
            boolean isLastPage = numberReturned.orElse(0) < context.query().getLimit();

            // initialize with the standard resource links for the collection,
            // but filter out "next" link, if we are on the last page
            context.encoding().getState().setCurrentFeatureCollectionLinks(context.encoding()
                                                                                  .getLinks()
                                                                                  .stream()
                                                                                  .filter(link -> !((isLastPage && Objects.equals(link.getRel(), "next"))))
                                                                                  .collect(Collectors.toUnmodifiableList()));

            // cache the link relation types to include for embedded features
            featureRels = context.encoding().getApiData()
                                               .getCollections()
                                               .get(context.encoding().getCollectionId())
                                               .getExtension(FeaturesCoreConfiguration.class)
                                               .map(cfg -> {
                                                   boolean addSelf = cfg.getShowsFeatureSelfLink();
                                                   if (!addSelf)
                                                       return cfg.getEmbeddedFeatureLinkRels();

                                                   return ImmutableSet.<String>builder()
                                                                      .addAll(cfg.getEmbeddedFeatureLinkRels())
                                                                      .add("self")
                                                                      .build();
                                               })
                                               .orElse(ImmutableSet.of());
        }

        next.accept(context);
    }

    @Override
    public void onEnd(EncodingAwareContextGeoJson context, Consumer<EncodingAwareContextGeoJson> next) throws IOException {
        if (context.encoding().isFeatureCollection()) {
            this.writeLinksIfAny(context.encoding().getJson(), context.encoding().getState().getCurrentFeatureCollectionLinks());
        }

        // next chain for extensions
        next.accept(context);
    }

    @Override
    public void onFeatureStart(EncodingAwareContextGeoJson context, Consumer<EncodingAwareContextGeoJson> next) throws IOException {
        if (context.encoding().isFeatureCollection()) {

            // initialize empty for embedded features
            context.encoding().getState().setCurrentFeatureLinks(ImmutableList.of());
        } else {

            // initialize with the standard resource links for the feature
            context.encoding().getState().setCurrentFeatureLinks(context.encoding().getLinks());
        }

        // next chain for extensions
        next.accept(context);
    }

    @Override
    public void onFeatureEnd(EncodingAwareContextGeoJson context, Consumer<EncodingAwareContextGeoJson> next) throws IOException {
        if (context.encoding().isFeatureCollection()) {
            // write links of the embedded feature
            this.writeLinksIfAny(context.encoding().getJson(), context.encoding().getState()
                                                                                       .getCurrentFeatureLinks()
                                                                                       .stream()
                                                                                       .filter(link -> featureRels.contains(link.getRel()))
                                                                                       .collect(Collectors.toUnmodifiableList()));
        } else {
            this.writeLinksIfAny(context.encoding().getJson(), context.encoding().getState()
                                                                                       .getCurrentFeatureLinks());
        }

        // next chain for extensions
        next.accept(context);
    }

    private void writeLinksIfAny(JsonGenerator json, List<Link> links) throws IOException {
        if (!links.isEmpty()) {
            json.writeArrayFieldStart("links");

            for (Link link : links) {
                json.writeStartObject();
                json.writeStringField("href", link.getHref());
                if (Objects.nonNull(link.getRel()))
                    json.writeStringField("rel", link.getRel());
                if (Objects.nonNull(link.getType()))
                    json.writeStringField("type", link.getType());
                if (Objects.nonNull(link.getTitle()))
                    json.writeStringField("title", link.getTitle());
                json.writeEndObject();
            }

            json.writeEndArray();
        }
    }
}