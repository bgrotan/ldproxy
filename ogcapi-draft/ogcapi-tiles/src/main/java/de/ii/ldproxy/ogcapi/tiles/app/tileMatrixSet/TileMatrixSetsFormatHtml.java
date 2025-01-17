/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.tiles.app.tileMatrixSet;

import com.google.common.collect.ImmutableList;
import de.ii.ldproxy.ogcapi.domain.ApiMediaType;
import de.ii.ldproxy.ogcapi.domain.ApiMediaTypeContent;
import de.ii.ldproxy.ogcapi.domain.ApiRequestContext;
import de.ii.ldproxy.ogcapi.domain.I18n;
import de.ii.ldproxy.ogcapi.domain.ImmutableApiMediaType;
import de.ii.ldproxy.ogcapi.domain.ImmutableApiMediaTypeContent;
import de.ii.ldproxy.ogcapi.domain.OgcApi;
import de.ii.ldproxy.ogcapi.domain.OgcApiDataV2;
import de.ii.ldproxy.ogcapi.html.domain.HtmlConfiguration;
import de.ii.ldproxy.ogcapi.html.domain.NavigationDTO;
import de.ii.ldproxy.ogcapi.tiles.domain.tileMatrixSet.TileMatrixSetData;
import de.ii.ldproxy.ogcapi.tiles.domain.tileMatrixSet.TileMatrixSets;
import de.ii.ldproxy.ogcapi.tiles.domain.tileMatrixSet.TileMatrixSetsFormatExtension;
import io.swagger.v3.oas.models.media.StringSchema;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;

import javax.ws.rs.core.MediaType;
import java.util.List;

@Component
@Provides
@Instantiate
public class TileMatrixSetsFormatHtml implements TileMatrixSetsFormatExtension {

    static final ApiMediaType MEDIA_TYPE = new ImmutableApiMediaType.Builder()
            .type(MediaType.TEXT_HTML_TYPE)
            .parameter("html")
            .build();

    private final I18n i18n;

    public TileMatrixSetsFormatHtml(@Requires I18n i18n) {
        this.i18n = i18n;
    }

    @Override
    public ApiMediaType getMediaType() {
        return MEDIA_TYPE;
    }

    @Override
    public ApiMediaTypeContent getContent(OgcApiDataV2 apiData, String path) {
        return new ImmutableApiMediaTypeContent.Builder()
                .schema(new StringSchema().example("<html>...</html>"))
                .schemaRef("#/components/schemas/htmlSchema")
                .ogcApiMediaType(MEDIA_TYPE)
                .build();
    }

    private boolean isNoIndexEnabledForApi(OgcApiDataV2 apiData) {
        return apiData.getExtension(HtmlConfiguration.class)
                .map(HtmlConfiguration::getNoIndexEnabled)
                .orElse(true);
    }

    @Override
    public Object getTileMatrixSetsEntity(TileMatrixSets tileMatrixSets,
                                          OgcApi api,
                                          ApiRequestContext requestContext) {
        String rootTitle = i18n.get("root", requestContext.getLanguage());
        String tileMatrixSetsTitle = i18n.get("tileMatrixSetsTitle", requestContext.getLanguage());

        final List<NavigationDTO> breadCrumbs = new ImmutableList.Builder<NavigationDTO>()
                .add(new NavigationDTO(rootTitle,
                        requestContext.getUriCustomizer().copy()
                                .removeLastPathSegments(api.getData()
                                                           .getSubPath()
                                                           .size() + 1)
                                .toString()))
                .add(new NavigationDTO(api.getData().getLabel(),
                        requestContext.getUriCustomizer()
                                .copy()
                                .removeLastPathSegments(1)
                                .toString()))
                .add(new NavigationDTO(tileMatrixSetsTitle))
                .build();

        HtmlConfiguration htmlConfig = api.getData()
                                                 .getExtension(HtmlConfiguration.class)
                                                 .orElse(null);

        return new TileMatrixSetsView(api.getData(), tileMatrixSets, breadCrumbs, requestContext.getStaticUrlPrefix(), htmlConfig, isNoIndexEnabledForApi(api.getData()), requestContext.getUriCustomizer(), i18n, requestContext.getLanguage());
    }

    @Override
    public Object getTileMatrixSetEntity(TileMatrixSetData tileMatrixSet,
                                         OgcApi api,
                                         ApiRequestContext requestContext) {
        String rootTitle = i18n.get("root", requestContext.getLanguage());
        String tileMatrixSetsTitle = i18n.get("tileMatrixSetsTitle", requestContext.getLanguage());
        String title = tileMatrixSet.getTitle().orElse(tileMatrixSet.getId());

        final List<NavigationDTO> breadCrumbs = new ImmutableList.Builder<NavigationDTO>()
                .add(new NavigationDTO(rootTitle,
                        requestContext.getUriCustomizer().copy()
                                .removeLastPathSegments(api.getData()
                                                           .getSubPath()
                                                           .size() + 2)
                                .toString()))
                .add(new NavigationDTO(api.getData().getLabel(),
                        requestContext.getUriCustomizer()
                                .copy()
                                .removeLastPathSegments(2)
                                .toString()))
                .add(new NavigationDTO(tileMatrixSetsTitle,
                        requestContext.getUriCustomizer()
                                .copy()
                                .removeLastPathSegments(1)
                                .toString()))
                .add(new NavigationDTO(title))
                .build();

        HtmlConfiguration htmlConfig = api.getData()
                                          .getExtension(HtmlConfiguration.class)
                                          .orElse(null);

        return new TileMatrixSetView(api.getData(), tileMatrixSet, breadCrumbs, requestContext.getStaticUrlPrefix(), htmlConfig, isNoIndexEnabledForApi(api.getData()), requestContext.getUriCustomizer(), i18n, requestContext.getLanguage());
    }
}
