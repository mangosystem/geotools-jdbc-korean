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
package org.geotools.data.altibase;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import org.geotools.data.jdbc.FilterToSQL;
import org.geotools.factory.Hints;
import org.geotools.filter.IsLessThenOrEqualToImpl;
import org.geotools.filter.LiteralExpressionImpl;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.jdbc.BasicSQLDialect;
import org.geotools.jdbc.ColumnMetadata;
import org.geotools.jdbc.JDBCDataStore;
import org.geotools.referencing.CRS;
import org.geotools.util.Version;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.feature.type.GeometryType;
import org.opengis.filter.Filter;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.MultiPoint;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;

public class AltibaseDialect extends BasicSQLDialect {

    boolean looseBBOXEnabled = false;

    boolean estimatedExtentsEnabled = false;

    Version version;

    @SuppressWarnings({ "rawtypes", "serial" })
    final static Map<String, Class> TYPE_TO_CLASS_MAP = new HashMap<String, Class>() {
        {
            put("GEOMETRY", Geometry.class);
            put("POINT", Point.class);
            put("POINTM", Point.class);
            put("LINESTRING", LineString.class);
            put("LINESTRINGM", LineString.class);
            put("POLYGON", Polygon.class);
            put("POLYGONM", Polygon.class);
            put("MULTIPOINT", MultiPoint.class);
            put("MULTIPOINTM", MultiPoint.class);
            put("MULTILINESTRING", MultiLineString.class);
            put("MULTILINESTRINGM", MultiLineString.class);
            put("MULTIPOLYGON", MultiPolygon.class);
            put("MULTIPOLYGONM", MultiPolygon.class);
            put("GEOMETRYCOLLECTION", GeometryCollection.class);
            put("GEOMETRYCOLLECTIONM", GeometryCollection.class);
            put("BYTEA", byte[].class);
        }
    };

    @SuppressWarnings({ "rawtypes", "serial" })
    final static Map<Class, String> CLASS_TO_TYPE_MAP = new HashMap<Class, String>() {
        {
            put(Geometry.class, "GEOMETRY");
            put(Point.class, "GEOMETRY");
            put(LineString.class, "GEOMETRY");
            put(Polygon.class, "GEOMETRY");
            put(MultiPoint.class, "GEOMETRY");
            put(MultiLineString.class, "GEOMETRY");
            put(MultiPolygon.class, "GEOMETRY");
            put(GeometryCollection.class, "GEOMETRY");
            put(byte[].class, "BYTEA");
        }
    };

    public AltibaseDialect(JDBCDataStore dataStore) {
        super(dataStore);
    }

    public boolean isLooseBBOXEnabled() {
        return looseBBOXEnabled;
    }

    public void setLooseBBOXEnabled(boolean looseBBOXEnabled) {
        this.looseBBOXEnabled = looseBBOXEnabled;
    }

    public boolean isEstimatedExtentsEnabled() {
        return estimatedExtentsEnabled;
    }

    public void setEstimatedExtentsEnabled(boolean estimatedExtentsEnabled) {
        this.estimatedExtentsEnabled = estimatedExtentsEnabled;
    }

    @Override
    public boolean isAggregatedSortSupported(String function) {
        return "distinct".equalsIgnoreCase(function);
    }

    @Override
    public void initializeConnection(Connection cx) throws SQLException {
        super.initializeConnection(cx);
    }

    @Override
    public boolean includeTable(String schemaName, String tableName, Connection cx)
            throws SQLException {
        if (tableName.equalsIgnoreCase("STO_GEOMETRY_COLUMNS")) {
            return false;
        } else if (tableName.equalsIgnoreCase("STO_SPATIAL_REF_SYS")) {
            return false;
        } else if (tableName.equalsIgnoreCase("GEOMETRY_COLUMNS")) {
            return false;
        } else if (tableName.equalsIgnoreCase("SPATIAL_REF_SYS")) {
            return false;
        }

        // others?
        return true;
    }

    ThreadLocal<WKBAttributeIO> wkbReader = new ThreadLocal<WKBAttributeIO>();

