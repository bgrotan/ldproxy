/*
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.features.jsonfg.app

import com.google.common.collect.ImmutableList
import de.ii.ldproxy.ogcapi.domain.ApiMediaType
import de.ii.ldproxy.ogcapi.domain.ApiRequestContext
import de.ii.ldproxy.ogcapi.domain.FeatureTypeConfigurationOgcApi
import de.ii.ldproxy.ogcapi.domain.ImmutableFeatureTypeConfigurationOgcApi
import de.ii.ldproxy.ogcapi.domain.ImmutableOgcApiDataV2
import de.ii.ldproxy.ogcapi.domain.OgcApi
import de.ii.ldproxy.ogcapi.domain.URICustomizer
import de.ii.ldproxy.ogcapi.features.geojson.app.FeaturesFormatGeoJson
import de.ii.ldproxy.ogcapi.features.geojson.domain.FeatureTransformationContextGeoJson
import de.ii.ldproxy.ogcapi.features.geojson.domain.FeatureTransformerGeoJson
import de.ii.ldproxy.ogcapi.features.geojson.domain.ImmutableFeatureTransformationContextGeoJson
import de.ii.ldproxy.ogcapi.features.geojson.domain.ImmutableGeoJsonConfiguration
import de.ii.ldproxy.ogcapi.features.geojson.domain.ModifiableStateGeoJson
import de.ii.ldproxy.ogcapi.features.jsonfg.domain.ImmutableFeatureTransformationContextJsonFgExtension
import de.ii.ldproxy.ogcapi.features.jsonfg.domain.ImmutableJsonFgConfiguration
import de.ii.ldproxy.ogcapi.features.jsonfg.domain.JsonFgConfiguration
import de.ii.xtraplatform.crs.domain.CrsTransformer
import de.ii.xtraplatform.crs.domain.EpsgCrs
import de.ii.xtraplatform.crs.domain.OgcCrs
import spock.lang.Shared
import spock.lang.Specification

import javax.ws.rs.core.Request
import java.nio.charset.StandardCharsets

class JsonFgWriterCrsSpec extends Specification {

    @Shared EpsgCrs DEFAULT_CRS = OgcCrs.CRS84
    @Shared EpsgCrs OTHER_CRS = EpsgCrs.of(4258)

    def "GeoJson writer CRS middleware for FeatureCollections if target is WGS84"() {
        given:
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream()
        boolean isCollection = true
        EpsgCrs crs = DEFAULT_CRS
        String expected = "{" + System.lineSeparator() +
                "  \"coord-ref-sys\" : \""+crs.toUriString()+"\"" + System.lineSeparator() +
                "}"

        when:
        runTransformer(outputStream, isCollection, crs)
        String actual = new String(outputStream.toByteArray(), StandardCharsets.UTF_8)

        then:
        actual == expected
    }

    def "JsonFg writer CRS middleware for FeatureCollections if target is not WGS84"() {
        given:
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream()
        boolean isCollection = true
        EpsgCrs crs = OTHER_CRS
        String expected = "{" + System.lineSeparator() +
                "  \"coord-ref-sys\" : \""+crs.toUriString()+"\"" + System.lineSeparator() +
                "}"

        when:
        runTransformer(outputStream, isCollection, crs)
        String actual = new String(outputStream.toByteArray(), StandardCharsets.UTF_8)

        then:
        actual == expected
    }


    def "JsonFg writer CRS middleware for single Features if target is WGS84"() {
        given:
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream()
        boolean isCollection = false
        EpsgCrs crs = DEFAULT_CRS
        String expected = "{" + System.lineSeparator() +
                "  \"coord-ref-sys\" : \""+crs.toUriString()+"\"" + System.lineSeparator() +
                "}"

        when:
        runTransformer(outputStream, isCollection, crs)
        String actual = new String(outputStream.toByteArray(), StandardCharsets.UTF_8)

        then:
        actual == expected
    }

    def "JsonFg writer CRS middleware for single Features if target is not WGS84"() {
        given:
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream()
        boolean isCollection = false
        EpsgCrs crs = OTHER_CRS
        String expected = "{" + System.lineSeparator() +
                "  \"coord-ref-sys\" : \""+crs.toUriString()+"\"" + System.lineSeparator() +
                "}"

        when:
        runTransformer(outputStream, isCollection, crs)
        String actual = new String(outputStream.toByteArray(), StandardCharsets.UTF_8)

        then:
        actual == expected
    }

    private void runTransformer(ByteArrayOutputStream outputStream, boolean isCollection, EpsgCrs crs) throws IOException, URISyntaxException {
        outputStream.reset()
        FeatureTransformationContextGeoJson transformationContext = createTransformationContext(outputStream, isCollection, crs)
        FeatureTransformerGeoJson transformer = new FeatureTransformerGeoJson(transformationContext, ImmutableList.of(new JsonFgWriterCrs()))

        transformationContext.getJson()
                .writeStartObject()
        transformer.onStart(OptionalLong.of(0L), OptionalLong.empty())
        transformer.onFeatureStart(null)
        transformationContext.getJson()
                .writeEndObject()
        transformer.onEnd()
    }

    private FeatureTransformationContextGeoJson createTransformationContext(OutputStream outputStream, boolean isCollection, EpsgCrs crs) throws URISyntaxException {
        CrsTransformer crsTransformer = null
        if (Objects.nonNull(crs)) {
            crsTransformer = Mock()
            crsTransformer.getTargetCrs() >> crs
        }

        return ImmutableFeatureTransformationContextGeoJson.builder()
                .crsTransformer(Optional.ofNullable(crsTransformer))
                .defaultCrs(OgcCrs.CRS84)
                .mediaType(FeaturesFormatJsonFg.MEDIA_TYPE)
                .putExtensions("jsonfg", ImmutableFeatureTransformationContextJsonFgExtension.builder()
                                .crsTransformerWhere(Optional.ofNullable(crsTransformer))
                                .crs(crs)
                                .build())
                .apiData(new ImmutableOgcApiDataV2.Builder()
                        .id("foo")
                        .serviceType("OGC_API")
                        .putCollections("bar", new ImmutableFeatureTypeConfigurationOgcApi.Builder()
                                .id("bar")
                                .label("Bar")
                                .addExtensions(new ImmutableJsonFgConfiguration.Builder()
                                        .enabled(true)
                                        .refSys(true)
                                        .build())
                                .build())
                        .build())
                .collectionId("bar")
                .outputStream(outputStream)
                .links(ImmutableList.of())
                .isFeatureCollection(isCollection)
                .ogcApiRequest(new ApiRequestContext() {
                    @Override
                    ApiMediaType getMediaType() {
                        return FeaturesFormatGeoJson.MEDIA_TYPE
                    }

                    @Override
                    List<ApiMediaType> getAlternateMediaTypes() {
                        return ImmutableList.of()
                    }

                    @Override
                    Optional<Locale> getLanguage() {
                        return Optional.empty()
                    }

                    @Override
                    OgcApi getApi() {
                        return null
                    }

                    @Override
                    URICustomizer getUriCustomizer() {
                        return new URICustomizer()
                    }

                    @Override
                    String getStaticUrlPrefix() {
                        return null
                    }

                    @Override
                    Map<String, String> getParameters() {
                        return null
                    }

                    @Override
                    Optional<Request> getRequest() {
                        return null
                    }
                })
                .limit(10)
                .offset(20)
                .maxAllowableOffset(0)
                .isHitsOnly(false)
                .state(ModifiableStateGeoJson.create())
                .geoJsonConfig(new ImmutableGeoJsonConfiguration.Builder()
                        .enabled(true)
                        .nestedObjectStrategy(FeatureTransformerGeoJson.NESTED_OBJECTS.NEST)
                        .multiplicityStrategy(FeatureTransformerGeoJson.MULTIPLICITY.ARRAY)
                        .useFormattedJsonOutput(true)
                        .build())
                .build()

    }
}