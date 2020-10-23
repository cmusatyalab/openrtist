package edu.cmu.cs.measurementDb;

import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;

import java.util.List;

import java.util.concurrent.TimeUnit;

import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;
import org.influxdb.dto.Point;
import org.influxdb.dto.BatchPoints;
import org.influxdb.dto.Pong;
import org.influxdb.dto.Query;
import org.influxdb.dto.QueryResult;
import org.influxdb.dto.QueryResult.Result;

public class InfluxDBHelper {
    private static final String TAG = "InfluxDBHelper";
    private static final int ADVPort = 30086;
    private static final int DEFPort = 8086;

    private static final String INFLUXDBHOST = "127.0.0.1";
    private static final int INFLUXDBPORT = ADVPort;

    private String hostname;
    private int port;
    private String dbName = "openrtistdb";
    String rpName = null;
    private InfluxDB influxDB = null;
    private MeasurementFactory mfact = new MeasurementFactory();

    public InfluxDBHelper() { // Constructor
        hostname = INFLUXDBHOST;
        port = INFLUXDBPORT;
        this.configure();
    }
    public InfluxDBHelper(String hostnamearg, int portarg) { // Constructor
        hostname = hostnamearg;
        port = portarg;
        this.configure();
    }
    private void configure() {
        this.connect();
        new InfluxDBTask().execute("ping");
        new InfluxDBTask().execute("setloglevel");
        new InfluxDBTask().execute("listdb");
        new InfluxDBTask().execute("setdatabase", dbName);
        new InfluxDBTask().execute("createretention", dbName);
//        new InfluxDBTask().execute("enablebatch"); // TODO Broken
        this.runTests();
    }
    private void connect() {
        try {
            influxDB = InfluxDBFactory.connect("http://" + hostname + ":" + port);
        } catch (Exception e) {
            System.err.print(e);
        }
    }
    public void close() {
        influxDB.close();
    }
    public void runTests() {
        new InfluxDBTask().execute("sendsamplepoint");
    }
    public void AsyncWritePoint(MeasurementFactory.Measurement msre) {
        new AsyncInfluxDBWritePoint().execute(msre);
    }

    private void runOnPostExecute(String [] params, String result) {
        Log.i(TAG, result);
    }
    private void runOnPostExecute(MeasurementFactory.Measurement msre, String result) {
        Log.i(TAG, result);
    }

    private class InfluxDBTask extends AsyncTask<String, Void, String> {
        String outp = null;
        String [] sparams = null;

        @Override
        protected String doInBackground(String... params) {
            sparams = params;
            return runTask(params);
        }
        private String runTask(String... params) {
            String ttask = params[0];
            outp = ttask;
            switch (ttask) {
                case "setloglevel":
                    InfluxDBHelper.this.setLogLevel();
                    break;
                case "ping":
                    outp = InfluxDBHelper.this.pingDB();
                    break;
                case "listdb":
                    InfluxDBHelper.this.listDB();
                    break;
                case "setdatabase":
                    outp = String.format("%s %s",ttask,params[1]);
                    InfluxDBHelper.this.setDatabase(params[1]);
                case "createretention":
                    outp = String.format("%s %s",ttask,params[1]);
                    InfluxDBHelper.this.createRetention(params[1]);
                    break;
//                case "sendsamplepoint":
//                    InfluxDBHelper.this.sendSamplePoint();
//                    break;
                default:
                    break;
            }
            return outp;
        }
        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            runOnPostExecute(sparams, result);
        }
    }

    private class AsyncInfluxDBWritePoint extends AsyncTask<MeasurementFactory.Measurement, Void, String> {
        String outp = null;
        MeasurementFactory.Measurement measure;
        @Override
        protected String doInBackground(MeasurementFactory.Measurement... pmeasure) {
            measure = pmeasure[0];
            return runTask(measure);
        }
        private String runTask(MeasurementFactory.Measurement measure) {
            outp = measure.typestr;
            InfluxDBHelper.this.addPoint(measure);
            return outp;
        }
        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            runOnPostExecute(measure, result);
        }
    }

    // MEASUREMENT FUNCTIONS
    //      Application Specfic
    private void addPoint(MeasurementFactory.Measurement msre) {
        Point lpoint;
        switch (msre.typestr) {
            case "traceroute":
                String lurl = msre.serverURL.replaceAll("ws://","").replaceAll(":.*","");
                lpoint = mfact.makeTraceRouteMeasurement(msre, mfact.TraceRoute(lurl));
                this.writePoint(lpoint);
                break;
            default:
                lpoint = mfact.makeMeasurement(msre);
                this.writePoint(lpoint);
        }
    }

    public void writePoint(Point cpoint) { // Write a single point
        influxDB.disableBatch();
        this.setDatabase(dbName);
        this.setRetention(dbName);
        influxDB.write(cpoint);
        this.enableBatch();
    }

    public void dropMeasurementRecords(String measurementname) {
        influxDB.query(new Query(String.format("DROP MEASUREMENT %s",measurementname), dbName));
    }
    public void showMeasurementPoints(String measurementname) {
        try {
            QueryResult response =  influxDB.query(new Query(String.format("SELECT * FROM %s",measurementname), dbName));
            System.out.println(String.format("SHOW POINTS IN %s",measurementname));
            this.showResults(response,measurementname);
        } catch (Exception e) {
            System.err.print(e);
        }
        System.out.println("END SHOW POINTS");
    }
    public void showMeasurements() {
        try {
            QueryResult response =  influxDB.query(new Query("SHOW MEASUREMENTS", dbName));
            System.out.println("SHOW MEASUREMENTS");
            this.showResults(response,"MEASUREMENT");
        } catch (Exception e) {
            System.err.print(e);
        }
        System.out.println("END SHOW MEASUREMENTS");
    }
    private void showResults(QueryResult response,String resname) {
        List<Result> results = response.getResults();
        List<List<Object>> reslst = results.get(0).getSeries().get(0).getValues();
        for (Object resv : reslst) {
            Log.i(TAG, String.format("%s: %s\n",resname,resv));
        }
    }

    // DATABASE MANAGEMENT FUNCTIONS
    public void createDB(String ndbname) {
        String qstr = String.format("CREATE DATABASE %s",ndbname);
        this.influxDB.query(new Query(qstr, dbName));
    }
    public void deleteDB(String ndbname) {
        String qstr = String.format("DROP DATABASE %s",ndbname);
        this.influxDB.query(new Query(qstr, dbName));
    }
    @SuppressWarnings("deprecation")
    private void createRetention(String fdbname) {
        rpName = "DefaultRetentionPolicy";
        influxDB.createRetentionPolicy(rpName, fdbname, "30d", 1, true);
    }

    private void setRetention(String fdbname) { influxDB.setRetentionPolicy(rpName); }
    private void setLogLevel() { influxDB.setLogLevel(InfluxDB.LogLevel.BASIC);}
    private void enableBatch() { influxDB.enableBatch(100, 200, TimeUnit.MILLISECONDS); }
    private void setDatabase(String fdbname) { influxDB.setDatabase(fdbname); }
    public void setSessionID() { mfact.setSessionID(); }
    // INFO FUNCTIONS
    public String getRetentionPolicy() { return rpName; }
    public String getDBName() { return dbName; }
    public MeasurementFactory getMeasurmentFactory() { return mfact; }

    public String pingDB() {
        String retstr = null;
        try {
            Pong response = this.influxDB.ping();
            if (response.getVersion().equalsIgnoreCase("unknown")) {
                retstr="Error pinging server.";
                System.err.print(retstr);
                return retstr;
            } else {
                retstr = String.format("InfluxDB server ping response %d ms\n",response.getResponseTime());
                System.out.printf(retstr);
            }
        } catch  (Exception e) {
            System.err.print(e);
            retstr="Error pinging server.";
        }
        return retstr;
    }
    public void listDB() {
        try {
            QueryResult response = influxDB.query(new Query("SHOW DATABASES",dbName));
            Log.i(TAG,"SHOW DATABASES");
            this.showResults(response, "DATABASE");
        } catch (Exception e) {
            System.err.print(e);
        }
        System.out.println("END SHOW DATABASES");
    }
    public static String getInfluxDBHost() { return INFLUXDBHOST; }
    public static int getInfluxDBPort() { return INFLUXDBPORT; }
    // TESTS
