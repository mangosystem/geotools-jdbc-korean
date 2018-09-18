/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2002-2008, Open Source Geospatial Foundation (OSGeo)
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
package org.geotools.data.kairos;

import java.io.IOException;
import java.nio.charset.Charset;
import java.sql.SQLException;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geotools.data.Transaction;
import org.geotools.jdbc.JDBCDataStore;
import org.geotools.jdbc.JDBCDataStoreFactory;
import org.geotools.jdbc.SQLDialect;
import org.geotools.util.Version;
import org.geotools.util.logging.Logging;

@SuppressWarnings("rawtypes")
public class KairosNGDataStoreFactory extends JDBCDataStoreFactory {
    protected static final Logger LOGGER = Logging.getLogger(KairosNGDataStoreFactory.class);

    static final Version V_6 = new Version("6.0");

    /** parameter for database type */
    public static final Param DBTYPE = new Param("dbtype", String.class, "Type", true, "kairos");

    /** parameter for database instance */
    public static final Param DATABASE = new Param("database", String.class, "Database", true,
            "test");

    /** parameter for database encoding */
    public static final Param ENCODING = new Param("encoding", String.class, "Encoding", false,
            Charset.defaultCharset().name());

    /** parameter for database schema */
    public static final Param SCHEMA = new Param("schema", String.class, "Schema", false, "root");

    /** parameter for database port */
    public static final Param PORT = new Param("port", Integer.class, "Port", true, 5000);

    /** parameter for database user */
    public static final Param USER = new Param("user", String.class, "User", true, "root");

    /** enables using && in bbox queries */
    public static final Param LOOSEBBOX = new Param("Loose bbox", Boolean.class,
            "Perform only primary filter on bbox", false, Boolean.TRUE);

    /** parameter that enables estimated extends instead of exact ones */
    public static final Param ESTIMATED_EXTENTS = new Param("Estimated extends", Boolean.class,
            "Use the spatial index information to quickly get an estimate of the data bounds",
            false, Boolean.FALSE);

    /** Whether a prepared statements based dialect should be used, or not */
    public static final Param PREPARED_STATEMENTS = new Param("preparedStatements", Boolean.class,
            "Use prepared statements", false, Boolean.TRUE);

    @Override
    protected SQLDialect createSQLDialect(JDBCDataStore dataStore) {
        return new KairosDialect(dataStore);
    }

    @Override
    protected String getDatabaseID() {
        return (String) DBTYPE.sample;
    }

    @Override
    public String getDisplayName() {
        return "Kairos";
    }

    public String getDescription() {
        return "REALTIMETECH(tm) Kairos Spatial 5.0+ Databae";
    }

    @Override
    protected String getDriverClassName() {
        return "kr.co.realtimetech.kairos.jdbc.kairosDriver";
    }

    @Override
    public boolean canProcess(Map params) {
        if (!super.canProcess(params)) {
            return false; // was not in agreement with getParametersInfo
        }

        return checkDBType(params);
    }

    @Override
    protected boolean checkDBType(Map params) {
        if (super.checkDBType(params)) {
            try {
                Class.forName("org.geotools.data.kairos.KairosNGDataStoreFactory");
                return true;
            } catch (ClassNotFoundException e) {
                return true;
            }
        } else {
            return checkDBType(params, "kairos");
        }
    }

    @SuppressWarnings("unchecked")
    protected JDBCDataStore createDataStoreInternal(JDBCDataStore dataStore, Map params)
            throws IOException {
        // database schema
        String schema = (String) SCHEMA.lookUp(params);
        if (schema != null) {
            // NOTE: schema is an owner in this database
            dataStore.setDatabaseSchema(schema);
        } else {
            dataStore.setDatabaseSchema((String) USER.lookUp(params));
        }

        // setup loose bbox
        KairosDialect dialect = (KairosDialect) dataStore.getSQLDialect();
        Boolean loose = (Boolean) LOOSEBBOX.lookUp(params);
        dialect.setLooseBBOXEnabled(loose == null || Boolean.TRUE.equals(loose));

        // check the estimated extents
        Boolean estimated = (Boolean) ESTIMATED_EXTENTS.lookUp(params);
        dialect.setEstimatedExtentsEnabled(estimated == null || Boolean.TRUE.equals(estimated));

        // setup the ps dialect if need be
        Boolean usePs = (Boolean) PREPARED_STATEMENTS.lookUp(params);
        if (usePs != null && Boolean.TRUE.equals(usePs)) {
            dataStore.setSQLDialect(new KairosPSDialect(dataStore, dialect));
        }

        // primary key finder
        try {
            Version version = dialect.getVersion(dataStore.getConnection(Transaction.AUTO_COMMIT));
            if (version.compareTo(V_6) >= 0) {
                dataStore.setPrimaryKeyFinder(new KairosPrimaryKeyFinder());
            }
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Failed to find version", e);
        }

        return dataStore;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void setupParameters(Map parameters) {
        // NOTE: when adding parameters here remember to add them to KairosNGJNDIDataStoreFactory
        super.setupParameters(parameters);

        parameters.put(DBTYPE.key, DBTYPE);
        parameters.put(HOST.key, HOST);
        parameters.put(PORT.key, PORT);
        parameters.put(DATABASE.key, DATABASE);
        parameters.put(ENCODING.key, ENCODING);
        parameters.put(SCHEMA.key, SCHEMA);
        parameters.put(USER.key, USER);
        parameters.put(PASSWD.key, PASSWD);
        parameters.put(NAMESPACE.key, NAMESPACE);
        parameters.put(EXPOSE_PK.key, EXPOSE_PK);
        parameters.put(PREPARED_STATEMENTS.key, PREPARED_STATEMENTS);
        parameters.put(MAXCONN.key, MAXCONN);
        parameters.put(MINCONN.key, MINCONN);
        parameters.put(FETCHSIZE.key, FETCHSIZE);
        parameters.put(MAXWAIT.key, MAXWAIT);
        if (getValidationQuery() != null) {
            parameters.put(VALIDATECONN.key, VALIDATECONN);
        }
        parameters.put(PK_METADATA_TABLE.key, PK_METADATA_TABLE);
    }

    @Override
    protected String getValidationQuery() {
        return "SELECT SYSDATE FROM DUAL";
    }

    @Override
    public boolean isAvailable() {
        try {
            Class.forName(getDriverClassName());
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    protected String getJDBCUrl(Map params) throws IOException {
        String host = (String) HOST.lookUp(params);
        String db = (String) DATABASE.lookUp(params);
        int port = (Integer) PORT.lookUp(params);
        String encoding = (String) ENCODING.lookUp(params);

        if (encoding == null || encoding.isEmpty()) {
            return "jdbc:kairos" + "://" + host + ":" + port + "/" + db;
        } else {
            // jdbc:kairos://localhost/dbname;CHARSET=SJIS
            return "jdbc:kairos" + "://" + host + ":" + port + "/" + db + ";CHARSET=" + encoding;
        }
    }

}
