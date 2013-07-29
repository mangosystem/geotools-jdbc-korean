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
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.factory.GeoTools;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.geotools.jdbc.JDBCDataStoreFactory;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.type.Name;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;

public class TiberoTest {

    static FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2(GeoTools.getDefaultHints());

    static GeometryFactory gf = JTSFactoryFinder.getGeometryFactory(GeoTools.getDefaultHints());

    public static void main(String[] args) throws IOException {
        new TiberoTest().execute();
    }

    private void execute() throws IOException {
        Map<String, Object> params = getConnection();

        // create datastore
        TiberoNGDataStoreFactory factory = new TiberoNGDataStoreFactory();
        DataStore dataStore = factory.createDataStore(params);

        // filter
        SimpleFeatureSource sgg = dataStore.getFeatureSource("admin_sgg");

        Filter filter = null;

        filter = ff.equal(ff.property("SGG_NM"), ff.literal("강남구"), true);
        System.out.println(sgg.getFeatures(filter).size());

        Geometry jtsGeom = gf.createPoint(new Coordinate(200000, 450000)).buffer(9500);
        filter = ff.intersects(ff.property("the_geom"), ff.literal(jtsGeom));
        System.out.println(sgg.getFeatures(filter).size());
        
        filter = ff.dwithin(ff.property("the_geom"), ff.literal(jtsGeom), 9500, "m");
        System.out.println(sgg.getFeatures(filter).size());

        if (true)
            return;

        // get typenames
        List<Name> typeNames = dataStore.getNames();
        for (Name typeName : typeNames) {
            SimpleFeatureSource sfs = dataStore.getFeatureSource(typeName);
            if (sfs.getSchema().getGeometryDescriptor() == null) {
                System.out.println(sfs.getName().toString() + " = " + sfs.getCount(Query.ALL));
            } else {
                System.out.println(sfs.getSchema().getGeometryDescriptor().getType());
                System.out.println(sfs.getName().toString() + " = " + sfs.getCount(Query.ALL));
            }
        }

        // upload shapefile firestation admin_sgg events
        DataStore shpStore = getShapefileDataStore("C:/OpenGeoSuite/data/seoul/");

        String typeName = "admin_sgg";
        SimpleFeatureSource shp_sfs = shpStore.getFeatureSource(typeName);
        System.out.println(shp_sfs.getName().toString() + " = " + shp_sfs.getCount(Query.ALL));

        SimpleFeatureSource out = convert(shp_sfs, dataStore);

        SimpleFeatureIterator featureIter = null;
        try {
            featureIter = out.getFeatures(filter).features();
            while (featureIter.hasNext()) {
                SimpleFeature feature = featureIter.next();
                System.out.println(feature);
            }
        } finally {
            featureIter.close();
        }

        System.out.println(out.getName().toString() + " inserted = " + out.getCount(Query.ALL));
        System.out.println("completed");
    }

    private SimpleFeatureSource convert(SimpleFeatureSource shp_sfs, DataStore dataStore)
            throws IOException {
        String typeName = shp_sfs.getSchema().getTypeName();

        dataStore.createSchema(shp_sfs.getSchema());

        SimpleFeatureSource out = dataStore.getFeatureSource(typeName);
        SimpleFeatureStore sfStore = (SimpleFeatureStore) out;

        Transaction transaction = new DefaultTransaction(typeName);
        sfStore.setTransaction(transaction);

        sfStore.addFeatures(shp_sfs.getFeatures());

        transaction.commit();
        sfStore.setTransaction(Transaction.AUTO_COMMIT);
        transaction.close();

        return out;
    }

    private Map<String, Object> getConnection() {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put(JDBCDataStoreFactory.DBTYPE.key, "tibero");
        params.put(JDBCDataStoreFactory.HOST.key, "localhost");
        params.put(JDBCDataStoreFactory.DATABASE.key, "tibero");
        params.put(JDBCDataStoreFactory.PORT.key, "8629");
        params.put(JDBCDataStoreFactory.USER.key, "sysgis");
        params.put(JDBCDataStoreFactory.PASSWD.key, "dlalsvk");
        return params;
    }

    private DataStore getShapefileDataStore(String folder) throws IOException {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("url", DataUtilities.fileToURL(new File(folder)));
        params.put("charset", "x-windows-949");

        return DataStoreFinder.getDataStore(params);
    }
}
