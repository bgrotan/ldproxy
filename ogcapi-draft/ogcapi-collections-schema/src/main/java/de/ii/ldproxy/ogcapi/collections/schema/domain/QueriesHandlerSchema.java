/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.collections.schema.domain;

import de.ii.ldproxy.ogcapi.collections.schema.app.QueriesHandlerSchemaImpl;
import de.ii.ldproxy.ogcapi.domain.QueriesHandler;

public interface QueriesHandlerSchema extends QueriesHandler<QueriesHandlerSchemaImpl.Query> {
}