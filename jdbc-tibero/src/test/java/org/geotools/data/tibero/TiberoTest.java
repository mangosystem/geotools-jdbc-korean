package org.geotools.data.tibero;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.measure.unit.SI;

import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.DataUtilities;
import org.geotools.data.DefaultTransaction;
import org.geotools.data.FeatureWriter;
import org.geotools.data.Query;
import org.geotools.data.Transaction;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.data.simple.SimpleFeatureStore;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.geometry.jts.JTS;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.jdbc.JDBCDataStoreFactory;
import org.geotools.util.logging.Logging;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.Name;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.io.ParseException;

public class TiberoTest {
    protected static final Logger LOGGER = Logging.getLogger(TiberoTest.class);

    FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2(null);

    GeometryFactory gf = new GeometryFactory();

    public static void main(String[] args) throws IOException, NoSuchAuthorityCodeException,
            FactoryException, ParseException {
        new TiberoTest().execute();
    }

    private void execute() throws IOException, NoSuchAuthorityCodeException, FactoryException {
        Map<String, Object> params = getConnection();

        // create datastore
        TiberoNGDataStoreFactory factory = new TiberoNGDataStoreFactory();
        DataStore dataStore = factory.createDataStore(params);

        // get layers
        List<Name> typeNames = dataStore.getNames();
        for (Name typeName : typeNames) {
            SimpleFeatureSource sfs = dataStore.getFeatureSource(typeName);
            ReferencedEnvelope extent = sfs.getBounds();

            System.out.println(sfs.getName().toString() + " = " + sfs.getCount(Query.ALL));
            System.out.println(typeName + "'s extent = " + extent.toString());

            CoordinateReferenceSystem crs = sfs.getSchema().getCoordinateReferenceSystem();
            System.out.println(crs);
        }

        // upload shapefile: road point line polygon
        String typeName = "AL_11_D002_20160604";
        DataStore shpStore = getShapefileDataStore("C:/spatial_data/NSDI/연속지적도형정보");
        SimpleFeatureSource shp_sfs = shpStore.getFeatureSource(typeName);
        System.out.println(shp_sfs.getName().toString() + " = " + shp_sfs.getCount(Query.ALL));

        SimpleFeatureSource out = uploadFeatures(shp_sfs, dataStore, shp_sfs.getSchema()
                .getTypeName());

        System.out.println(out.getName().toString() + " inserted = " + out.getCount(Query.ALL));

        // filter test
        String geom = out.getSchema().getGeometryDescriptor().getLocalName();
        ReferencedEnvelope extent = out.getBounds();

        // Intersect filter
        Filter filter = ff.intersects(ff.property(geom), ff.literal(JTS.toGeometry(extent)));
        System.out.println("Intersects Filter = " + out.getFeatures(filter).size());

        extent.expandBy(-extent.getWidth() / 3);
        filter = ff.intersects(ff.property(geom), ff.literal(JTS.toGeometry(extent)));
        System.out.println("Intersects Filter = " + out.getFeatures(filter).size());

        filter = ff.disjoint(ff.property(geom), ff.literal(JTS.toGeometry(extent)));
        System.out.println("Disjoint Filter = " + out.getFeatures(filter).size());

        // BBOX Filter
        filter = ff.bbox(ff.property(geom), extent);
        System.out.println("BBOX Filter = " + out.getFeatures(filter).size());

        // Distance Filter = DWithin, Beyond
        // Tibero does not support DWithin, Beyond, so wi should use ST_Distance function!
        Point center = gf.createPoint(extent.centre());
        filter = ff.dwithin(ff.property(geom), ff.literal(center), 500, SI.METRE.getSymbol());
        System.out.println("DWithin Filter = " + out.getFeatures(filter).size());

        dataStore.dispose();

        System.out.println("completed");
    }

    private SimpleFeatureSource uploadFeatures(SimpleFeatureSource source, DataStore targetStore,
            String targetName) throws IOException {
        int flushInterval = 1000;
        boolean overwrite = false;

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

                SimpleFeature newFeature = writer.next();
                newFeature.setAttributes(feature.getAttributes());
                writer.write();

                if ((featureCount % flushInterval) == 0) {
                    transaction.commit();
                }
            }
            transaction.commit();
        } catch (Exception e) {
            transaction.rollback();
        } finally {
            featureIter.close();
            transaction.close();
            featureStore.setTransaction(ot); // restore transaction
        }

        return target;
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
        params.put(JDBCDataStoreFactory.DBTYPE.key, "tibero");
        params.put(JDBCDataStoreFactory.HOST.key, "localhost");
        params.put(JDBCDataStoreFactory.DATABASE.key, "tibero");
        params.put(JDBCDataStoreFactory.SCHEMA.key, "SYSGIS");
        params.put(JDBCDataStoreFactory.PORT.key, "8629");
        params.put(JDBCDataStoreFactory.USER.key, "sysgis");
        params.put(JDBCDataStoreFactory.PASSWD.key, "tibero");
        params.put(JDBCDataStoreFactory.EXPOSE_PK.key, Boolean.FALSE);
        params.put(TiberoNGDataStoreFactory.PREPARED_STATEMENTS.key, Boolean.FALSE);
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
