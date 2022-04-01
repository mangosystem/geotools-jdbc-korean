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

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.geotools.data.FeatureReader;
import org.geotools.data.store.ContentDataStore;
import org.geotools.data.store.ContentEntry;
import org.geotools.data.store.ContentFeatureSource;
import org.geotools.feature.NameImpl;
import org.geotools.util.logging.Logging;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.Name;

/**
 * ESRI Personal Geodatabase DataStore
 * 
 * @author MapPlus, mapplus@gmail.com, http://onspatial.com
 * @since 2012-10-30
 * @see
 * 
 */
public class PGDBDataStore extends ContentDataStore {
    protected static final Logger LOGGER = Logging.getLogger(PGDBDataStore.class);

    Connection cx;

    final File pgdbFile;

    final String user;

    final String password;

    PGDBSchemaReader sr;

    public PGDBDataStore(File pgdbFile, String user, String password) {
        super();

        this.pgdbFile = pgdbFile;
        this.user = user;
        this.password = password;
        this.sr = new PGDBSchemaReader(getConnection());
    }

    protected FeatureReader<SimpleFeatureType, SimpleFeature> getFeatureReader(String typeName)
            throws IOException {
        return new PGDBFeatureReader(getConnection(), getSchema(typeName));
    }

    @Override
    public void dispose() {
        JdbcUtilities.closeSafe(cx);
    }

    @Override
    protected List<Name> createTypeNames() {
        return sr.getSchemas().keySet().stream().map(key -> new NameImpl(key)).collect(Collectors.toList());
    }

    @Override
    protected ContentFeatureSource createFeatureSource(ContentEntry contentEntry) throws IOException {
        try {
            return new PGDBFeatureSource(this, contentEntry);
        } catch (IOException e) {
            LOGGER.log(Level.FINER, e.getMessage(), e);
        }
        return null;
    }

    Connection getConnection() {
        if (cx != null) {
            return cx;
        }

        try {
            String path = JdbcUtilities.toAccess(pgdbFile.getPath());

            StringBuffer sb = new StringBuffer();
            sb.append("jdbc:ucanaccess://").append(path);

            java.util.Properties properties = new java.util.Properties();
            properties.put("charSet", "8859_1");

            if (user != null && !user.isEmpty()) {
                properties.put("user", JdbcUtilities.toAccess(user));
            }

            if (password != null && !password.isEmpty()) {
                properties.put("password", JdbcUtilities.toAccess(password));
            }
            cx = DriverManager.getConnection(sb.toString(), properties);
        } catch (SQLException e) {
            LOGGER.log(Level.FINE, e.getMessage(), e);
        }
        return cx;
    }
}
