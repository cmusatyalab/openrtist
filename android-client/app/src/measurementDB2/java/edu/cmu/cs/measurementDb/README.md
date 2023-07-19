# OpenRTIST Measurement Database Connector
The measurmentDb package is an add-on measurment consumer for OpenRTIST. It specifically implements the ability to remotely connect to an [InfluxDb](https://www.influxdata.com/products/influxdb-overview/) database to store measurements for later analysis. By default, the InfluxDb should reside on the OpenRTIST server and be listening on port 30086. The default database name is openrtistdb.

*(You will need to install influxdb and create openrtistdb -- including assigning a retention policy. The default port for InfluxDb is typically 8086 so you may need to adjust to 30086.)*

---
DISCLAIMER: This package uses three android permissions that may cause some users privacy concerns. It is recommended to notify any users of these permissions prior to use. They are:
```
ACCESS_FINE_LOCATION
READ_PHONE_STATE
ACCESS_WIFI_STATE
```

The measurmentDb consumer is activated in MeasurementComm.java with this code:

```
MeasurementDbConsumer measurementDbconsumer = new MeasurementDbConsumer(
        gabrielClientActivity, endpoint);
Application application = gabrielClientActivity.getApplication();
if (tokenLimit.equals("None")) {
    this.measurementServerComm = MeasurementServerComm.createMeasurementServerComm(
            consumer, endpoint, port, application, onDisconnect, measurementDbconsumer);
} else {
    this.measurementServerComm = MeasurementServerComm.createMeasurementServerComm(
            consumer, endpoint, port, application, onDisconnect, measurementDbconsumer,
            Integer.parseInt(tokenLimit));
}
```

---

## Measurements
Currently, this package collects the following measurments:
### Application Measurements
|Measurement|Detail|
|-------|----|
|roundtriptime|Interval and overall* round trip time (RTT) in milliseconds between client to server to client|
|framerate|Interval and overall frames per second between client to server to client|

\* *For interval measures, the measurement is taken as average over a fixed number of frames; for overall measures, the measurement is taken as the average since the beginning of the session. A new session begins each time the client attaches to the server.*

### Environment Measurements
|Measurement|Detail|
|-------|----|
|location|GPS coordinates of the client at the interval boundary|
|signal|Signal strength for WIFI or LTE connections to the client taken at the interval boundary. Also includes CellID of the connected cell site|
|traceroute|Traceroute for path from client to server taken at the beginning of session. The measurement fields are integer IP addresses as described [here](http://www.aboutmyip.com/AboutMyXApp/IP2Integer.jsp).|

There is also a measurement called *allmeasure* which combines all of the above measures except traceroute.

### Measurement Tags
For each measurement, the following tags are attached to each point as it is written the database:

|Tag|Detail|
|-------|----|
|CARRIER|e.g., T-Mobile, Verizon|
|CONNECTTYPE|e.g., WiFi, LTE|
|COUNTRY|Current Country Code|
|MANUFACTURER|e.g, samsung|
|MODEL|e.g., SM-G950U|
|PHONETYPE|e.g., gsm|
|SERVERURL|IP and PORT for client connection to server|
|SESSIONID|YYYY-MM-DD-HH-MM-SS timestamp at the beginning of session|

Other measurement-specific tags are attached as needed.

