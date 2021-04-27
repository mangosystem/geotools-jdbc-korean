package org.geotools.data.kairos;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.DefaultTransaction;
import org.geotools.data.FeatureWriter;
import org.geotools.data.Query;
import org.geotools.data.Transaction;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.data.simple.SimpleFeatureStore;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.geometry.jts.JTS;
import org.geotools.jdbc.JDBCDataStoreFactory;
import org.geotools.util.URLs;
import org.geotools.util.logging.Logging;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;

public class KairosTest {
    protected static final Logger LOGGER = Logging.getLogger(KairosTest.class);

    public static void main(String[] args) throws IOException, NoSuchAuthorityCodeException,
            FactoryException, ParseException {
        try {
            new KairosTest().testJdbcConnection();
        } catch (ClassNotFoundException e) {
            LOGGER.log(Level.FINER, e.getMessage(), e);
        } catch (SQLException e) {
            LOGGER.log(Level.FINER, e.getMessage(), e);
        }

        new KairosTest().execute();
    }

    private void execute() throws IOException, NoSuchAuthorityCodeException, FactoryException {
        Map<String, Object> params = getConnection();

        // create datastore
        KairosNGDataStoreFactory factory = new KairosNGDataStoreFactory();
        DataStore dataStore = factory.createDataStore(params);

        SimpleFeatureSource polyfs = dataStore.getFeatureSource("polygon");
        printFeatures(polyfs);

        // upload shapefile: road point line polygon road_network 3857
        String typeName = "line";
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

    private void testJdbcConnection() throws ClassNotFoundException, SQLException, IOException {
        Class.forName("kr.co.realtimetech.kairos.jdbc.kairosDriver");

        Connection cx = DriverManager.getConnection(getJDBCUrl(), "root", "root");

        // metadata bug
        DatabaseMetaData metaData = cx.getMetaData();
        ResultSet typeInfo = metaData.getTypeInfo(); // ERROR
        typeInfo.close();

        // transaction bug
        boolean autoCommit = cx.getAutoCommit();
        cx.setAutoCommit(autoCommit);
        cx.setAutoCommit(autoCommit); // ERROR!

        // select table
        String sql = "SELECT * from GEOMETRY_COLUMNS ORDER BY F_GEOMETRY_TYPE";
        Statement st = cx.createStatement();
        ResultSet rs = st.executeQuery(sql);
        while (rs.next()) {
            String schema = rs.getString(2);
            String name = rs.getString(3);
            String column = rs.getString(4);
            int dim = rs.getInt(5);
            int srid = rs.getInt(6);
            String type = rs.getString(7);
            System.out.println(schema + "," + name + "," + column + "," + dim + "," + srid + ","
                    + type);
        }
        rs.close();
        st.close();

        // select layer as WKT
        sql = "Select ST_ASTEXT(the_geom) from root.point LIMIT 1";
        st = cx.createStatement();
        rs = st.executeQuery(sql);
        while (rs.next()) {
            String wellKnownText = rs.getString(1);
            Geometry geom;
            try {
                geom = new WKTReader().read(wellKnownText);
                // byte[] bytes = new WKBWriter(2, ByteOrderValues.LITTLE_ENDIAN).write(geom);
                System.out.println(geom.getGeometryType() + " = " + geom.getArea());
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
        rs.close();
        st.close();

        // select layer as WKB
        sql = "Select ST_ASBINARY(the_geom) from root.polygon LIMIT 1";
        st = cx.createStatement();
        rs = st.executeQuery(sql);
        while (rs.next()) {
            byte[] wkbBytes = rs.getBytes(1);
            Geometry geom;
            try {
                geom = new WKBReader().read(wkbBytes);
                System.out.println(geom.getGeometryType() + " = " + geom.getArea());
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
        rs.close();
        st.close();

        // finally close connection
        cx.close();
    }

    private SimpleFeatureSource uploadFeatures(SimpleFeatureSource source, DataStore targetStore,
            String targetName) throws IOException {
        int flushInterval = 1000;
        boolean overwrite = true;

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
        SimpleFeatureSource target = null;
        try {
            targetStore.createSchema(source.getSchema());
            target = targetStore.getFeatureSource(targetName);
        } catch (IOException e) {
            LOGGER.log(Level.INFO, e.getMessage());
        }

        if (target == null) {
            return target;
        }

        // create transaction & feature writer
        Transaction transaction = new DefaultTransaction(targetName);
        FeatureWriter<SimpleFeatureType, SimpleFeature> writer = null;
        writer = targetStore.getFeatureWriterAppend(targetName, transaction);
        if (writer == null) {
            transaction.close();
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

    private void printFeatures(SimpleFeatureSource sfs) throws IOException {
        SimpleFeatureIterator featureIter = null;
        try {
            featureIter = sfs.getFeatures(Filter.INCLUDE).features();
            while (featureIter.hasNext()) {
                SimpleFeature feature = featureIter.next();
                System.out.println(feature);
                break;
            }
        } finally {
            featureIter.close();
        }
    }

    private Map<String, Object> getConnection() {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put(JDBCDataStoreFactory.DBTYPE.key, "kairos");
        params.put(JDBCDataStoreFactory.HOST.key, "localhost");
        params.put(JDBCDataStoreFactory.DATABASE.key, "test");
        params.put(JDBCDataStoreFactory.PORT.key, "5000");
        params.put(JDBCDataStoreFactory.USER.key, "root");
        params.put(JDBCDataStoreFactory.PASSWD.key, "root");
        params.put(KairosNGDataStoreFactory.PREPARED_STATEMENTS.key, Boolean.FALSE);
        return params;
    }

    private String getJDBCUrl() throws IOException {
        Map<String, Object> params = getConnection();
        String host = (String) KairosNGDataStoreFactory.HOST.lookUp(params);
        String db = (String) KairosNGDataStoreFactory.DATABASE.lookUp(params);
        int port = (Integer) KairosNGDataStoreFactory.PORT.lookUp(params);
        String encoding = (String) KairosNGDataStoreFactory.ENCODING.lookUp(params);

        if (encoding == null || encoding.isEmpty()) {
            return "jdbc:kairos" + "://" + host + ":" + port + "/" + db;
        } else {
            return "jdbc:kairos" + "://" + host + ":" + port + "/" + db + ";CHARSET=" + encoding;
        }
    }

    private DataStore getShapefileDataStore(String folder) throws IOException {
        Map<String, Object> params = new HashMap<String, Object>();

        final File file = new File(folder);
        params.put("url", URLs.fileToUrl(file));
        params.put("charset", "x-windows-949");

        return DataStoreFinder.getDataStore(params);
    }
}