//    private void sendSamplePoint() {
//        String typestr = "location", manufacturer = "UNK",model = "UNK", serverURL = "UNK", connect_type = "UNK";
//        MeasurementFactory.Measurement tmeasure =
//                new MeasurementFactory.Measurement(typestr, manufacturer,model, serverURL, connect_type);
//        tmeasure.setVar0(10.0);
//        tmeasure.setVar1(10.0);
//        new AsyncInfluxDBWritePoint().execute(tmeasure);
//    }
    @SuppressWarnings("unused")
    private void _test_WritePoint() {
        influxDB.setRetentionPolicy(rpName);
        influxDB.write(Point.measurement("cpu")
                .time(System.currentTimeMillis(), TimeUnit.MILLISECONDS)
                .addField("idle", 90L)
                .addField("user", 9L)
                .addField("system", 1L)
                .build());
    }
    @SuppressWarnings("unused")
    private void _test_CreateMeasurement() {
        Point point1 = Point.measurement("cpu")
                .time(System.currentTimeMillis(), TimeUnit.MILLISECONDS)
                .addField("idle", 90L)
                .addField("user", 9L)
                .addField("system", 1L)
                .build();
        Point point2 = Point.measurement("disk")
                .time(System.currentTimeMillis(), TimeUnit.MILLISECONDS)
                .addField("used", 80L)
                .addField("free", 1L)
                .build();
        influxDB.write(dbName, rpName, point1);
        influxDB.write(dbName, rpName, point2);
    }
    @SuppressWarnings("unused")
    private void _test_GetMeasurements() {
        Query query = new Query("SELECT * from cpu",dbName);
        QueryResult response = influxDB.query(query);
        List<Result> results = response.getResults();
        List<List<Object>> vallst = results.get(0).getSeries().get(0).getValues();
        List<String> collst = results.get(0).getSeries().get(0).getColumns();
        response = influxDB.query(query);
        results = response.getResults();

    }
    @SuppressWarnings("unused")
    private void _test_CreateDelete() {
        this.createDB("testdb");
        this.setRetention("testdb");
        this.listDB();
        this.deleteDB("testdb");
        this.listDB();
    }

    @SuppressWarnings("unused")
    private void _test_WriteBatchPoint() {
        BatchPoints batchPoints = BatchPoints
                .database(dbName)
                .retentionPolicy(rpName)
                .build();

        Point point1 = Point.measurement("memory")
                .time(System.currentTimeMillis(), TimeUnit.MILLISECONDS)
                .addField("name", "server1")
                .addField("free", 4743656L)
                .addField("used", 1015096L)
                .addField("buffer", 1010467L)
                .build();

        Point point2 = Point.measurement("memory")
                .time(System.currentTimeMillis() - 100, TimeUnit.MILLISECONDS)
                .addField("name", "server1")
                .addField("free", 4743696L)
                .addField("used", 1016096L)
                .addField("buffer", 1008467L)
                .build();

        batchPoints.point(point1);
        batchPoints.point(point2);
        influxDB.write(batchPoints);
    }
}