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
package org.geotools.data.tibero;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;
import java.util.Map;

import org.geotools.factory.Hints;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.jdbc.ColumnMetadata;
import org.geotools.jdbc.JDBCDataStore;
import org.geotools.jdbc.PreparedFilterToSQL;
import org.geotools.jdbc.PreparedStatementSQLDialect;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.io.ByteOrderValues;
import org.locationtech.jts.io.WKBWriter;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.GeometryDescriptor;

public class TiberoPSDialect extends PreparedStatementSQLDialect {

    private TiberoDialect delegate;

    public TiberoPSDialect(JDBCDataStore store, TiberoDialect delegate) {
        super(store);
        this.delegate = delegate;
    }

    public boolean isLooseBBOXEnabled() {
        return delegate.isLooseBBOXEnabled();
    }

    public void setLooseBBOXEnabled(boolean looseBBOXEnabled) {
        delegate.setLooseBBOXEnabled(looseBBOXEnabled);
    }

    public boolean isEstimatedExtentsEnabled() {
        return delegate.isEstimatedExtentsEnabled();
    }

    public void setEstimatedExtentsEnabled(boolean estimatedExtentsEnabled) {
        delegate.setEstimatedExtentsEnabled(estimatedExtentsEnabled);
    }

    @Override
    public boolean isAggregatedSortSupported(String function) {
        return delegate.isAggregatedSortSupported(function);
    }

    @Override
    public void initializeConnection(Connection cx) throws SQLException {
        super.initializeConnection(cx);
    }

    @Override
    public boolean includeTable(String schemaName, String tableName, Connection cx)
            throws SQLException {
        return delegate.includeTable(schemaName, tableName, cx);
    }

    public Geometry decodeGeometryValue(GeometryDescriptor descriptor, ResultSet rs, int column,
            GeometryFactory factory, Connection cx) throws IOException, SQLException {
        return delegate.decodeGeometryValue(descriptor, rs, column, factory, cx);
    }

    public Geometry decodeGeometryValue(GeometryDescriptor descriptor, ResultSet rs, String column,
            GeometryFactory factory, Connection cx) throws IOException, SQLException {
        return delegate.decodeGeometryValue(descriptor, rs, column, factory, cx);
    }

    @Override
    public Geometry decodeGeometryValue(GeometryDescriptor descriptor, ResultSet rs, String column,
            GeometryFactory factory, Connection cx, Hints hints) throws IOException, SQLException {
        return delegate.decodeGeometryValue(descriptor, rs, column, factory, cx, hints);
    }

    @Override
    public void encodeGeometryColumn(GeometryDescriptor gatt, String prefix, int srid, Hints hints,
            StringBuffer sql) {
        delegate.encodeGeometryColumn(gatt, prefix, srid, hints, sql);
    }

    @Override
    public void encodeGeometryEnvelope(String tableName, String geometryColumn, StringBuffer sql) {
        delegate.encodeGeometryEnvelope(tableName, geometryColumn, sql);
    }

    @Override
    public void encodeColumnName(String prefix, String raw, StringBuffer sql) {
        delegate.encodeColumnName(prefix, raw, sql);
    }

    @Override
    public List<ReferencedEnvelope> getOptimizedBounds(String schema, SimpleFeatureType featureType,
            Connection cx) throws SQLException, IOException {
        return delegate.getOptimizedBounds(schema, featureType, cx);
    }

    @Override
    public Envelope decodeGeometryEnvelope(ResultSet rs, int column, Connection cx)
            throws SQLException, IOException {
        return delegate.decodeGeometryEnvelope(rs, column, cx);
    }

    @Override
    public Class<?> getMapping(ResultSet columnMetaData, Connection cx) throws SQLException {
        return delegate.getMapping(columnMetaData, cx);
    }

    @Override
    public void handleUserDefinedType(ResultSet columnMetaData, ColumnMetadata metadata,
            Connection cx) throws SQLException {
        delegate.handleUserDefinedType(columnMetaData, metadata, cx);
    }

    @Override
    public Integer getGeometrySRID(String schemaName, String tableName, String columnName,
            Connection cx) throws SQLException {
        return delegate.getGeometrySRID(schemaName, tableName, columnName, cx);
    }

    @Override
    public int getGeometryDimension(String schemaName, String tableName, String columnName,
            Connection cx) throws SQLException {
        return delegate.getGeometryDimension(schemaName, tableName, columnName, cx);
    }

    @Override
    public String getSequenceForColumn(String schemaName, String tableName, String columnName,
            Connection cx) throws SQLException {
        return delegate.getSequenceForColumn(schemaName, tableName, columnName, cx);
    }

