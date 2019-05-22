package org.geotools.data.altibase;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.DataUtilities;
import org.geotools.data.DefaultTransaction;
import org.geotools.data.FeatureWriter;
import org.geotools.data.Query;
import org.geotools.data.Transaction;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.data.simple.SimpleFeatureStore;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.geometry.jts.JTS;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.jdbc.JDBCDataStoreFactory;
import org.geotools.util.logging.Logging;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.ParseException;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.Name;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;

public class AltibaseTest {
    protected static final Logger LOGGER = Logging.getLogger(AltibaseTest.class);

    public static void main(String[] args) throws IOException, NoSuchAuthorityCodeException,
            FactoryException, ParseException {
        new AltibaseTest().execute();
    }

    private void execute() throws IOException, NoSuchAuthorityCodeException, FactoryException {
        Map<String, Object> params = getConnection();

        // create datastore
        AltibaseNGDataStoreFactory factory = new AltibaseNGDataStoreFactory();
        DataStore dataStore = factory.createDataStore(params);

        // get layers
        List<Name> typeNames = dataStore.getNames();
        for (Name typeName : typeNames) {
            SimpleFeatureSource sfs = dataStore.getFeatureSource(typeName);
            ReferencedEnvelope extent = sfs.getBounds();
            System.out.println(typeName + "'s extent = " + extent.toString());
            if (sfs.getSchema().getGeometryDescriptor() == null) {
                System.out.println(sfs.getName().toString() + " = " + sfs.getCount(Query.ALL));
            } else {
                System.out.println(sfs.getSchema().getGeometryDescriptor().getType());
                System.out.println(sfs.getName().toString() + " = " + sfs.getCount(Query.ALL));
            }
        }

        // upload shapefile: road point line polygon road_network
        String typeName = "point";
        DataStore shpStore = getShapefileDataStore("C:/data/road");
        SimpleFeatureSource shp_sfs = shpStore.getFeatureSource(typeName);
        System.out.println(shp_sfs.getName().toString() + " = " + shp_sfs.getCount(Query.ALL));

        SimpleFeatureSource out = uploadFeatures(shp_sfs, dataStore, shp_sfs.getSchema()
                .getTypeName());

        System.out.println(out.getName().toString() + " inserted = " + out.getCount(Query.ALL));

        // filter test
        String geom = out.getSchema().getGeometryDescriptor().getLocalName();
        Geometry bounds = JTS.toGeometry(out.getBounds());

        FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2(null);
        Filter filter = ff.intersects(ff.property(geom), ff.literal(bounds));
        System.out.println("Intersects = " + out.getFeatures(filter).size());

        dataStore.dispose();

        System.out.println("completed");
    }

    private SimpleFeatureSource uploadFeatures(SimpleFeatureSource source, DataStore targetStore,
            String targetName) throws IOException {
        int flushInterval = 1000;
        boolean overwrite = true;

        // Altibase Geometry 필드 크기 최적화: Retype FeatureCollection
        int maxGeomSize = getMaximumGeometrySize(source.getFeatures(Filter.INCLUDE));
        System.out.println("maxGeomSize = " + maxGeomSize);

        // 레이어이름 대소문자 전환

        // 좌표변환

        try {
            SimpleFeatureType existType = targetStore.getSchema(targetName);
            if (existType != null) {
                if (false == overwrite) {
                    LOGGER.log(Level.INFO, targetName + " already exist!");
                    return targetStore.getFeatureSource(targetName);
                } else {
                    // drop schema
                    targetStore.removeSchema(targetName);
                }
            }
        } catch (IOException e) {
            LOGGER.log(Level.INFO, e.getMessage());
        }

        // try create schema
        targetStore.createSchema(source.getSchema());
        SimpleFeatureSource target = targetStore.getFeatureSource(targetName);
        if (target == null) {
            return target;
        }

        // create transaction & feature writer
        Transaction transaction = new DefaultTransaction(targetName);
        FeatureWriter<SimpleFeatureType, SimpleFeature> writer = null;
        writer = targetStore.getFeatureWriterAppend(targetName, transaction);
        if (writer == null) {
            return target;
        }

        // set transaction
        SimpleFeatureStore featureStore = (SimpleFeatureStore) target;
        Transaction ot = featureStore.getTransaction(); // AUTO_COMMIT
        featureStore.setTransaction(transaction);

        // insert features
        SimpleFeatureIterator featureIter = source.getFeatures(Filter.INCLUDE).features();
        try {
            int featureCount = 0;
            while (featureIter.hasNext()) {
                featureCount++;
                SimpleFeature feature = featureIter.next();

                // Altibase does not support none-simple geometry
                Geometry geometry = (Geometry) feature.getDefaultGeometry();
                if (!geometry.isSimple()) {
                    geometry = geometry.buffer(0);
                }

                SimpleFeature newFeature = writer.next();
                newFeature.setAttributes(feature.getAttributes());
                newFeature.setDefaultGeometry(geometry);

                try {
                    writer.write();
                } catch (Exception e) {
                    System.out.println(e.getMessage());
                }

                if ((featureCount % flushInterval) == 0) {
                    transaction.commit();
                }
            }
            transaction.commit();
        } catch (Exception e) {
            transaction.rollback();
            System.out.println(e.getMessage());
        } finally {
            featureIter.close();
            transaction.close();
            featureStore.setTransaction(ot); // restore transaction
        }

        return target;
    }

    private int getMaximumGeometrySize(SimpleFeatureCollection source) throws IOException {
        int max_num_points = Integer.MIN_VALUE;
        SimpleFeatureIterator featureIter = source.features();
        try {
            while (featureIter.hasNext()) {
                SimpleFeature feature = featureIter.next();
                Geometry geometry = (Geometry) feature.getDefaultGeometry();
                int num_points = geometry.getNumPoints();
                max_num_points = Math.max(max_num_points, num_points);
            }
        } finally {
            featureIter.close();
        }

        int max = max_num_points == 1 ? 25 : max_num_points * 25;
        return Math.min(max, 104857600); // Limit Max ST_OBJECT_BUFFER_SIZE
    }

    @SuppressWarnings("unused")
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
        params.put(JDBCDataStoreFactory.DBTYPE.key, "altibase");
        params.put(JDBCDataStoreFactory.HOST.key, "localhost");
        params.put(JDBCDataStoreFactory.DATABASE.key, "mydb");
        params.put(JDBCDataStoreFactory.SCHEMA.key, "SYS");
        params.put(JDBCDataStoreFactory.PORT.key, "20300");
        params.put(JDBCDataStoreFactory.USER.key, "sys");
        params.put(JDBCDataStoreFactory.PASSWD.key, "manager");

        // Altibase ERROR: preparedStatements
        params.put(AltibaseNGDataStoreFactory.PREPARED_STATEMENTS.key, Boolean.FALSE);
        params.put(AltibaseNGDataStoreFactory.ENCODING.key, "UTF8");
        return params;
    }

    private DataStore getShapefileDataStore(String folder) throws IOException {
        Map<String, Object> params = new HashMap<String, Object>();

        final File file = new File(folder);
        params.put("url", DataUtilities.fileToURL(file));
        params.put("charset", "x-windows-949");

        return DataStoreFinder.getDataStore(params);
    }
}
