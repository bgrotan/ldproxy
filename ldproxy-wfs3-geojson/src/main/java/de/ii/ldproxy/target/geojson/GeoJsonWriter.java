/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.target.geojson;

import de.ii.ldproxy.wfs3.api.FeatureWriterGeoJson;

/**
 * @author zahnen
 */
public interface GeoJsonWriter extends FeatureWriterGeoJson<FeatureTransformationContextGeoJson> {
    GeoJsonWriter create();
}