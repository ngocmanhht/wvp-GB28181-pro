package com.genersoft.iot.vmp.gb28181.bean;

/**
 * Voice broadcast status
 * @author lin
 */
public enum AudioBroadcastCatchStatus {

    // Send a voice broadcast message and wait for the other party to reply with a voice broadcast
    Ready,
    // Receive reply and wait for invite message
    WaiteInvite,
    // Receive invite message
    Ok,
}
