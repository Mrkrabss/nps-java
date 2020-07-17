package com.gh.nps;

public class HelloMsg extends Message {
    String version;
    int clientType;
    String key;

    
    public HelloMsg(String userId) {
        super(1,userId);
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public int getClientType() {
        return clientType;
    }

    public void setClientType(int clientType) {
        this.clientType = clientType;
    }


    
    
}