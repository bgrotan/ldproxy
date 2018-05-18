/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.wfs3;

import com.google.common.collect.ImmutableList;
import org.apache.http.client.utils.URIBuilder;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author zahnen
 */
public class Wfs3LinksGenerator {

    public List<Wfs3Link> generateDatasetLinks(URICustomizer uriBuilder, String describeFeatureTypeUrl, String mediaType, String... alternativeMediaTypes) {
        uriBuilder
                .ensureParameter("f", Wfs3MediaTypes.FORMATS.get(mediaType));

        final boolean isCollections = uriBuilder.isLastPathSegment("collections");

        final ImmutableList.Builder<Wfs3Link> builder = new ImmutableList.Builder<Wfs3Link>()
                .add(new Wfs3Link(uriBuilder.toString(), "self", mediaType, "this document"))
                .addAll(Arrays.stream(alternativeMediaTypes)
                              .map(generateAlternateLink(uriBuilder.copy()))
                              .collect(Collectors.toList()))
                .add(new Wfs3Link(uriBuilder.copy()
                                            .removeLastPathSegment("collections")
                                            .ensureLastPathSegment("api")
                                            .setParameter("f", "json")
                                            .toString(), "service", "application/openapi+json;version=3.0", "the OpenAPI definition as JSON"))
                .add(new Wfs3Link(uriBuilder.copy()
                                            .removeLastPathSegment("collections")
                                            .ensureLastPathSegment("api")
                                            .setParameter("f", "html")
                                            .toString(), "service", "text/html", "the OpenAPI definition as HTML"))
                .add(new Wfs3Link(uriBuilder.copy()
                                            .removeLastPathSegment("collections")
                                            .ensureLastPathSegment("conformance")
                                            .setParameter("f", "json")
                                            .toString(), "conformance", "application/json", "WFS 3.0 conformance classes implemented by this server"));

        if (!isCollections) {
            builder
                    .add(new Wfs3Link(uriBuilder.copy()
                                                .ensureLastPathSegment("collections")
                                                .setParameter("f", "json")
                                                .toString(), "data", "application/json", "Metadata about the feature collections"));
        }

        return builder.add(new Wfs3Link(describeFeatureTypeUrl, "describedBy", "application/xml", "XML schema for all feature types"))
                      .build();
    }

    public List<Wfs3Link> generateDatasetCollectionLinks(URICustomizer uriBuilder, String mediaType, String collectionId, String collectionName, String describeFeatureTypeUrl) {
        uriBuilder
                .ensureParameter("f", Wfs3MediaTypes.FORMATS.get(mediaType))
                .ensureLastPathSegments("collections", collectionId, "items");

        return new ImmutableList.Builder<Wfs3Link>()
                .add(new Wfs3Link(uriBuilder
                        .setParameter("f", "json")
                        .toString(), "item", "application/geo+json", collectionName + " as GeoJSON"))
                .add(new Wfs3Link(uriBuilder
                        .setParameter("f", "html")
                        .toString(), "item", "text/html", collectionName + " as HTML"))
                .add(new Wfs3Link(uriBuilder
                        .setParameter("f", "xml")
                        .toString(), "item", "application/gml+xml;version=3.2;profile=http://www.opengis.net/def/profile/ogc/2.0/gml-sf2", collectionName + " as GML"))
                .add(new Wfs3Link(describeFeatureTypeUrl, "describedBy", "application/xml", "XML schema for feature type " + collectionName))
                .build();
    }


    public List<Wfs3Link> generateCollectionOrFeatureLinks(URICustomizer uriBuilder, boolean isFeatureCollection, int page, int count, String mediaType, String... alternativeMediaTypes) {
        uriBuilder
                .ensureParameter("f", Wfs3MediaTypes.FORMATS.get(mediaType));

        final ImmutableList.Builder<Wfs3Link> links = new ImmutableList.Builder<Wfs3Link>()
                .add(new Wfs3Link(uriBuilder.toString(), "self", mediaType, "this document"))
                .addAll(Arrays.stream(alternativeMediaTypes)
                              .map(generateAlternateLink(uriBuilder.copy()))
                              .collect(Collectors.toList()));

        if (isFeatureCollection) {
            links.add(new Wfs3Link(getUrlWithPageAndCount(uriBuilder.copy(), page + 1, count), "next", mediaType, "next page"));
            if (page > 1) {
                links.add(new Wfs3Link(getUrlWithPageAndCount(uriBuilder.copy(), page - 1, count), "prev", mediaType, "previous page"));
            }
        } else {
            links.add(new Wfs3Link(uriBuilder.copy()
                                             .removeLastPathSegments(1)
                                             .clearParameters()
                                             .setParameter("f", Wfs3MediaTypes.FORMATS.get(mediaType))
                                             .toString(), "collection", mediaType, "the collection document"));
        }

        return links.build();
    }

    private Function<String, Wfs3Link> generateAlternateLink(final URIBuilder uriBuilder) {
        return mediaType -> new Wfs3Link(uriBuilder
                .setParameter("f", Wfs3MediaTypes.FORMATS.get(mediaType))
                .toString(), "alternate", mediaType, "this document as " + Wfs3MediaTypes.NAMES.get(mediaType));
    }

    private String getUrlWithPageAndCount(final URICustomizer uriBuilder, final int page, final int count) {
        return uriBuilder
                .removeParameters("page", "startIndex", "offset", "count", "limit")
                .ensureParameter("page", String.valueOf(page))
                .ensureParameter("limit", String.valueOf(count))
                .toString();
    }
}