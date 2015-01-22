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

import java.io.IOException;
import java.util.Set;
import java.util.logging.Logger;

import org.geotools.data.AbstractFeatureSource;
import org.geotools.data.DataSourceException;
import org.geotools.data.DataStore;
import org.geotools.data.DefaultFeatureResults;
import org.geotools.data.DefaultQuery;
import org.geotools.data.FeatureListener;
import org.geotools.data.Query;
import org.geotools.data.QueryCapabilities;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.store.EmptyFeatureCollection;
import org.geotools.util.logging.Logging;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.Filter;

/**
 * NGI FeatureSource
 * 
 * @author MapPlus, mapplus@gmail.com, http://onspatial.com
 * @since 2012-10-30
 * @see
 * 
 */
@SuppressWarnings({ "unchecked", "deprecation" })
public class NGIFeatureSource extends AbstractFeatureSource {
    protected static final Logger LOGGER = Logging.getLogger(NGIFeatureSource.class);

    private final NGIDataStore dataStore;

    private final SimpleFeatureType featureType;

    @SuppressWarnings("rawtypes")
    public NGIFeatureSource(NGIDataStore dataStore, Set hints, SimpleFeatureType featureType) {
        super(hints);
        this.dataStore = dataStore;
        this.featureType = featureType;
    }

    public DataStore getDataStore() {
        return dataStore;
    }

    public void addFeatureListener(FeatureListener listener) {
        dataStore.listenerManager.addFeatureListener(this, listener);
    }

    public void removeFeatureListener(FeatureListener listener) {
        dataStore.listenerManager.removeFeatureListener(this, listener);
    }

    public SimpleFeatureType getSchema() {
        return featureType;
    }

    // ================= getFeatures(), getFeatures(Filter), getFeatures(Query)
    // for WMS GetFeatureInfo(), WFS services
    // ================================================================================

    public SimpleFeatureCollection getFeatures() throws IOException {
        return getFeatures(Filter.INCLUDE);
    }

    public SimpleFeatureCollection getFeatures(Filter filter) throws IOException {
        return getFeatures(new DefaultQuery(getSchema().getTypeName(), filter));
    }

    public SimpleFeatureCollection getFeatures(Query query) throws IOException {
        SimpleFeatureType schema = getSchema();
        String typeName = schema.getTypeName();

        if (query.getTypeName() == null) { // typeName unspecified we will "any" use a default
            DefaultQuery defaultQuery = new DefaultQuery(query);
            defaultQuery.setTypeName(typeName);
        } else if (!typeName.equals(query.getTypeName())) {
            return new EmptyFeatureCollection(schema);
        }

        final QueryCapabilities queryCapabilities = getQueryCapabilities();
        if (!queryCapabilities.supportsSorting(query.getSortBy())) {
            throw new DataSourceException("DataStore cannot provide the requested sort order");
        }

        SimpleFeatureCollection collection = new DefaultFeatureResults(this, query);
        if (collection.getSchema().getGeometryDescriptor() == null) {
            return collection; // no geometry no reprojection needed
        }

        return collection;
    }
}
