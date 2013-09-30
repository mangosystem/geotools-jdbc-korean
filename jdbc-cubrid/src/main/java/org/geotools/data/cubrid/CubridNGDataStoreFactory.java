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
package org.geotools.data.cubrid;

import java.io.IOException;
import java.util.Map;

import org.geotools.jdbc.JDBCDataStore;
import org.geotools.jdbc.JDBCDataStoreFactory;
import org.geotools.jdbc.SQLDialect;

@SuppressWarnings("rawtypes")
public class CubridNGDataStoreFactory extends JDBCDataStoreFactory {

    // 인코딩 추가?

    /** parameter for database type */
    public static final Param DBTYPE = new Param("dbtype", String.class, "Type", true, "cubrid");

    /** parameter for database instance */
    public static final Param DATABASE = new Param("database", String.class, "Database", false, "demodb");

    /** parameter for database port */
    public static final Param PORT = new Param("port", Integer.class, "Port", true, 33000);

    /** parameter for database user */
    public static final Param USER = new Param("user", String.class, "User", true, "dba");

    /** parameter for database user */
    public static final Param PASSWD = new Param("passwd", String.class, "Password", true, "");

    /** enables using && in bbox queries */
    public static final Param LOOSEBBOX = new Param("Loose bbox", Boolean.class, "Perform only primary filter on bbox", false, Boolean.TRUE);

    /** parameter that enables estimated extends instead of exact ones */
    public static final Param ESTIMATED_EXTENTS = new Param("Estimated extends", Boolean.class, "Use the spatial index information to quickly get an estimate of the data bounds", false, Boolean.FALSE);

    /** Wheter a prepared statements based dialect should be used, or not */
    public static final Param PREPARED_STATEMENTS = new Param("preparedStatements", Boolean.class, "Use prepared statements", false, Boolean.TRUE);

    @Override
    protected SQLDialect createSQLDialect(JDBCDataStore dataStore) {
        return new CubridDialect(dataStore);
    }

    @Override
    protected String getDatabaseID() {
        return (String) "cubrid";
    }

    @Override
    public String getDisplayName() {
        return "CUBRID";
    }

    public String getDescription() {
        return "CUBRID HDB 9.1.0+ Database";
    }

    @Override
    protected String getDriverClassName() {
        return "cubrid.jdbc.driver.CUBRIDDriver";
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
                Class.forName("org.geotools.data.cubrid.CubridNGJNDIDataStoreFactory");
                return true;
            } catch (ClassNotFoundException e) {
                return true;
            }
        } else {
            return checkDBType(params, "cubrid");
        }
    }

    @SuppressWarnings("unchecked")
    protected JDBCDataStore createDataStoreInternal(JDBCDataStore dataStore, Map params)
            throws IOException {
        // database schema
        String schema = (String) USER.lookUp(params);
        if (schema != null) {
            // NOTE: schema is an owner in this database
            dataStore.setDatabaseSchema(schema.toUpperCase());
        }

        // setup loose bbox
        CubridDialect dialect = (CubridDialect) dataStore.getSQLDialect();
        Boolean loose = (Boolean) LOOSEBBOX.lookUp(params);
        dialect.setLooseBBOXEnabled(loose == null || Boolean.TRUE.equals(loose));

        // check the estimated extents
        Boolean estimated = (Boolean) ESTIMATED_EXTENTS.lookUp(params);
        dialect.setEstimatedExtentsEnabled(estimated == null || Boolean.TRUE.equals(estimated));

        // setup the ps dialect if need be
        Boolean usePs = (Boolean) PREPARED_STATEMENTS.lookUp(params);
        if (usePs == null) {
            dataStore.setSQLDialect(new CubridPSDialect(dataStore, dialect));
        } else {
            if (Boolean.TRUE.equals(usePs)) {
                dataStore.setSQLDialect(new CubridPSDialect(dataStore, dialect));
            }
        }

        return dataStore;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void setupParameters(Map parameters) {
        super.setupParameters(parameters);
        
        parameters.put(DBTYPE.key, DBTYPE);
        parameters.put(HOST.key, HOST);
        parameters.put(PORT.key, PORT);
        parameters.put(DATABASE.key, DATABASE);
        parameters.put(USER.key, USER);
        parameters.put(PASSWD.key, PASSWD);
        parameters.put(NAMESPACE.key, NAMESPACE);
        parameters.put(EXPOSE_PK.key, EXPOSE_PK);
        parameters.put(PREPARED_STATEMENTS.key, PREPARED_STATEMENTS);
        parameters.put(MAXCONN.key, MAXCONN);
        parameters.put(MINCONN.key, MINCONN);
        parameters.put(FETCHSIZE.key, FETCHSIZE);
        parameters.put(MAXWAIT.key, MAXWAIT);
        if (getValidationQuery() != null)
            parameters.put(VALIDATECONN.key, VALIDATECONN);
        parameters.put(PK_METADATA_TABLE.key, PK_METADATA_TABLE);
    }

    @Override
    protected String getValidationQuery() {
        return "select SYS_DATE from db_root LIMIT 1";
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
        Integer port = (Integer) PORT.lookUp(params);
        String db = (String) DATABASE.lookUp(params);

        // jdbc:cubrid:<host>:<port>:<db-name>:[user-id]:[password]:[?<property> [& <property>]]
        return "jdbc:cubrid" + ":" + host + ":" + port + ":" + db + ":::";
    }

}
