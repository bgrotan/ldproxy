/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.tiles.domain;

import de.ii.ldproxy.ogcapi.domain.ApiExtension;
import de.ii.ldproxy.ogcapi.domain.OgcApiDataV2;
import de.ii.xtraplatform.crs.domain.BoundingBox;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.Date;
import java.util.Optional;

/**
 * Access to the cache for tile files.
 */
public interface TileCache extends ApiExtension {

    /**
     * check whether a tile is cached
     * @param tile the tile
     * @return {@code true}, if the tile is included in the cache
     * @throws IOException an error occurred while accessing files
     * @throws SQLException an error occurred while accessing an Mbtiles file
     */
    boolean tileExists(Tile tile) throws IOException, SQLException;

    /**
     * fetch a tile from the cache
     * @param tile the tile
     * @return the tile as an input stream; the result is empty, if the tile is not cached
     * @throws IOException an error occurred while accessing files
     * @throws SQLException an error occurred while accessing an Mbtiles file
     */
    Optional<InputStream> getTile(Tile tile) throws IOException, SQLException;

    /**
     * fetch the timestamp, when a tile was last modified in the cache
     * @param tile the tile
     * @return the timestamp of the last change; the result is empty, if the tile is not cached or the timestamp could not be retrieved
     */
    Optional<Date> getLastModified(Tile tile) throws IOException, SQLException;

    /**
     * checks whether a tile is cached, but contains no features
     * @param tile the tile
     * @return {@code true}, if the tile contains no features; the result is empty, if the tile is not cached
     * @throws IOException an error occurred while accessing files
     * @throws SQLException an error occurred while accessing an Mbtiles file
     */
    Optional<Boolean> tileIsEmpty(Tile tile) throws IOException, SQLException;

    /**
     * store a tile in the cache
     * @param tile the tile
     * @param content the byte array of the file content
     * @throws IOException an error occurred while accessing files
     * @throws SQLException an error occurred while accessing an Mbtiles file
     */
    void storeTile(Tile tile, byte[] content) throws IOException, SQLException;

    /**
     * delete a tile from the cache
     * @param tile the tile
     * @throws IOException an error occurred while accessing files
     * @throws SQLException an error occurred while accessing an Mbtiles file
     */
    void deleteTile(Tile tile) throws IOException, SQLException;

    /**
     * delete tiles from the cache by collection or bbox
     * @param apiData the API
     * @param tileMatrixSetId the tiling scheme for which tiles are to be deleted, empty = all tiling schemes
     * @param collectionId the collection for which tiles are to be deleted, empty = all collections
     * @param boundingBox the bounding box in which tiles are to be deleted, empty = no spatial restriction
     * @throws IOException an error occurred while accessing files
     * @throws SQLException an error occurred while accessing an Mbtiles file
     */
    void deleteTiles(OgcApiDataV2 apiData, Optional<String> collectionId, Optional<String> tileMatrixSetId, Optional<BoundingBox> boundingBox) throws IOException, SQLException;

    /**
     * clean-up temporary files that cannot be cached due to the use of parameters
     */
    void cleanup();
}