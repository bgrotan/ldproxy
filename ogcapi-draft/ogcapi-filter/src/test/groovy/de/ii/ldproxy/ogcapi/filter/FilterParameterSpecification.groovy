/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.filter

import de.ii.xtraplatform.cql.app.CqlImpl
import de.ii.xtraplatform.cql.domain.Cql
import java.util.stream.IntStream
import groovyx.net.http.ContentType
import groovyx.net.http.Method
import groovyx.net.http.RESTClient
import spock.lang.Requires
import spock.lang.Shared
import spock.lang.Specification

/** These test assume that the following APIs are available:
 * <ul>
 *     <li>daraa (same data and configuration as https://demo.ldproxy.net/daraa)
 * </ul>
 */
@Requires({env['SUT_URL'] != null})
class FilterParameterSpecification extends Specification {

    static final String SUT_URL = System.getenv('SUT_URL')
    static final String API_PATH_DARAA = "/daraa"
    static final String CULTURE_PNT = "CulturePnt"
    static final String TRANSPORTATION_GROUND_CRV = "TransportationGroundCrv"
    static final String CULTURE_PNT_PATH = API_PATH_DARAA + "/collections/" + CULTURE_PNT + "/items"
    static final String API_PATH_GEOINFODOK = "/geoinfodok"
    static final String AX_GEBAEUDEFUNKTION = "ax_gebaeudefunktion"
    static final String AX_GEBAEUDEFUNKTION_PATH = API_PATH_GEOINFODOK + "/collections/" + AX_GEBAEUDEFUNKTION + "/items"
    static final String GEO_JSON = "application/geo+json";
    static final String JSON = "application/json";

    @Shared
    Cql cql = new CqlImpl()
    @Shared
    boolean json = false // set to true to test CQL JSON, to false to test CQL Text
    @Shared
    int limit = 250
    @Shared
    RESTClient restClient = new RESTClient(SUT_URL)
    @Shared
    def allCulturePntFeatures = getRequest(restClient, CULTURE_PNT_PATH, null)
    @Shared
    def collection = getRequest(restClient, API_PATH_DARAA + "/collections/" + CULTURE_PNT, null)
    @Shared
    def envelopeCollection = "ENVELOPE(" + String.join(",", collection.responseData.extent.spatial.bbox[0].stream().map( n -> String.valueOf(n)).toList()) + ")"
    @Shared
    def id = allCulturePntFeatures.responseData.features[0].id
    @Shared
    def lon = allCulturePntFeatures.responseData.features[0].geometry.coordinates[0] as double
    @Shared
    def lat = allCulturePntFeatures.responseData.features[0].geometry.coordinates[1] as double
    @Shared
    def delta = 0.01
    @Shared
    def envelopeFeature = "ENVELOPE(" + String.join(",", String.valueOf(lon-delta), String.valueOf(lat-delta), String.valueOf(lon+delta), String.valueOf(lat+delta)) + ")"
    @Shared
    def polygonFeature = "POLYGON((" + String.join(",",
            String.valueOf(lon-delta)+" "+String.valueOf(lat),
            String.valueOf(lon)+" "+String.valueOf(lat-delta),
            String.valueOf(lon+delta)+" "+String.valueOf(lat),
            String.valueOf(lon)+" "+String.valueOf(lat+delta),
            String.valueOf(lon-delta)+" "+String.valueOf(lat)) + "))"
    @Shared
    def pointFeature = "POINT(" + String.valueOf(lon) + " " + String.valueOf(lat) + ")"
    @Shared
    def epsg4326 = "http://www.opengis.net/def/crs/EPSG/0/4326"
    @Shared
    def envelopeFeature4326 = "ENVELOPE(" + String.join(",", String.valueOf(lat-delta), String.valueOf(lon-delta), String.valueOf(lat+delta), String.valueOf(lon+delta)) + ")"
    @Shared
    def polygonFeature4326 = "POLYGON((" + String.join(",",
            String.valueOf(lat)+" "+String.valueOf(lon-delta),
            String.valueOf(lat-delta)+" "+String.valueOf(lon),
            String.valueOf(lat)+" "+String.valueOf(lon+delta),
            String.valueOf(lat+delta)+" "+String.valueOf(lon),
            String.valueOf(lat)+" "+String.valueOf(lon-delta)) + "))"
    @Shared
    def pointFeature4326 = "POINT(" + String.valueOf(lat) + " " + String.valueOf(lon) + ")"
    @Shared
    def allAxGebaeudefunktion = getRequest(restClient, AX_GEBAEUDEFUNKTION_PATH, [limit:limit])

    def "Preconditions Daraa"() {
        given: "CulturePnt features in the Daraa dataset"

        when: "Fetch all features"

        then: "Success and returns JSON"
        assertSuccess(allCulturePntFeatures)
        assertSuccess(collection)
    }

    def "Preconditions GeoInfoDok"() {
        given: "AX_Gebaeudefunktion records"

        when: "Fetch all records"

        then: "Success and returns JSON"
        assertSuccess(allAxGebaeudefunktion)
    }

    // Comparison predicates

    def "Operator eq"() {
        given: "CulturePnt features in the Daraa dataset"

        when: "1. Data is selected using a filter F_CODE=F_CODE"
        def twoProperties = getRequest(restClient, CULTURE_PNT_PATH, getQuery("F_CODE=F_CODE"))

        then: "Success and returns GeoJSON"
        assertSuccess(twoProperties)

        and: "Returns all features"
        twoProperties.responseData.numberReturned == allCulturePntFeatures.responseData.numberReturned

        when: "2. Data is selected using a filter F_CODE='AL030'"
        def propertyAndLiteralString = getRequest(restClient, CULTURE_PNT_PATH, getQuery("F_CODE='AL030'"))
        def propertyAndLiteralStringCheck = allCulturePntFeatures.responseData.features.stream().filter(f -> f.properties.F_CODE=='AL030' ).toList()

        then: "Success and returns GeoJSON"
        assertSuccess(propertyAndLiteralString)

        and: "Returns the same number of features"
        propertyAndLiteralString.responseData.numberReturned == propertyAndLiteralStringCheck.size()

        and: "Returns the same feature arrays"
        for (int i=0; i<propertyAndLiteralString.responseData.numberReturned; i++) {
            assertFeature(propertyAndLiteralString.responseData.features[i], propertyAndLiteralStringCheck.get(i))
        }

        when: "3. Data is selected using F_CODE=AL030"
        def usingQueryParam = getRequest(restClient, CULTURE_PNT_PATH, [F_CODE:"AL030"])

        then: "Success and returns GeoJSON"
        assertSuccess(usingQueryParam)

        and: "Returns the same number of features as a filter"
        propertyAndLiteralString.responseData.numberReturned == usingQueryParam.responseData.numberReturned

        and: "Returns the same feature arrays"
        for (int i=0; i<propertyAndLiteralString.responseData.numberReturned; i++) {
            assertFeature(propertyAndLiteralString.responseData.features[i], usingQueryParam.responseData.features[i])
        }

        when: "4. Data is selected using a filter ZI037_REL=11"
        def propertyAndLiteralNumeric = getRequest(restClient, CULTURE_PNT_PATH, getQuery("ZI037_REL=11"))
        def propertyAndLiteralNumericCheck = allCulturePntFeatures.responseData.features.stream().filter(f -> f.properties.ZI037_REL==11 ).toList()

        then: "Success and returns GeoJSON"
        assertSuccess(propertyAndLiteralNumeric)

        and: "Returns the same number of features"
        propertyAndLiteralNumeric.responseData.numberReturned == propertyAndLiteralNumericCheck.size()

        and: "Returns the expected features"
        for (int i=0; i<propertyAndLiteralNumeric.responseData.numberReturned; i++) {
            assertFeature(propertyAndLiteralNumeric.responseData.features[i], propertyAndLiteralNumericCheck.get(i))
        }

        when: "5. Data is selected using a filter 'AL030'=F_CODE"
        def literalAndProperty = getRequest(restClient, CULTURE_PNT_PATH, getQuery("'AL030'=F_CODE"))

        then: "Success and returns GeoJSON"
        assertSuccess(literalAndProperty)

        and: "Returns one feature"
        literalAndProperty.responseData.numberReturned == propertyAndLiteralString.responseData.numberReturned

        and: "Returns the expected feature"
        for (int i=0; i<literalAndProperty.responseData.numberReturned; i++) {
            assertFeature(literalAndProperty.responseData.features[i], propertyAndLiteralStringCheck.get(i))
        }

        when: "6. Data is selected using a filter 'A'='A'"
        def literals = getRequest(restClient, CULTURE_PNT_PATH, getQuery("'A'='A'"))

        then: "Success and returns GeoJSON"
        assertSuccess(literals)

        and: "Returns all features"
        literals.responseData.numberReturned == allCulturePntFeatures.responseData.numberReturned

        when: "7. Data is selected using a filter \"F_CODE\"='AL030' with a double quote"
        def propertyAndLiteral2String = getRequest(restClient, CULTURE_PNT_PATH, getQuery("\"F_CODE\"='AL030'"))

        then: "Success and returns GeoJSON"
        assertSuccess(propertyAndLiteral2String)

        and: "Returns the same number of features"
        propertyAndLiteral2String.responseData.numberReturned == propertyAndLiteralStringCheck.size()

        and: "Returns the same feature arrays"
        for (int i=0; i<propertyAndLiteral2String.responseData.numberReturned; i++) {
            assertFeature(propertyAndLiteral2String.responseData.features[i], propertyAndLiteralStringCheck.get(i))
        }

        when: "8. Data is selected using a filter ZI001_SDV='2011-12-26T20:55:27Z'"
        def temporalProperty = getRequest(restClient, CULTURE_PNT_PATH, getQuery("ZI001_SDV='2011-12-26T20:55:27Z'"))
        def temporalPropertyCheck = allCulturePntFeatures.responseData.features.stream().filter(f -> f.properties.ZI001_SDV=='2011-12-26T20:55:27Z' ).toList()

        then: "Success and returns GeoJSON"
        assertSuccess(temporalProperty)

        and: "Returns the same number of features"
        temporalProperty.responseData.numberReturned == temporalPropertyCheck.size()

        and: "Returns the same feature arrays"
        for (int i=0; i<temporalProperty.responseData.numberReturned; i++) {
            assertFeature(temporalProperty.responseData.features[i], temporalPropertyCheck.get(i))
        }
    }

    def "Operator neq"() {
        given: "CulturePnt features in the Daraa dataset"

        when: "1. Data is selected using a filter F_CODE<>F_CODE"
        def twoProperties = getRequest(restClient, CULTURE_PNT_PATH, getQuery("F_CODE<>F_CODE"))

        then: "Success and returns GeoJSON"
        assertSuccess(twoProperties)

        and: "Returns no features"
        twoProperties.responseData.numberReturned == 0

        when: "2. Data is selected using a filter F_CODE<>'AL030'"
        def propertyAndLiteralString = getRequest(restClient, CULTURE_PNT_PATH, getQuery("F_CODE<>'AL030'"))
        def propertyAndLiteralStringCheck = allCulturePntFeatures.responseData.features.stream().filter( f -> f.properties.F_CODE!='AL030' ).toList()

        then: "Success and returns GeoJSON"
        assertSuccess(propertyAndLiteralString)

        and: "Returns the same number of features"
        propertyAndLiteralString.responseData.numberReturned == propertyAndLiteralStringCheck.size()

        and: "Returns the same feature arrays"
        for (int i=0; i<propertyAndLiteralString.responseData.numberReturned; i++) {
            assertFeature(propertyAndLiteralString.responseData.features[i], propertyAndLiteralStringCheck.get(i))
        }

        when: "3. Data is selected using a filter ZI037_REL<>11"
        def propertyAndLiteralNumeric = getRequest(restClient, CULTURE_PNT_PATH, getQuery("ZI037_REL<>11"))
        def propertyAndLiteralNumericCheck = allCulturePntFeatures.responseData.features.stream().filter( f -> Objects.nonNull(f.properties.ZI037_REL) && f.properties.ZI037_REL!=11 ).toList()

        then: "Success and returns GeoJSON"
        assertSuccess(propertyAndLiteralNumeric)

        and: "Returns the same number of features"
        propertyAndLiteralNumeric.responseData.numberReturned == propertyAndLiteralNumericCheck.size()

        and: "Returns the expected features"
        for (int i=0; i<propertyAndLiteralNumeric.responseData.numberReturned; i++) {
            assertFeature(propertyAndLiteralNumeric.responseData.features[i], propertyAndLiteralNumericCheck.get(i))
        }

        when: "4. Data is selected using a filter ZI037_REL<>10"
        def propertyAndLiteralNumeric2 = getRequest(restClient, CULTURE_PNT_PATH, getQuery("ZI037_REL<>10"))
        def propertyAndLiteralNumeric2Check = allCulturePntFeatures.responseData.features.stream().filter( f -> Objects.nonNull(f.properties.ZI037_REL) && f.properties.ZI037_REL!=10 ).toList()

        then: "Success and returns GeoJSON"
        assertSuccess(propertyAndLiteralNumeric2)

        and: "Returns the same number of features"
        propertyAndLiteralNumeric2.responseData.numberReturned == propertyAndLiteralNumeric2Check.size()

        and: "Returns the expected features"
        for (int i=0; i<propertyAndLiteralNumeric2.responseData.numberReturned; i++) {
            assertFeature(propertyAndLiteralNumeric2.responseData.features[i], propertyAndLiteralNumeric2Check.get(i))
        }

        when: "5. Data is selected using a filter 'AL030'<>F_CODE"
        def literalAndProperty = getRequest(restClient, CULTURE_PNT_PATH, getQuery("'AL030'<>F_CODE"))

        then: "Success and returns GeoJSON"
        assertSuccess(literalAndProperty)

        and: "Returns the same number of features"
        literalAndProperty.responseData.numberReturned == propertyAndLiteralString.responseData.numberReturned

        and: "Returns the expected features"
        for (int i=0; i<literalAndProperty.responseData.numberReturned; i++) {
            assertFeature(literalAndProperty.responseData.features[i], propertyAndLiteralStringCheck.get(i))
        }

        when: "6. Data is selected using a filter 'A'<>'A'"
        def literals = getRequest(restClient, CULTURE_PNT_PATH, getQuery("'A'<>'A'"))

        then: "Success and returns GeoJSON"
        assertSuccess(literals)

        and: "Returns no features"
        literals.responseData.numberReturned == 0

        when: "7. Data is selected using a filter ZI001_SDV<>'2011-12-26T20:55:27Z'"
        def temporalProperty = getRequest(restClient, CULTURE_PNT_PATH, getQuery("ZI001_SDV<>'2011-12-26T20:55:27Z'"))
        def temporalPropertyCheck = allCulturePntFeatures.responseData.features.stream().filter(f -> f.properties.ZI001_SDV!='2011-12-26T20:55:27Z' ).toList()

        then: "Success and returns GeoJSON"
        assertSuccess(temporalProperty)

        and: "Returns the same number of features"
        temporalProperty.responseData.numberReturned == temporalPropertyCheck.size()

        and: "Returns the same feature arrays"
        for (int i=0; i<temporalProperty.responseData.numberReturned; i++) {
            assertFeature(temporalProperty.responseData.features[i], temporalPropertyCheck.get(i))
        }
    }

