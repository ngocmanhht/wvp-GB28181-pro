package com.genersoft.iot.vmp.jt1078.bean;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * text message sign
 */
@Data
@Schema(description = "text message sign")
public class JTTextSign {

    @Schema(description = "1Emergency, 2 services, 3 notifications")
    private int type;

    @Schema(description = "1Terminal monitor display")
    private boolean terminalDisplay;

    @Schema(description = "1Advertising screen display")
    private boolean adScreen;

    @Schema(description = "1Terminal TTS reading")
    private boolean tts;

    @Schema(description = "false: Center navigation information true CAN fault code information")
    private boolean source;

    public byte encode(){
        byte byteSign = 0;
        byteSign |= (byte) type;
        if (terminalDisplay) {
            byteSign |= (0x1 << 2);
        }
        if (tts) {
            byteSign |= (0x1 << 3);
        }
        if (adScreen) {
            byteSign |= (0x1 << 4);
        }
        if (source) {
            byteSign |= (0x1 << 5);
        }
        return byteSign;
    }
}
