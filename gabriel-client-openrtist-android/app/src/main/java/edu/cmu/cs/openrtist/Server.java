package edu.cmu.cs.openrtist;

public class Server {
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    String name;
    String endpoint;

    public Server(String name, String endpoint ) {
        this.name = name;
        this.endpoint = endpoint;
    }
}