    def "Operator lt"() {
        given: "CulturePnt features in the Daraa dataset"

        when: "1. Data is selected using a filter F_CODE<F_CODE"
        def twoProperties = getRequest(restClient, CULTURE_PNT_PATH, getQuery("F_CODE<F_CODE"))

        then: "Success and returns GeoJSON"
        assertSuccess(twoProperties)

        and: "Returns no features"
        twoProperties.responseData.numberReturned == 0

        when: "2. Data is selected using a filter F_CODE<'AL030'"
        def propertyAndLiteralString = getRequest(restClient, CULTURE_PNT_PATH, getQuery("F_CODE<'AL030'"))
        def propertyAndLiteralStringCheck = allCulturePntFeatures.responseData.features.stream().filter( f -> f.properties.F_CODE<'AL030' ).toList()

        then: "Success and returns GeoJSON"
        assertSuccess(propertyAndLiteralString)

        and: "Returns the same number of features"
        propertyAndLiteralString.responseData.numberReturned == propertyAndLiteralStringCheck.size()

        and: "Returns the same feature arrays"
        for (int i=0; i<propertyAndLiteralString.responseData.numberReturned; i++) {
            assertFeature(propertyAndLiteralString.responseData.features[i], propertyAndLiteralStringCheck.get(i))
        }

        when: "3. Data is selected using a filter ZI037_REL<11"
        def propertyAndLiteralNumeric = getRequest(restClient, CULTURE_PNT_PATH, getQuery("ZI037_REL<11"))
        def propertyAndLiteralNumericCheck = allCulturePntFeatures.responseData.features.stream().filter( f -> Objects.nonNull(f.properties.ZI037_REL) && f.properties.ZI037_REL<11 ).toList()

        then: "Success and returns GeoJSON"
        assertSuccess(propertyAndLiteralNumeric)

        and: "Returns the same number of features"
        propertyAndLiteralNumeric.responseData.numberReturned == propertyAndLiteralNumericCheck.size()

        and: "Returns the expected features"
        for (int i=0; i<propertyAndLiteralNumeric.responseData.numberReturned; i++) {
            assertFeature(propertyAndLiteralNumeric.responseData.features[i], propertyAndLiteralNumericCheck.get(i))
        }

        when: "4. Data is selected using a filter ZI037_REL<12"
        def propertyAndLiteralNumeric2 = getRequest(restClient, CULTURE_PNT_PATH, getQuery("ZI037_REL<12"))
        def propertyAndLiteralNumeric2Check = allCulturePntFeatures.responseData.features.stream().filter( f -> Objects.nonNull(f.properties.ZI037_REL) && f.properties.ZI037_REL<12 ).toList()

        then: "Success and returns GeoJSON"
        assertSuccess(propertyAndLiteralNumeric2)

        and: "Returns the same number of features"
        propertyAndLiteralNumeric2.responseData.numberReturned == propertyAndLiteralNumeric2Check.size()

        and: "Returns the expected features"
        for (int i=0; i<propertyAndLiteralNumeric2.responseData.numberReturned; i++) {
            assertFeature(propertyAndLiteralNumeric2.responseData.features[i], propertyAndLiteralNumeric2Check.get(i))
        }

        when: "5. Data is selected using a filter 'AL030'<F_CODE"
        def literalAndProperty = getRequest(restClient, CULTURE_PNT_PATH, getQuery("'AL030'<F_CODE"))
        def literalAndPropertyCheck = allCulturePntFeatures.responseData.features.stream().filter( f -> f.properties.F_CODE>'AL030' ).toList()

        then: "Success and returns GeoJSON"
        assertSuccess(literalAndProperty)

        and: "Returns the same number of features"
        literalAndProperty.responseData.numberReturned == propertyAndLiteralString.responseData.numberReturned

        and: "Returns the expected features"
        for (int i=0; i<literalAndProperty.responseData.numberReturned; i++) {
            assertFeature(literalAndProperty.responseData.features[i], literalAndPropertyCheck.get(i))
        }

        when: "6. Data is selected using a filter 'A'<'A'"
        def literals = getRequest(restClient, CULTURE_PNT_PATH, getQuery("'A'<'A'"))

        then: "Success and returns GeoJSON"
        assertSuccess(literals)

        and: "Returns no features"
        literals.responseData.numberReturned == 0

        when: "7. Data is selected using a filter ZI001_SDV<'2011-12-26T20:55:27Z'"
        def temporalProperty = getRequest(restClient, CULTURE_PNT_PATH, getQuery("ZI001_SDV<'2011-12-26T20:55:27Z'"))
        def temporalPropertyCheck = allCulturePntFeatures.responseData.features.stream().filter(f -> f.properties.ZI001_SDV<'2011-12-26T20:55:27Z' ).toList()

        then: "Success and returns GeoJSON"
        assertSuccess(temporalProperty)

        and: "Returns the same number of features"
        temporalProperty.responseData.numberReturned == temporalPropertyCheck.size()

        and: "Returns the same feature arrays"
        for (int i=0; i<temporalProperty.responseData.numberReturned; i++) {
            assertFeature(temporalProperty.responseData.features[i], temporalPropertyCheck.get(i))
        }
    }

    def "Operator gt"() {
        given: "CulturePnt features in the Daraa dataset"

        when: "1. Data is selected using a filter F_CODE>F_CODE"
        def twoProperties = getRequest(restClient, CULTURE_PNT_PATH, getQuery("F_CODE>F_CODE"))

        then: "Success and returns GeoJSON"
        assertSuccess(twoProperties)

        and: "Returns no features"
        twoProperties.responseData.numberReturned == 0

        when: "2. Data is selected using a filter F_CODE>'AL030'"
        def propertyAndLiteralString = getRequest(restClient, CULTURE_PNT_PATH, getQuery("F_CODE>'AL030'"))
        def propertyAndLiteralStringCheck = allCulturePntFeatures.responseData.features.stream().filter( f -> f.properties.F_CODE>'AL030' ).toList()

        then: "Success and returns GeoJSON"
        assertSuccess(propertyAndLiteralString)

        and: "Returns the same number of features"
        propertyAndLiteralString.responseData.numberReturned == propertyAndLiteralStringCheck.size()

        and: "Returns the same feature arrays"
        for (int i=0; i<propertyAndLiteralString.responseData.numberReturned; i++) {
            assertFeature(propertyAndLiteralString.responseData.features[i], propertyAndLiteralStringCheck.get(i))
        }

        when: "3. Data is selected using a filter ZI037_REL>11"
        def propertyAndLiteralNumeric = getRequest(restClient, CULTURE_PNT_PATH, getQuery("ZI037_REL>11"))
        def propertyAndLiteralNumericCheck = allCulturePntFeatures.responseData.features.stream().filter( f -> Objects.nonNull(f.properties.ZI037_REL) && f.properties.ZI037_REL>11 ).toList()

        then: "Success and returns GeoJSON"
        assertSuccess(propertyAndLiteralNumeric)

        and: "Returns the same number of features"
        propertyAndLiteralNumeric.responseData.numberReturned == propertyAndLiteralNumericCheck.size()

        and: "Returns the expected features"
        for (int i=0; i<propertyAndLiteralNumeric.responseData.numberReturned; i++) {
            assertFeature(propertyAndLiteralNumeric.responseData.features[i], propertyAndLiteralNumericCheck.get(i))
        }

        when: "4. Data is selected using a filter ZI037_REL>0"
        def propertyAndLiteralNumeric2 = getRequest(restClient, CULTURE_PNT_PATH, getQuery("ZI037_REL>0"))
        def propertyAndLiteralNumeric2Check = allCulturePntFeatures.responseData.features.stream().filter( f -> Objects.nonNull(f.properties.ZI037_REL) && f.properties.ZI037_REL>0 ).toList()

        then: "Success and returns GeoJSON"
        assertSuccess(propertyAndLiteralNumeric2)

        and: "Returns the same number of features"
        propertyAndLiteralNumeric2.responseData.numberReturned == propertyAndLiteralNumeric2Check.size()

        and: "Returns the expected features"
        for (int i=0; i<propertyAndLiteralNumeric2.responseData.numberReturned; i++) {
            assertFeature(propertyAndLiteralNumeric2.responseData.features[i], propertyAndLiteralNumeric2Check.get(i))
        }

        when: "5. Data is selected using a filter 'AL030'>F_CODE"
        def literalAndProperty = getRequest(restClient, CULTURE_PNT_PATH, getQuery("'AL030'>F_CODE"))
        def literalAndPropertyCheck = allCulturePntFeatures.responseData.features.stream().filter( f -> f.properties.F_CODE<'AL030' ).toList()

        then: "Success and returns GeoJSON"
        assertSuccess(literalAndProperty)

        and: "Returns the same number of features"
        literalAndProperty.responseData.numberReturned == propertyAndLiteralString.responseData.numberReturned

        and: "Returns the expected features"
        for (int i=0; i<literalAndProperty.responseData.numberReturned; i++) {
            assertFeature(literalAndProperty.responseData.features[i], literalAndPropertyCheck.get(i))
        }

        when: "6. Data is selected using a filter 'A'>'A'"
        def literals = getRequest(restClient, CULTURE_PNT_PATH, getQuery("'A'>'A'"))

        then: "Success and returns GeoJSON"
        assertSuccess(literals)

        and: "Returns n0 features"
        literals.responseData.numberReturned == 0

        when: "7. Data is selected using a filter ZI001_SDV>'2011-12-26T20:55:27Z'"
        def temporalProperty = getRequest(restClient, CULTURE_PNT_PATH, getQuery("ZI001_SDV>'2011-12-26T20:55:27Z'"))
        def temporalPropertyCheck = allCulturePntFeatures.responseData.features.stream().filter(f -> f.properties.ZI001_SDV>'2011-12-26T20:55:27Z' ).toList()

        then: "Success and returns GeoJSON"
        assertSuccess(temporalProperty)

        and: "Returns the same number of features"
        temporalProperty.responseData.numberReturned == temporalPropertyCheck.size()

        and: "Returns the same feature arrays"
        for (int i=0; i<temporalProperty.responseData.numberReturned; i++) {
            assertFeature(temporalProperty.responseData.features[i], temporalPropertyCheck.get(i))
        }
    }

    def "Operator lteq"() {
        given: "CulturePnt features in the Daraa dataset"

        when: "1. Data is selected using a filter F_CODE<=F_CODE"
        def twoProperties = getRequest(restClient, CULTURE_PNT_PATH, getQuery("F_CODE<=F_CODE"))

        then: "Success and returns GeoJSON"
        assertSuccess(twoProperties)

        and: "Returns all features"
        twoProperties.responseData.numberReturned == allCulturePntFeatures.responseData.numberReturned

        when: "2. Data is selected using a filter F_CODE<='AL030'"
        def propertyAndLiteralString = getRequest(restClient, CULTURE_PNT_PATH, getQuery("F_CODE<='AL030'"))
        def propertyAndLiteralStringCheck = allCulturePntFeatures.responseData.features.stream().filter( f -> f.properties.F_CODE<='AL030' ).toList()

        then: "Success and returns GeoJSON"
        assertSuccess(propertyAndLiteralString)

        and: "Returns the same number of features"
        propertyAndLiteralString.responseData.numberReturned == propertyAndLiteralStringCheck.size()

        and: "Returns the same feature arrays"
        for (int i=0; i<propertyAndLiteralString.responseData.numberReturned; i++) {
            assertFeature(propertyAndLiteralString.responseData.features[i], propertyAndLiteralStringCheck.get(i))
        }

        when: "3. Data is selected using a filter ZI037_REL<=11"
        def propertyAndLiteralNumeric = getRequest(restClient, CULTURE_PNT_PATH, getQuery("ZI037_REL<=11"))
        def propertyAndLiteralNumericCheck = allCulturePntFeatures.responseData.features.stream().filter( f -> Objects.nonNull(f.properties.ZI037_REL) && f.properties.ZI037_REL<=11 ).toList()

        then: "Success and returns GeoJSON"
        assertSuccess(propertyAndLiteralNumeric)

        and: "Returns the same number of features"
        propertyAndLiteralNumeric.responseData.numberReturned == propertyAndLiteralNumericCheck.size()

        and: "Returns the expected features"
        for (int i=0; i<propertyAndLiteralNumeric.responseData.numberReturned; i++) {
            assertFeature(propertyAndLiteralNumeric.responseData.features[i], propertyAndLiteralNumericCheck.get(i))
        }

        when: "4. Data is selected using a filter ZI037_REL<=10"
        def propertyAndLiteralNumeric2 = getRequest(restClient, CULTURE_PNT_PATH, getQuery("ZI037_REL<=10"))
        def propertyAndLiteralNumeric2Check = allCulturePntFeatures.responseData.features.stream().filter( f -> Objects.nonNull(f.properties.ZI037_REL) && f.properties.ZI037_REL<=10 ).toList()

        then: "Success and returns GeoJSON"
        assertSuccess(propertyAndLiteralNumeric2)

        and: "Returns the same number of features"
        propertyAndLiteralNumeric2.responseData.numberReturned == propertyAndLiteralNumeric2Check.size()

        and: "Returns the expected features"
        for (int i=0; i<propertyAndLiteralNumeric2.responseData.numberReturned; i++) {
            assertFeature(propertyAndLiteralNumeric2.responseData.features[i], propertyAndLiteralNumeric2Check.get(i))
        }

        when: "5. Data is selected using a filter 'AL030'<=F_CODE"
        def literalAndProperty = getRequest(restClient, CULTURE_PNT_PATH, getQuery("'AL030'<=F_CODE"))
        def literalAndPropertyCheck = allCulturePntFeatures.responseData.features.stream().filter( f -> f.properties.F_CODE>='AL030' ).toList()

        then: "Success and returns GeoJSON"
        assertSuccess(literalAndProperty)

        and: "Returns the same number of features"
        literalAndProperty.responseData.numberReturned == propertyAndLiteralString.responseData.numberReturned

        and: "Returns the expected features"
        for (int i=0; i<literalAndProperty.responseData.numberReturned; i++) {
            assertFeature(literalAndProperty.responseData.features[i], literalAndPropertyCheck.get(i))
        }

        when: "6. Data is selected using a filter 'A'<='A'"
        def literals = getRequest(restClient, CULTURE_PNT_PATH, getQuery("'A'<='A'"))

        then: "Success and returns GeoJSON"
        assertSuccess(literals)

        and: "Returns all features"
        literals.responseData.numberReturned == allCulturePntFeatures.responseData.numberReturned

        when: "7. Data is selected using a filter ZI001_SDV<='2011-12-26T20:55:27Z'"
        def temporalProperty = getRequest(restClient, CULTURE_PNT_PATH, getQuery("ZI001_SDV<='2011-12-26T20:55:27Z'"))
        def temporalPropertyCheck = allCulturePntFeatures.responseData.features.stream().filter(f -> f.properties.ZI001_SDV<='2011-12-26T20:55:27Z' ).toList()

        then: "Success and returns GeoJSON"
        assertSuccess(temporalProperty)

        and: "Returns the same number of features"
        temporalProperty.responseData.numberReturned == temporalPropertyCheck.size()

        and: "Returns the same feature arrays"
        for (int i=0; i<temporalProperty.responseData.numberReturned; i++) {
            assertFeature(temporalProperty.responseData.features[i], temporalPropertyCheck.get(i))
        }
    }

