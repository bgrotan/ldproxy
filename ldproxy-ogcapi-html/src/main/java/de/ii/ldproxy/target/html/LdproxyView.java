/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.target.html;

import de.ii.ldproxy.ogcapi.domain.OgcApiDatasetData;
import de.ii.ldproxy.ogcapi.domain.OgcApiLink;
import io.dropwizard.views.View;

import javax.annotation.Nullable;
import java.nio.charset.Charset;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public abstract class LdproxyView extends View {

    protected final OgcApiDatasetData apiData;
    protected final List<NavigationDTO> breadCrumbs;
    protected final List<OgcApiLink> links;
    protected final HtmlConfig htmlConfig;
    protected final String urlPrefix;
    protected final String title;
    protected final String description;

    protected LdproxyView(String templateName, @Nullable Charset charset, @Nullable OgcApiDatasetData apiData,
                          List<NavigationDTO> breadCrumbs, HtmlConfig htmlConfig, String urlPrefix,
                          List<OgcApiLink> links, String title, String description) {
        super(templateName, charset);
        this.apiData = apiData;
        this.breadCrumbs = breadCrumbs;
        this.links = links;
        this.htmlConfig = htmlConfig;
        this.urlPrefix = urlPrefix;
        this.title = title;
        this.description = description;
    }

    public List<NavigationDTO> getFormats() {
        return links.stream()
                .filter(link -> Objects.equals(link.getRel(), "alternate"))
                .sorted(Comparator.comparing(link -> link.getTypeLabel()
                        .toUpperCase()))
                .map(link -> new NavigationDTO(link.getTypeLabel(), link.getHref()))
                .collect(Collectors.toList());
    }

    public List<NavigationDTO> getBreadCrumbs() {
        return breadCrumbs;
    }

    public String getUrlPrefix() {
        return urlPrefix;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }
}