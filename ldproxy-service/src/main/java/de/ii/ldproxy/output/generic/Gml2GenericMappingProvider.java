/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.output.generic;

import de.ii.ldproxy.output.generic.GenericMapping.GENERIC_TYPE;
import de.ii.ogc.wfs.proxy.WfsProxyFeatureTypeAnalyzer;
import de.ii.ogc.wfs.proxy.WfsProxyMappingProvider;
import de.ii.xtraplatform.feature.query.api.TargetMapping;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static de.ii.ogc.wfs.proxy.WfsProxyFeatureTypeAnalyzer.GML_NS_URI;

/**
 * @author zahnen
 */
public class Gml2GenericMappingProvider implements WfsProxyMappingProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(Gml2GenericMappingProvider.class);
    public static final String MIME_TYPE = TargetMapping.BASE_TYPE;

    private boolean hasSpatialField;
    private boolean hasTemporalField;

    @Override
    public String getTargetType() {
        return MIME_TYPE;
    }

    @Override
    public TargetMapping getTargetMappingForFeatureType(String nsUri, String localName) {
        GenericMapping targetMapping = new GenericMapping();
        targetMapping.setEnabled(true);

        this.hasSpatialField = false;
        this.hasTemporalField = false;

        return targetMapping;
    }

    @Override
    public TargetMapping getTargetMappingForAttribute(String path, String nsUri, String localName, WfsProxyFeatureTypeAnalyzer.GML_TYPE type) {

        if ((localName.equals("id") && nsUri.startsWith(GML_NS_URI)) || localName.equals("fid")) {

            LOGGER.debug("ID {} {} {}", nsUri, localName, type);

            GENERIC_TYPE dataType = GENERIC_TYPE.forGmlType(type);

            if (dataType.isValid()) {
                GenericMapping targetMapping = new GenericMapping();
                targetMapping.setEnabled(true);
                targetMapping.setName("id");
                targetMapping.setType(dataType);

                return targetMapping;
            }
        }

        return null;
    }

    @Override
    public TargetMapping getTargetMappingForProperty(String path, String nsUri, String localName, WfsProxyFeatureTypeAnalyzer.GML_TYPE type) {

        GENERIC_TYPE dataType = GENERIC_TYPE.forGmlType(type);

        if (dataType.isValid()) {
            LOGGER.debug("PROPERTY {} {}", path, dataType);

            GenericMapping targetMapping = new GenericMapping();
            targetMapping.setEnabled(true);
            targetMapping.setName(path);
            targetMapping.setType(dataType);

            if (dataType.equals(GENERIC_TYPE.TEMPORAL) && !hasTemporalField) {
                targetMapping.setFilterable(true);
                this.hasTemporalField = true;
            }

            return targetMapping;
        }

        return null;
    }

    @Override
    public TargetMapping getTargetMappingForGeometry(String path, String nsUri, String localName, WfsProxyFeatureTypeAnalyzer.GML_GEOMETRY_TYPE type) {

        if (type.isValid()) {
            LOGGER.debug("GEOMETRY {} {}", path, type);

            GenericMapping targetMapping = new GenericMapping();
            targetMapping.setEnabled(true);
            targetMapping.setType(GENERIC_TYPE.SPATIAL);

            if (!hasSpatialField) {
                targetMapping.setFilterable(true);
                this.hasSpatialField = true;
            }

            return targetMapping;
        }

        return null;
    }
}