    def "Operator gteq"() {
        given: "CulturePnt features in the Daraa dataset"

        when: "1. Data is selected using a filter F_CODE>=F_CODE"
        def twoProperties = getRequest(restClient, CULTURE_PNT_PATH, getQuery("F_CODE>=F_CODE"))

        then: "Success and returns GeoJSON"
        assertSuccess(twoProperties)

        and: "Returns all features"
        twoProperties.responseData.numberReturned == allCulturePntFeatures.responseData.numberReturned

        when: "2. Data is selected using a filter F_CODE<='AL030'"
        def propertyAndLiteralString = getRequest(restClient, CULTURE_PNT_PATH, getQuery("F_CODE>='AL030'"))
        def propertyAndLiteralStringCheck = allCulturePntFeatures.responseData.features.stream().filter( f -> f.properties.F_CODE>='AL030' ).toList()

        then: "Success and returns GeoJSON"
        assertSuccess(propertyAndLiteralString)

        and: "Returns the same number of features"
        propertyAndLiteralString.responseData.numberReturned == propertyAndLiteralStringCheck.size()

        and: "Returns the same feature arrays"
        for (int i=0; i<propertyAndLiteralString.responseData.numberReturned; i++) {
            assertFeature(propertyAndLiteralString.responseData.features[i], propertyAndLiteralStringCheck.get(i))
        }

        when: "3. Data is selected using a filter ZI037_REL>=11"
        def propertyAndLiteralNumeric = getRequest(restClient, CULTURE_PNT_PATH, getQuery("ZI037_REL>=11"))
        def propertyAndLiteralNumericCheck = allCulturePntFeatures.responseData.features.stream().filter( f -> Objects.nonNull(f.properties.ZI037_REL) && f.properties.ZI037_REL>=11 ).toList()

        then: "Success and returns GeoJSON"
        assertSuccess(propertyAndLiteralNumeric)

        and: "Returns the same number of features"
        propertyAndLiteralNumeric.responseData.numberReturned == propertyAndLiteralNumericCheck.size()

        and: "Returns the expected features"
        for (int i=0; i<propertyAndLiteralNumeric.responseData.numberReturned; i++) {
            assertFeature(propertyAndLiteralNumeric.responseData.features[i], propertyAndLiteralNumericCheck.get(i))
        }

        when: "4. Data is selected using a filter ZI037_REL>=12"
        def propertyAndLiteralNumeric2 = getRequest(restClient, CULTURE_PNT_PATH, getQuery("ZI037_REL>=12"))
        def propertyAndLiteralNumeric2Check = allCulturePntFeatures.responseData.features.stream().filter( f -> Objects.nonNull(f.properties.ZI037_REL) && f.properties.ZI037_REL>=12 ).toList()

        then: "Success and returns GeoJSON"
        assertSuccess(propertyAndLiteralNumeric2)

        and: "Returns the same number of features"
        propertyAndLiteralNumeric2.responseData.numberReturned == propertyAndLiteralNumeric2Check.size()

        and: "Returns the expected features"
        for (int i=0; i<propertyAndLiteralNumeric2.responseData.numberReturned; i++) {
            assertFeature(propertyAndLiteralNumeric2.responseData.features[i], propertyAndLiteralNumeric2Check.get(i))
        }

        when: "5. Data is selected using a filter 'AL030'>=F_CODE"
        def literalAndProperty = getRequest(restClient, CULTURE_PNT_PATH, getQuery("'AL030'>=F_CODE"))
        def literalAndPropertyCheck = allCulturePntFeatures.responseData.features.stream().filter( f -> f.properties.F_CODE<='AL030' ).toList()

        then: "Success and returns GeoJSON"
        assertSuccess(literalAndProperty)

        and: "Returns the same number of features"
        literalAndProperty.responseData.numberReturned == propertyAndLiteralString.responseData.numberReturned

        and: "Returns the expected features"
        for (int i=0; i<literalAndProperty.responseData.numberReturned; i++) {
            assertFeature(literalAndProperty.responseData.features[i], literalAndPropertyCheck.get(i))
        }

        when: "6. Data is selected using a filter 'A'>='A'"
        def literals = getRequest(restClient, CULTURE_PNT_PATH, getQuery("'A'>='A'"))

        then: "Success and returns GeoJSON"
        assertSuccess(literals)

        and: "Returns all features"
        literals.responseData.numberReturned == allCulturePntFeatures.responseData.numberReturned

        when: "7. Data is selected using a filter ZI001_SDV>='2011-12-26T20:55:27Z'"
        def temporalProperty = getRequest(restClient, CULTURE_PNT_PATH, getQuery("ZI001_SDV>='2011-12-26T20:55:27Z'"))
        def temporalPropertyCheck = allCulturePntFeatures.responseData.features.stream().filter(f -> f.properties.ZI001_SDV>='2011-12-26T20:55:27Z' ).toList()

        then: "Success and returns GeoJSON"
        assertSuccess(temporalProperty)

        and: "Returns the same number of features"
        temporalProperty.responseData.numberReturned == temporalPropertyCheck.size()

        and: "Returns the same feature arrays"
        for (int i=0; i<temporalProperty.responseData.numberReturned; i++) {
            assertFeature(temporalProperty.responseData.features[i], temporalPropertyCheck.get(i))
        }
    }

    def "Operator like"() {
        given: "CulturePnt features in the Daraa dataset"

        when: "1. Data is selected using a filter F_CODE LiKe F_CODE"
        def twoProperties = getRequest(restClient, CULTURE_PNT_PATH, getQuery("F_CODE LiKe F_CODE"))

        then: "Success and returns GeoJSON"
        assertSuccess(twoProperties)

        and: "Returns all features"
        twoProperties.responseData.numberReturned == allCulturePntFeatures.responseData.numberReturned

        when: "2. Data is selected using a filter F_CODE LiKe 'AL0%'"
        def propertyAndLiteralString = getRequest(restClient, CULTURE_PNT_PATH, getQuery("F_CODE LiKe 'AL0%'"))
        def propertyAndLiteralStringCheck = allCulturePntFeatures.responseData.features.stream().filter( f -> f.properties.F_CODE.startsWith('AL0') ).toList()

        then: "Success and returns GeoJSON"
        assertSuccess(propertyAndLiteralString)

        and: "Returns the same number of features"
        propertyAndLiteralString.responseData.numberReturned == propertyAndLiteralStringCheck.size()

        and: "Returns the same feature arrays"
        for (int i=0; i<propertyAndLiteralString.responseData.numberReturned; i++) {
            assertFeature(propertyAndLiteralString.responseData.features[i], propertyAndLiteralStringCheck.get(i))
        }

        when: "3. Data is selected using a filter F_CODE LiKe 'AL0*' wildCard '*'"
        def propertyAndLiteralString2 = getRequest(restClient, CULTURE_PNT_PATH, getQuery("F_CODE LiKe 'AL0*' wildCard '*'"))

        then: "Success and returns GeoJSON"
        assertSuccess(propertyAndLiteralString2)

        and: "Returns the same number of features"
        propertyAndLiteralString2.responseData.numberReturned == propertyAndLiteralStringCheck.size()

        and: "Returns the same feature arrays"
        for (int i=0; i<propertyAndLiteralString2.responseData.numberReturned; i++) {
            assertFeature(propertyAndLiteralString2.responseData.features[i], propertyAndLiteralStringCheck.get(i))
        }

        when: "4. Data is selected using a filter F_CODE LiKe 'AL0..' singleChar '.'"
        def propertyAndLiteralString3 = getRequest(restClient, CULTURE_PNT_PATH, getQuery("F_CODE LiKe 'AL0..' singleChar '.'"))

        then: "Success and returns GeoJSON"
        assertSuccess(propertyAndLiteralString3)

        and: "Returns the same number of features"
        propertyAndLiteralString3.responseData.numberReturned == propertyAndLiteralStringCheck.size()

        and: "Returns the same feature arrays"
        for (int i=0; i<propertyAndLiteralString3.responseData.numberReturned; i++) {
            assertFeature(propertyAndLiteralString3.responseData.features[i], propertyAndLiteralStringCheck.get(i))
        }

        when: "5. Data is selected using a filter F_CODE LiKe 'al0..' singleChar '.' ESCAPECHAR '?'"
        def propertyAndLiteralString4 = getRequest(restClient, CULTURE_PNT_PATH, getQuery("F_CODE LiKe 'al0..' singleChar '.' ESCAPECHAR '?'"))

        then: "Success and returns GeoJSON"
        assertSuccess(propertyAndLiteralString4)

        and: "Returns the same number of features"
        propertyAndLiteralString4.responseData.numberReturned == propertyAndLiteralStringCheck.size()

        and: "Returns the same feature arrays"
        for (int i=0; i<propertyAndLiteralString4.responseData.numberReturned; i++) {
            assertFeature(propertyAndLiteralString4.responseData.features[i], propertyAndLiteralStringCheck.get(i))
        }

        when: "6. Data is selected using a filter F_CODE LiKe 'al0%' NoCasE true"
        def propertyAndLiteralString5 = getRequest(restClient, CULTURE_PNT_PATH, getQuery("F_CODE LiKe 'al0%' NoCasE true"))
        def propertyAndLiteralString5Check = allCulturePntFeatures.responseData.features.stream().filter( f -> f.properties.F_CODE.toLowerCase().startsWith('al0') ).toList()

        then: "Success and returns GeoJSON"
        assertSuccess(propertyAndLiteralString5)

        and: "Returns the same number of features"
        propertyAndLiteralString5.responseData.numberReturned == propertyAndLiteralString5Check.size()

        and: "Returns the same feature arrays"
        for (int i=0; i<propertyAndLiteralString5.responseData.numberReturned; i++) {
            assertFeature(propertyAndLiteralString5.responseData.features[i], propertyAndLiteralString5Check.get(i))
        }

        when: "7. Data is selected using a filter F_CODE LiKe 'al0%' NoCasE FalsE"
        // TODO currently only true/false (case-insensitive), not the other options that will likely be removed: "T" | "t" | "F" | "f" | "1" | "0"
        def propertyAndLiteralString6 = getRequest(restClient, CULTURE_PNT_PATH, getQuery("F_CODE LiKe 'al0%' NoCasE FalsE"))
        def propertyAndLiteralString6Check = allCulturePntFeatures.responseData.features.stream().filter( f -> f.properties.F_CODE.startsWith('al0') ).toList()

        then: "Success and returns GeoJSON"
        assertSuccess(propertyAndLiteralString6)

        and: "Returns the same number of features"
        propertyAndLiteralString6.responseData.numberReturned == propertyAndLiteralString6Check.size()

        and: "Returns the same feature arrays"
        for (int i=0; i<propertyAndLiteralString6.responseData.numberReturned; i++) {
            assertFeature(propertyAndLiteralString6.responseData.features[i], propertyAndLiteralString6Check.get(i))
        }
    }

    def "Operator between"() {
        given: "CulturePnt features in the Daraa dataset"

        when: "1. Data is selected using a filter ZI037_REL BeTweeN ZI037_REL AnD ZI037_REL"
        def twoProperties = getRequest(restClient, CULTURE_PNT_PATH, getQuery("ZI037_REL BeTweeN ZI037_REL AnD ZI037_REL"))
        def twoPropertiesCheck = allCulturePntFeatures.responseData.features.stream().filter( f -> Objects.nonNull(f.properties.ZI037_REL) ).toList()

        then: "Success and returns GeoJSON"
        assertSuccess(twoProperties)

        and: "Returns all features that are not null"
        twoProperties.responseData.numberReturned == twoPropertiesCheck.size()

        and: "Returns the same feature arrays"
        for (int i=0; i<twoProperties.responseData.numberReturned; i++) {
            assertFeature(twoProperties.responseData.features[i], twoPropertiesCheck.get(i))
        }

        when: "2. Data is selected using a filter ZI037_REL NoT BeTweeN ZI037_REL AnD ZI037_REL"
        def twoProperties2 = getRequest(restClient, CULTURE_PNT_PATH, getQuery("ZI037_REL NoT BeTweeN ZI037_REL AnD ZI037_REL"))

        then: "Success and returns GeoJSON"
        assertSuccess(twoProperties2)

        and: "Returns no features"
        twoProperties2.responseData.numberReturned == 0

        when: "3. Data is selected using a filter ZI037_REL BeTweeN 0 AnD 10"
        def propertyAndLiteral = getRequest(restClient, CULTURE_PNT_PATH, getQuery("ZI037_REL BeTweeN 0 AnD 10"))
        def propertyAndLiteralCheck = allCulturePntFeatures.responseData.features.stream().filter( f -> Objects.nonNull(f.properties.ZI037_REL) && f.properties.ZI037_REL>=0 && f.properties.ZI037_REL<=10 ).toList()

        then: "Success and returns GeoJSON"
        assertSuccess(propertyAndLiteral)

        and: "Returns the same number of features"
        propertyAndLiteral.responseData.numberReturned == propertyAndLiteralCheck.size()

        and: "Returns the same feature arrays"
        for (int i=0; i<propertyAndLiteral.responseData.numberReturned; i++) {
            assertFeature(propertyAndLiteral.responseData.features[i], propertyAndLiteralCheck.get(i))
        }

        when: "4. Data is selected using a filter ZI037_REL BeTweeN 0 AnD 11"
        def propertyAndLiteral2 = getRequest(restClient, CULTURE_PNT_PATH, getQuery("ZI037_REL BeTweeN 0 AnD 11"))
        def propertyAndLiteral2Check = allCulturePntFeatures.responseData.features.stream().filter( f -> Objects.nonNull(f.properties.ZI037_REL) && f.properties.ZI037_REL>=0 && f.properties.ZI037_REL<=11 ).toList()

        then: "Success and returns GeoJSON"
        assertSuccess(propertyAndLiteral2)

        and: "Returns the same number of features"
        propertyAndLiteral2.responseData.numberReturned == propertyAndLiteral2Check.size()

        and: "Returns the same feature arrays"
        for (int i=0; i<propertyAndLiteral2.responseData.numberReturned; i++) {
            assertFeature(propertyAndLiteral2.responseData.features[i], propertyAndLiteral2Check.get(i))
        }

        when: "5. Data is selected using a filter ZI037_REL NoT BeTweeN 0 AnD 10"
        def propertyAndLiteral3 = getRequest(restClient, CULTURE_PNT_PATH, getQuery("ZI037_REL NoT BeTweeN 0 AnD 10"))
        def propertyAndLiteral3Check = allCulturePntFeatures.responseData.features.stream().filter( f -> Objects.nonNull(f.properties.ZI037_REL) && !(f.properties.ZI037_REL>=0 && f.properties.ZI037_REL<=10) ).toList()

        then: "Success and returns GeoJSON"
        assertSuccess(propertyAndLiteral3)

        and: "Returns the same number of features"
        propertyAndLiteral3.responseData.numberReturned == propertyAndLiteral3Check.size()

        and: "Returns the same feature arrays"
        for (int i=0; i<propertyAndLiteral3.responseData.numberReturned; i++) {
            assertFeature(propertyAndLiteral3.responseData.features[i], propertyAndLiteral3Check.get(i))
        }

        when: "6. Data is selected using a filter ZI037_REL NoT BeTweeN 0 AnD 11"
        def propertyAndLiteral4 = getRequest(restClient, CULTURE_PNT_PATH, getQuery("ZI037_REL NoT BeTweeN 0 AnD 11"))
        def propertyAndLiteral4Check = allCulturePntFeatures.responseData.features.stream().filter( f -> Objects.nonNull(f.properties.ZI037_REL) && !(f.properties.ZI037_REL>=0 && f.properties.ZI037_REL<=11) ).toList()

        then: "Success and returns GeoJSON"
        assertSuccess(propertyAndLiteral4)

        and: "Returns the same number of features"
        propertyAndLiteral4.responseData.numberReturned == propertyAndLiteral4Check.size()

        and: "Returns the same feature arrays"
        for (int i=0; i<propertyAndLiteral4.responseData.numberReturned; i++) {
            assertFeature(propertyAndLiteral4.responseData.features[i], propertyAndLiteral4Check.get(i))
        }

        when: "7. Data is selected using a filter 6 BeTweeN 0 AnD ZI037_REL"
        def literalAndProperty = getRequest(restClient, CULTURE_PNT_PATH, getQuery("6 BeTweeN 0 AnD ZI037_REL"))
        def literalAndPropertyCheck = allCulturePntFeatures.responseData.features.stream().filter( f -> Objects.nonNull(f.properties.ZI037_REL) && f.properties.ZI037_REL>=6 ).toList()

        then: "Success and returns GeoJSON"
        assertSuccess(literalAndProperty)

        and: "Returns the same number of features"
        literalAndProperty.responseData.numberReturned == literalAndPropertyCheck.size()

        and: "Returns the expected features"
        for (int i=0; i<literalAndProperty.responseData.numberReturned; i++) {
            assertFeature(literalAndProperty.responseData.features[i], literalAndPropertyCheck.get(i))
        }

        when: "7. Data is selected using a filter ZI001_SDV BETWEEN '2011-01-01T00:00:00Z' AND '2012-01-01T00:00:00Z'"
        def temporalProperty = getRequest(restClient, CULTURE_PNT_PATH, getQuery("ZI001_SDV BETWEEN '2011-01-01T00:00:00Z' AND '2012-01-01T00:00:00Z'"))
        def temporalPropertyCheck = allCulturePntFeatures.responseData.features.stream().filter(f -> f.properties.ZI001_SDV.startsWith('2011')).toList()

        then: "Success and returns GeoJSON"
        assertSuccess(temporalProperty)

        and: "Returns the same number of features"
        temporalProperty.responseData.numberReturned == temporalPropertyCheck.size()

        and: "Returns the same feature arrays"
        for (int i=0; i<temporalProperty.responseData.numberReturned; i++) {
            assertFeature(temporalProperty.responseData.features[i], temporalPropertyCheck.get(i))
        }
    }

