/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.tiles.app.tileMatrixSet;

import com.google.common.base.Charsets;
import de.ii.ldproxy.ogcapi.domain.I18n;
import de.ii.ldproxy.ogcapi.domain.OgcApiDataV2;
import de.ii.ldproxy.ogcapi.domain.URICustomizer;
import de.ii.ldproxy.ogcapi.html.domain.HtmlConfiguration;
import de.ii.ldproxy.ogcapi.html.domain.NavigationDTO;
import de.ii.ldproxy.ogcapi.html.domain.OgcApiView;
import de.ii.ldproxy.ogcapi.tiles.domain.tileMatrixSet.TileMatrixSetLinks;
import de.ii.ldproxy.ogcapi.tiles.domain.tileMatrixSet.TileMatrixSets;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class TileMatrixSetsView extends OgcApiView {
    public List<TileMatrixSetLinks> tileMatrixSets;
    public String none;

    public TileMatrixSetsView(OgcApiDataV2 apiData,
                              TileMatrixSets tileMatrixSets,
                              List<NavigationDTO> breadCrumbs,
                              String staticUrlPrefix,
                              HtmlConfiguration htmlConfig,
                              boolean noIndex,
                              URICustomizer uriCustomizer,
                              I18n i18n,
                              Optional<Locale> language) {
        super("tileMatrixSets.mustache", Charsets.UTF_8, apiData, breadCrumbs, htmlConfig, noIndex, staticUrlPrefix,
                tileMatrixSets.getLinks(),
                i18n.get("tileMatrixSetsTitle", language),
                i18n.get("tileMatrixSetsDescription", language));
        this.tileMatrixSets = tileMatrixSets.getTileMatrixSets();
        this.none = i18n.get ("none", language);
    }
}