    @Override
    public Geometry decodeGeometryValue(GeometryDescriptor descriptor, ResultSet rs, String column,
            GeometryFactory factory, Connection cx) throws IOException, SQLException {
        WKBAttributeIO reader = getWKBReader(factory);
        return (Geometry) reader.read(rs, column);
    }

    public Geometry decodeGeometryValue(GeometryDescriptor descriptor, ResultSet rs, int column,
            GeometryFactory factory, Connection cx) throws IOException, SQLException {
        WKBAttributeIO reader = getWKBReader(factory);
        return (Geometry) reader.read(rs, column);
    }

    WKBAttributeIO getWKBReader(GeometryFactory factory) {
        WKBAttributeIO reader = wkbReader.get();
        if (reader == null) {
            reader = new WKBAttributeIO(factory);
            wkbReader.set(reader);
        } else {
            reader.setGeometryFactory(factory);
        }
        return reader;
    }

    @Override
    public void encodeGeometryColumn(GeometryDescriptor gatt, String prefix, int srid, Hints hints,
            StringBuffer sql) {
        sql.append(" ASBINARY(");
        encodeColumnName(prefix, gatt.getLocalName(), sql);
        sql.append(")");
    }

    @Override
    public void encodeGeometryEnvelope(String tableName, String geometryColumn, StringBuffer sql) {
        sql.append(" ASTEXT(ENVELOPE(");
        encodeColumnName(null, geometryColumn, sql);
        sql.append("))");
    }

    @Override
    public void encodeColumnName(String prefix, String raw, StringBuffer sql) {
        if (prefix != null) {
            sql.append(ne()).append(prefix).append(ne()).append(".");
        }
        sql.append(ne()).append(raw).append(ne());
    }

    @Override
    public List<ReferencedEnvelope> getOptimizedBounds(String schema,
            SimpleFeatureType featureType, Connection cx) throws SQLException, IOException {
        if (!estimatedExtentsEnabled) {
            return null;
        }

        String tableName = featureType.getTypeName();

        Statement st = null;
        ResultSet rs = null;

        List<ReferencedEnvelope> result = new ArrayList<ReferencedEnvelope>();
        Savepoint savePoint = null;
        try {
            st = cx.createStatement();
            if (!cx.getAutoCommit()) {
                savePoint = cx.setSavepoint();
            }

            GeometryDescriptor att = featureType.getGeometryDescriptor();
            String geometryField = att.getLocalName();

            // ==================Altibase======================
            // SELECT MIN(MINX(OBJ)), MIN(MINY(OBJ)), MAX(MAXX(OBJ)), MAX(MAXY(OBJ)) FROM ROAD;
            // ================================================

            StringBuffer sql = new StringBuffer();
            sql.append("SELECT ");
            sql.append(" MIN(MINX(\"").append(geometryField).append("\"))");
            sql.append(", MIN(MINY(\"").append(geometryField).append("\"))");
            sql.append(", MAX(MAXX(\"").append(geometryField).append("\"))");
            sql.append(", MAX(MAXY(\"").append(geometryField).append("\"))");
            sql.append(" FROM \"");
            sql.append(tableName);
            sql.append("\"");

            rs = st.executeQuery(sql.toString());

            if (rs.next()) {
                CoordinateReferenceSystem crs = att.getCoordinateReferenceSystem();
                final double x1 = rs.getDouble(1);
                final double y1 = rs.getDouble(2);
                final double x2 = rs.getDouble(3);
                final double y2 = rs.getDouble(4);

                // reproject and merge
                result.add(new ReferencedEnvelope(x1, x2, y1, y2, crs));
            }
        } catch (SQLException e) {
            if (savePoint != null) {
                cx.rollback(savePoint);
            }
            LOGGER.log(Level.WARNING, e.getMessage(), e);
            return null;
        } finally {
            if (savePoint != null) {
                cx.releaseSavepoint(savePoint);
            }
            dataStore.closeSafe(rs);
            dataStore.closeSafe(st);
        }
        return result;
    }

