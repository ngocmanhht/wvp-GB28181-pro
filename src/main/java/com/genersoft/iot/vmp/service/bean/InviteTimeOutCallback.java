package com.genersoft.iot.vmp.service.bean;

public interface InviteTimeOutCallback {

    void run(int code, String msg); // code: 0 siptimeout, 1 traffic collection timeout
}
