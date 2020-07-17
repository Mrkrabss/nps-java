package com.gh.nps;

public class ConnectPortRequestMsg extends Message {

    public ConnectPortRequestMsg(String userId) {
        super(3, userId);
    }
    String destUserId;
    String ip;
    int port;
    int connectType;
    long connectId;

}