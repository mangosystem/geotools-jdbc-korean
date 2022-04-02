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
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.NoSuchElementException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geotools.data.FeatureReader;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.geotools.util.Converters;
import org.geotools.util.factory.GeoTools;
import org.geotools.util.logging.Logging;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.GeometryDescriptor;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;

/**
 * ESRI Personal Geodatabase FeatureReader
 * 
 * @author MapPlus, mapplus@gmail.com, http://onspatial.com
 * @since 2012-10-30
 * @see
 * 
 */
public class PGDBFeatureReader implements FeatureReader<SimpleFeatureType, SimpleFeature> {
    protected static final Logger LOGGER = Logging.getLogger(PGDBFeatureReader.class);

    Connection cx;

    ResultSet rs = null;

    Statement stmt = null;

    PGDBDecoder geomDecoder = PGDBDecoder.newInstance();

    Class<?> geomBinding;

    SimpleFeatureType schema;

    SimpleFeatureBuilder fb;

    int featureID = 0;

    GeometryFactory gf = JTSFactoryFinder.getGeometryFactory(GeoTools.getDefaultHints());

    public PGDBFeatureReader(Connection cx, SimpleFeatureType schema) {
        this.cx = cx;
        this.schema = schema;
        this.geomBinding = schema.getGeometryDescriptor().getType().getBinding();
        this.fb = new SimpleFeatureBuilder(schema);
        this.featureID = 0;

        this.queryLayer();
    }

    private void queryLayer() {
        try {
            String sql = "SELECT * FROM ";
            sql += JdbcUtilities.toAccess(schema.getTypeName());
            stmt = cx.createStatement();
            rs = stmt.executeQuery(sql);
        } catch (SQLException e) {
            LOGGER.log(Level.FINER, e.getMessage(), e);
        }
    }

    public SimpleFeatureType getFeatureType() {
        return schema;
    }

    public SimpleFeature next() throws IOException, IllegalArgumentException,
            NoSuchElementException {
        SimpleFeature feature = fb.buildFeature(schema.getTypeName() + "." + ++featureID);

        try {
            for (int index = 0; index < schema.getAttributeCount(); index++) {
                AttributeDescriptor desc = schema.getDescriptor(index);
                String accessFieldName = JdbcUtilities.toAccess(desc.getLocalName());

                if (desc instanceof GeometryDescriptor) {
                    byte[] bytes = rs.getBytes(accessFieldName);
                    Geometry geometry = geomDecoder.decodeGeometry(bytes);
                    feature.setDefaultGeometry(geometry);
                } else if (desc.getType().getBinding().isAssignableFrom(String.class)) {
                    String strVal = JdbcUtilities.fromAccess(rs.getString(accessFieldName));
                    feature.setAttribute(desc.getLocalName(), strVal);
                } else {
                    Object objValue = rs.getObject(accessFieldName);
                    Object value = Converters.convert(objValue, desc.getType().getBinding());
                    feature.setAttribute(desc.getLocalName(), value);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, e.getMessage(), e);
        }

        return feature;
    }

    public boolean hasNext() throws IOException {
        try {
            return rs.next();
        } catch (SQLException e) {
            LOGGER.log(Level.FINER, e.getMessage(), e);
        }
        return false;
    }

    public void close() throws IOException {
        JdbcUtilities.closeSafe(rs, stmt);
    }
}