    def "Operator in"() {
        given: "CulturePnt features in the Daraa dataset"

        // TODO currently restricted to a property on the left side and literals on the right side
        // TODO NOCASE currently not supported

        when: "1. Data is selected using a filter F_CODE iN ('AL030', 'AL012')"
        def propertyAndLiteralString = getRequest(restClient, CULTURE_PNT_PATH, getQuery("F_CODE in ('AL030','AL012')"))
        def propertyAndLiteralStringCheck = allCulturePntFeatures.responseData.features.stream().filter( f -> f.properties.F_CODE.equals('AL012') || f.properties.F_CODE.equals('AL030') ).toList()

        then: "Success and returns GeoJSON"
        assertSuccess(propertyAndLiteralString)

        and: "Returns the same number of features"
        propertyAndLiteralString.responseData.numberReturned == propertyAndLiteralStringCheck.size()

        and: "Returns the same feature arrays"
        for (int i=0; i<propertyAndLiteralString.responseData.numberReturned; i++) {
            assertFeature(propertyAndLiteralString.responseData.features[i], propertyAndLiteralStringCheck.get(i))
        }

        when: "2. Data is selected using a filter F_CODE NoT iN ('AL030', 'AL012')"
        def propertyAndLiteralString2 = getRequest(restClient, CULTURE_PNT_PATH, getQuery("F_CODE NoT iN ('AL030', 'AL012')"))
        def propertyAndLiteralString2Check = allCulturePntFeatures.responseData.features.stream().filter( f -> !f.properties.F_CODE.equals('AL012') && !f.properties.F_CODE.equals('AL030') ).toList()

        then: "Success and returns GeoJSON"
        assertSuccess(propertyAndLiteralString2)

        and: "Returns the same number of features"
        propertyAndLiteralString2.responseData.numberReturned == propertyAndLiteralString2Check.size()

        and: "Returns the same feature arrays"
        for (int i=0; i<propertyAndLiteralString2.responseData.numberReturned; i++) {
            assertFeature(propertyAndLiteralString2.responseData.features[i], propertyAndLiteralString2Check.get(i))
        }

        when: "3. Data is selected using a filter ZI037_REL iN (11, 12)"
        def propertyAndLiteralNumeric = getRequest(restClient, CULTURE_PNT_PATH, getQuery("ZI037_REL in (11, 12)"))
        def propertyAndLiteralNumericCheck = allCulturePntFeatures.responseData.features.stream().filter( f -> Objects.nonNull(f.properties.ZI037_REL) && (f.properties.ZI037_REL==11 || f.properties.ZI037_REL==12) ).toList()

        then: "Success and returns GeoJSON"
        assertSuccess(propertyAndLiteralNumeric)

        and: "Returns the same number of features"
        propertyAndLiteralNumeric.responseData.numberReturned == propertyAndLiteralNumericCheck.size()

        and: "Returns the expected features"
        for (int i=0; i<propertyAndLiteralNumeric.responseData.numberReturned; i++) {
            assertFeature(propertyAndLiteralNumeric.responseData.features[i], propertyAndLiteralNumericCheck.get(i))
        }

        when: "4. Data is selected using a filter ZI037_REL NoT iN (11, 12)"
        def propertyAndLiteralNumeric2 = getRequest(restClient, CULTURE_PNT_PATH, getQuery("ZI037_REL NoT iN (11, 12)"))
        def propertyAndLiteralNumeric2Check = allCulturePntFeatures.responseData.features.stream().filter( f -> Objects.nonNull(f.properties.ZI037_REL) && !(f.properties.ZI037_REL==11 || f.properties.ZI037_REL==12) ).toList()

        then: "Success and returns GeoJSON"
        assertSuccess(propertyAndLiteralNumeric2)

        and: "Returns the same number of features"
        propertyAndLiteralNumeric2.responseData.numberReturned == propertyAndLiteralNumeric2Check.size()

        and: "Returns the expected features"
        for (int i=0; i<propertyAndLiteralNumeric2.responseData.numberReturned; i++) {
            assertFeature(propertyAndLiteralNumeric2.responseData.features[i], propertyAndLiteralNumeric2Check.get(i))
        }
        when: "7. Data is selected using a filter ZI001_SDV IN ('2011-12-26T20:55:27Z','2021-10-10T10:10:10Z','2011-12-27T18:39:59Z')"
        def temporalProperty = getRequest(restClient, CULTURE_PNT_PATH, getQuery("ZI001_SDV  IN ('2011-12-26T20:55:27Z','2021-10-10T10:10:10Z','2011-12-27T18:39:59Z')"))
        def temporalPropertyCheck = allCulturePntFeatures.responseData.features.stream().filter(f -> f.properties.ZI001_SDV=='2011-12-26T20:55:27Z' ||  f.properties.ZI001_SDV=='2021-10-10T10:10:10Z' || f.properties.ZI001_SDV=='2011-12-27T18:39:59Z').toList()

        then: "Success and returns GeoJSON"
        assertSuccess(temporalProperty)

        and: "Returns the same number of features"
        temporalProperty.responseData.numberReturned == temporalPropertyCheck.size()

        and: "Returns the same feature arrays"
        for (int i=0; i<temporalProperty.responseData.numberReturned; i++) {
            assertFeature(temporalProperty.responseData.features[i], temporalPropertyCheck.get(i))
        }

    }

    def "Operator null"() {
        given: "CulturePnt features in the Daraa dataset"

        when: "1. Data is selected using a filter ZI037_REL iS NulL"
        def propertyAndLiteral = getRequest(restClient, CULTURE_PNT_PATH, getQuery("ZI037_REL iS NulL"))
        def propertyAndLiteralCheck = allCulturePntFeatures.responseData.features.stream().filter( f -> Objects.isNull(f.properties.ZI037_REL) ).toList()

        then: "Success and returns GeoJSON"
        assertSuccess(propertyAndLiteral)

        and: "Returns the same number of features"
        propertyAndLiteral.responseData.numberReturned == propertyAndLiteralCheck.size()

        and: "Returns the same feature arrays"
        for (int i=0; i<propertyAndLiteral.responseData.numberReturned; i++) {
            assertFeature(propertyAndLiteral.responseData.features[i], propertyAndLiteralCheck.get(i))
        }

        when: "2. Data is selected using a filter ZI037_REL iS NoT NulL"
        def propertyAndLiteral2 = getRequest(restClient, CULTURE_PNT_PATH, getQuery("ZI037_REL iS NoT NulL"))
        def propertyAndLiteral2Check = allCulturePntFeatures.responseData.features.stream().filter( f -> Objects.nonNull(f.properties.ZI037_REL) ).toList()

        then: "Success and returns GeoJSON"
        assertSuccess(propertyAndLiteral2)

        and: "Returns the same number of features"
        propertyAndLiteral2.responseData.numberReturned == propertyAndLiteral2Check.size()

        and: "Returns the same feature arrays"
        for (int i=0; i<propertyAndLiteral2.responseData.numberReturned; i++) {
            assertFeature(propertyAndLiteral2.responseData.features[i], propertyAndLiteral2Check.get(i))
        }
    }

    // Spatial predicates, including filter-crs

    def "Operator intersects"() {
        given: "CulturePnt features in the Daraa dataset"

        when: "1. Data is selected using a filter InterSectS(geometry,geometry)"
        def twoProperties = getRequest(restClient, CULTURE_PNT_PATH, getQuery( "InterSectS(geometry,geometry)"))

        then: "Success and returns GeoJSON"
        assertSuccess(twoProperties)

        and: "Returns all features"
        twoProperties.responseData.numberReturned == allCulturePntFeatures.responseData.numberReturned

        when: "2. Data is selected using a filter InterSectS(geometry,<bbox of collection>)"
        def propertyAndLiteral = getRequest(restClient, CULTURE_PNT_PATH, getQuery( "InterSectS(geometry," + envelopeCollection + ")"))

        then: "Success and returns GeoJSON"
        assertSuccess(propertyAndLiteral)

        and: "Returns the same number of features"
        propertyAndLiteral.responseData.numberReturned == allCulturePntFeatures.responseData.numberReturned

        when: "3. Data is selected using a filter InterSectS(<bbox of collection>,geometry)"
        def propertyAndLiterala = getRequest(restClient, CULTURE_PNT_PATH, getQuery( "InterSectS(" + envelopeCollection + ",geometry)"))

        then: "Success and returns GeoJSON"
        assertSuccess(propertyAndLiterala)

        and: "Returns the same number of features"
        propertyAndLiterala.responseData.numberReturned == allCulturePntFeatures.responseData.numberReturned

        when: "4. Data is selected using a filter InterSectS(geometry,<bbox around first feature>)"
        def propertyAndLiteral2 = getRequest(restClient, CULTURE_PNT_PATH, getQuery( "InterSectS(geometry," + envelopeFeature + ")"))

        then: "Success and returns GeoJSON"
        assertSuccess(propertyAndLiteral2)

        and: "Returns the feature"
        propertyAndLiteral2.responseData.features.stream().anyMatch(f -> f.id == id)

        when: "5. Data is selected using a filter InterSectS(<bbox around first feature>,geometry)"
        def propertyAndLiteral2a = getRequest(restClient, CULTURE_PNT_PATH, getQuery( "InterSectS(" + envelopeFeature + ",geometry)"))

        then: "Success and returns GeoJSON"
        assertSuccess(propertyAndLiteral2a)

        and: "Returns the feature"
        propertyAndLiteral2a.responseData.features.stream().anyMatch(f -> f.id == id)

        when: "6. The same request using EPSG:4326"
        def propertyAndLiteral2b = getRequest(restClient, CULTURE_PNT_PATH, getQuery4326( "InterSectS(geometry," + envelopeFeature4326 + ")"))

        then: "Success and returns GeoJSON"
        assertSuccess(propertyAndLiteral2b)

        and: "Returns the same result"
        assertSameResult(propertyAndLiteral2, propertyAndLiteral2b)

        when: "7. Data is selected using a filter InterSectS(geometry,<polygon around first feature>)"
        def propertyAndLiteral3 = getRequest(restClient, CULTURE_PNT_PATH, getQuery( "InterSectS(geometry," + polygonFeature + ")"))

        then: "Success and returns GeoJSON"
        assertSuccess(propertyAndLiteral3)

        and: "Returns the feature"
        propertyAndLiteral3.responseData.features.stream().anyMatch(f -> f.id == id)

        when: "8. The same request using EPSG:4326"
        def propertyAndLiteral3b = getRequest(restClient, CULTURE_PNT_PATH, getQuery4326( "InterSectS(geometry," + polygonFeature4326 + ")"))

        then: "Success and returns GeoJSON"
        assertSuccess(propertyAndLiteral3b)

        and: "Returns the same result"
        assertSameResult(propertyAndLiteral3, propertyAndLiteral3b)
    }

    def "Operator disjoint"() {
        given: "CulturePnt features in the Daraa dataset"

        when: "1. Data is selected using a filter NoT DisJoinT(geometry,<polygon around first feature>)"
        def propertyAndLiteral4 = getRequest(restClient, CULTURE_PNT_PATH, getQuery("NoT DisJoinT(geometry," + polygonFeature + ")"))
        def propertyAndLiteral4Check = getRequest(restClient, CULTURE_PNT_PATH, getQuery( "InterSectS(geometry," + polygonFeature + ")"))

        then: "Success and returns GeoJSON"
        assertSuccess(propertyAndLiteral4)

        and: "Returns the same features as INTERSECTS"
        propertyAndLiteral4.responseData.numberReturned == propertyAndLiteral4Check.responseData.numberReturned
        for (int i=0; i<propertyAndLiteral4.responseData.numberReturned; i++) {
            assertFeature(propertyAndLiteral4.responseData.features[i], propertyAndLiteral4Check.responseData.features[i])
        }

        when: "2. The same request using EPSG:4326"
        def propertyAndLiteral4b = getRequest(restClient, CULTURE_PNT_PATH, getQuery4326("NoT DisJoinT(geometry," + polygonFeature4326 + ")"))

        then: "Success and returns GeoJSON"
        assertSuccess(propertyAndLiteral4b)

        and: "Returns the same result"
        assertSameResult(propertyAndLiteral4, propertyAndLiteral4b)

        when: "3. Data is selected using a filter DisJoinT(geometry,<polygon around first feature>)"
        def propertyAndLiteral5 = getRequest(restClient, CULTURE_PNT_PATH, getQuery("DisJoinT(geometry," + polygonFeature + ")"))

        then: "Success and returns GeoJSON"
        assertSuccess(propertyAndLiteral5)

        and: "Returns the feature"
        propertyAndLiteral5.responseData.numberReturned == allCulturePntFeatures.responseData.numberReturned - propertyAndLiteral4Check.responseData.numberReturned
        propertyAndLiteral5.responseData.features.stream().noneMatch( f -> f.id == id )

        when: "4. The same request using EPSG:4326"
        def propertyAndLiteral5b = getRequest(restClient, CULTURE_PNT_PATH, getQuery4326("DisJoinT(geometry," + polygonFeature4326 + ")"))

        then: "Success and returns GeoJSON"
        assertSuccess(propertyAndLiteral5b)

        and: "Returns the same result"
        assertSameResult(propertyAndLiteral5, propertyAndLiteral5b)
    }

    def "Operator equals"() {
        given: "CulturePnt features in the Daraa dataset"

        when: "1. Data is selected using a filter EqualS(geometry,<point of first feature>)"
        def propertyAndLiteral6 = getRequest(restClient, CULTURE_PNT_PATH, getQuery("EqualS(geometry, " + pointFeature + ")"))

        then: "Success and returns GeoJSON"
        assertSuccess(propertyAndLiteral6)

        and: "Returns the feature"
        propertyAndLiteral6.responseData.numberReturned == 1
        assertFeature(propertyAndLiteral6.responseData.features[0], allCulturePntFeatures.responseData.features[0])

        when: "2. The same request using EPSG:4326"
        def propertyAndLiteral6b = getRequest(restClient, CULTURE_PNT_PATH, getQuery4326("EqualS(geometry, " + pointFeature4326 + ")"))

        then: "Success and returns GeoJSON"
        assertSuccess(propertyAndLiteral6b)

        and: "Returns the same result"
        assertSameResult(propertyAndLiteral6, propertyAndLiteral6b)

        when: "3. Data is selected using a filter NoT EqualS(geometry,<point of first feature>)"
        def propertyAndLiteral7 = getRequest(restClient, CULTURE_PNT_PATH, getQuery("NoT EqualS(geometry, " + pointFeature + ")"))

        then: "Success and returns GeoJSON"
        assertSuccess(propertyAndLiteral7)

        and: "Returns everything but the feature"
        propertyAndLiteral7.responseData.numberReturned == allCulturePntFeatures.responseData.numberReturned - 1

        when: "4. The same request using EPSG:4326"
        def propertyAndLiteral7b = getRequest(restClient, CULTURE_PNT_PATH, getQuery4326("NoT EqualS(geometry, " + pointFeature4326 + ")"))

        then: "Success and returns GeoJSON"
        assertSuccess(propertyAndLiteral7b)

        and: "Returns the same result"
        assertSameResult(propertyAndLiteral7, propertyAndLiteral7b)
    }