    @Override
    public Envelope decodeGeometryEnvelope(ResultSet rs, int column, Connection cx)
            throws SQLException, IOException {
        try {
            String envelope = rs.getString(column);
            if (envelope != null) {
                return new WKTReader().read(envelope).getEnvelopeInternal();
            } else {
                return new Envelope();
            }
        } catch (ParseException e) {
            throw (IOException) new IOException("Error occurred parsing the bounds WKT")
                    .initCause(e);
        }
    }

    @SuppressWarnings("rawtypes")
    @Override
    public Class<?> getMapping(ResultSet columnMetaData, Connection cx) throws SQLException {
        String typeName = columnMetaData.getString("TYPE_NAME");
        String gType = null;
        if ("geometry".equalsIgnoreCase(typeName)) {
            gType = lookupGeometryType(columnMetaData, cx, "geometry_columns", "f_geometry_column");
        } else {
            return null;
        }

        // decode the type into
        if (gType == null) {
            // it's either a generic geography or geometry not registered in the medatata tables
            return Geometry.class;
        } else {
            Class geometryClass = (Class) TYPE_TO_CLASS_MAP.get(gType.toUpperCase());
            if (geometryClass == null) {
                geometryClass = Geometry.class;
            }

            return geometryClass;
        }
    }

    String lookupGeometryType(ResultSet columnMetaData, Connection cx, String gTableName,
            String gColumnName) throws SQLException {
        // grab the information we need to proceed
        String tableName = columnMetaData.getString("TABLE_NAME");
        String columnName = columnMetaData.getString("COLUMN_NAME");
        String schemaName = columnMetaData.getString("TABLE_SCHEM");

        // first attempt, try with the geometry metadata
        Statement st = null;
        ResultSet rs = null;

        try {
            if (schemaName == null || schemaName.isEmpty()) {
                schemaName = "SYS";
            }
            
            StringBuffer sb = new StringBuffer();
            sb.append("SELECT GEOMETRYTYPE(");
            sb.append("\"").append(columnName).append("\")");
            sb.append(" FROM ");
            sb.append("\"").append(schemaName).append("\"");
            sb.append(".");
            sb.append("\"").append(tableName).append("\"");
            sb.append(" LIMIT 1");

            LOGGER.log(Level.FINE, "Geometry type check; {0} ", sb.toString());
            st = cx.createStatement();
            rs = st.executeQuery(sb.toString());
            if (rs.next()) {
                return rs.getString(1);
            }
        } finally {
            dataStore.closeSafe(rs);
            dataStore.closeSafe(st);
        }

        return "GEOMETRY";
    }

    @Override
    public void handleUserDefinedType(ResultSet columnMetaData, ColumnMetadata metadata,
            Connection cx) throws SQLException {
        String tableName = columnMetaData.getString("TABLE_NAME");
        String columnName = columnMetaData.getString("COLUMN_NAME");
        String schemaName = columnMetaData.getString("TABLE_SCHEM");
        
        if (schemaName == null || schemaName.isEmpty()) {
            schemaName = "SYS";
        }

        String sql = "SELECT udt_name FROM information_schema.columns " + " WHERE table_schema = '"
                + schemaName + "' " + " AND table_name = '" + tableName + "' "
                + " AND column_name = '" + columnName + "' ";

        LOGGER.fine(sql);

        Statement st = cx.createStatement();
        try {
            ResultSet rs = st.executeQuery(sql);
            try {
                if (rs.next()) {
                    metadata.setTypeName(rs.getString(1));
                }
            } finally {
                dataStore.closeSafe(rs);
            }
        } finally {
            dataStore.closeSafe(st);
        }
    }

