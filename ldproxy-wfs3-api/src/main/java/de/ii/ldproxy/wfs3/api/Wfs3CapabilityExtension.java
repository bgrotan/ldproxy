/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.wfs3.api;

import de.ii.ldproxy.ogcapi.domain.ExtensionConfiguration;
import de.ii.ldproxy.ogcapi.domain.OgcApiConfigPreset;
import de.ii.ldproxy.ogcapi.domain.OgcApiDatasetData;
import de.ii.ldproxy.ogcapi.domain.OgcApiExtension;

/**
 * @author zahnen
 */
public interface Wfs3CapabilityExtension extends OgcApiExtension {

    @Override
    default boolean isEnabledForDataset(OgcApiDatasetData dataset) {
        return true;
    }

    ExtensionConfiguration getDefaultConfiguration(OgcApiConfigPreset preset);
}