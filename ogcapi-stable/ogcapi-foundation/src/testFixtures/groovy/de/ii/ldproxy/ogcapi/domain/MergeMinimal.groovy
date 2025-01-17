/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.domain

trait MergeMinimal<T extends ExtensionConfiguration> {

    abstract T getMinimal();

    abstract T getMinimalFullMerged();

    def getUseCases() {
        super.getUseCases() << [
                "minimal into full",
                "merging a minimal configuration into a full configuration",
                "null/empty values should not override target values",
                getMinimal(),
                getFull(),
                getMinimalFullMerged()
        ] << [
                "full into minimal",
                "merging a full configuration into a minimal configuration",
                "source values should override target null/empty values",
                getFull(),
                getMinimal(),
                getMinimalFullMerged()
        ]
    }

}
