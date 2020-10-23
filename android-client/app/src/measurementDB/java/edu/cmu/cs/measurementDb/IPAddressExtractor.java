package edu.cmu.cs.measurementDb;

//Java program to validate an IP address
//using Regular Expression or ReGex

import java.util.regex.*;

class IPAddressExtractor {


    // Regex for digit from 0 to 255.
    // Regex for a digit from 0 to 255 and
    // followed by a dot, repeat 4 times.
    // this is the regex to validate an IP address.
    private static String regexis = "^(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$";
    private static String regexmtch = "\\b(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\b";


    //	private static String zeroTo255 = "(\\d{1,2}|(0|1)\\" + "d{2}|2[0-4]\\d|25[0-5])";
//	private static String regex = zeroTo255 + "\\." + zeroTo255 + "\\." + zeroTo255 + "\\." + zeroTo255;
    // Compile the ReGex
    private static Pattern pis = Pattern.compile(regexis);
    private static Pattern pmtch = Pattern.compile(regexmtch);

    // Function to validate the IPs address.
    public static boolean isValidIPAddress(String ip) {
        // If the IP address is empty
        // return false
        if (ip == null) {
            return false;
        }

        // Pattern class contains matcher() method
        // to find matching between given IP address
        // and regular expression.
        Matcher m = pis.matcher(ip);

        // Return if the IP address
        // matched the ReGex
        boolean rettest = m.matches();
        return rettest;
    }
    public static String extractIPAddress(String ip) {

        // Compile the ReGex

        // If the IP address is empty
        // return false
        if (ip == null) {
            return null;
        }

        // Pattern class contains matcher() method
        // to find matching between given IP address
        // and regular expression.
        Matcher m = pmtch.matcher(ip);

        // Return the IP address from ReGex
        if (m.find()) {
            int grpcnt = m.groupCount();
            for (int ii=0;ii<grpcnt;ii++) { System.out.println(m.group(ii));}
            String retstr = m.group(0);
//			System.out.println(retstr);
            return retstr;
        }

        return null;
    }
    public static long IPString2IPValue(String ipstr) {
        String tup[] = ipstr.split("\\.");
        long retint = Long.parseLong(tup[0]) * 16777216
                + Long.parseLong(tup[1]) * 65536
                + Long.parseLong(tup[2]) * 256
                + Long.parseLong(tup[3]);
        return retint;
    }
    public static String IPValue2IPString(long ipno) {
        String retstr = String.format("%d.%d.%d.%d",
                (ipno / 16777216 ) % 256,
                ( ipno / 65536    ) % 256,
                ( ipno / 256      ) % 256,
                ( ipno            ) % 256);
        return retstr;
    }
}