    @Override
    public Integer getGeometrySRID(String schemaName, String tableName, String columnName,
            Connection cx) throws SQLException {
        // first attempt, try with the geometry metadata
        Statement statement = null;
        ResultSet result = null;
        Integer srid = null;
        try {
            // try geometry_columns
            try {
                if (schemaName == null || schemaName.isEmpty()) {
                    schemaName = "SYS";
                }

                String sridSQL = "SELECT SRID FROM GEOMETRY_COLUMNS WHERE " //
                        + "F_TABLE_SCHEMA = '" + schemaName + "' " //
                        + "AND F_TABLE_NAME = '" + tableName + "' " //
                        + "AND F_GEOMETRY_COLUMN = '" + columnName + "'";

                String sqlStatement = "SELECT AUTH_SRID FROM SPATIAL_REF_SYS WHERE SRID = ("
                        + sridSQL + ")";

                LOGGER.log(Level.FINE, "Geometry srid check; {0} ", sqlStatement);
                statement = cx.createStatement();
                result = statement.executeQuery(sqlStatement);

                if (result.next()) {
                    srid = result.getInt(1);
                }
            } catch (SQLException e) {
                LOGGER.log(Level.WARNING, "Failed to retrieve information about " + schemaName
                        + "." + tableName + "." + columnName
                        + " from the geometry_columns table, checking the first geometry instead",
                        e);
            } finally {
                dataStore.closeSafe(result);
            }

        } finally {
            dataStore.closeSafe(result);
            dataStore.closeSafe(statement);
        }

        return srid;
    }

    @Override
    public int getGeometryDimension(String schemaName, String tableName, String columnName,
            Connection cx) throws SQLException {
        // first attempt, try with the geometry metadata
        Statement statement = null;
        ResultSet result = null;
        int dimension = 2; // default
        try {
            // try geometry_columns
            try {
                if (schemaName == null || schemaName.isEmpty()) {
                    schemaName = "SYS";
                }

                String sqlStatement = "SELECT COORD_DIMENSION FROM GEOMETRY_COLUMNS WHERE " //
                        + "F_TABLE_SCHEMA = '" + schemaName + "' " //
                        + "AND F_TABLE_NAME = '" + tableName + "' " //
                        + "AND F_GEOMETRY_COLUMN = '" + columnName + "'";

                LOGGER.log(Level.FINE, "Geometry dimension check; {0} ", sqlStatement);
                statement = cx.createStatement();
                result = statement.executeQuery(sqlStatement);

                if (result.next()) {
                    dimension = result.getInt(1);
                }
            } catch (SQLException e) {
                LOGGER.log(Level.WARNING, "Failed to retrieve information about " + schemaName
                        + "." + tableName + "." + columnName
                        + " from the geometry_columns table, checking the first geometry instead",
                        e);
            } finally {
                dataStore.closeSafe(result);
            }

        } finally {
            dataStore.closeSafe(result);
            dataStore.closeSafe(statement);
        }

        return dimension;
    }

    @Override
    public String getSequenceForColumn(String schemaName, String tableName, String columnName,
            Connection cx) throws SQLException {
        return "seq_" + tableName + "_" + columnName;
    }

    @Override
    public Object getNextSequenceValue(String schemaName, String sequenceName, Connection cx)
            throws SQLException {
        Statement st = cx.createStatement();
        try {
            // SELECT seq_building_fid.NEXTVAL FROM DUAL;
            String sql = "SELECT \"" + sequenceName + "\".NEXTVAL FROM DUAL";

            dataStore.getLogger().fine(sql);
            ResultSet rs = st.executeQuery(sql);
            try {
                if (rs.next()) {
                    return rs.getLong(1);
                } else {
                    LOGGER.log(Level.WARNING, "Failed to retrieve sequence from " + sequenceName);
                }
            } finally {
                dataStore.closeSafe(rs);
            }
        } finally {
            dataStore.closeSafe(st);
        }

        return 0;
    }

    @Override
    public boolean lookupGeneratedValuesPostInsert() {
        return true;
    }

    @Override
    public Object getLastAutoGeneratedValue(String schemaName, String tableName, String columnName,
            Connection cx) throws SQLException {
        Statement st = cx.createStatement();
        try {
            String sql = "SELECT lastval()";
            dataStore.getLogger().fine(sql);

            ResultSet rs = st.executeQuery(sql);
            try {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            } finally {
                dataStore.closeSafe(rs);
            }
        } finally {
            dataStore.closeSafe(st);
        }

        return null;
    }

    @Override
    public void registerClassToSqlMappings(Map<Class<?>, Integer> mappings) {
        super.registerClassToSqlMappings(mappings);

        // jdbc metadata for geom columns reports DATA_TYPE=1111=Types.OTHER
        mappings.put(Geometry.class, Types.OTHER);
    }