    def "Operator within"() {
        given: "CulturePnt features in the Daraa dataset"

        when: "1. Data is selected using a filter WithiN(geometry,<polygon around first feature>)"
        def propertyAndLiteral8 = getRequest(restClient, CULTURE_PNT_PATH, getQuery("WithiN(geometry, " + polygonFeature + ")"))

        then: "Success and returns GeoJSON"
        assertSuccess(propertyAndLiteral8)

        and: "Returns the feature"
        propertyAndLiteral8.responseData.numberReturned > 0
        propertyAndLiteral8.responseData.features.stream().anyMatch( f -> f.id == id )

        when: "2. The same request using EPSG:4326"
        def propertyAndLiteral8b = getRequest(restClient, CULTURE_PNT_PATH, getQuery4326("WithiN(geometry, " + polygonFeature4326 + ")"))

        then: "Success and returns GeoJSON"
        assertSuccess(propertyAndLiteral8b)

        and: "Returns the same result"
        assertSameResult(propertyAndLiteral8, propertyAndLiteral8b)

        when: "3. Data is selected using a filter NoT WithiN(geometry,<polygon around first feature>)"
        def propertyAndLiteral9 = getRequest(restClient, CULTURE_PNT_PATH, getQuery("NoT WithiN(geometry, " + polygonFeature + ")"))

        then: "Success and returns GeoJSON"
        assertSuccess(propertyAndLiteral9)

        and: "Returns everything but the feature"
        propertyAndLiteral9.responseData.numberReturned == allCulturePntFeatures.responseData.numberReturned - propertyAndLiteral8.responseData.numberReturned
        propertyAndLiteral9.responseData.features.stream().noneMatch( f -> f.id == id )

        when: "4. The same request using EPSG:4326"
        def propertyAndLiteral9b = getRequest(restClient, CULTURE_PNT_PATH, getQuery4326("NoT WithiN(geometry, " + polygonFeature4326 + ")"))

        then: "Success and returns GeoJSON"
        assertSuccess(propertyAndLiteral9b)

        and: "Returns the same result"
        assertSameResult(propertyAndLiteral9, propertyAndLiteral9b)

        when: "5. Data is selected using a filter WithiN(<point of first feature>,<polygon around first feature>)"
        def propertyAndLiteral10 = getRequest(restClient, CULTURE_PNT_PATH, getQuery("WithiN(" + pointFeature + ", " + polygonFeature + ")"))

        then: "Success and returns GeoJSON"
        assertSuccess(propertyAndLiteral10)

        and: "Returns all features"
        propertyAndLiteral10.responseData.numberReturned == allCulturePntFeatures.responseData.numberReturned

        when: "6. The same request using EPSG:4326"
        def propertyAndLiteral10b = getRequest(restClient, CULTURE_PNT_PATH, getQuery4326("WithiN(" + pointFeature4326 + ", " + polygonFeature4326 + ")"))

        then: "Success and returns GeoJSON"
        assertSuccess(propertyAndLiteral10b)

        and: "Returns the same result"
        assertSameResult(propertyAndLiteral10, propertyAndLiteral10b)

        when: "7. Data is selected using a filter NoT WithiN(geometry,<polygon around first feature>)"
        def propertyAndLiteral11 = getRequest(restClient, CULTURE_PNT_PATH, getQuery("NoT WithiN(" + pointFeature + ", " + polygonFeature + ")"))

        then: "Success and returns GeoJSON"
        assertSuccess(propertyAndLiteral11)

        and: "Returns no feature"
        propertyAndLiteral11.responseData.numberReturned == 0

        when: "8. The same request using EPSG:4326"
        def propertyAndLiteral11b = getRequest(restClient, CULTURE_PNT_PATH, getQuery4326("NoT WithiN(" + pointFeature4326 + ", " + polygonFeature4326 + ")"))

        then: "Success and returns GeoJSON"
        assertSuccess(propertyAndLiteral11b)

        and: "Returns the same result"
        assertSameResult(propertyAndLiteral11, propertyAndLiteral11b)
    }

    // TODO tests for TOUCHES, OVERLAPS, CROSSES using other collections with line string/polygon geometry

    // Temporal predicates
    // TODO ANYINTERACTS, TEQUALS, AFTER, BEFORE and DURING are the only implemented predicates;
    //      add tests for BEGINS, BEGUNBY, TCONTAINS, ENDEDBY, ENDS, MEETS, METBY, TOVERLAPS,
    //      OVERLAPPEDBY once they are implemented

    def "Operator anyinteracts"() {
        given: "CulturePnt features in the Daraa dataset"

        when: "1. Data is selected using a filter ZI001_SDV AnyInterActS 2011-12-01T00:00:00Z/2011-12-31T23:59:59Z"
        def propertyAndLiteral = getRequest(restClient, CULTURE_PNT_PATH, getQuery("ZI001_SDV AnyInterActS 2011-12-01T00:00:00Z/2011-12-31T23:59:59Z"))
        def propertyAndLiteralCheck = allCulturePntFeatures.responseData.features.stream().filter( f -> f.properties.ZI001_SDV > '2011-12' && f.properties.ZI001_SDV < '2012' ).toList()

        then: "Success and returns GeoJSON"
        assertSuccess(propertyAndLiteral)

        and: "Returns the same number of features"
        propertyAndLiteral.responseData.numberReturned == propertyAndLiteralCheck.size()

        and: "Returns the same feature arrays"
        for (int i=0; i<propertyAndLiteral.responseData.numberReturned; i++) {
            assertFeature(propertyAndLiteral.responseData.features[i], propertyAndLiteralCheck.get(i))
        }

        when: "2. Data is selected using a filter ZI001_SDV AnyInterActS ../2011-12-31T23:59:59Z"
        def propertyAndLiteral2 = getRequest(restClient, CULTURE_PNT_PATH, getQuery("ZI001_SDV AnyInterActS ../2011-12-31T23:59:59Z"))
        def propertyAndLiteral2Check = allCulturePntFeatures.responseData.features.stream().filter( f -> f.properties.ZI001_SDV < '2012' ).toList()

        then: "Success and returns GeoJSON"
        assertSuccess(propertyAndLiteral2)

        and: "Returns the same number of features"
        propertyAndLiteral2.responseData.numberReturned == propertyAndLiteral2Check.size()

        and: "Returns the same feature arrays"
        for (int i=0; i<propertyAndLiteral2.responseData.numberReturned; i++) {
            assertFeature(propertyAndLiteral2.responseData.features[i], propertyAndLiteral2Check.get(i))
        }

        when: "3. Data is selected using a filter ZI001_SDV AnyInterActS 2012-01-01T00:00:00Z/.."
        def propertyAndLiteral3 = getRequest(restClient, CULTURE_PNT_PATH, getQuery("ZI001_SDV AnyInterActS 2012-01-01T00:00:00Z/.."))
        def propertyAndLiteral3Check = allCulturePntFeatures.responseData.features.stream().filter( f -> f.properties.ZI001_SDV > '2012' ).toList()

        then: "Success and returns GeoJSON"
        assertSuccess(propertyAndLiteral3)

        and: "Returns the same number of features"
        propertyAndLiteral3.responseData.numberReturned == propertyAndLiteral3Check.size()

        and: "Returns the same feature arrays"
        for (int i=0; i<propertyAndLiteral3.responseData.numberReturned; i++) {
            assertFeature(propertyAndLiteral3.responseData.features[i], propertyAndLiteral3Check.get(i))
        }

        when: "4. Data is selected using a filter ZI001_SDV AnyInterActS ../.."
        def propertyAndLiteral4 = getRequest(restClient, CULTURE_PNT_PATH, getQuery("ZI001_SDV AnyInterActS ../.."))

        then: "Success and returns GeoJSON"
        assertSuccess(propertyAndLiteral4)

        and: "Returns all features"
        assertSameResult(propertyAndLiteral4, allCulturePntFeatures)

        when: "5. Data is selected using a filter ZI001_SDV AnyInterActS 2011-12-27"
        def propertyAndLiteral5 = getRequest(restClient, CULTURE_PNT_PATH, getQuery("ZI001_SDV AnyInterActS 2011-12-27"))
        def propertyAndLiteral5Check = allCulturePntFeatures.responseData.features.stream().filter( f -> f.properties.ZI001_SDV > '2011-12-27' && f.properties.ZI001_SDV < '2011-12-28' ).toList()

        then: "Success and returns GeoJSON"
        assertSuccess(propertyAndLiteral5)

        and: "Returns the same number of features"
        propertyAndLiteral5.responseData.numberReturned == propertyAndLiteral5Check.size()

        and: "Returns the same feature arrays"
        for (int i=0; i<propertyAndLiteral5.responseData.numberReturned; i++) {
            assertFeature(propertyAndLiteral5.responseData.features[i], propertyAndLiteral5Check.get(i))
        }

        when: "6. Data is selected using a filter ZI001_SDV AnyInterActS 2011-12-26T20:55:27Z"
        def propertyAndLiteral6 = getRequest(restClient, CULTURE_PNT_PATH, getQuery("ZI001_SDV AnyInterActS 2011-12-26T20:55:27Z"))
        def propertyAndLiteral6Check = allCulturePntFeatures.responseData.features.stream().filter( f -> f.properties.ZI001_SDV == '2011-12-26T20:55:27Z' ).toList()

        then: "Success and returns GeoJSON"
        assertSuccess(propertyAndLiteral6)

        and: "Returns the same number of features"
        propertyAndLiteral6.responseData.numberReturned == propertyAndLiteral6Check.size()

        and: "Returns the same feature arrays"
        for (int i=0; i<propertyAndLiteral6.responseData.numberReturned; i++) {
            assertFeature(propertyAndLiteral6.responseData.features[i], propertyAndLiteral6Check.get(i))
        }

        when: "7. Data is selected using a filter ZI001_SDV AnyInterActS ZI001_SDV"
        def propertyAndLiteral7 = getRequest(restClient, CULTURE_PNT_PATH, getQuery("ZI001_SDV AnyInterActS ZI001_SDV"))

        then: "Success and returns GeoJSON"
        assertSuccess(propertyAndLiteral7)

        and: "Returns all features"
        assertSameResult(propertyAndLiteral7, allCulturePntFeatures)

        when: "8. Data is selected using a filter 2011-12-01T00:00:00Z/2011-12-31T23:59:59Z AnyInterActS ZI001_SDV"
        def propertyAndLiteral8 = getRequest(restClient, CULTURE_PNT_PATH, getQuery("2011-12-01T00:00:00Z/2011-12-31T23:59:59Z AnyInterActS ZI001_SDV"))

        then: "Success and returns GeoJSON"
        assertSuccess(propertyAndLiteral8)

        and: "Returns the same features"
        assertSameResult(propertyAndLiteral8, propertyAndLiteral)

        when: "9. Data is selected using a filter ../2011-12-31T23:59:59Z AnyInterActS ZI001_SDV"
        def propertyAndLiteral9 = getRequest(restClient, CULTURE_PNT_PATH, getQuery("../2011-12-31T23:59:59Z AnyInterActS ZI001_SDV"))

        then: "Success and returns GeoJSON"
        assertSuccess(propertyAndLiteral9)

        and: "Returns the same features"
        assertSameResult(propertyAndLiteral9, propertyAndLiteral2)

        when: "10. Data is selected using a filter 2012-01-01T00:00:00Z/.. AnyInterActS ZI001_SDV"
        def propertyAndLiteral10 = getRequest(restClient, CULTURE_PNT_PATH, getQuery("2012-01-01T00:00:00Z/.. AnyInterActS ZI001_SDV"))

        then: "Success and returns GeoJSON"
        assertSuccess(propertyAndLiteral10)

        and: "Returns the same features"
        assertSameResult(propertyAndLiteral10, propertyAndLiteral3)

        when: "11. Data is selected using a filter ../.. AnyInterActS ZI001_SDV"
        def propertyAndLiteral11 = getRequest(restClient, CULTURE_PNT_PATH, getQuery("../.. AnyInterActS ZI001_SDV"))

        then: "Success and returns GeoJSON"
        assertSuccess(propertyAndLiteral11)

        and: "Returns the same features"
        assertSameResult(propertyAndLiteral11, propertyAndLiteral4)

        when: "12. Data is selected using a filter 2011-12-27 AnyInterActS ZI001_SDV"
        def propertyAndLiteral12 = getRequest(restClient, CULTURE_PNT_PATH, getQuery("2011-12-27 AnyInterActS ZI001_SDV"))

        then: "Success and returns GeoJSON"
        assertSuccess(propertyAndLiteral12)

        and: "Returns the same features"
        assertSameResult(propertyAndLiteral12, propertyAndLiteral5)

        when: "13. Data is selected using a filter 2011-12-26T20:55:27Z AnyInterActS ZI001_SDV"
        def propertyAndLiteral13 = getRequest(restClient, CULTURE_PNT_PATH, getQuery("2011-12-26T20:55:27Z AnyInterActS ZI001_SDV"))

        then: "Success and returns GeoJSON"
        assertSuccess(propertyAndLiteral13)

        and: "Returns the same features"
        assertSameResult(propertyAndLiteral13, propertyAndLiteral6)

        when: "14. Data is selected using a filter 2011-12-26T20:55:27Z AnyInterActS 2011-01-01/2011-12-31"
        def propertyAndLiteral14 = getRequest(restClient, CULTURE_PNT_PATH, getQuery("2011-12-26T20:55:27Z AnyInterActS 2011-01-01/2011-12-31"))

        then: "Success and returns GeoJSON"
        assertSuccess(propertyAndLiteral14)

        and: "Returns all features"
        assertSameResult(propertyAndLiteral14, allCulturePntFeatures)

        when: "14. Data is selected using a filter ../2010-12-26 AnyInterActS 2011-01-01/2011-12-31"
        def propertyAndLiteral15 = getRequest(restClient, CULTURE_PNT_PATH, getQuery("../2010-12-26 AnyInterActS 2011-01-01/2011-12-31"))

        then: "Success and returns GeoJSON"
        assertSuccess(propertyAndLiteral15)

        and: "Returns no features"
        propertyAndLiteral15.responseData.numberReturned == 0
    }

    def "Operator tequals"() {
        given: "CulturePnt features in the Daraa dataset"

        when: "1. Data is selected using a filter ZI001_SDV TEqualS ZI001_SDV"
        def twoProperties = getRequest(restClient, CULTURE_PNT_PATH, getQuery("ZI001_SDV TEqualS ZI001_SDV"))

        then: "Success and returns GeoJSON"
        assertSuccess(twoProperties)

        and: "Returns all features"
        twoProperties.responseData.numberReturned == allCulturePntFeatures.responseData.numberReturned

        when: "2. Data is selected using a filter ZI001_SDV TEqualS 2011-12-26T20:55:27Z"
        def propertyAndLiteral = getRequest(restClient, CULTURE_PNT_PATH, getQuery("ZI001_SDV TEqualS 2011-12-26T20:55:27Z"))
        def propertyAndLiteralCheck = allCulturePntFeatures.responseData.features.stream().filter( f -> f.properties.ZI001_SDV == '2011-12-26T20:55:27Z' ).toList()

        then: "Success and returns GeoJSON"
        assertSuccess(propertyAndLiteral)

        and: "Returns the same number of features"
        propertyAndLiteral.responseData.numberReturned == propertyAndLiteralCheck.size()

        and: "Returns the same feature arrays"
        for (int i=0; i<propertyAndLiteral.responseData.numberReturned; i++) {
            assertFeature(propertyAndLiteral.responseData.features[i], propertyAndLiteralCheck.get(i))
        }

        when: "3. Data is selected using datetime=2011-12-26T20:55:27Z"
        def datetime3 = getRequest(restClient, CULTURE_PNT_PATH, [datetime:"2011-12-26T20:55:27Z"])

        then: "Success and returns GeoJSON"
        assertSuccess(datetime3)

        and: "Returns the same number of features"
        datetime3.responseData.numberReturned == propertyAndLiteralCheck.size()

        and: "Returns the same feature arrays"
        for (int i=0; i<datetime3.responseData.numberReturned; i++) {
            assertFeature(datetime3.responseData.features[i], propertyAndLiteralCheck.get(i))
        }

        when: "4. Data is selected using a filter ZI001_SDV TEQUALS 2011-12-26T21:55:27+01:00"
        def propertyAndLiteral6 = getRequest(restClient, CULTURE_PNT_PATH, getQuery("ZI001_SDV TEQUALS 2011-12-26T21:55:27+01:00"))

        then: "Success and returns GeoJSON"
        assertSuccess(propertyAndLiteral6)

        and: "Returns the same number of features"
        propertyAndLiteral6.responseData.numberReturned == propertyAndLiteralCheck.size()

        and: "Returns the same feature arrays"
        for (int i=0; i<propertyAndLiteral6.responseData.numberReturned; i++) {
            assertFeature(propertyAndLiteral6.responseData.features[i], propertyAndLiteralCheck.get(i))
        }
    }

