/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.styles.infra;

import com.google.common.collect.ImmutableList;
import de.ii.ldproxy.ogcapi.collections.domain.EndpointSubCollection;
import de.ii.ldproxy.ogcapi.domain.ApiEndpointDefinition;
import de.ii.ldproxy.ogcapi.domain.ApiOperation;
import de.ii.ldproxy.ogcapi.domain.ApiRequestContext;
import de.ii.ldproxy.ogcapi.domain.ConformanceClass;
import de.ii.ldproxy.ogcapi.domain.ExtensionConfiguration;
import de.ii.ldproxy.ogcapi.domain.ExtensionRegistry;
import de.ii.ldproxy.ogcapi.domain.FormatExtension;
import de.ii.ldproxy.ogcapi.domain.FoundationConfiguration;
import de.ii.ldproxy.ogcapi.domain.HttpMethods;
import de.ii.ldproxy.ogcapi.domain.ImmutableApiEndpointDefinition;
import de.ii.ldproxy.ogcapi.domain.ImmutableOgcApiResourceSet;
import de.ii.ldproxy.ogcapi.domain.OgcApi;
import de.ii.ldproxy.ogcapi.domain.OgcApiDataV2;
import de.ii.ldproxy.ogcapi.domain.OgcApiPathParameter;
import de.ii.ldproxy.ogcapi.domain.OgcApiQueryParameter;
import de.ii.ldproxy.ogcapi.styles.domain.ImmutableQueryInputStyles;
import de.ii.ldproxy.ogcapi.styles.domain.QueriesHandlerStyles;
import de.ii.ldproxy.ogcapi.styles.domain.StylesConfiguration;
import de.ii.ldproxy.ogcapi.styles.domain.StylesFormatExtension;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Optional;

/**
 * fetch list of styles associated with a collection
 */
@Component
@Provides
@Instantiate
public class EndpointStylesCollection extends EndpointSubCollection implements ConformanceClass {

    private static final Logger LOGGER = LoggerFactory.getLogger(EndpointStylesCollection.class);

    private static final List<String> TAGS = ImmutableList.of("Discover and fetch styles");

    private final QueriesHandlerStyles queryHandler;

    public EndpointStylesCollection(@Requires ExtensionRegistry extensionRegistry,
                                    @Requires QueriesHandlerStyles queryHandler) {
        super(extensionRegistry);
        this.queryHandler = queryHandler;
    }

    @Override
    public List<String> getConformanceClassUris() {
        return ImmutableList.of("http://www.opengis.net/spec/ogcapi-styles-1/0.0/conf/core");
    }

    @Override
    public Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
        return StylesConfiguration.class;
    }

    @Override
    public List<? extends FormatExtension> getFormats() {
        if (formats==null)
            formats = extensionRegistry.getExtensionsForType(StylesFormatExtension.class);
        return formats;
    }

    protected ApiEndpointDefinition computeDefinition(OgcApiDataV2 apiData) {
        ImmutableApiEndpointDefinition.Builder definitionBuilder = new ImmutableApiEndpointDefinition.Builder()
                .apiEntrypoint("collections")
                .sortPriority(ApiEndpointDefinition.SORT_PRIORITY_STYLES_COLLECTION);
        final String subSubPath = "/styles";
        final String path = "/collections/{collectionId}" + subSubPath;
        final HttpMethods method = HttpMethods.GET;
        final List<OgcApiPathParameter> pathParameters = getPathParameters(extensionRegistry, apiData, path);
        final Optional<OgcApiPathParameter> optCollectionIdParam = pathParameters.stream().filter(param -> param.getName().equals("collectionId")).findAny();
        if (!optCollectionIdParam.isPresent()) {
            LOGGER.error("Path parameter 'collectionId' missing for resource at path '" + path + "'. The GET method will not be available.");
        } else {
            final OgcApiPathParameter collectionIdParam = optCollectionIdParam.get();
            boolean explode = collectionIdParam.getExplodeInOpenApi(apiData);
            final List<String> collectionIds = (explode) ?
                    collectionIdParam.getValues(apiData) :
                    ImmutableList.of("{collectionId}");
            for (String collectionId : collectionIds) {
                List<OgcApiQueryParameter> queryParameters = getQueryParameters(extensionRegistry, apiData, path, collectionId);
                String operationSummary = "retrieve a list of the available styles for collection `"+collectionId+"`";
                Optional<String> operationDescription = Optional.of("This operation fetches the set of styles available for this collection. " +
                                                                            "For each style the id, a title, links to the stylesheet of the style in each supported encoding, " +
                                                                            "and the link to the metadata is provided.");
                String resourcePath = path.replace("{collectionId}",collectionId);
                ImmutableOgcApiResourceSet.Builder resourceBuilder = new ImmutableOgcApiResourceSet.Builder()
                        .path(resourcePath)
                        .pathParameters(pathParameters)
                        .subResourceType("Style");
                ApiOperation operation = addOperation(apiData, HttpMethods.GET, queryParameters, collectionId, subSubPath, operationSummary, operationDescription, TAGS);
                if (operation != null)
                    resourceBuilder.putOperations(method.name(), operation);
                definitionBuilder.putResources(resourcePath, resourceBuilder.build());
            }
        }

        return definitionBuilder.build();
    }

    /**
     * fetch all available styles for the service
     *
     * @return all styles in a JSON styles object or an HTML page
     */
    @Path("/{collectionId}/styles")
    @GET
    @Produces({MediaType.APPLICATION_JSON,MediaType.TEXT_HTML})
    public Response getStyles(@Context OgcApi api, @Context ApiRequestContext requestContext,
                              @PathParam("collectionId") String collectionId) {
        OgcApiDataV2 apiData = api.getData();
        checkPathParameter(extensionRegistry, apiData, "/collections/{collectionId}/styles", "collectionId", collectionId);
        checkCollectionExists(apiData, collectionId);

        boolean includeLinkHeader = apiData.getExtension(FoundationConfiguration.class)
                                           .map(FoundationConfiguration::getIncludeLinkHeader)
                                           .orElse(false);
        QueriesHandlerStyles.QueryInputStyles queryInput = new ImmutableQueryInputStyles.Builder()
                .collectionId(collectionId)
                .includeLinkHeader(includeLinkHeader)
                .build();

        return queryHandler.handle(QueriesHandlerStyles.Query.STYLES, queryInput, requestContext);
    }
}