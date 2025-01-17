/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.resources.app;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.hash.Funnel;
import de.ii.ldproxy.ogcapi.domain.Link;
import de.ii.ldproxy.ogcapi.domain.PageRepresentation;
import de.ii.ldproxy.ogcapi.styles.domain.StyleEntry;
import de.ii.ldproxy.ogcapi.styles.domain.Styles;
import org.immutables.value.Value;

import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.List;

@Value.Immutable
@Value.Style(deepImmutablesDetection = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonDeserialize(as = ImmutableResources.class)
public abstract class Resources extends PageRepresentation {

    public abstract List<Resource> getResources();

    @SuppressWarnings("UnstableApiUsage")
    public static final Funnel<Resources> FUNNEL = (from, into) -> {
        PageRepresentation.FUNNEL.funnel(from, into);
        from.getResources()
            .stream()
            .sorted(Comparator.comparing(Resource::getId))
            .forEachOrdered(val -> Resource.FUNNEL.funnel(val, into));
    };
}
