/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.domain;

import de.ii.xtraplatform.feature.transformer.api.TransformingFeatureProvider;

import java.util.Map;

/**
 * @author zahnen
 */
//TODO: ServiceBackgroundTask
public interface Wfs3StartupTask extends OgcApiExtension {
    Runnable getTask(OgcApiDatasetData datasetData, TransformingFeatureProvider featureProvider);
    Map<Thread,String> getThreadMap();
    void removeThreadMapEntry(Thread t);
}