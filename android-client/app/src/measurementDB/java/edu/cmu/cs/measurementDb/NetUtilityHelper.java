package edu.cmu.cs.measurementDb;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;




public class NetUtilityHelper {

    //    private String osname;
    private boolean android;
    private String pingpath;

    NetUtilityHelper() {
//        this.osname = System.getProperty("os.name");
        this.android = "The Android Project".equals(System.getProperty("java.specification.vendor"));
        if(android) {
            pingpath = "/system/bin/ping";
        } else {
            pingpath = "/bin/ping";
        }
        IPAddressExtractor.isValidIPAddress("128.2.209.144");
    }
    public class TraceRouteResult {
        String[] output; String[] iplist;float[] timings;float lsttime;
        TraceRouteResult(ArrayList<String> outbufarg, ArrayList<String> ipbufarg) {
            output = outbufarg.toArray(new String[outbufarg.size()]);
            iplist = ipbufarg.toArray(new String[ipbufarg.size()]);
            timings = new float[outbufarg.size()];
            lsttime = extractTime(this.output[output.length-1]);
            this.setLastTime();
        }
        TraceRouteResult(ArrayList<String> outbufarg, ArrayList<String> ipbufarg,
                         ArrayList<Float> timingsarg) {
            output = outbufarg.toArray(new String[outbufarg.size()]);
            iplist = ipbufarg.toArray(new String[ipbufarg.size()]);

            timings = new float[timingsarg.size()];
            int ii = 0;
            for (Float f : timingsarg) {
                timings[ii++] = (float) (f != null ? f : -1.0);
            }
            this.setLastTime();
        }
        private void setLastTime() {
            this.lsttime = extractTime(this.output[output.length-1]);
        }
        public void print() {
            int ii = 0;
            String ip; float timng;
            for (String outp: this.output) {
                if (ii < iplist.length) {
                    outp = String.format("%s IP: %s",outp,iplist[ii]);
                }
                if (ii < timings.length) {
                    outp = String.format("%s TIME: %f",outp,timings[ii]);
                }
                System.out.println(outp);
                ii++;
            }
        }
    }
    public TraceRouteResult TraceRoute(String dest, int maxhops, boolean...varargs) {
        ArrayList<String> strlst = new ArrayList<String>();
        ArrayList<String> interdestlst = new ArrayList<String>();
        boolean timing = false;
        for (int ii = 0 ; ii < varargs.length;ii++) {
            if (ii == 0) { timing = varargs[ii]; }
        }
        String strout;
        for (int ii = 3; ii < maxhops;ii++) {
            String[] strarr = this.Ping(dest, 1, ii);
            if (strarr[1].contains("Time to live exceeded")) {
                strout = String.format("HOP: %s",strarr[1].replace(": icmp_seq=1 Time to live exceeded",""));
                System.out.println(String.format(strout));
                strlst.add(strout);
                String interdest = IPAddressExtractor.extractIPAddress(strarr[1]);
                interdestlst.add(interdest);
            } else if (strarr[1].contains(dest)) {
                strout = String.format("END: %s ",strarr[1]);
                System.out.println(strout);
                strlst.add(strout);
                interdestlst.add(dest);
                break;
            }
            continue;
        }
        TraceRouteResult retres = null;
        if(timing) {
            ArrayList<Float> timings = runTimings(interdestlst);
            retres = new TraceRouteResult(strlst,interdestlst,timings);
        } else {
            retres = new TraceRouteResult(strlst,interdestlst);
        }
        retres.print();
        return retres;

    }

    private ArrayList<Float> runTimings(ArrayList<String> destlst) {
        ArrayList<Float> retfloats = new ArrayList<Float>();
        for (String dest : destlst) {
            String[] pingres = this.Ping(dest,1,30,1);
            for (String str: pingres) {
                if(str.contains(dest) & str.contains("bytes from") ) {
                    retfloats.add(extractTime(str));
//        			System.out.println(str);
                } else if (str.contains("0 received")){
                    retfloats.add((float) -1);
                }
            }
        }
        return retfloats;
    }
    public static float extractTime(String timestr) {
        Pattern pat = Pattern.compile("time=(.*?) ms");
        Matcher m = pat.matcher(timestr);
        float rettime = -1;
        while (m.find()) {
            rettime = Math.round(Float.parseFloat(m.group(1))*1000)/1000;
        }
        return rettime;
    }
    public String[] Ping(String dest, int...varargs) {
        int count = -1;int ttl = -1;int wait = -1;
        for (int ii = 0; ii < varargs.length;ii++) {
            if (ii == 0) { count = varargs[ii]; } else
            if (ii == 1) { ttl = varargs[ii]; } else
            if (ii == 2) { wait = varargs[ii]; }
        }
        String optstr = "";
        if(count > 0) {
            optstr += String.format(" -c %d", count);
        }
        if(ttl > 0) {
            optstr += String.format(" -t %d", ttl);
        }
        if(wait > 0) {
            optstr += String.format(" -w %d", wait);
        }
        try {
            String pingcmd = String.format("%s %s %s\n",this.pingpath,optstr,dest);
            Process process = Runtime.getRuntime().exec(pingcmd);
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            int read;
            char[] buffer = new char[4096];
            StringBuffer output = new StringBuffer();
            while ((read = reader.read(buffer)) > 0) {
                output.append(buffer, 0, read);
            }
            reader.close();

            // Waits for the command to finish.
            process.waitFor();
            String retstrarr[]  = output.toString().split("\\n");
            return retstrarr;
        } catch(Exception e){
            e.printStackTrace();
        }

        return null;
    }
}