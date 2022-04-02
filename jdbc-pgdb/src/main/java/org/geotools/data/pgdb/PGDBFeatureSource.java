/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2002-2012, Open Source Geospatial Foundation (OSGeo)
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    version 2.1 of the License.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 */
package org.geotools.data.pgdb;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geotools.data.FeatureReader;
import org.geotools.data.Query;
import org.geotools.data.store.ContentEntry;
import org.geotools.data.store.ContentFeatureSource;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.util.logging.Logging;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

/**
 * ESRI Personal Geodatabase FeatureSource
 * 
 * @author MapPlus, mapplus@gmail.com, http://onspatial.com
 * @since 2012-10-30
 * @see
 * 
 */
@SuppressWarnings("unchecked")
public class PGDBFeatureSource extends ContentFeatureSource {
    protected static final Logger LOGGER = Logging.getLogger(PGDBFeatureSource.class);

    private final PGDBDataStore dataStore;

    @SuppressWarnings("rawtypes")
    public PGDBFeatureSource(PGDBDataStore dataStore, ContentEntry contentEntry) throws IOException {
        super(contentEntry, new Query(Query.ALL));
        this.query.setTypeName(contentEntry.getTypeName());
        this.dataStore = dataStore;
    }

    @Override
    protected SimpleFeatureType buildFeatureType() throws IOException {
        String typeName = this.entry.getTypeName();
        GDBSchema gdbSchema = dataStore.sr.getSchemas().get(typeName);
        if (gdbSchema == null) {
            throw new IOException(typeName + " does not exist!");
        }

        if (gdbSchema.getSchema() == null) {
            dataStore.sr.buildFeatureType(gdbSchema);
        }
        return gdbSchema.getSchema();
    }

    @Override
    protected ReferencedEnvelope getBoundsInternal(Query query) throws IOException {
        GDBSchema gdbSchema = dataStore.sr.getSchemas().get(query.getTypeName());
        if (gdbSchema == null) {
            throw new IOException(query.getTypeName() + " does not exist!");
        }
        return gdbSchema.getExtent();
    }

    @Override
    protected int getCountInternal(Query query) throws IOException {
        GDBSchema gdbSchema = dataStore.sr.getSchemas().get(query.getTypeName());
        if (gdbSchema == null) {
            throw new IOException(query.getTypeName() + " does not exist!");
        }

        Statement stmt = null;
        ResultSet rs = null;
        try {
            String sql = "SELECT COUNT(*) FROM \"" + JdbcUtilities.toAccess(query.getTypeName())
                    + "\"";
            stmt = dataStore.getConnection().createStatement();
            rs = stmt.executeQuery(sql);
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.FINER, e.getMessage(), e);
        } finally {
            JdbcUtilities.closeSafe(rs, stmt);
        }
        return -1;
    }

    @Override
    protected FeatureReader<SimpleFeatureType, SimpleFeature> getReaderInternal(Query query) throws IOException {
        return dataStore.getFeatureReader(query.getTypeName());
    }
}