    @Override
    public void registerSqlTypeNameToClassMappings(Map<String, Class<?>> mappings) {
        super.registerSqlTypeNameToClassMappings(mappings);
        mappings.put("GEOMETRY", Geometry.class);
        mappings.put("geometry", Geometry.class);
        mappings.put("text", String.class);
    }

    @Override
    public void registerSqlTypeToSqlTypeNameOverrides(Map<Integer, String> overrides) {
        overrides.put(new Integer(Types.VARCHAR), "VARCHAR");
        overrides.put(new Integer(Types.BOOLEAN), "BOOL");
        overrides.put(new Integer(Types.SMALLINT), "INTEGER");
        overrides.put(new Integer(Types.INTEGER), "INTEGER");
        overrides.put(new Integer(Types.REAL), "FLOAT");
        overrides.put(new Integer(Types.FLOAT), "FLOAT");
        overrides.put(new Integer(Types.DOUBLE), "DOUBLE");
        overrides.put(new Integer(Types.DECIMAL), "NUMBER");
        overrides.put(new Integer(Types.NUMERIC), "NUMBER");
    }

    @Override
    public String getGeometryTypeName(Integer type) {
        return "GEOMETRY";
    }

    @Override
    public void encodePrimaryKey(String column, StringBuffer sql) {
        encodeColumnName(null, column, sql);
        sql.append(" INTEGER PRIMARY KEY");
    }

    @Override
    public void postCreateTable(String schemaName, SimpleFeatureType featureType, Connection cx)
            throws SQLException {
        String tableName = featureType.getName().getLocalPart();

        Statement st = null;
        try {
            st = cx.createStatement();
            if (schemaName == null || schemaName.isEmpty()) {
                schemaName = "SYS";
            }

            // register all geometry columns in the database
            for (AttributeDescriptor att : featureType.getAttributeDescriptors()) {
                if (att instanceof GeometryDescriptor) {
                    GeometryDescriptor gd = (GeometryDescriptor) att;

                    // lookup or reverse engineer the srid
                    int srid = 2097;

                    if (gd.getUserData().get(JDBCDataStore.JDBC_NATIVE_SRID) != null) {
                        srid = (Integer) gd.getUserData().get(JDBCDataStore.JDBC_NATIVE_SRID);
                    } else if (gd.getCoordinateReferenceSystem() != null) {
                        try {
                            Integer result = CRS.lookupEpsgCode(gd.getCoordinateReferenceSystem(),
                                    true);
                            if (result != null) {
                                srid = result;
                            }
                        } catch (Exception e) {
                            LOGGER.log(Level.FINE, "Error looking up the "
                                    + "epsg code for metadata " + "insertion, assuming " + srid, e);
                        }
                    }

                    // assume 2 dimensions, but ease future customisation
                    int dimensions = 2;
                    if (gd.getUserData().get(Hints.COORDINATE_DIMENSION) != null) {
                        dimensions = (Integer) gd.getUserData().get(Hints.COORDINATE_DIMENSION);
                    }

                    // register the geometry type, first remove and eventual
                    // leftover, then write out the real one
                    String sql = "DELETE FROM GEOMETRY_COLUMNS" + " WHERE F_TABLE_SCHEMA = '"
                            + schemaName + "'" //
                            + " AND F_TABLE_NAME = '" + tableName + "'" //
                            + " AND F_GEOMETRY_COLUMN = '" + gd.getLocalName() + "'";

                    LOGGER.fine(sql);
                    st.execute(sql);

                    sql = "INSERT INTO GEOMETRY_COLUMNS VALUES (" + "'" + schemaName + "'," //
                            + "'" + tableName + "'," //
                            + "'" + gd.getLocalName() + "', " //
                            + dimensions + "," //
                            + srid + ")";
                    LOGGER.fine(sql);
                    st.execute(sql);

                    // add the spatial index
                    // Altibase: CREATE INDEX index_name ON table_name ( column_name ) [INDEXTYPE IS
                    // RTREE] ;
                    sql = "CREATE INDEX \"spatial_" + tableName //
                            + "_" + gd.getLocalName() + "\""//
                            + " ON " //
                            + "\"" + schemaName + "\"" //
                            + "." //
                            + "\"" + tableName + "\"" //
                            + " (" //
                            + "\"" + gd.getLocalName() + "\"" //
                            + ") INDEXTYPE IS RTREE";
                    LOGGER.fine(sql);
                    st.execute(sql);

                    // create sequence
                    String sequenceName = getSequenceForColumn(schemaName, tableName, "fid", cx);
                    sql = "DROP SEQUENCE \"" + sequenceName + "\"";
                    try {
                        st.execute(sql);
                    } catch (Exception e) {
                        LOGGER.fine(e.getMessage());
                    }

                    // CREATE SEQUENCE seq_building_fid START WITH 1 INCREMENT BY 1 MINVALUE 1
                    // NOMAXVALUE
                    sql = "CREATE SEQUENCE \"" + sequenceName
                            + "\" START WITH 1 INCREMENT BY 1 MINVALUE 1 NOMAXVALUE";
                    LOGGER.fine(sql);
                    st.execute(sql);
                }
            }
            cx.commit();
        } finally {
            dataStore.closeSafe(st);
        }
    }