    @Override
    public Object getNextSequenceValue(String schemaName, String sequenceName, Connection cx)
            throws SQLException {
        return delegate.getNextSequenceValue(schemaName, sequenceName, cx);
    }

    @Override
    public boolean lookupGeneratedValuesPostInsert() {
        return delegate.lookupGeneratedValuesPostInsert();
    }

    @Override
    public Object getLastAutoGeneratedValue(String schemaName, String tableName, String columnName,
            Connection cx) throws SQLException {
        return delegate.getLastAutoGeneratedValue(schemaName, tableName, columnName, cx);
    }

    @Override
    public Object getNextAutoGeneratedValue(String schemaName, String tableName, String columnName,
            Connection cx) throws SQLException {
        return delegate.getNextAutoGeneratedValue(schemaName, tableName, columnName, cx);
    }

    @Override
    public void registerClassToSqlMappings(Map<Class<?>, Integer> mappings) {
        delegate.registerClassToSqlMappings(mappings);
    }

    @Override
    public void registerSqlTypeNameToClassMappings(Map<String, Class<?>> mappings) {
        delegate.registerSqlTypeNameToClassMappings(mappings);
    }

    @Override
    public void registerSqlTypeToSqlTypeNameOverrides(Map<Integer, String> overrides) {
        delegate.registerSqlTypeToSqlTypeNameOverrides(overrides);
    }

    @Override
    public String getGeometryTypeName(Integer type) {
        return delegate.getGeometryTypeName(type);
    }

    @Override
    public void encodePrimaryKey(String column, StringBuffer sql) {
        delegate.encodePrimaryKey(column, sql);
    }

    @Override
    public void postCreateTable(String schemaName, SimpleFeatureType featureType, Connection cx)
            throws SQLException {
        delegate.postCreateTable(schemaName, featureType, cx);
    }

    @Override
    public void postDropTable(String schemaName, SimpleFeatureType featureType, Connection cx)
            throws SQLException {
        delegate.postDropTable(schemaName, featureType, cx);
    }

    @Override
    public boolean isLimitOffsetSupported() {
        return delegate.isLimitOffsetSupported();
    }

    @Override
    public void applyLimitOffset(StringBuffer sql, int limit, int offset) {
        delegate.applyLimitOffset(sql, limit, offset);
    }

    @Override
    public int getDefaultVarcharSize() {
        return delegate.getDefaultVarcharSize();
    }

    @Override
    public String[] getDesiredTablesType() {
        return delegate.getDesiredTablesType();
    }

    @Override
    public void encodePostColumnCreateTable(AttributeDescriptor att, StringBuffer sql) {
        delegate.encodePostColumnCreateTable(att, sql);
    }

    @Override
    public void postCreateAttribute(AttributeDescriptor att, String tableName, String schemaName,
            Connection cx) throws SQLException {
        delegate.postCreateAttribute(att, tableName, schemaName, cx);
    }

    @Override
    public void postCreateFeatureType(SimpleFeatureType featureType, DatabaseMetaData metadata,
            String schemaName, Connection cx) throws SQLException {
        delegate.postCreateFeatureType(featureType, metadata, schemaName, cx);
    }

    @SuppressWarnings("rawtypes")
    @Override
    public void prepareGeometryValue(Class<? extends Geometry> gClass, int dimension, int srid,
            Class binding, StringBuffer sql) {
        if (gClass != null) {
            sql.append("ST_GEOMFROMWKB(?)");
        } else {
            super.prepareGeometryValue(gClass, dimension, srid, binding, sql);
        }
    }

    @SuppressWarnings("rawtypes")
    @Override
    public void setGeometryValue(Geometry g, int dimension, int srid, Class binding,
            PreparedStatement ps, int column) throws SQLException {
        if (g != null) {
            if (g instanceof LinearRing) {
                // WKT does not support linear rings
                g = g.getFactory().createLineString(((LinearRing) g).getCoordinateSequence());
            }

            byte[] bytes = new WKBWriter(dimension, ByteOrderValues.LITTLE_ENDIAN).write(g);
            ps.setBytes(column, bytes);
        } else {
            ps.setNull(column, Types.OTHER, "Geometry");
        }
    }

    @Override
    public PreparedFilterToSQL createPreparedFilterToSQL() {
        TiberoPSFilterToSql fts = new TiberoPSFilterToSql(this);
        fts.setLooseBBOXEnabled(delegate.isLooseBBOXEnabled());
        // fts.setEncodeBBOXFilterAsEnvelope(delegate.isEncodeBBOXFilterAsEnvelope());
        return fts;
    }
}
