/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.infra.persistence;

import de.ii.ldproxy.ogcapi.domain.ExtensionRegistry;
import de.ii.ldproxy.ogcapi.domain.OgcApiDataHydratorExtension;
import de.ii.ldproxy.ogcapi.domain.OgcApiDataV2;
import de.ii.xtraplatform.services.domain.Service;
import de.ii.xtraplatform.store.domain.entities.EntityHydrator;
import de.ii.xtraplatform.store.domain.entities.handler.Entity;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.StaticServiceProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;
import java.util.List;

@Component
@Provides(properties = {
        @StaticServiceProperty(name = Entity.TYPE_KEY, type = "java.lang.String", value = Service.TYPE),
        @StaticServiceProperty(name = Entity.SUB_TYPE_KEY, type = "java.lang.String", value = OgcApiDataV2.SERVICE_TYPE)
})
@Instantiate
public class OgcApiDatasetHydrator implements EntityHydrator<OgcApiDataV2> {

    private static final Logger LOGGER = LoggerFactory.getLogger(OgcApiDatasetHydrator.class);

    private final ExtensionRegistry extensionRegistry;

    public OgcApiDatasetHydrator(@Requires ExtensionRegistry extensionRegistry) {
        this.extensionRegistry = extensionRegistry;
    }

    @Override
    public OgcApiDataV2 hydrateData(OgcApiDataV2 apiData) {

        OgcApiDataV2 hydrated = apiData;

        if (hydrated.isAuto()) {
            LOGGER.info("Service with id '{}' is in auto mode, generating configuration ...", hydrated.getId());
        }

        List<OgcApiDataHydratorExtension> extensions = extensionRegistry.getExtensionsForType(OgcApiDataHydratorExtension.class);
        extensions.sort(Comparator.comparing(OgcApiDataHydratorExtension::getSortPriority));
        for (OgcApiDataHydratorExtension hydrator : extensions) {
            if (hydrator.isEnabledForApi(hydrated)) {
                hydrated = hydrator.getHydratedData(hydrated);
            }
        }

        return hydrated;
    }

}
