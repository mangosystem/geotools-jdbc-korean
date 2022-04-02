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

import java.io.UnsupportedEncodingException;
import java.lang.Character.UnicodeBlock;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geotools.util.logging.Logging;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.MultiPoint;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;

/**
 * Jdbc Utilities for Access
 * 
 * @author MapPlus, mapplus@gmail.com, http://onspatial.com
 * @since 2012-10-30
 * @see
 * 
 */
final class JdbcUtilities {
    protected static final Logger LOGGER = Logging.getLogger(JdbcUtilities.class);

    public static void closeSafe(Object... jdbcObjects) {
        for (Object dbObject : jdbcObjects) {
            try {
                if (dbObject instanceof Connection) {
                    ((Connection) dbObject).close();
                } else if (dbObject instanceof Statement) {
                    ((Statement) dbObject).close();
                } else if (dbObject instanceof ResultSet) {
                    ((ResultSet) dbObject).close();
                }
            } catch (SQLException e) {
                LOGGER.log(Level.FINE, e.getMessage(), e);
            }
        }
    }

    public static boolean isAccessDatabase(Connection cx) {
        try {
            return cx.getMetaData().getDatabaseProductName().equalsIgnoreCase("ACCESS");
        } catch (SQLException e) {
            LOGGER.log(Level.FINER, e.getMessage(), e);
        }
        return false;
    }

    public static boolean existTable(Connection cx, String tableName) {
        ResultSet rs = null;
        try {
            DatabaseMetaData dbm = cx.getMetaData();
            rs = dbm.getTables(null, null, JdbcUtilities.toAccess(tableName), null);
            return rs.next();
        } catch (SQLException e) {
            LOGGER.log(Level.FINER, e.getMessage(), e);
        } finally {
            closeSafe(rs);
        }
        return false;
    }

    public static boolean containsHangul(String val) {
        for (int i = 0; i < val.length(); i++) {
            char ch = val.charAt(i);
            Character.UnicodeBlock unicodeBlock = Character.UnicodeBlock.of(ch);
            if (UnicodeBlock.HANGUL_SYLLABLES.equals(unicodeBlock)
                    || UnicodeBlock.HANGUL_COMPATIBILITY_JAMO.equals(unicodeBlock)
                    || UnicodeBlock.HANGUL_JAMO.equals(unicodeBlock))
                return true;
        }
        return false;
    }

    public static String fromAccess(String val) {
        if (val == null || val.isEmpty()) {
            return val;
        } else {
            try {
                return new String(val.getBytes("8859_1"), "x-windows-949");
            } catch (UnsupportedEncodingException e) {
                LOGGER.log(Level.FINER, e.getMessage(), e);
            }
        }
        return val;
    }

    public static String toAccess(String val) {
        if (val == null || val.isEmpty()) {
            return val;
        } else {
            try {
                return new String(val.getBytes("x-windows-949"), "8859_1");
            } catch (UnsupportedEncodingException e) {
                LOGGER.log(Level.FINER, e.getMessage(), e);
            }
        }
        return val;
    }

    public static Class<?> findBestBinding(String typeName) {
        Class<?> binding = null;

        if (typeName.contains("AUTOINCREMENT") || typeName.contains("SERIAL")
                || typeName.contains("COUNTER")) {
            binding = Integer.class;
            return binding;
        }

        if ("GEOMETRY".equalsIgnoreCase(typeName)) {
            binding = Geometry.class;
        } else if ("MULTIPOLYGON".equalsIgnoreCase(typeName)) {
            binding = MultiPolygon.class;
        } else if ("MULTILINESTRING".equalsIgnoreCase(typeName)) {
            binding = MultiLineString.class;
        } else if ("MULTIPOINT".equalsIgnoreCase(typeName)) {
            binding = MultiPoint.class;
        } else if ("POLYGON".equalsIgnoreCase(typeName)) {
            binding = Polygon.class;
        } else if ("LINESTRING".equalsIgnoreCase(typeName)) {
            binding = LineString.class;
        } else if ("POINT".equalsIgnoreCase(typeName)) {
            binding = Point.class;
        } else if ("GEOMETRYCOLLECTION".equalsIgnoreCase(typeName)) {
            binding = GeometryCollection.class;
        } else if ("SMALLINT".equalsIgnoreCase(typeName)) {
            binding = Short.class;
        } else if ("INTEGER".equalsIgnoreCase(typeName)) {
            binding = Integer.class;
        } else if ("INT4".equalsIgnoreCase(typeName)) {
            binding = Integer.class;
        } else if ("BIGINT".equalsIgnoreCase(typeName)) {
            binding = Long.class;
        } else if ("REAL".equalsIgnoreCase(typeName)) {
            binding = Float.class;
        } else if ("FLOAT".equalsIgnoreCase(typeName)) {
            binding = Double.class;
        } else if ("FLOAT8".equalsIgnoreCase(typeName)) {
            binding = Double.class;
        } else if ("INT8".equalsIgnoreCase(typeName)) {
            binding = Long.class;
        } else if ("DOUBLE".equalsIgnoreCase(typeName)) {
            binding = Double.class;
        } else if ("DECIMAL".equalsIgnoreCase(typeName)) {
            binding = Double.class;
        } else if ("NUMERIC".equalsIgnoreCase(typeName)) {
            binding = Double.class;
        } else if (typeName.contains("CHAR")) {
            binding = String.class;
        } else if ("CLOB".equalsIgnoreCase(typeName)) {
            binding = String.class;
        } else if ("TEXT".equalsIgnoreCase(typeName)) {
            binding = String.class;
        } else if ("DATE".equalsIgnoreCase(typeName)) {
            binding = Date.class;
        } else if ("DATETIME".equalsIgnoreCase(typeName)) {
            binding = Date.class;
        } else if ("TIMESTAMP".equalsIgnoreCase(typeName)) {
            binding = Date.class;
        } else if ("BLOB".equalsIgnoreCase(typeName)) {
            binding = byte[].class;
        } else if ("BINARY".equalsIgnoreCase(typeName)) {
            binding = byte[].class;
        } else if ("LONGBINARY".equalsIgnoreCase(typeName)) {
            binding = byte[].class;
        } else if ("LONGVARBINARY".equalsIgnoreCase(typeName)) {
            binding = byte[].class;
        } else if ("VARBINARY".equalsIgnoreCase(typeName)) {
            binding = byte[].class;
        }

        return binding;
    }
}
