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
package org.geotools.data.ngi;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geotools.data.AbstractDataStore;
import org.geotools.data.DataSourceException;
import org.geotools.data.DataUtilities;
import org.geotools.data.EmptyFeatureReader;
import org.geotools.data.FeatureReader;
import org.geotools.data.Query;
import org.geotools.data.Transaction;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.feature.SchemaException;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.util.logging.Logging;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.Filter;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * NGI DataStore
 * 
 * @author MapPlus, mapplus@gmail.com, http://onspatial.com
 * @since 2012-10-30
 * @see
 * 
 */
public class NGIDataStore extends AbstractDataStore {
    protected static final Logger LOGGER = Logging.getLogger(NGIDataStore.class);

    private final NGISchemaReader schemaReader;

    private File ngiFile;

    private File ndaFile;

    private final Charset charset;

    public NGIDataStore(File ngiFile, Charset charset, CoordinateReferenceSystem crs)
            throws IOException {
        super(false);
        this.ngiFile = ngiFile;
        this.charset = charset;

        // Linux
        final int endIndex = ngiFile.getPath().length() - 4;
        this.ndaFile = new File(ngiFile.getPath().substring(0, endIndex) + ".nda");
        if (!ndaFile.exists()) {
            this.ndaFile = new File(ngiFile.getPath().substring(0, endIndex) + ".NDA");
        }

        if (!ndaFile.exists()) {
            LOGGER.log(Level.WARNING, "NDA file does not exist!");
        }

        this.schemaReader = new NGISchemaReader(ngiFile, ndaFile, charset, crs);
    }

    @Override
    public String[] getTypeNames() throws IOException {
        return schemaReader.getSchemas().keySet()
                .toArray(new String[schemaReader.getSchemas().size()]);
    }

    @Override
    public SimpleFeatureType getSchema(String typeName) throws IOException {
        return schemaReader.getSchemas().get(typeName);
    }

    @Override
    protected ReferencedEnvelope getBounds(Query query) throws IOException {
        // return full extent!
        return schemaReader.getBounds().get(query.getTypeName());
    }

    @Override
    protected int getCount(Query query) throws IOException {
        // return all count!
        return schemaReader.getCounts().get(query.getTypeName());
    }

    @Override
    protected FeatureReader<SimpleFeatureType, SimpleFeature> getFeatureReader(String typeName)
            throws IOException {
        return new NGIFeatureReader(new NGIReader(ngiFile, ndaFile, charset), getSchema(typeName));
    }

    @Override
    public FeatureReader<SimpleFeatureType, SimpleFeature> getFeatureReader(Query query,
            Transaction transaction) throws IOException {
        Filter filter = query.getFilter();
        String typeName = query.getTypeName();
        String propertyNames[] = query.getPropertyNames();

        if (filter == null) {
            throw new NullPointerException("getFeatureReader requires Filter: "
                    + "did you mean Filter.INCLUDE?");
        }

        if (typeName == null) {
            throw new NullPointerException("getFeatureReader requires typeName: "
                    + "use getTypeNames() for a list of available types");
        }

        if (transaction == null) {
            throw new NullPointerException("getFeatureReader requires Transaction: "
                    + "did you mean to use Transaction.AUTO_COMMIT?");
        }

        SimpleFeatureType featureType = getSchema(query.getTypeName());
        if (propertyNames != null || query.getCoordinateSystem() != null) {
            try {
                featureType = DataUtilities.createSubType(featureType, propertyNames,
                        query.getCoordinateSystem());
            } catch (SchemaException e) {
                LOGGER.log(Level.FINEST, e.getMessage(), e);
                throw new DataSourceException("Could not create Feature Type for query", e);

            }
        }

        if (filter == Filter.EXCLUDE || filter.equals(Filter.EXCLUDE)) {
            return new EmptyFeatureReader<SimpleFeatureType, SimpleFeature>(featureType);
        }

        // GR: allow subclases to implement as much filtering as they can,
        // by returning just it's unsupperted filter
        filter = getUnsupportedFilter(typeName, filter);
        if (filter == null) {
            throw new NullPointerException(
                    "getUnsupportedFilter shouldn't return null. Do you mean Filter.INCLUDE?");
        }

        return new NGIFeatureReader(new NGIReader(ngiFile, ndaFile, charset), getSchema(typeName),
                query);
    }

    @Override
    public SimpleFeatureSource getFeatureSource(final String typeName) {
        try {
            return new NGIFeatureSource(this, Collections.EMPTY_SET, getSchema(typeName));
        } catch (IOException e) {
            LOGGER.log(Level.FINER, e.getMessage(), e);
        }
        return null;
    }
}