    def "Operator after"() {
        given: "CulturePnt features in the Daraa dataset"

        when: "1. Data is selected using a filter ZI001_SDV AFTER 2011-12-31T23:59:59Z"
        def propertyAndLiteral = getRequest(restClient, CULTURE_PNT_PATH, getQuery("ZI001_SDV AFTER 2011-12-31T23:59:59Z"))
        def propertyAndLiteralCheck = allCulturePntFeatures.responseData.features.stream().filter( f -> f.properties.ZI001_SDV > '2011-12-31T23:59:59Z' ).toList()

        then: "Success and returns GeoJSON"
        assertSuccess(propertyAndLiteral)

        and: "Returns the same number of features"
        propertyAndLiteral.responseData.numberReturned == propertyAndLiteralCheck.size()

        and: "Returns the same feature arrays"
        for (int i=0; i<propertyAndLiteral.responseData.numberReturned; i++) {
            assertFeature(propertyAndLiteral.responseData.features[i], propertyAndLiteralCheck.get(i))
        }
    }

    def "Operator before"() {
        given: "CulturePnt features in the Daraa dataset"

        when: "1. Data is selected using a filter ZI001_SDV BeForE 2012-01-01T00:00:00Z"
        def propertyAndLiteral = getRequest(restClient, CULTURE_PNT_PATH, getQuery("ZI001_SDV BeForE 2012-01-01T00:00:00Z"))
        def propertyAndLiteralCheck = allCulturePntFeatures.responseData.features.stream().filter( f -> f.properties.ZI001_SDV < '2012-01-01T00:00:00Z' ).toList()

        then: "Success and returns GeoJSON"
        assertSuccess(propertyAndLiteral)

        and: "Returns the same number of features"
        propertyAndLiteral.responseData.numberReturned == propertyAndLiteralCheck.size()

        and: "Returns the same feature arrays"
        for (int i=0; i<propertyAndLiteral.responseData.numberReturned; i++) {
            assertFeature(propertyAndLiteral.responseData.features[i], propertyAndLiteralCheck.get(i))
        }
    }

    def "Operator during"() {
        given: "CulturePnt features in the Daraa dataset"

        when: "1. Data is selected using a filter ZI001_SDV DuRinG ../2011-12-31T23:59:59Z"
        def propertyAndLiteral = getRequest(restClient, CULTURE_PNT_PATH, getQuery("ZI001_SDV DuRinG ../2011-12-31T23:59:59Z"))
        def propertyAndLiteralCheck = allCulturePntFeatures.responseData.features.stream().filter( f -> f.properties.ZI001_SDV <= '2011-12-31T23:59:59Z' ).toList()

        then: "Success and returns GeoJSON"
        assertSuccess(propertyAndLiteral)

        and: "Returns the same number of features"
        propertyAndLiteral.responseData.numberReturned == propertyAndLiteralCheck.size()

        and: "Returns the same feature arrays"
        for (int i=0; i<propertyAndLiteral.responseData.numberReturned; i++) {
            assertFeature(propertyAndLiteral.responseData.features[i], propertyAndLiteralCheck.get(i))
        }

        when: "2. Data is selected using datetime=../2011-12-31T23:59:59Z"
        def datetime = getRequest(restClient, CULTURE_PNT_PATH, [datetime:"../2011-12-31T23:59:59Z"])

        then: "Success and returns GeoJSON"
        assertSuccess(datetime)

        and: "Returns the same number of features"
        datetime.responseData.numberReturned == propertyAndLiteralCheck.size()

        and: "Returns the same feature arrays"
        for (int i=0; i<datetime.responseData.numberReturned; i++) {
            assertFeature(datetime.responseData.features[i], propertyAndLiteralCheck.get(i))
        }

        when: "3. Data is selected using a filter ZI001_SDV DURING 2012-01-01T00:00:00Z/.."
        def propertyAndLiteral2 = getRequest(restClient, CULTURE_PNT_PATH, getQuery("ZI001_SDV DURING 2012-01-01T00:00:00Z/.."))
        def propertyAndLiteral2Check = allCulturePntFeatures.responseData.features.stream().filter( f -> f.properties.ZI001_SDV >= '2012-01-01T00:00:00Z' ).toList()

        then: "Success and returns GeoJSON"
        assertSuccess(propertyAndLiteral2)

        and: "Returns the same number of features"
        propertyAndLiteral2.responseData.numberReturned == propertyAndLiteral2Check.size()

        and: "Returns the same feature arrays"
        for (int i=0; i<propertyAndLiteral2.responseData.numberReturned; i++) {
            assertFeature(propertyAndLiteral2.responseData.features[i], propertyAndLiteral2Check.get(i))
        }

        when: "4. Data is selected using datetime=2012-01-01T00:00:00Z/.."
        def datetime2 = getRequest(restClient, CULTURE_PNT_PATH, [datetime:"2012-01-01T00:00:00Z/.."])

        then: "Success and returns GeoJSON"
        assertSuccess(datetime2)

        and: "Returns the same number of features"
        datetime2.responseData.numberReturned == propertyAndLiteral2Check.size()

        and: "Returns the same feature arrays"
        for (int i=0; i<datetime2.responseData.numberReturned; i++) {
            assertFeature(datetime2.responseData.features[i], propertyAndLiteral2Check.get(i))
        }
    }

    // Array predicates

    def "Operator acontains"() {
        given: "Records in the AX_Gebaeudefunktion codelist"

        when: "1. Data is selected using a filter theme.concept ACONTAINS ['DLKM', 'Basis-DLM', 'DLM50']"
        def propertyAndLiteral = getRequest(restClient, AX_GEBAEUDEFUNKTION_PATH, getQuery("theme.concept AContainS ['DLKM', 'Basis-DLM', 'DLM50']"))
        def propertyAndLiteralCheck = allAxGebaeudefunktion.responseData.features.stream().filter(f -> f.properties.theme.stream()
                    .map(theme -> theme.concept)
                    .flatMap(List::stream)
                    .filter(concept -> concept.equals('DLKM') || concept.equals('Basis-DLM') || concept.equals('DLM50'))
                    .distinct()
                    .count()==3).toList()

        then: "Success and returns GeoJSON"
        assertSuccess(propertyAndLiteral)

        and: "Returns the same number of records"
        propertyAndLiteral.responseData.numberReturned == propertyAndLiteralCheck.size()

        and: "Returns the same records arrays"
        for (int i=0; i<propertyAndLiteral.responseData.numberReturned; i++) {
            assertFeature(propertyAndLiteral.responseData.features[i], propertyAndLiteralCheck.get(i))
        }

        when: "1a. Data is selected using a nested filter theme[scheme='profile'].concept ACONTAINS ['DLKM', 'Basis-DLM', 'DLM50']"
        def propertyAndLiteralNested = getRequest(restClient, AX_GEBAEUDEFUNKTION_PATH, getQuery("theme[scheme='profile'].concept AContainS ['DLKM', 'Basis-DLM', 'DLM50']"))
        def propertyAndLiteralNestedCheck = allAxGebaeudefunktion.responseData.features.stream().filter(f -> {
            def themes = f.properties.theme.stream()
                    .filter(theme -> theme.scheme.equals('profile'))
                    .toList()
            if (themes.size()==0)
                return false
            return themes.stream()
                    .map(theme -> theme.concept)
                    .flatMap(List::stream)
                    .filter(concept -> concept.equals('DLKM') || concept.equals('Basis-DLM') || concept.equals('DLM50'))
                    .distinct()
                    .count()==3
        }).toList()

        then: "Success and returns GeoJSON"
        assertSuccess(propertyAndLiteralNested)

        and: "Returns the same number of records"
        propertyAndLiteralNested.responseData.numberReturned == propertyAndLiteralNestedCheck.size()

        and: "Returns the same records arrays"
        for (int i=0; i<propertyAndLiteralNested.responseData.numberReturned; i++) {
            assertFeature(propertyAndLiteralNested.responseData.features[i], propertyAndLiteralNestedCheck.get(i))
        }

        when: "1b. Data is selected using a nested filter theme[scheme<>'profile'].concept ACONTAINS ['DLKM', 'Basis-DLM', 'DLM50']"
        def propertyAndLiteralNested2 = getRequest(restClient, AX_GEBAEUDEFUNKTION_PATH, getQuery("theme[scheme<>'profile'].concept AContainS ['DLKM', 'Basis-DLM', 'DLM50']"))
        def propertyAndLiteralNested2Check = allAxGebaeudefunktion.responseData.features.stream().filter(f -> {
            def themes = f.properties.theme.stream()
                    .filter(theme -> !theme.scheme.equals('profile'))
                    .toList()
            if (themes.size()==0)
                return false
            return themes.stream()
                    .map(theme -> theme.concept)
                    .flatMap(List::stream)
                    .filter(concept -> concept.equals('DLKM') || concept.equals('Basis-DLM') || concept.equals('DLM50'))
                    .distinct()
                    .count()==3
        }).toList()

        then: "Success and returns GeoJSON"
        assertSuccess(propertyAndLiteralNested2)

        and: "Returns the same number of records"
        propertyAndLiteralNested2.responseData.numberReturned == propertyAndLiteralNested2Check.size()

        and: "Returns the same records arrays"
        for (int i=0; i<propertyAndLiteralNested2.responseData.numberReturned; i++) {
            assertFeature(propertyAndLiteralNested2.responseData.features[i], propertyAndLiteralNested2Check.get(i))
        }

        when: "1c. Data is selected using a nested filter theme[position()=1].concept ACONTAINS ['DLKM', 'Basis-DLM', 'DLM50']"
        def propertyAndLiteralNestedPosition = getRequest(restClient, AX_GEBAEUDEFUNKTION_PATH, getQuery("theme[position()=1].concept AContainS ['DLKM', 'Basis-DLM', 'DLM50']"))
        def propertyAndLiteralNestedPositionCheck = allAxGebaeudefunktion.responseData.features.stream().filter(f -> {
            def themes = (List) f.properties.theme
            if (themes.size()==0)
                return false
            return themes.get(0)
                    .getAt("concept")
                    .stream()
                    .filter(concept -> concept.equals('DLKM') || concept.equals('Basis-DLM') || concept.equals('DLM50'))
                    .distinct()
                    .count()==3
        }).toList()

        then: "Success and returns GeoJSON"
        assertSuccess(propertyAndLiteralNestedPosition)

        and: "Returns the same number of records"
        propertyAndLiteralNestedPosition.responseData.numberReturned == propertyAndLiteralNestedPositionCheck.size()

        and: "Returns the same records arrays"
        for (int i=0; i<propertyAndLiteralNestedPosition.responseData.numberReturned; i++) {
            assertFeature(propertyAndLiteralNestedPosition.responseData.features[i], propertyAndLiteralNestedPositionCheck.get(i))
        }

        when: "1d. Data is selected using a nested filter theme[position()>1].concept ACONTAINS ['DLKM', 'Basis-DLM', 'DLM50']"
        def propertyAndLiteralNestedPosition2 = getRequest(restClient, AX_GEBAEUDEFUNKTION_PATH, getQuery("theme[position()>1].concept AContainS ['DLKM', 'Basis-DLM', 'DLM50']"))
        def propertyAndLiteralNestedPosition2Check = allAxGebaeudefunktion.responseData.features.stream().filter(f -> {
            def themes = (List) f.properties.theme
            def selectedThemes = IntStream.range(0, themes.size())
                    .filter(i -> i>0)
                    .mapToObj(i-> themes.get(i))
                    .toList()
            if (selectedThemes.size()==0)
                return false
            return selectedThemes.stream()
                    .map(theme -> theme.concept)
                    .flatMap(List::stream)
                    .filter(concept -> concept.equals('DLKM') || concept.equals('Basis-DLM') || concept.equals('DLM50'))
                    .distinct()
                    .count()==3
        }).toList()

        then: "Success and returns GeoJSON"
        assertSuccess(propertyAndLiteralNestedPosition2)

        and: "Returns the same number of records"
        propertyAndLiteralNestedPosition2.responseData.numberReturned == propertyAndLiteralNestedPosition2Check.size()

        and: "Returns the same records arrays"
        for (int i=0; i<propertyAndLiteralNestedPosition2.responseData.numberReturned; i++) {
            assertFeature(propertyAndLiteralNestedPosition2.responseData.features[i], propertyAndLiteralNestedPosition2Check.get(i))
        }

        when: "2. Data is selected using a filter ['DLKM', 'Basis-DLM', 'DLM50] ACONTAINS theme.concept"
        def propertyAndLiteral2 = getRequest(restClient, AX_GEBAEUDEFUNKTION_PATH, getQuery("['DLKM', 'Basis-DLM', 'DLM50'] AContainS theme.concept"))
        def propertyAndLiteralCheck2 = allAxGebaeudefunktion.responseData.features.stream().filter(f -> f.properties.theme.stream()
                .map(theme -> theme.concept)
                .flatMap(List::stream)
                .noneMatch(concept -> !concept.equals('DLKM') && !concept.equals('Basis-DLM') && !concept.equals('DLM50'))).toList()

        then: "Success and returns GeoJSON"
        assertSuccess(propertyAndLiteral2)

        and: "Returns the same number of records"
        propertyAndLiteral2.responseData.numberReturned == propertyAndLiteralCheck2.size()

        and: "Returns the same records arrays"
        for (int i=0; i<propertyAndLiteral2.responseData.numberReturned; i++) {
            assertFeature(propertyAndLiteral2.responseData.features[i], propertyAndLiteralCheck2.get(i))
        }

        when: "3. Data is selected using a filter ['DLKM', 'Basis-DLM', 'DLM50'] ACONTAINS ['DLKM', 'Basis-DLM', 'DLM50]"
        def literalAndLiteral = getRequest(restClient, AX_GEBAEUDEFUNKTION_PATH, getQuery("['DLKM', 'Basis-DLM', 'DLM50'] AContainS ['DLKM', 'Basis-DLM', 'DLM50']"))

        then: "Success and returns GeoJSON"
        assertSuccess(literalAndLiteral)

        and: "Returns all records"
        literalAndLiteral.responseData.numberReturned == allAxGebaeudefunktion.responseData.features.size()

        when: "4. Data is selected using a filter ['DLKM', 'Basis-DLM'] ACONTAINS ['DLKM', 'Basis-DLM', 'DLM50]"
        def literalAndLiteral2 = getRequest(restClient, AX_GEBAEUDEFUNKTION_PATH, getQuery("['DLKM', 'Basis-DLM'] AContainS ['DLKM', 'Basis-DLM', 'DLM50']"))

        then: "Success and returns GeoJSON"
        assertSuccess(literalAndLiteral2)

        and: "Returns no records"
        literalAndLiteral2.responseData.numberReturned == 0

        /* TODO not implemented
        when: "5. Data is selected using a filter theme.concept"
        def twoProperties = getRequest(restClient, AX_GEBAEUDEFUNKTION_PATH, getQuery("theme.concept AContainS theme.concept"))

        then: "Success and returns GeoJSON"
        assertSuccess(twoProperties)

        and: "Returns all records"
        twoProperties.responseData.numberReturned == allAxGebaeudefunktion.responseData.numberReturned
         */
    }

