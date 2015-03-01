package org.geotools.data.tibero;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.DataUtilities;
import org.geotools.data.DefaultTransaction;
import org.geotools.data.Query;
import org.geotools.data.Transaction;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.data.simple.SimpleFeatureStore;
import org.geotools.jdbc.JDBCDataStoreFactory;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.type.Name;
import org.opengis.filter.Filter;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;

import com.sun.org.apache.xerces.internal.impl.dv.util.Base64;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.ByteOrderValues;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKBWriter;
import com.vividsolutions.jts.io.WKTReader;

public class TiberoTest {

    public static void main(String[] args) throws IOException, NoSuchAuthorityCodeException,
            FactoryException, ParseException {        
        new TiberoTest().execute();
    }

    private void execute() throws IOException, NoSuchAuthorityCodeException, FactoryException {
        Map<String, Object> params = getConnection();

        // create datastore
        TiberoNGDataStoreFactory factory = new TiberoNGDataStoreFactory();
        DataStore dataStore = factory.createDataStore(params);

        // get typenames
        List<Name> typeNames = dataStore.getNames();
        for (Name typeName : typeNames) {
            SimpleFeatureSource sfs = dataStore.getFeatureSource(typeName);
            // ReferencedEnvelope extent = sfs.getBounds();
            // System.out.println(extent);
            if (sfs.getSchema().getGeometryDescriptor() == null) {
                System.out.println(sfs.getName().toString() + " = " + sfs.getCount(Query.ALL));
            } else {
                System.out.println(sfs.getSchema().getGeometryDescriptor().getType());
                System.out.println(sfs.getName().toString() + " = " + sfs.getCount(Query.ALL));
            }

            //printFeatures(sfs);
        }

        // upload shapefile : ROAD_LINK2 STORES KOB_TL_LINK2
        String typeName = "KOB_TL_LINK2";
        DataStore shpStore = getShapefileDataStore("C:/OpenGeoSuite/tibero/");
        SimpleFeatureSource shp_sfs = shpStore.getFeatureSource(typeName);
        System.out.println(shp_sfs.getName().toString() + " = " + shp_sfs.getCount(Query.ALL));

        dataStore.createSchema(shp_sfs.getSchema());
        SimpleFeatureSource out = dataStore.getFeatureSource(typeName);
        SimpleFeatureStore sfStore = (SimpleFeatureStore) out;

        Transaction transaction = new DefaultTransaction(typeName);
        sfStore.setTransaction(transaction);

        sfStore.addFeatures(shp_sfs.getFeatures());

        transaction.commit();
        sfStore.setTransaction(Transaction.AUTO_COMMIT);
        transaction.close();

        System.out.println(out.getName().toString() + " inserted = " + out.getCount(Query.ALL));
        SimpleFeatureIterator featureIter = null;
        try {
            featureIter = out.getFeatures(Filter.INCLUDE).features();
            while (featureIter.hasNext()) {
                SimpleFeature feature = featureIter.next();
                System.out.println(feature);
            }
        } finally {
            featureIter.close();
        }
        System.out.println("completed");
    }

    private void printFeatures(SimpleFeatureSource sfs) throws IOException {
        SimpleFeatureIterator featureIter = null;
        try {
            featureIter = sfs.getFeatures(Filter.INCLUDE).features();
            while (featureIter.hasNext()) {
                SimpleFeature feature = featureIter.next();
                System.out.println(feature);
            }
        } finally {
            featureIter.close();
        }
    }

    private Map<String, Object> getConnection() {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put(JDBCDataStoreFactory.DBTYPE.key, "tibero");
        params.put(JDBCDataStoreFactory.HOST.key, "localhost");
        params.put(JDBCDataStoreFactory.DATABASE.key, "tibero");
        params.put(JDBCDataStoreFactory.PORT.key, "8629");
        params.put(JDBCDataStoreFactory.USER.key, "sysgis");
        params.put(JDBCDataStoreFactory.PASSWD.key, "dlalsvk");
        params.put("preparedStatements", Boolean.TRUE);
        return params;
    }

    public static DataStore getShapefileDataStore(String folder) throws IOException {
        Map<String, Object> params = new HashMap<String, Object>();

        final File file = new File(folder);
        params.put("url", DataUtilities.fileToURL(file));
        params.put("charset", "x-windows-949");

        return DataStoreFinder.getDataStore(params);
    }
}
