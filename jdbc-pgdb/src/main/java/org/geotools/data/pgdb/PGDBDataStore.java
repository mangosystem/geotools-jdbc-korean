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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geotools.data.AbstractDataStore;
import org.geotools.data.FeatureReader;
import org.geotools.data.Query;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.util.logging.Logging;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

/**
 * ESRI Personal Geodatabase DataStore
 * 
 * @author MapPlus, mapplus@gmail.com, http://onspatial.com
 * @since 2012-10-30
 * @see
 * 
 */
public class PGDBDataStore extends AbstractDataStore {
    protected static final Logger LOGGER = Logging.getLogger(PGDBDataStore.class);

    Connection cx;

    final File pgdbFile;

    final String user;

    final String password;

    PGDBSchemaReader sr;

    public PGDBDataStore(File pgdbFile, String user, String password) {
        super(false);

        this.pgdbFile = pgdbFile;
        this.user = user;
        this.password = password;
        this.sr = new PGDBSchemaReader(getConnection());
    }

    @Override
    public String[] getTypeNames() throws IOException {
        return sr.getSchemas().keySet().toArray(new String[sr.getSchemas().size()]);
    }

    @Override
    public SimpleFeatureType getSchema(String typeName) throws IOException {
        GDBSchema gdbSchema = sr.getSchemas().get(typeName);
        if (gdbSchema == null) {
            throw new IOException(typeName + " does not exist!");
        }

        if (gdbSchema.getSchema() == null) {
            sr.buildFeatureType(gdbSchema);
        }
        return gdbSchema.getSchema();
    }

    @Override
    protected ReferencedEnvelope getBounds(Query query) throws IOException {
        GDBSchema gdbSchema = sr.getSchemas().get(query.getTypeName());
        if (gdbSchema == null) {
            throw new IOException(query.getTypeName() + " does not exist!");
        }
        return gdbSchema.getExtent();
    }

    @Override
    protected int getCount(Query query) throws IOException {
        GDBSchema gdbSchema = sr.getSchemas().get(query.getTypeName());
        if (gdbSchema == null) {
            throw new IOException(query.getTypeName() + " does not exist!");
        }

        Statement stmt = null;
        ResultSet rs = null;
        try {
            String sql = "SELECT COUNT(*) FROM \"" + JdbcUtilities.toAccess(query.getTypeName())
                    + "\"";
            stmt = getConnection().createStatement();
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
    protected FeatureReader<SimpleFeatureType, SimpleFeature> getFeatureReader(String typeName)
            throws IOException {
        return new PGDBFeatureReader(getConnection(), getSchema(typeName));
    }

    @Override
    public SimpleFeatureSource getFeatureSource(final String typeName) {
        try {
            return new PGDBFeatureSource(this, Collections.EMPTY_SET, getSchema(typeName));
        } catch (IOException e) {
            LOGGER.log(Level.FINER, e.getMessage(), e);
        }
        return null;
    }

    @Override
    public void dispose() {
        JdbcUtilities.closeSafe(cx);
    }

    private Connection getConnection() {
        if (cx != null) {
            return cx;
        }

        try {
            String path = JdbcUtilities.toAccess(pgdbFile.getPath());
            Class.forName("sun.jdbc.odbc.JdbcOdbcDriver");

            StringBuffer sb = new StringBuffer();
            sb.append("jdbc:odbc:Driver={Microsoft Access Driver (*.mdb)}");
            sb.append(";DBQ=").append(path);
            sb.append(";DriverID=22;READONLY=true");

            java.util.Properties properties = new java.util.Properties();
            properties.put("charSet", "8859_1");

            if (user != null && !user.isEmpty()) {
                properties.put("user", JdbcUtilities.toAccess(user));
            }

            if (password != null && !password.isEmpty()) {
                properties.put("password", JdbcUtilities.toAccess(password));
            }
            cx = DriverManager.getConnection(sb.toString(), properties);
        } catch (ClassNotFoundException e) {
            LOGGER.log(Level.FINE, e.getMessage(), e);
        } catch (SQLException e) {
            LOGGER.log(Level.FINE, e.getMessage(), e);
        }
        return cx;
    }
}
