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

import org.geotools.geometry.jts.ReferencedEnvelope;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.MultiPoint;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Point;

/**
 * GDB Schema for ESRI Personal Geodatabase
 * 
 * @author MapPlus, mapplus@gmail.com, http://onspatial.com
 * @since 2012-10-30
 * @see
 * 
 */
public class GDBSchema {
    private String typeName;

    private int shapeType = 1;

    private Class<?> geometryBinding = Geometry.class;

    private String shapeField;

    private int srid = -1;

    private CoordinateReferenceSystem crs;

    private double gridSize = -1.0;

    private ReferencedEnvelope extent;

    private SimpleFeatureType schema;

    public GDBSchema() {
    }

    public GDBSchema(String typeName) {
        this.typeName = typeName;
    }

    public String getTypeName() {
        return typeName;
    }

    public void setTypeName(String typeName) {
        this.typeName = typeName;
    }

    public int getShapeType() {
        return shapeType;
    }

    public void setShapeType(int shapeType) {
        this.shapeType = shapeType;
        if (shapeType == 1) {
            // Point, PointM, PointZ, PointMZ
            geometryBinding = Point.class;
        } else if (shapeType == 2) {
            // MultiPoint, MultiPointM, MultiPointZ, MultiPointMZ
            geometryBinding = MultiPoint.class;
        } else if (shapeType == 3) {
            // LineString, LineStringM, LineStringZ, LineStringMZ
            geometryBinding = MultiLineString.class;
        } else if (shapeType == 4) {
            // Polygon, PolygonM, PolygonZ, PolygonMZ
            geometryBinding = MultiPolygon.class;
        } else if (shapeType == 9) {
            // MultiPatch
            geometryBinding = MultiPolygon.class;
        }
    }

    public Class<?> getGeometryBinding() {
        return geometryBinding;
    }

    public String getShapeField() {
        return shapeField;
    }

    public void setShapeField(String shapeField) {
        this.shapeField = shapeField;
    }

    public int getSrid() {
        return srid;
    }

    public void setSrid(int srid) {
        this.srid = srid;
    }

    public CoordinateReferenceSystem getCrs() {
        return crs;
    }

    public void setCrs(CoordinateReferenceSystem crs) {
        this.crs = crs;
    }

    public double getGridSize() {
        return gridSize;
    }

    public void setGridSize(double gridSize) {
        this.gridSize = gridSize;
    }

    public ReferencedEnvelope getExtent() {
        return extent;
    }

    public void setExtent(ReferencedEnvelope extent) {
        this.extent = extent;
    }

    public SimpleFeatureType getSchema() {
        return schema;
    }

    public void setSchema(SimpleFeatureType schema) {
        this.schema = schema;
    }

}
