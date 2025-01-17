/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.features.core.app;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import de.ii.ldproxy.ogcapi.common.domain.ConformanceDeclaration;
import de.ii.ldproxy.ogcapi.domain.ApiMediaType;
import de.ii.ldproxy.ogcapi.domain.ApiRequestContext;
import de.ii.ldproxy.ogcapi.domain.I18n;
import de.ii.ldproxy.ogcapi.domain.Link;
import de.ii.ldproxy.ogcapi.domain.OgcApi;
import de.ii.ldproxy.ogcapi.domain.OgcApiDataV2;
import de.ii.ldproxy.ogcapi.domain.QueryHandler;
import de.ii.ldproxy.ogcapi.domain.QueryInput;
import de.ii.ldproxy.ogcapi.features.core.domain.FeatureFormatExtension;
import de.ii.ldproxy.ogcapi.features.core.domain.FeatureLinksGenerator;
import de.ii.ldproxy.ogcapi.features.core.domain.FeaturesCoreConfiguration;
import de.ii.ldproxy.ogcapi.features.core.domain.FeaturesCoreQueriesHandler;
import de.ii.ldproxy.ogcapi.features.core.domain.FeaturesLinksGenerator;
import de.ii.ldproxy.ogcapi.features.core.domain.ImmutableFeatureTransformationContextGeneric;
import de.ii.xtraplatform.codelists.domain.Codelist;
import de.ii.xtraplatform.crs.domain.CrsTransformer;
import de.ii.xtraplatform.crs.domain.CrsTransformerFactory;
import de.ii.xtraplatform.crs.domain.EpsgCrs;
import de.ii.xtraplatform.dropwizard.domain.Dropwizard;
import de.ii.xtraplatform.features.domain.FeatureConsumer;
import de.ii.xtraplatform.features.domain.FeatureProvider2;
import de.ii.xtraplatform.features.domain.FeatureQuery;
import de.ii.xtraplatform.features.domain.FeatureSourceStream;
import de.ii.xtraplatform.features.domain.FeatureStream;
import de.ii.xtraplatform.features.domain.FeatureStream.Result;
import de.ii.xtraplatform.features.domain.FeatureStream2.ResultOld;
import de.ii.xtraplatform.features.domain.FeatureTokenEncoder;
import de.ii.xtraplatform.features.domain.transform.PropertyTransformations;
import de.ii.xtraplatform.store.domain.entities.EntityRegistry;
import de.ii.xtraplatform.streams.domain.OutputStreamToByteConsumer;
import de.ii.xtraplatform.streams.domain.Reactive.Sink;
import de.ii.xtraplatform.streams.domain.Reactive.SinkTransformed;
import de.ii.xtraplatform.stringtemplates.domain.StringTemplateFilters;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.NotAcceptableException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import java.io.OutputStream;
import java.text.MessageFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletionException;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.codahale.metrics.MetricRegistry.name;

