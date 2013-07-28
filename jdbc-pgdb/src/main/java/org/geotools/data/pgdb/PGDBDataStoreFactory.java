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

import java.awt.RenderingHints.Key;
import java.io.IOException;
import java.io.Serializable;
import java.net.URL;
import java.util.Collections;
import java.util.Map;
import java.util.logging.Logger;

import org.geotools.data.AbstractDataStoreFactory;
import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFactorySpi;
import org.geotools.data.DataUtilities;
import org.geotools.data.Parameter;
import org.geotools.util.KVP;
import org.geotools.util.SimpleInternationalString;
import org.geotools.util.logging.Logging;

/**
 * ESRI Personal Geodatabase DataStoreFactory
 * 
 * @author MapPlus, mapplus@gmail.com, http://onspatial.com
 * @since 2012-10-30
 * @see
 * 
 */
public class PGDBDataStoreFactory extends AbstractDataStoreFactory implements DataStoreFactorySpi {
    protected static final Logger LOGGER = Logging.getLogger(PGDBDataStoreFactory.class);

    static final String FILE_TYPE = "mdb";

    public static final Param PARAM_FILE = new Param("url", URL.class,
            "url to a ESRI Personal Geodatabase(.mdb) file", true, null, new KVP(Param.EXT,
                    FILE_TYPE));

    /** parameter for database user */
    public static final Param PARAM_USER = new Param("user", String.class, "User", false, null);

    /** parameter for database password */
    public static final Param PARAM_PASSWD = new Param("passwd", String.class,
            new SimpleInternationalString("password used to login"), false, null,
            Collections.singletonMap(Parameter.IS_PASSWORD, Boolean.TRUE));

    public String getDisplayName() {
        return "ESRI Personal Geodatabase (PGDB)";
    }

    public String getDescription() {
        return "ESRI Personal Geodatabase (*.mdb)";
    }

    public Param[] getParametersInfo() {
        return new Param[] { PARAM_FILE, PARAM_USER, PARAM_PASSWD };
    }

    public boolean isAvailable() {
        return true;
    }

    @SuppressWarnings("unchecked")
    public Map<Key, ?> getImplementationHints() {
        return Collections.EMPTY_MAP;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public boolean canProcess(Map params) {
        boolean result = false;
        if (params.containsKey(PARAM_FILE.key)) {
            try {
                URL url = (URL) PARAM_FILE.lookUp(params);
                result = url.getFile().toLowerCase().endsWith(".mdb");
            } catch (IOException ioe) {
                /* return false on any exception */
            }
        }
        return result;
    }

    public DataStore createDataStore(Map<String, Serializable> params) throws IOException {
        URL url = (URL) PARAM_FILE.lookUp(params);
        String user = (String) PARAM_USER.lookUp(params);
        String password = (String) PARAM_PASSWD.lookUp(params);

        // it is immutable and cannot be modified
        final DataStore dataStore = new PGDBDataStore(DataUtilities.urlToFile(url), user, password);
        return dataStore;
    }

    public DataStore createNewDataStore(Map<String, Serializable> params) throws IOException {
        return null;
    }

}