    @Override
    public void postDropTable(String schemaName, SimpleFeatureType featureType, Connection cx)
            throws SQLException {
        Statement st = cx.createStatement();
        try {
            if (schemaName == null || schemaName.isEmpty()) {
                schemaName = "SYS";
            }

            String tableName = featureType.getTypeName();
            // remove all the geometry_column entries
            String sql = "DELETE FROM GEOMETRY_COLUMNS" + " WHERE F_TABLE_SCHEMA = '" + schemaName
                    + "'" + " AND F_TABLE_NAME = '" + tableName + "'";
            LOGGER.fine(sql);
            st.execute(sql);
        } finally {
            dataStore.closeSafe(st);
        }
    }

    @Override
    public void encodeGeometryValue(Geometry value, int dimension, int srid, StringBuffer sql)
            throws IOException {
        if (value == null) {
            sql.append("NULL");
        } else {
            if (value instanceof LinearRing) {
                // WKT does not support linear rings
                value = value.getFactory().createLineString(
                        ((LinearRing) value).getCoordinateSequence());
            }

            // Altibase WKT string is too long. Max length is 32KB
            sql.append(" GEOMFROMTEXT('" + value.toText() + "')");
        }
    }

    @Override
    public FilterToSQL createFilterToSQL() {
        AltibaseFilterToSQL sql = new AltibaseFilterToSQL(this);
        sql.setLooseBBOXEnabled(looseBBOXEnabled);
        return sql;
    }

    @Override
    public boolean isLimitOffsetSupported() {
        return true;
    }

    @Override
    public void applyLimitOffset(StringBuffer sql, int limit, int offset) {
        if (limit >= 0 && limit < Integer.MAX_VALUE) {
            sql.append(" LIMIT " + limit);
            if (offset > 0) {
                sql.append(" OFFSET " + offset);
            }
        } else if (offset > 0) {
            sql.append(" OFFSET " + offset);
        }
    }

    @SuppressWarnings("rawtypes")
    @Override
    public void encodeValue(Object value, Class type, StringBuffer sql) {
        if (byte[].class.equals(type)) {
            // escape the into bytea representation
            StringBuffer sb = new StringBuffer();
            byte[] input = (byte[]) value;
            for (int i = 0; i < input.length; i++) {
                byte b = input[i];
                if (b == 0) {
                    sb.append("\\\\000");
                } else if (b == 39) {
                    sb.append("\\'");
                } else if (b == 92) {
                    sb.append("\\\\134'");
                } else if (b < 31 || b >= 127) {
                    sb.append("\\\\");
                    String octal = Integer.toOctalString(b);
                    if (octal.length() == 1) {
                        sb.append("00");
                    } else if (octal.length() == 2) {
                        sb.append("0");
                    }
                    sb.append(octal);
                } else {
                    sb.append((char) b);
                }
            }
            super.encodeValue(sb.toString(), String.class, sql);
        } else {
            super.encodeValue(value, type, sql);
        }
    }