    def "Operator aequals"() {
        given: "Records in the AX_Gebaeudefunktion codelist"

        when: "1. Data is selected using a filter theme.concept AEQUALS ['DLKM', 'Basis-DLM', 'DLM50']"
        def propertyAndLiteral = getRequest(restClient, AX_GEBAEUDEFUNKTION_PATH, getQuery("theme.concept AEqualS ['DLKM', 'Basis-DLM', 'DLM50']"))
        def propertyAndLiteralCheck = allAxGebaeudefunktion.responseData.features.stream().filter(f -> {
            def concepts = f.properties.theme.stream()
                    .map(theme -> theme.concept)
                    .flatMap(List::stream)
                    .distinct()
                    .toList()
            return concepts.size()==3 && concepts.contains('DLKM') && concepts.contains('Basis-DLM') && concepts.contains('DLM50')
        }).toList()

        then: "Success and returns GeoJSON"
        assertSuccess(propertyAndLiteral)

        and: "Returns the same number of records"
        propertyAndLiteral.responseData.numberReturned == propertyAndLiteralCheck.size()

        and: "Returns the same records arrays"
        for (int i=0; i<propertyAndLiteral.responseData.numberReturned; i++) {
            assertFeature(propertyAndLiteral.responseData.features[i], propertyAndLiteralCheck.get(i))
        }

        when: "1a. Data is selected using a nested filter theme[scheme='profile'].concept AEQUALS ['DLKM', 'Basis-DLM', 'DLM50']"
        def propertyAndLiteralNested = getRequest(restClient, AX_GEBAEUDEFUNKTION_PATH, getQuery("theme[scheme='profile'].concept AEQUALS ['DLKM', 'Basis-DLM', 'DLM50']"))
        def propertyAndLiteralNestedCheck = allAxGebaeudefunktion.responseData.features.stream().filter(f -> {
            def themes = f.properties.theme.stream()
                    .filter(theme -> theme.scheme.equals('profile'))
                    .toList()
            if (themes.size()==0)
                return false
            def concepts = themes.stream()
                    .map(theme -> theme.concept)
                    .flatMap(List::stream)
                    .distinct()
                    .toList()
            return concepts.size()==3 && concepts.contains('DLKM') && concepts.contains('Basis-DLM') && concepts.contains('DLM50')
        }).toList()

        then: "Success and returns GeoJSON"
        assertSuccess(propertyAndLiteralNested)

        and: "Returns the same number of records"
        propertyAndLiteralNested.responseData.numberReturned == propertyAndLiteralNestedCheck.size()

        and: "Returns the same records arrays"
        for (int i=0; i<propertyAndLiteralNested.responseData.numberReturned; i++) {
            assertFeature(propertyAndLiteralNested.responseData.features[i], propertyAndLiteralNestedCheck.get(i))
        }

        when: "1b. Data is selected using a nested filter theme[scheme<>'profile'].concept AEQUALS ['DLKM', 'Basis-DLM', 'DLM50']"
        def propertyAndLiteralNested2 = getRequest(restClient, AX_GEBAEUDEFUNKTION_PATH, getQuery("theme[scheme<>'profile'].concept AEQUALS ['DLKM', 'Basis-DLM', 'DLM50']"))
        def propertyAndLiteralNested2Check = allAxGebaeudefunktion.responseData.features.stream().filter(f -> {
            def themes = f.properties.theme.stream()
                    .filter(theme -> !theme.scheme.equals('profile'))
                    .toList()
            if (themes.size()==0)
                return false
            def concepts = themes.stream()
                    .map(theme -> theme.concept)
                    .flatMap(List::stream)
                    .distinct()
                    .toList()
            return concepts.size()==3 && concepts.contains('DLKM') && concepts.contains('Basis-DLM') && concepts.contains('DLM50')
        }).toList()

        then: "Success and returns GeoJSON"
        assertSuccess(propertyAndLiteralNested2)

        and: "Returns the same number of records"
        propertyAndLiteralNested2.responseData.numberReturned == propertyAndLiteralNested2Check.size()

        and: "Returns the same records arrays"
        for (int i=0; i<propertyAndLiteralNested2.responseData.numberReturned; i++) {
            assertFeature(propertyAndLiteralNested2.responseData.features[i], propertyAndLiteralNested2Check.get(i))
        }

        when: "1c. Data is selected using a nested filter theme[position()=1].concept AEQUALS ['DLKM', 'Basis-DLM', 'DLM50']"
        def propertyAndLiteralNestedPosition = getRequest(restClient, AX_GEBAEUDEFUNKTION_PATH, getQuery("theme[position()=1].concept AEQUALS ['DLKM', 'Basis-DLM', 'DLM50']"))
        def propertyAndLiteralNestedPositionCheck = allAxGebaeudefunktion.responseData.features.stream().filter(f -> {
            def themes = (List) f.properties.theme
            if (themes.size()==0)
                return false
            def concepts = themes.get(0)
                    .getAt("concept")
                    .stream()
                    .distinct()
                    .toList()
            return concepts.size()==3 && concepts.contains('DLKM') && concepts.contains('Basis-DLM') && concepts.contains('DLM50')
        }).toList()

        then: "Success and returns GeoJSON"
        assertSuccess(propertyAndLiteralNestedPosition)

        and: "Returns the same number of records"
        propertyAndLiteralNestedPosition.responseData.numberReturned == propertyAndLiteralNestedPositionCheck.size()

        and: "Returns the same records arrays"
        for (int i=0; i<propertyAndLiteralNestedPosition.responseData.numberReturned; i++) {
            assertFeature(propertyAndLiteralNestedPosition.responseData.features[i], propertyAndLiteralNestedPositionCheck.get(i))
        }

        when: "1d. Data is selected using a nested filter theme[position()>1].concept AEQUALS ['DLKM', 'Basis-DLM', 'DLM50']"
        def propertyAndLiteralNestedPosition2 = getRequest(restClient, AX_GEBAEUDEFUNKTION_PATH, getQuery("theme[position()>1].concept AEQUALS ['DLKM', 'Basis-DLM', 'DLM50']"))
        def propertyAndLiteralNestedPosition2Check = allAxGebaeudefunktion.responseData.features.stream().filter(f -> {
            def themes = (List) f.properties.theme
            def selectedThemes = IntStream.range(0, themes.size())
                    .filter(i -> i>0)
                    .mapToObj(i-> themes.get(i))
                    .toList()
            if (selectedThemes.size()==0)
                return false
            def concepts = selectedThemes.stream()
                    .map(theme -> theme.concept)
                    .stream()
                    .distinct()
                    .toList()
            return concepts.size()==3 && concepts.contains('DLKM') && concepts.contains('Basis-DLM') && concepts.contains('DLM50')
        }).toList()

        then: "Success and returns GeoJSON"
        assertSuccess(propertyAndLiteralNestedPosition2)

        and: "Returns the same number of records"
        propertyAndLiteralNestedPosition2.responseData.numberReturned == propertyAndLiteralNestedPosition2Check.size()

        and: "Returns the same records arrays"
        for (int i=0; i<propertyAndLiteralNestedPosition2.responseData.numberReturned; i++) {
            assertFeature(propertyAndLiteralNestedPosition2.responseData.features[i], propertyAndLiteralNestedPosition2Check.get(i))
        }

        when: "2. Data is selected using a filter ['DLKM', 'Basis-DLM', 'DLM50] AEQUALS theme.concept"
        def propertyAndLiteral2 = getRequest(restClient, AX_GEBAEUDEFUNKTION_PATH, getQuery("['DLKM', 'Basis-DLM', 'DLM50'] AEQUALS theme.concept"))

        then: "Success and returns GeoJSON"
        assertSuccess(propertyAndLiteral2)

        and: "Returns the same number of records"
        propertyAndLiteral2.responseData.numberReturned == propertyAndLiteralCheck.size()

        and: "Returns the same records arrays"
        for (int i=0; i<propertyAndLiteral2.responseData.numberReturned; i++) {
            assertFeature(propertyAndLiteral2.responseData.features[i], propertyAndLiteralCheck.get(i))
        }

        when: "3. Data is selected using a filter ['DLKM', 'Basis-DLM', 'DLM50'] AEQUALS ['DLKM', 'Basis-DLM', 'DLM50]"
        def literalAndLiteral = getRequest(restClient, AX_GEBAEUDEFUNKTION_PATH, getQuery("['DLKM', 'Basis-DLM', 'DLM50'] AEQUALS ['DLKM', 'Basis-DLM', 'DLM50']"))

        then: "Success and returns GeoJSON"
        assertSuccess(literalAndLiteral)

        and: "Returns all records"
        literalAndLiteral.responseData.numberReturned == allAxGebaeudefunktion.responseData.features.size()

        when: "4. Data is selected using a filter ['DLKM', 'Basis-DLM'] ACONTAINS ['DLKM', 'Basis-DLM', 'DLM50]"
        def literalAndLiteral2 = getRequest(restClient, AX_GEBAEUDEFUNKTION_PATH, getQuery("['DLKM', 'Basis-DLM'] AEQUALS ['DLKM', 'Basis-DLM', 'DLM50']"))

        then: "Success and returns GeoJSON"
        assertSuccess(literalAndLiteral2)

        and: "Returns no records"
        literalAndLiteral2.responseData.numberReturned == 0

        /* TODO not implemented
        when: "5. Data is selected using a filter theme.concept"
        def twoProperties = getRequest(restClient, AX_GEBAEUDEFUNKTION_PATH, getQuery("theme.concept AEQUALS theme.concept"))

        then: "Success and returns GeoJSON"
        assertSuccess(twoProperties)

        and: "Returns all records"
        twoProperties.responseData.numberReturned == allAxGebaeudefunktion.responseData.numberReturned
         */
    }

    def "Operator aoverlaps"() {
        given: "Records in the AX_Gebaeudefunktion codelist"

        when: "1. Data is selected using a filter theme.concept AOVERLAPS ['DLKM', 'Basis-DLM', 'DLM50']"
        def propertyAndLiteral = getRequest(restClient, AX_GEBAEUDEFUNKTION_PATH, getQuery("theme.concept AOverlapS ['DLKM', 'Basis-DLM', 'DLM50']"))
        def propertyAndLiteralCheck = allAxGebaeudefunktion.responseData.features.stream().filter(f -> f.properties.theme.stream()
                .map(theme -> theme.concept)
                .flatMap(List::stream)
                .anyMatch(concept -> concept.equals('DLKM') || concept.equals('Basis-DLM') || concept.equals('DLM50'))).toList()

        then: "Success and returns GeoJSON"
        assertSuccess(propertyAndLiteral)

        and: "Returns the same number of records"
        propertyAndLiteral.responseData.numberReturned == propertyAndLiteralCheck.size()

        and: "Returns the same records arrays"
        for (int i=0; i<propertyAndLiteral.responseData.numberReturned; i++) {
            assertFeature(propertyAndLiteral.responseData.features[i], propertyAndLiteralCheck.get(i))
        }

        when: "1a. Data is selected using a nested filter theme[scheme='profile'].concept AOVERLAPS ['DLKM', 'Basis-DLM', 'DLM50']"
        def propertyAndLiteralNested = getRequest(restClient, AX_GEBAEUDEFUNKTION_PATH, getQuery("theme[scheme='profile'].concept AOVERLAPS ['DLKM', 'Basis-DLM', 'DLM50']"))
        def propertyAndLiteralNestedCheck = allAxGebaeudefunktion.responseData.features.stream().filter(f -> {
            def themes = f.properties.theme.stream()
                    .filter(theme -> theme.scheme.equals('profile'))
                    .toList()
            if (themes.size()==0)
                return false
            return themes.stream()
                    .map(theme -> theme.concept)
                    .flatMap(List::stream)
                    .anyMatch(concept -> concept.equals('DLKM') || concept.equals('Basis-DLM') || concept.equals('DLM50'))
        }).toList()

        then: "Success and returns GeoJSON"
        assertSuccess(propertyAndLiteralNested)

        and: "Returns the same number of records"
        propertyAndLiteralNested.responseData.numberReturned == propertyAndLiteralNestedCheck.size()

        and: "Returns the same records arrays"
        for (int i=0; i<propertyAndLiteralNested.responseData.numberReturned; i++) {
            assertFeature(propertyAndLiteralNested.responseData.features[i], propertyAndLiteralNestedCheck.get(i))
        }

        when: "1b. Data is selected using a nested filter theme[scheme<>'profile'].concept AOVERLAPS ['DLKM', 'Basis-DLM', 'DLM50']"
        def propertyAndLiteralNested2 = getRequest(restClient, AX_GEBAEUDEFUNKTION_PATH, getQuery("theme[scheme<>'profile'].concept AOVERLAPS ['DLKM', 'Basis-DLM', 'DLM50']"))
        def propertyAndLiteralNested2Check = allAxGebaeudefunktion.responseData.features.stream().filter(f -> {
            def themes = f.properties.theme.stream()
                    .filter(theme -> !theme.scheme.equals('profile'))
                    .toList()
            if (themes.size()==0)
                return false
            return themes.stream()
                    .map(theme -> theme.concept)
                    .flatMap(List::stream)
                    .anyMatch(concept -> concept.equals('DLKM') || concept.equals('Basis-DLM') || concept.equals('DLM50'))
        }).toList()

        then: "Success and returns GeoJSON"
        assertSuccess(propertyAndLiteralNested2)

        and: "Returns the same number of records"
        propertyAndLiteralNested2.responseData.numberReturned == propertyAndLiteralNested2Check.size()

        and: "Returns the same records arrays"
        for (int i=0; i<propertyAndLiteralNested2.responseData.numberReturned; i++) {
            assertFeature(propertyAndLiteralNested2.responseData.features[i], propertyAndLiteralNested2Check.get(i))
        }

        when: "1c. Data is selected using a nested filter theme[position()=1].concept AOVERLAPS ['DLKM', 'Basis-DLM', 'DLM50']"
        def propertyAndLiteralNestedPosition = getRequest(restClient, AX_GEBAEUDEFUNKTION_PATH, getQuery("theme[position()=1].concept AOVERLAPS ['DLKM', 'Basis-DLM', 'DLM50']"))
        def propertyAndLiteralNestedPositionCheck = allAxGebaeudefunktion.responseData.features.stream().filter(f -> {
            def themes = (List) f.properties.theme
            if (themes.size()==0)
                return false
            return themes.get(0)
                    .getAt("concept")
                    .stream()
                    .anyMatch(concept -> concept.equals('DLKM') || concept.equals('Basis-DLM') || concept.equals('DLM50'))
        }).toList()

        then: "Success and returns GeoJSON"
        assertSuccess(propertyAndLiteralNestedPosition)

        and: "Returns the same number of records"
        propertyAndLiteralNestedPosition.responseData.numberReturned == propertyAndLiteralNestedPositionCheck.size()

        and: "Returns the same records arrays"
        for (int i=0; i<propertyAndLiteralNestedPosition.responseData.numberReturned; i++) {
            assertFeature(propertyAndLiteralNestedPosition.responseData.features[i], propertyAndLiteralNestedPositionCheck.get(i))
        }

        when: "1d. Data is selected using a nested filter theme[position()>1].concept AOVERLAPS ['DLKM', 'Basis-DLM', 'DLM50']"
        def propertyAndLiteralNestedPosition2 = getRequest(restClient, AX_GEBAEUDEFUNKTION_PATH, getQuery("theme[position()>1].concept AOVERLAPS ['DLKM', 'Basis-DLM', 'DLM50']"))
        def propertyAndLiteralNestedPosition2Check = allAxGebaeudefunktion.responseData.features.stream().filter(f -> {
            def themes = (List) f.properties.theme
            def selectedThemes = IntStream.range(0, themes.size())
                    .filter(i -> i>0)
                    .mapToObj(i-> themes.get(i))
                    .toList()
            if (selectedThemes.size()==0)
                return false
            return selectedThemes.stream()
                    .map(theme -> theme.concept)
                    .flatMap(List::stream)
                    .anyMatch(concept -> concept.equals('DLKM') || concept.equals('Basis-DLM') || concept.equals('DLM50'))
        }).toList()

        then: "Success and returns GeoJSON"
        assertSuccess(propertyAndLiteralNestedPosition2)

        and: "Returns the same number of records"
        propertyAndLiteralNestedPosition2.responseData.numberReturned == propertyAndLiteralNestedPosition2Check.size()

        and: "Returns the same records arrays"
        for (int i=0; i<propertyAndLiteralNestedPosition2.responseData.numberReturned; i++) {
            assertFeature(propertyAndLiteralNestedPosition2.responseData.features[i], propertyAndLiteralNestedPosition2Check.get(i))
        }

        when: "2. Data is selected using a filter ['DLKM', 'Basis-DLM', 'DLM50] AOVERLAPS theme.concept"
        def propertyAndLiteral2 = getRequest(restClient, AX_GEBAEUDEFUNKTION_PATH, getQuery("['DLKM', 'Basis-DLM', 'DLM50'] AOVERLAPS theme.concept"))

        then: "Success and returns GeoJSON"
        assertSuccess(propertyAndLiteral2)

        and: "Returns the same number of records"
        propertyAndLiteral2.responseData.numberReturned == propertyAndLiteralCheck.size()

        and: "Returns the same records arrays"
        for (int i=0; i<propertyAndLiteral2.responseData.numberReturned; i++) {
            assertFeature(propertyAndLiteral2.responseData.features[i], propertyAndLiteralCheck.get(i))
        }

        when: "3. Data is selected using a filter ['DLKM', 'Basis-DLM', 'DLM50'] AOVERLAPS ['DLKM', 'Basis-DLM', 'DLM50]"
        def literalAndLiteral = getRequest(restClient, AX_GEBAEUDEFUNKTION_PATH, getQuery("['DLKM', 'Basis-DLM', 'DLM50'] AOVERLAPS ['DLKM', 'Basis-DLM', 'DLM50']"))

        then: "Success and returns GeoJSON"
        assertSuccess(literalAndLiteral)

        and: "Returns all records"
        literalAndLiteral.responseData.numberReturned == allAxGebaeudefunktion.responseData.features.size()

        when: "4. Data is selected using a filter ['DLKM', 'Basis-DLM'] AOVERLAPS ['DLM50]"
        def literalAndLiteral2 = getRequest(restClient, AX_GEBAEUDEFUNKTION_PATH, getQuery("['DLKM', 'Basis-DLM'] AOVERLAPS ['DLM50']"))

        then: "Success and returns GeoJSON"
        assertSuccess(literalAndLiteral2)

        and: "Returns no records"
        literalAndLiteral2.responseData.numberReturned == 0

        /* TODO not implemented
        when: "5. Data is selected using a filter theme.concept"
        def twoProperties = getRequest(restClient, AX_GEBAEUDEFUNKTION_PATH, getQuery("theme.concept AOVERLAPS theme.concept"))

        then: "Success and returns GeoJSON"
        assertSuccess(twoProperties)

        and: "Returns all records"
        twoProperties.responseData.numberReturned == allAxGebaeudefunktion.responseData.numberReturned
         */
    }

