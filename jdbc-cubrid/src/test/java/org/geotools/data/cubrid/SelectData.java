package org.geotools.data.cubrid;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

import org.geotools.data.DataSourceException;
import org.geotools.data.DataStore;
import org.geotools.jdbc.JDBCDataStoreFactory;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.io.ByteArrayInStream;
import com.vividsolutions.jts.io.ByteOrderValues;
import com.vividsolutions.jts.io.WKBReader;
import com.vividsolutions.jts.io.WKBWriter;
import com.vividsolutions.jts.io.WKTReader;

public class SelectData {

    public static void main(String[] args) throws Exception {
   
        byte[] wkbBytes = SpCubrid.ST_POINT(12345, 67890);

        if (wkbBytes == null) // DJB: null value from database --> null geometry (the same behavior
            // as WKT). NOTE: sending back a GEOMETRYCOLLECTION(EMPTY) is also a
            // possibility, but this is not the same as NULL
            return;
        try {
            ByteArrayInStream inStream = new ByteArrayInStream(new byte[0]);     
            inStream.setBytes(wkbBytes);

            com.vividsolutions.jts.io.WKBReader wkbr = new WKBReader(new GeometryFactory());
            Geometry point =  wkbr.read(inStream);
            System.out.println("Geometry ==> " + point);
        } catch (Exception e) {
            throw new DataSourceException("An exception occurred while parsing WKB data", e);
        }
        
        Map<String, Object> params = new HashMap<String, Object>();
        params.put(JDBCDataStoreFactory.DBTYPE.key, "cubrid");
        params.put(JDBCDataStoreFactory.HOST.key, "localhost");
        params.put(JDBCDataStoreFactory.DATABASE.key, "demodb");
        params.put(JDBCDataStoreFactory.PORT.key, "33000");
        params.put(JDBCDataStoreFactory.USER.key, "dba");
        params.put(JDBCDataStoreFactory.PASSWD.key, "dlalsvk");
        
        CubridNGDataStoreFactory factory = new CubridNGDataStoreFactory();
        DataStore dataStore = factory.createDataStore(params);
        
        String[] names = dataStore.getTypeNames();
        for (String name : names) {
            System.out.println("name ==> " + name);
        }
                
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;

        WKBReader reader = new WKBReader();
        WKBWriter writer = new WKBWriter(2, ByteOrderValues.LITTLE_ENDIAN);
        
        WKTReader re = new WKTReader();

        // writer.write(geom);
        // WKBReader.hexToBytes(hex);

        try {
            Class.forName("cubrid.jdbc.driver.CUBRIDDriver");
            conn = DriverManager.getConnection("jdbc:cubrid:localhost:33000:demodb:::", "dba", "dlalsvk");
            // conn = DriverManager.getConnection("jdbc:cubrid:localhost:8001:demodb:::", "admin", "dlalsvk");
            String sql = "select name, players, ST_POINT(code, players) as geom  from event";
            stmt = conn.createStatement();
            rs = stmt.executeQuery(sql);
            while (rs.next()) {
                String name = rs.getString("name");
                String players = rs.getString("players");
                Object bytes = rs.getObject("geom");     // rs.getBytes("geom");
                
                System.out.println("name ==> " + name);
                System.out.println("Number of players==> " + players);
                System.out.println("geom==> " + bytes);
                                
                System.out.println("\n=========================================\n");

            }
            rs.close();
            stmt.close();
            conn.close();
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (conn != null)
                conn.close();
        }
    }
}
