/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.domain

trait MergeCollection<T extends ExtensionConfiguration> {

    abstract T getCollection();

    abstract T getCollectionFullMerged();

    def getUseCases() {
        super.getUseCases() << [
                "collection into full",
                "merging a configuration with collection values into a full configuration with differing values",
                "source and target collections should be merged",
                getCollection(),
                getFull(),
                getCollectionFullMerged()
        ]
    }

}
