package com.genersoft.iot.vmp.common;

/**
 * Identifies each status after the invite message is sent.，
 * Stop sending invite after receiving ok moneycancel，
 * Send BYE after receiving 200ok to stopinvite
 */
public enum InviteSessionStatus {
    ready,
    ok,
}