    def "Operator containedby"() {
        given: "Records in the AX_Gebaeudefunktion codelist"

        when: "1. Data is selected using a filter theme.concept CONTAINEDBY ['DLKM', 'Basis-DLM', 'DLM50']"
        def propertyAndLiteral = getRequest(restClient, AX_GEBAEUDEFUNKTION_PATH, getQuery("theme.concept CONtainEDBY ['DLKM', 'Basis-DLM', 'DLM50']"))
        def propertyAndLiteralCheck = allAxGebaeudefunktion.responseData.features.stream().filter(f -> f.properties.theme.stream()
                .map(theme -> theme.concept)
                .flatMap(List::stream)
                .noneMatch(concept -> !concept.equals('DLKM') && !concept.equals('Basis-DLM') && !concept.equals('DLM50'))).toList()

        then: "Success and returns GeoJSON"
        assertSuccess(propertyAndLiteral)

        and: "Returns the same number of records"
        propertyAndLiteral.responseData.numberReturned == propertyAndLiteralCheck.size()

        and: "Returns the same records arrays"
        for (int i=0; i<propertyAndLiteral.responseData.numberReturned; i++) {
            assertFeature(propertyAndLiteral.responseData.features[i], propertyAndLiteralCheck.get(i))
        }

        when: "1a. Data is selected using a nested filter theme[scheme='profile'].concept CONTAINEDBY ['DLKM', 'Basis-DLM', 'DLM50']"
        def propertyAndLiteralNested = getRequest(restClient, AX_GEBAEUDEFUNKTION_PATH, getQuery("theme[scheme='profile'].concept CONTAINEDBY ['DLKM', 'Basis-DLM', 'DLM50']"))
        def propertyAndLiteralNestedCheck = allAxGebaeudefunktion.responseData.features.stream().filter(f -> {
            def themes = f.properties.theme.stream()
                    .filter(theme -> theme.scheme.equals('profile'))
                    .toList()
            if (themes.size()==0)
                return false
            return themes.stream()
                    .map(theme -> theme.concept)
                    .flatMap(List::stream)
                    .noneMatch(concept -> !concept.equals('DLKM') && !concept.equals('Basis-DLM') && !concept.equals('DLM50'))
        }).toList()

        then: "Success and returns GeoJSON"
        assertSuccess(propertyAndLiteralNested)

        and: "Returns the same number of records"
        propertyAndLiteralNested.responseData.numberReturned == propertyAndLiteralNestedCheck.size()

        and: "Returns the same records arrays"
        for (int i=0; i<propertyAndLiteralNested.responseData.numberReturned; i++) {
            assertFeature(propertyAndLiteralNested.responseData.features[i], propertyAndLiteralNestedCheck.get(i))
        }

        when: "1b. Data is selected using a nested filter theme[scheme<>'profile'].concept CONTAINEDBY ['DLKM', 'Basis-DLM', 'DLM50']"
        def propertyAndLiteralNested2 = getRequest(restClient, AX_GEBAEUDEFUNKTION_PATH, getQuery("theme[scheme<>'profile'].concept CONTAINEDBY ['DLKM', 'Basis-DLM', 'DLM50']"))
        def propertyAndLiteralNested2Check = allAxGebaeudefunktion.responseData.features.stream().filter(f -> {
            def themes = f.properties.theme.stream()
                    .filter(theme -> !theme.scheme.equals('profile'))
                    .toList()
            if (themes.size()==0)
                return false
            return themes.stream()
                    .map(theme -> theme.concept)
                    .flatMap(List::stream)
                    .noneMatch(concept -> !concept.equals('DLKM') && !concept.equals('Basis-DLM') && !concept.equals('DLM50'))
        }).toList()

        then: "Success and returns GeoJSON"
        assertSuccess(propertyAndLiteralNested2)

        and: "Returns the same number of records"
        propertyAndLiteralNested2.responseData.numberReturned == propertyAndLiteralNested2Check.size()

        and: "Returns the same records arrays"
        for (int i=0; i<propertyAndLiteralNested2.responseData.numberReturned; i++) {
            assertFeature(propertyAndLiteralNested2.responseData.features[i], propertyAndLiteralNested2Check.get(i))
        }

        when: "1c. Data is selected using a nested filter theme[position() IN (1)].concept CONTAINEDBY ['DLKM', 'Basis-DLM', 'DLM50']"
        def propertyAndLiteralNestedPosition = getRequest(restClient, AX_GEBAEUDEFUNKTION_PATH, getQuery("theme[position() IN (1)].concept CONTAINEDBY ['DLKM', 'Basis-DLM', 'DLM50']"))
        def propertyAndLiteralNestedPositionCheck = allAxGebaeudefunktion.responseData.features.stream().filter(f -> {
            def themes = (List) f.properties.theme
            if (themes.size()==0)
                return false
            return  themes.get(0)
                    .getAt("concept")
                    .stream()
                    .noneMatch(concept -> !concept.equals('DLKM') && !concept.equals('Basis-DLM') && !concept.equals('DLM50'))
        }).toList()

        then: "Success and returns GeoJSON"
        assertSuccess(propertyAndLiteralNestedPosition)

        and: "Returns the same number of records"
        propertyAndLiteralNestedPosition.responseData.numberReturned == propertyAndLiteralNestedPositionCheck.size()

        and: "Returns the same records arrays"
        for (int i=0; i<propertyAndLiteralNestedPosition.responseData.numberReturned; i++) {
            assertFeature(propertyAndLiteralNestedPosition.responseData.features[i], propertyAndLiteralNestedPositionCheck.get(i))
        }

        when: "1d. Data is selected using a nested filter theme[position() BETWEEN 2 AND 3].concept CONTAINEDBY ['DLKM', 'Basis-DLM', 'DLM50']"
        def propertyAndLiteralNestedPosition2 = getRequest(restClient, AX_GEBAEUDEFUNKTION_PATH, getQuery("theme[position() between 2 and 3].concept CONTAINEDBY ['DLKM', 'Basis-DLM', 'DLM50']"))
        def propertyAndLiteralNestedPosition2Check = allAxGebaeudefunktion.responseData.features.stream().filter(f -> {
            def themes = (List) f.properties.theme
            def selectedThemes = IntStream.range(0, themes.size())
                    .filter(i -> i>0 && i<3)
                    .mapToObj(i-> themes.get(i))
                    .toList()
            if (selectedThemes.size()==0)
                return false
            return selectedThemes.stream()
                    .map(theme -> theme.concept)
                    .flatMap(List::stream)
                    .noneMatch(concept -> !concept.equals('DLKM') && !concept.equals('Basis-DLM') && !concept.equals('DLM50'))
        }).toList()

        then: "Success and returns GeoJSON"
        assertSuccess(propertyAndLiteralNestedPosition2)

        and: "Returns the same number of records"
        propertyAndLiteralNestedPosition2.responseData.numberReturned == propertyAndLiteralNestedPosition2Check.size()

        and: "Returns the same records arrays"
        for (int i=0; i<propertyAndLiteralNestedPosition2.responseData.numberReturned; i++) {
            assertFeature(propertyAndLiteralNestedPosition2.responseData.features[i], propertyAndLiteralNestedPosition2Check.get(i))
        }

        when: "2. Data is selected using a filter ['DLKM', 'Basis-DLM', 'DLM50] CONTAINEDBY theme.concept"
        def propertyAndLiteral2 = getRequest(restClient, AX_GEBAEUDEFUNKTION_PATH, getQuery("['DLKM', 'Basis-DLM', 'DLM50'] CONTAINEDBY theme.concept"))
        def propertyAndLiteralCheck2 = allAxGebaeudefunktion.responseData.features.stream().filter(f -> f.properties.theme.stream()
                .map(theme -> theme.concept)
                .flatMap(List::stream)
                .filter(concept -> concept.equals('DLKM') || concept.equals('Basis-DLM') || concept.equals('DLM50'))
                .distinct()
                .count()==3).toList()

        then: "Success and returns GeoJSON"
        assertSuccess(propertyAndLiteral2)

        and: "Returns the same number of records"
        propertyAndLiteral2.responseData.numberReturned == propertyAndLiteralCheck2.size()

        and: "Returns the same records arrays"
        for (int i=0; i<propertyAndLiteral2.responseData.numberReturned; i++) {
            assertFeature(propertyAndLiteral2.responseData.features[i], propertyAndLiteralCheck2.get(i))
        }

        when: "3. Data is selected using a filter ['DLKM', 'Basis-DLM', 'DLM50'] CONTAINEDBY ['DLKM', 'Basis-DLM', 'DLM50']"
        def literalAndLiteral = getRequest(restClient, AX_GEBAEUDEFUNKTION_PATH, getQuery("['DLKM', 'Basis-DLM', 'DLM50'] CONTAINEDBY ['DLKM', 'Basis-DLM', 'DLM50']"))

        then: "Success and returns GeoJSON"
        assertSuccess(literalAndLiteral)

        and: "Returns all records"
        literalAndLiteral.responseData.numberReturned == allAxGebaeudefunktion.responseData.features.size()

        when: "4. Data is selected using a filter ['DLKM', 'Basis-DLM', 'DLM50'] CONTAINEDBY ['DLKM', 'Basis-DLM']"
        def literalAndLiteral2 = getRequest(restClient, AX_GEBAEUDEFUNKTION_PATH, getQuery("['DLKM', 'Basis-DLM', 'DLM50'] CONTAINEDBY ['DLKM', 'Basis-DLM']"))

        then: "Success and returns GeoJSON"
        assertSuccess(literalAndLiteral2)

        and: "Returns no records"
        literalAndLiteral2.responseData.numberReturned == 0

        /* TODO not implemented
        when: "5. Data is selected using a filter theme.concept"
        def twoProperties = getRequest(restClient, AX_GEBAEUDEFUNKTION_PATH, getQuery("theme.concept CONTAINEDBY theme.concept"))

        then: "Success and returns GeoJSON"
        assertSuccess(twoProperties)

        and: "Returns all records"
        twoProperties.responseData.numberReturned == allAxGebaeudefunktion.responseData.numberReturned
         */
    }

    // Logical operators

    def "Logical operators"() {
        given: "CulturePnt features in the Daraa dataset"

        when: "1. Data is selected using a filter F_CODE=F_CODE AnD NoT (F_CODE='AL030' oR (ZI001_SDV AFTER 2011-12-31T23:59:59Z aNd ZI037_REL iS nULL))"
        def logical = getRequest(restClient, CULTURE_PNT_PATH, getQuery( "F_CODE=F_CODE AnD NoT (F_CODE='AL030' oR (ZI001_SDV AFTER 2011-12-31T23:59:59Z aNd ZI037_REL iS nULL))"))
        def logicalCheck = allCulturePntFeatures.responseData.features.stream()
                .filter(f -> !(f.properties.F_CODE == 'AL030' || (f.properties.ZI001_SDV > '2011-12-31T23:59:59Z' && Objects.isNull(f.properties.ZI037_REL))))
                .toList()

        then: "Success and returns GeoJSON"
        assertSuccess(logical)

        and: "Returns all selected features"
        logical.responseData.numberReturned == logicalCheck.size()
        for (int i = 0; i < logical.responseData.numberReturned; i++) {
            assertFeature(logical.responseData.features[i], logicalCheck.get(i))
        }

        when: "2. Data is selected using a filter F_CODE='AL030' or F_CODE='AL012'"
        def logical2 = getRequest(restClient, CULTURE_PNT_PATH, getQuery( "F_CODE='AL030' or F_CODE='AL012'"))
        def logical2Check = allCulturePntFeatures.responseData.features.stream().filter(f -> f.properties.F_CODE == 'AL030' || f.properties.F_CODE == 'AL012').toList()

        then: "Success and returns GeoJSON"
        assertSuccess(logical2)

        and: "Returns the same number of features"
        logical2.responseData.numberReturned == logical2Check.size()

        and: "Returns the same feature arrays"
        for (int i = 0; i < logical2.responseData.numberReturned; i++) {
            assertFeature(logical2.responseData.features[i], logical2Check.get(i))
        }
    }

    LinkedHashMap<String, String> getQuery(String filter) {
        return json
        ? [filter:cql.write(cql.read(filter, Cql.Format.TEXT), Cql.Format.JSON).replace("\n",""),"filter-lang":"cql-json",limit:limit]
        : [filter:filter,limit:limit]
    }

    LinkedHashMap<String, String> getQuery4326(String filter) {
        return json
                ? [filter:cql.write(cql.read(filter, Cql.Format.TEXT), Cql.Format.JSON).replace("\n",""),"filter-lang":"cql-json","filter-crs":epsg4326,limit:limit]
                : [filter:filter,"filter-crs":epsg4326,limit:limit]
    }

    static void assertSuccess(Object response) {
        assert response.status == 200
        assert response.getContentType() == "application/geo+json" || response.getContentType() == "application/json"
    }

    static void assertFeature(Object feature1, Object feature2) {
        assert feature1.id == feature2.id
        assert feature1.type == feature2.type
        assert feature1.properties == feature2.properties
        assert feature1.geometry == feature2.geometry
    }

    static void assertSameResult(Object request1, Object request2) {
        request1.responseData.numberReturned == request2.responseData.numberReturned
        for (int i=0; i<request1.responseData.numberReturned; i++) {
            assertFeature(request1.responseData.features[i], request2.responseData.features[i])
        }
    }

    static Object getRequest(restClient, path, query) {
        return restClient.request(Method.GET,  ContentType.JSON, { req ->
            uri.path = path
            uri.query = query
            headers.Accept = path.contains("/items") ? GEO_JSON : JSON
        })
    }
}