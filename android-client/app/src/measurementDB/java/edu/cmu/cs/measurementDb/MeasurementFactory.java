package edu.cmu.cs.measurementDb;

import org.influxdb.dto.Point;
import org.influxdb.dto.Point.Builder;

import edu.cmu.cs.measurementDb.NetUtilityHelper.TraceRouteResult;

import org.influxdb.dto.BatchPoints;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class MeasurementFactory {
    private static NetUtilityHelper nuh = new NetUtilityHelper();
    protected String sessionID = "0000-00-00-00-00-00";

    public MeasurementFactory() { // Constructor
    }
    public static class Measurement {
        String typestr;
        String manufacturer = "DEFAULT",model = "DEFAULT", serverURL = "DEFAULT", connect_type = "DEFAULT";
        String carrier_name = "DEFAULT", country_name = "DEFAULT", phone_type = "DEFAULT";
        String var0name = "DEFAULT", var1name = "DEFAULT";
        HashMap<String,String> tagMap = new HashMap<String,String>();
        HashMap<String,Float> fieldMap = new HashMap<String,Float>();
        double var0 = 0, var1 = 0, var2 = 0;

        public Measurement(String ptypestr, String pmanufacturer,String pmodel,
                           String pserverURL, String pconnect_type, String pphone_type, String pcarrier_name,
                           String pcountry_name, String pvar0name, String pvar1name) {
            typestr = ptypestr; manufacturer = pmanufacturer;model = pmodel;
            serverURL = pserverURL; connect_type = pconnect_type; phone_type = pphone_type;
            carrier_name = pcarrier_name; country_name = pcountry_name;
            var0name = pvar0name; var1name = pvar1name;
            setTagMap("MANUFACTURER",manufacturer);
            setTagMap("MODEL",model);
            setTagMap("SERVERURL",serverURL);
            setTagMap("CONNECTYPE",connect_type);
            setTagMap("PHONETYPE",phone_type);
            setTagMap("CARRIER",carrier_name);
            setTagMap("COUNTRY",country_name);
        }
        public void setVar0(double pvar0){ var0 = pvar0;setFieldMap(var0name, (float) pvar0);}
        public void setVar1(double pvar1){ var1 = pvar1;setFieldMap(var1name, (float) pvar1);}
        public void setTagMap(String key, String value) { tagMap.put(key, value); }
        public void setFieldMap(String key, float value) { fieldMap.put(key, value); }
        public void setTypeStr(String ptypeStr) { this.typestr = ptypeStr;}
    }
    // Application Specific Measurements
    public Point makeMeasurement(Measurement msre) {
        Builder pt = Point.measurement(msre.typestr)
                .time(System.currentTimeMillis(), TimeUnit.MILLISECONDS)
                .tag("SESSIONID",this.sessionID);
        Set<String> ks = msre.tagMap.keySet();
        if (ks.size() > 0) {
            for (String key : ks) {
                pt.tag(key, msre.tagMap.get(key));
            }
        }
        ks = msre.fieldMap.keySet();
        if (ks.size() > 0) {
            for (String key : ks) {
                pt.addField(key, msre.fieldMap.get(key));
            }
        }
        Point retpoint = pt.build();
        return retpoint;
    }

    public Point makeTraceRouteMeasurement(Measurement msre, TraceRouteResult trres) {
        Builder pt = Point.measurement(msre.typestr);
        Builder bldpt = Point.measurement(msre.typestr)
                .time(System.currentTimeMillis(), TimeUnit.MILLISECONDS)
                .tag("SESSIONID",this.sessionID)
                .tag("MANUFACTURER",msre.manufacturer)
                .tag("MODEL",msre.model)
                .tag("SERVERURL",msre.serverURL)
                .tag("CONNECTYPE",msre.connect_type)
                .tag("PHONETYPE",msre.phone_type)
                .tag("CARRIER",msre.carrier_name)
                .tag("COUNTRY",msre.country_name)
                .tag("ERROR",trres.errorcode);

        Point retpoint = parseTraceRoute(bldpt,trres);
        return retpoint;
    }
    private Point parseTraceRoute(Builder bldpt, TraceRouteResult trres) {
        String[] output = trres.output; String[] iplist = trres.iplist;float[] timings=trres.timings;
        float time = 0; String ip; long ipval;
//    	int maxhops = 30;
        for (int ii = 0; ii< iplist.length ;ii++) {
            if(ii < timings.length) {
                time = timings[ii];
            } else {
                time = 0;
            }
            ip = iplist[ii];
            ipval = IPAddressExtractor.IPString2IPValue(ip);
            if (output[ii].contains("END")) {
                bldpt.addField("FINALTIME",NetUtilityHelper.extractTime(output[ii]));
            }
            bldpt.tag(String.format("IP%02d",ii), ip);
            bldpt.addField(String.format("IPVAL%02d",ii), ipval);
            if (time != 0) {
                bldpt.addField(String.format("TIME%02d",ii), time);
            }
        }

        Point retpoint = bldpt.build();
        return retpoint;
    }

    /// Network Measurements ///

    public String[] Ping(String dest) {
        return nuh.Ping(dest, 1, 2);
    }
    public TraceRouteResult TraceRoute(String dest) {
        TraceRouteResult trres = nuh.TraceRoute(dest,30,false);
        return trres;
    }
    /// Utilities ///
    public void setSessionID() { this.sessionID = this.getHumanDateNow(); }
    public String getHumanDateNow() {
        String datstr = null;
        Date date = new Date(System.currentTimeMillis());
        datstr = getHumanDate(date);
        return datstr;
    }
    public String getHumanDate(Date date) {
        String datstr = null;
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
        datstr = formatter.format(date);
        return datstr;
    }
}