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

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.util.logging.Logging;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * ESRI Personal Geodatabase FeatureSource
 * 
 * @author MapPlus, mapplus@gmail.com, http://onspatial.com
 * @since 2012-10-30
 * @see
 * 
 */
public class PGDBSchemaReader {
    protected static final Logger LOGGER = Logging.getLogger(PGDBSchemaReader.class);

    final Connection cx;

    final Map<Integer, String> spatialRefs = new TreeMap<Integer, String>();

    final Map<String, GDBSchema> schemas = new TreeMap<String, GDBSchema>();

    public Map<String, GDBSchema> getSchemas() {
        return Collections.unmodifiableMap(schemas);
    }

    public PGDBSchemaReader(Connection conn) {
        this.cx = conn;

        // 1. load spatial reference
        loadSpatialReference();

        // 2. load layer schema
        loadLayerSchema();
    }

    public void buildFeatureType(GDBSchema schema) {
        SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
        builder.setName(schema.getTypeName());
        builder.setCRS(schema.getCrs());

        ResultSet rs = null;
        try {
            DatabaseMetaData dbm = cx.getMetaData();
            rs = dbm.getColumns(null, null, JdbcUtilities.toAccess(schema.getTypeName()), null);
            while (rs.next()) {
                String propertyName = JdbcUtilities.fromAccess(rs.getString("COLUMN_NAME"));
                if (schema.getShapeField().equalsIgnoreCase(propertyName)) {
                    builder.add(schema.getShapeField(), schema.getGeometryBinding(),
                            schema.getCrs());
                } else {
                    String dataTypeName = rs.getString("TYPE_NAME");
                    Class<?> binding = JdbcUtilities.findBestBinding(dataTypeName);

                    int precision = rs.getInt("COLUMN_SIZE");
                    if (binding.isAssignableFrom(String.class) && precision > 0) {
                        builder.length(precision).add(propertyName, binding);
                    } else if (binding.isAssignableFrom(String.class) && precision == 0) {
                        builder.length(255).add(propertyName, binding);
                    } else {
                        builder.add(propertyName, binding);
                    }
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.FINER, e.getMessage(), e);
        } finally {
            JdbcUtilities.closeSafe(rs);
        }

        schema.setSchema(builder.buildFeatureType());
    }

    private void loadSpatialReference() {
        Statement stmt = null;
        ResultSet rs = null;
        try {
            String sql = "SELECT SRID, SRTEXT FROM GDB_SpatialRefs";
            stmt = cx.createStatement();
            rs = stmt.executeQuery(sql);
            while (rs.next()) {
                spatialRefs.put(rs.getInt(1), rs.getString(2));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.FINER, e.getMessage(), e);
        } finally {
            JdbcUtilities.closeSafe(rs, stmt);
        }
    }

    private void loadLayerSchema() {
        Statement stmt = null;
        ResultSet rs = null;
        try {
            String sql = "SELECT TableName, ShapeType, FieldName, SRID, ";
            sql += "ExtentLeft, ExtentBottom, ExtentRight, ExtentTop, IdxGridSize";
            sql += " FROM GDB_GeomColumns WHERE TableName <> 'GDB_Items'";

            stmt = cx.createStatement();
            rs = stmt.executeQuery(sql);
            while (rs.next()) {
                final String typeName = JdbcUtilities.fromAccess(rs.getString(1));
                GDBSchema gdbSchema = new GDBSchema(typeName);
                gdbSchema.setShapeType(rs.getInt(2));
                gdbSchema.setShapeField(JdbcUtilities.fromAccess(rs.getString(3)));
                gdbSchema.setSrid(rs.getInt(4));
                gdbSchema.setGridSize(rs.getDouble(9));

                // srs & extent
                CoordinateReferenceSystem crs = null;
                if (spatialRefs.get(gdbSchema.getSrid()) != null) {
                    String srText = spatialRefs.get(gdbSchema.getSrid());
                    try {
                        crs = CRS.parseWKT(srText);
                    } catch (FactoryException e) {
                        LOGGER.log(Level.FINER, e.getMessage(), e);
                    }
                }
                gdbSchema.setCrs(crs);

                double xMin = rs.getDouble(5);
                double yMin = rs.getDouble(6);
                double xMax = rs.getDouble(7);
                double yMax = rs.getDouble(8);
                gdbSchema.setExtent(new ReferencedEnvelope(xMin, xMax, yMin, yMax, crs));

                schemas.put(gdbSchema.getTypeName(), gdbSchema);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.FINER, e.getMessage(), e);
        } finally {
            JdbcUtilities.closeSafe(rs, stmt);
        }
    }

}