@Component
@Instantiate
@Provides
public class FeaturesCoreQueriesHandlerImpl implements FeaturesCoreQueriesHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(FeaturesCoreQueriesHandlerImpl.class);

    private final I18n i18n;
    private final CrsTransformerFactory crsTransformerFactory;
    private final Map<Query, QueryHandler<? extends QueryInput>> queryHandlers;
    private final MetricRegistry metricRegistry;
    private final EntityRegistry entityRegistry;

    public FeaturesCoreQueriesHandlerImpl(@Requires I18n i18n,
                                          @Requires CrsTransformerFactory crsTransformerFactory,
                                          @Requires Dropwizard dropwizard,
                                          @Requires EntityRegistry entityRegistry) {
        this.i18n = i18n;
        this.crsTransformerFactory = crsTransformerFactory;
        this.entityRegistry = entityRegistry;

        this.metricRegistry = dropwizard.getEnvironment()
                                        .metrics();

        this.queryHandlers = ImmutableMap.of(
                Query.FEATURES, QueryHandler.with(QueryInputFeatures.class, this::getItemsResponse),
                Query.FEATURE, QueryHandler.with(QueryInputFeature.class, this::getItemResponse)
        );
    }

    @Override
    public Map<Query, QueryHandler<? extends QueryInput>> getQueryHandlers() {
        return queryHandlers;
    }

    public static void ensureCollectionIdExists(OgcApiDataV2 apiData, String collectionId) {
        if (!apiData.isCollectionEnabled(collectionId)) {
            throw new NotFoundException(MessageFormat.format("The collection ''{0}'' does not exist in this API.", collectionId));
        }
    }

    private static void ensureFeatureProviderSupportsQueries(FeatureProvider2 featureProvider) {
        if (!featureProvider.supportsQueries()) {
            throw new IllegalStateException("Feature provider does not support queries.");
        }
    }

    private Response getItemsResponse(QueryInputFeatures queryInput, ApiRequestContext requestContext) {

        OgcApi api = requestContext.getApi();
        OgcApiDataV2 apiData = api.getData();
        String collectionId = queryInput.getCollectionId();
        FeatureQuery query = queryInput.getQuery();
        Optional<Integer> defaultPageSize = queryInput.getDefaultPageSize();
        boolean onlyHitsIfMore = false; // TODO check

        FeatureFormatExtension outputFormat = api.getOutputFormat(
                FeatureFormatExtension.class,
                requestContext.getMediaType(),
                "/collections/" + collectionId + "/items",
                Optional.of(collectionId))
                                                 .orElseThrow(() -> new NotAcceptableException(MessageFormat.format("The requested media type ''{0}'' is not supported for this resource.", requestContext.getMediaType())));

        return getItemsResponse(api, requestContext, collectionId, null, queryInput, query, queryInput.getFeatureProvider(), null, outputFormat, onlyHitsIfMore, defaultPageSize,
                queryInput.getShowsFeatureSelfLink(), queryInput.getIncludeLinkHeader(), queryInput.getDefaultCrs());
    }

    private Response getItemResponse(QueryInputFeature queryInput,
                                     ApiRequestContext requestContext) {

        OgcApi api = requestContext.getApi();
        OgcApiDataV2 apiData = api.getData();
        String collectionId = queryInput.getCollectionId();
        String featureId = queryInput.getFeatureId();
        FeatureQuery query = queryInput.getQuery();

        FeatureFormatExtension outputFormat = api.getOutputFormat(
                FeatureFormatExtension.class,
                requestContext.getMediaType(),
                "/collections/" + collectionId + "/items/" + featureId,
                Optional.of(collectionId))
                                                 .orElseThrow(() -> new NotAcceptableException(MessageFormat.format("The requested media type ''{0}'' is not supported for this resource.", requestContext.getMediaType())));

        String persistentUri = null;
        Optional<String> template = api.getData()
                                       .getCollections()
                                       .get(collectionId)
                                       .getPersistentUriTemplate();
        if (template.isPresent()) {
            persistentUri = StringTemplateFilters.applyTemplate(template.get(), featureId);
        }

        return getItemsResponse(api, requestContext, collectionId, featureId, queryInput, query, queryInput.getFeatureProvider(), persistentUri, outputFormat, false, Optional.empty(),
                false, queryInput.getIncludeLinkHeader(), queryInput.getDefaultCrs());
    }

    private Response getItemsResponse(OgcApi api, ApiRequestContext requestContext, String collectionId, String featureId,
                                      QueryInput queryInput, FeatureQuery query, FeatureProvider2 featureProvider,
                                      String canonicalUri,
                                      FeatureFormatExtension outputFormat,
                                      boolean onlyHitsIfMore, Optional<Integer> defaultPageSize,
                                      boolean showsFeatureSelfLink, boolean includeLinkHeader,
                                      EpsgCrs defaultCrs) {

        ensureCollectionIdExists(api.getData(), collectionId);
        ensureFeatureProviderSupportsQueries(featureProvider);

        Optional<CrsTransformer> crsTransformer = Optional.empty();
        boolean swapCoordinates = false;

        EpsgCrs sourceCrs = null;
        EpsgCrs targetCrs = query.getCrs()
                                 .orElse(defaultCrs);
        if (featureProvider.supportsCrs()) {
            sourceCrs = featureProvider.crs()
                                       .getNativeCrs();
            crsTransformer = crsTransformerFactory.getTransformer(sourceCrs, targetCrs);
            swapCoordinates = crsTransformer.isPresent() && crsTransformer.get()
                                                                          .needsCoordinateSwap();
        }


        List<ApiMediaType> alternateMediaTypes = requestContext.getAlternateMediaTypes();

        List<Link> links =
                Objects.isNull(featureId) ?
                        new FeaturesLinksGenerator().generateLinks(requestContext.getUriCustomizer(), query.getOffset(), query.getLimit(), defaultPageSize.orElse(0), requestContext.getMediaType(), alternateMediaTypes, i18n, requestContext.getLanguage()) :
                        new FeatureLinksGenerator().generateLinks(requestContext.getUriCustomizer(), requestContext.getMediaType(), alternateMediaTypes, outputFormat.getCollectionMediaType(), canonicalUri, i18n, requestContext.getLanguage());

        String featureTypeId = api.getData().getCollections()
                                  .get(collectionId)
                                  .getExtension(FeaturesCoreConfiguration.class)
                                  .map(cfg -> cfg.getFeatureType().orElse(collectionId))
                                  .orElse(collectionId);

        ImmutableFeatureTransformationContextGeneric.Builder transformationContext = new ImmutableFeatureTransformationContextGeneric.Builder()
                .apiData(api.getData())
                .featureSchema(featureProvider.getData().getTypes().get(featureTypeId))
                .collectionId(collectionId)
                .ogcApiRequest(requestContext)
                .crsTransformer(crsTransformer)
                .codelists(entityRegistry.getEntitiesForType(Codelist.class)
                                         .stream()
                                         .collect(Collectors.toMap(c -> c.getId(), c -> c)))
                .defaultCrs(defaultCrs)
                .sourceCrs(Optional.ofNullable(sourceCrs))
                .links(links)
                .isFeatureCollection(Objects.isNull(featureId))
                .isHitsOnly(query.hitsOnly())
                .isPropertyOnly(query.propertyOnly())
                .fields(query.getFields())
                .limit(query.getLimit())
                .offset(query.getOffset())
                .maxAllowableOffset(query.getMaxAllowableOffset())
                .geometryPrecision(query.getGeometryPrecision())
                .shouldSwapCoordinates(swapCoordinates)
                .isHitsOnlyIfMore(onlyHitsIfMore)
                .showsFeatureSelfLink(showsFeatureSelfLink);

        StreamingOutput streamingOutput;

        if (outputFormat.canPassThroughFeatures() && featureProvider.supportsPassThrough() && outputFormat.getMediaType()
                                                                                                          .matches(featureProvider.passThrough()
                                                                                                                                  .getMediaType())) {
            FeatureSourceStream<?> featureStream = featureProvider.passThrough()
                                                                  .getFeatureSourceStream(query);

            streamingOutput = stream2(featureStream, Objects.nonNull(featureId), outputStream -> outputFormat.getFeatureConsumer(transformationContext.outputStream(outputStream)
                                                                                                                                         .build())
                                                                                                .get());
        } else if (outputFormat.canEncodeFeatures()) {
            FeatureStream featureStream = featureProvider.queries()
                                                          .getFeatureStream(query);

            ImmutableFeatureTransformationContextGeneric transformationContextGeneric = transformationContext
                .outputStream(new OutputStreamToByteConsumer())
                .build();
            FeatureTokenEncoder<?> encoder = outputFormat
                .getFeatureEncoder(transformationContextGeneric, requestContext.getLanguage()).get();

            Optional<PropertyTransformations> propertyTransformations = outputFormat
                .getPropertyTransformations(api.getData().getCollections().get(collectionId))
                .map(pt -> pt.withSubstitutions(ImmutableMap.of("serviceUrl", transformationContextGeneric.getServiceUrl())));

            streamingOutput = stream(featureStream, Objects.nonNull(featureId), encoder, propertyTransformations);
        } else {
            throw new NotAcceptableException(MessageFormat.format("The requested media type {0} cannot be generated, because it does not support streaming.", requestContext.getMediaType().type()));
        }

        Date lastModified = getLastModified(queryInput, requestContext.getApi(), featureProvider);
        EntityTag etag = Objects.isNull(featureId) ? null : getEtag(lastModified);
        Response.ResponseBuilder response = evaluatePreconditions(requestContext, lastModified, etag);
        if (Objects.nonNull(response))
            return response.build();

        // TODO support lastModified and etag from the content, in particular for a single feature
        // TODO determine numberMatched, numberReturned and optionally return them as OGC-numberMatched and OGC-numberReturned headers
        // TODO For now remove the "next" links from the headers since at this point we don't know, whether there will be a next page

        return prepareSuccessResponse(requestContext, includeLinkHeader ? links.stream()
                                                                               .filter(link -> !"next".equalsIgnoreCase(link.getRel()))
                                                                               .collect(ImmutableList.toImmutableList()) : null,
                                      lastModified, etag,
                                      queryInput.getCacheControl().orElse(null),
                                      queryInput.getExpires().orElse(null),
                                      targetCrs,
                                      true,
                                      String.format("%s.%s", Objects.isNull(featureId) ? collectionId : featureId, outputFormat.getMediaType().fileExtension()))
                .entity(streamingOutput)
                .build();
    }

    private StreamingOutput stream(FeatureStream featureTransformStream, boolean failIfEmpty,
        final FeatureTokenEncoder<?> encoder,
        Optional<PropertyTransformations> propertyTransformations) {
        Timer.Context timer = metricRegistry.timer(name(FeaturesCoreQueriesHandlerImpl.class, "stream"))
            .time();

        return outputStream -> {
            SinkTransformed<Object, byte[]> featureSink = encoder.to(Sink.outputStream(outputStream));

            try {
                Result result = featureTransformStream.runWith(featureSink, propertyTransformations)
                    .toCompletableFuture()
                    .join();
                timer.stop();

                if (result.getError()
                    .isPresent()) {
                    processStreamError(result.getError().get());
                    // the connection has been lost, typically the client has cancelled the request, log on debug level
                    LOGGER.debug("Request cancelled due to lost connection.");
                }

                if (result.isEmpty() && failIfEmpty) {
                    throw new NotFoundException("The requested feature does not exist.");
                }

            } catch (CompletionException e) {
                if (e.getCause() instanceof WebApplicationException) {
                    throw (WebApplicationException) e.getCause();
                }
                throw new IllegalStateException("Feature stream error.", e.getCause());
            }
        };
    }

    private StreamingOutput stream2(FeatureSourceStream<?> featureTransformStream, boolean failIfEmpty,
                                    final Function<OutputStream, FeatureConsumer> featureTransformer) {
        return outputStream -> {
            try {
                ResultOld result = featureTransformStream.runWith(featureTransformer.apply(outputStream))
                                                                     .toCompletableFuture()
                                                                     .join();
                if (result.getError()
                          .isPresent()) {
                    processStreamError(result.getError().get());
                    // the connection has been lost, typically the client has cancelled the request, log on debug level
                    LOGGER.debug("Request cancelled due to lost connection.");
                }

                if (result.isEmpty() && failIfEmpty) {
                    throw new NotFoundException("The requested feature does not exist.");
                }

            } catch (CompletionException e) {
                if (e.getCause() instanceof WebApplicationException) {
                    throw (WebApplicationException) e.getCause();
                }
                throw new IllegalStateException("Feature stream error.", e.getCause());
            }
        };
    }
}