    @Override
    public int getDefaultVarcharSize() {
        return 255;
    }

    @Override
    public String[] getDesiredTablesType() {
        return new String[] { "TABLE", "VIEW", "MATERIALIZED VIEW" };
    }

    @Override
    public void encodePostColumnCreateTable(AttributeDescriptor att, StringBuffer sql) {
        if (att.getType() instanceof GeometryType) {
            Class<?> binding = att.getType().getBinding();
            int size = getAttributeLength(att);
            if (size == 0 || size < 25) {
                // #=================================================================
                // Insert Error: Invalid length of the data type : WKB Geometry Size
                // #=================================================================
                // # altibase.properties: ST Object Buffer Size Properties
                // #=================================================================
                // ST_OBJECT_BUFFER_SIZE = 32000 # default : 32000 ==> 1,523
                // - min : 32000
                // - max : 104857600 ==> 4,993,219

                size = 32000;
                if (binding.isAssignableFrom(MultiPolygon.class)) {
                    size = 32000 * 10;
                } else if (binding.isAssignableFrom(Polygon.class)) {
                    size = 32000 * 10;
                } else if (binding.isAssignableFrom(MultiLineString.class)) {
                    size = 32000 * 2;
                } else if (binding.isAssignableFrom(LineString.class)) {
                    size = 32000;
                } else if (binding.isAssignableFrom(MultiPoint.class)) {
                    size = 25 * 10;
                } else if (binding.isAssignableFrom(Point.class)) {
                    size = 25;
                }
            }
            sql.append("(").append(size).append(")");
        }
    }

    int getAttributeLength(AttributeDescriptor descriptor) {
        int length = 0;
        List<Filter> filterList = descriptor.getType().getRestrictions();
        for (Filter filter : filterList) {
            if (filter instanceof IsLessThenOrEqualToImpl) {
                IsLessThenOrEqualToImpl ite = (IsLessThenOrEqualToImpl) filter;
                LiteralExpressionImpl exp = (LiteralExpressionImpl) ite.getExpression2();
                try {
                    length = Integer.valueOf(exp.getValue().toString());
                    break;
                } catch (NumberFormatException e) {
                    LOGGER.log(Level.WARNING, e.getMessage(), e);
                }
            }
        }
        return length;
    }

    Map<String, Integer> getColumnMeta(DatabaseMetaData metadata, String schemaName,
            String tableName) throws SQLException {
        Map<String, Integer> map = new HashMap<String, Integer>();
        ResultSet rs = null;

        try {
            rs = metadata.getColumns(null, schemaName, tableName, "%");
            while (rs.next()) {
                String columnName = rs.getString("COLUMN_NAME");
                Integer columnSize = rs.getInt("COLUMN_SIZE");

                map.put(columnName, columnSize);
            }
        } finally {
            dataStore.closeSafe(rs);
        }

        return Collections.unmodifiableMap(map);
    }

    @Override
    public void postCreateAttribute(AttributeDescriptor att, String tableName, String schemaName,
            Connection cx) throws SQLException {
        // postCreateAttribute
    }

    @Override
    public void postCreateFeatureType(SimpleFeatureType featureType, DatabaseMetaData metadata,
            String schemaName, Connection cx) throws SQLException {
        // postCreateFeatureType
    }

    /**
     * Returns the Altibase version
     * 
     * @return
     */
    public Version getVersion(Connection conn) throws SQLException {
        if (version == null) {
            version = new Version("V_5_5_1"); // Minimum Version

            Statement st = null;
            ResultSet rs = null;
            try {
                st = conn.createStatement();
                rs = st.executeQuery("SELECT PRODUCT_VERSION FROM v$version");
                if (rs.next()) {
                    version = new Version(rs.getString(1));
                }
            } finally {
                dataStore.closeSafe(rs);
                dataStore.closeSafe(st);
            }
        }

        return version;
    }

    /**
     * Returns true if the Kairos version is >= x.x
     */
    boolean supportsGeography(Connection cx) throws SQLException {
        return false; // getVersion(cx).compareTo(V_5_5_1) >= 0;
    }

}
