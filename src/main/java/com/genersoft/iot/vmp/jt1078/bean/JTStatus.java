package com.genersoft.iot.vmp.jt1078.bean;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "status information")
public class JTStatus {

    @Schema(description = "false:ACCOff; true: ACC on")
    private boolean acc;

    @Schema(description = "false:Not positioned; true: positioned")
    private boolean positioning;

    @Schema(description = "false:North latitude; true: south latitude")
    private boolean southLatitude;

    @Schema(description = "false:East longitude; true: West longitude")
    private boolean wesLongitude;

    @Schema(description = "false:Operation status; true: out of service status")
    private boolean outage;

    @Schema(description = "false:The latitude and longitude have not been encrypted by the security plug-in; true: the latitude and longitude have been encrypted by the security plug-in.")
    private boolean positionEncryption;


    @Schema(description = "true: Forward collision warning collected by emergency braking system")
    private boolean warningFrontCrash;

    @Schema(description = "true: Lane departure warning")
    private boolean warningShifting;

    @Schema(description = "00:Empty; 01: Half loaded; 10: Reserved; 11: Fully loaded. It can represent the empty state of passenger cars, the empty and full state of heavy vehicles and trucks. This state can be obtained by manual input or sensors.")
    private int load;

    @Schema(description = "false:The vehicle oil line is normal; true: the vehicle oil line is disconnected")
    private boolean oilWayBreak;

    @Schema(description = "false:The vehicle circuit is normal; true: the vehicle circuit is disconnected")
    private boolean circuitBreak;

    @Schema(description = "false:Door unlock; true: door locked")
    private boolean doorLocking;

    @Schema(description = "false:Door 1 is closed; true: Door 1 is open(front door)")
    private boolean door1Open;

    @Schema(description = "false:Door 2 is closed; true: Door 2 is open(middle gate)")
    private boolean door2Open;

    @Schema(description = "false:Door 3 is closed; true: Door 3 is open(back door)")
    private boolean door3Open;

    @Schema(description = "false:Door 4 is closed; true: Door 4 is open(driver's seat door)")
    private boolean door4Open;

    @Schema(description = "false:Door 5 is closed; true: Door 5 is open(Customize)")
    private boolean door5Open;

    @Schema(description = "false:GPS satellites are not used for positioning; true: GPS satellites are used for positioning")
    private boolean gps;

    @Schema(description = "false:Beidou satellites are not used for positioning; true: Beidou satellites are used for positioning")
    private boolean beidou;

    @Schema(description = "false:GLONASS satellites are not used for positioning; true: GLONASS satellites are used for positioning")
    private boolean glonass;

    @Schema(description = "false:GaLiLeo satellites are not used for positioning; true: GaLiLeo satellites are used for positioning")
    private boolean gaLiLeo;

    @Schema(description = "false:The vehicle is in a stopped state; true: the vehicle is in a driving state")
    private boolean driving;

    public JTStatus() {
    }

    public JTStatus(long statusInt) {
        if (statusInt == 0) {
            return;
        }
        this.acc = (statusInt & 1) == 1;
        this.positioning = (statusInt >>> 1 & 1) == 1;
        this.southLatitude = (statusInt >>> 2 & 1) == 1;
        this.wesLongitude = (statusInt >>> 3 & 1) == 1;
        this.outage = (statusInt >>> 4 & 1) == 1;
        this.positionEncryption = (statusInt >>> 5 & 1) == 1;
        this.warningFrontCrash = (statusInt >>> 6 & 1) == 1;
        this.warningShifting = (statusInt >>> 7 & 1) == 1;
        this.load = (int)(statusInt >>> 8 & 3);
        this.oilWayBreak = (statusInt >>> 10 & 1) == 1;
        this.circuitBreak = (statusInt >>> 11 & 1) == 1;
        this.doorLocking = (statusInt >>> 12 & 1) == 1;
        this.door1Open = (statusInt >>> 13 & 1) == 1;
        this.door2Open = (statusInt >>> 14 & 1) == 1;
        this.door3Open = (statusInt >>> 15 & 1) == 1;
        this.door4Open = (statusInt >>> 16 & 1) == 1;
        this.door5Open = (statusInt >>> 17 & 1) == 1;
        this.gps = (statusInt >>> 18 & 1) == 1;
        this.beidou = (statusInt >>> 19 & 1) == 1;
        this.glonass = (statusInt >>> 20 & 1) == 1;
        this.gaLiLeo = (statusInt >>> 21 & 1) == 1;
        this.driving = (statusInt >>> 22 & 1) == 1;
    }

    @Override
    public String toString() {
        return "status bit：" +
                "\n      accStatus：" + (acc?"open":"close") +
                "\n      Positioning status：" + (positioning?"Positioning":"Not located") +
                "\n      North and south latitude：" + (southLatitude?"Southern latitude":"Northern latitude") +
                "\n      Eastern and Western Classics：" + (wesLongitude?"west longitude":"east longitude") +
                "\n      Operation status：" + (outage?"Out of service":"Operation") +
                "\n      Longitude and latitude confidential：" + (positionEncryption?"Encryption":"Not encrypted") +
                "\n      forward collision warning：" + (warningFrontCrash?"Forward collision warning collected by emergency braking system":"None") +
                "\n      Lane departure warning：" + (warningShifting?"Lane departure warning":"None") +
                "\n      empty/half/Fully loaded：" + (load == 0?"Empty car":(load == 1?"Half a year":(load == 3?"Fully loaded":"undefined state"))) +
                "\n      Vehicle oil line status：" + (oilWayBreak?"Vehicle oil line disconnected":"Vehicle oil line is normal") +
                "\n      vehicle circuit status：" + (circuitBreak?"Vehicle circuit disconnected":"Vehicle circuit is normal") +
                "\n      door lock status：" + (doorLocking?"car door lock":"door unlock") +
                "\n      door1(front door)Status：" + (door1Open?"open":"close") +
                "\n      door2(middle gate)Status：" + (door2Open?"open":"close") +
                "\n      door3(back door)Status：" + (door3Open?"open":"close") +
                "\n      door4(driver's seat door)Status：" + (door4Open?"open":"close") +
                "\n      door5(Customize)Status：" + (door5Open?"open":"close") +
                "\n      GPSSatellite positioning status： " + (gps?"Use":"Not used") +
                "\n      Beidou satellite positioning status： " + (beidou?"Use":"Not used") +
                "\n      GLONASSSatellite positioning status： " + (glonass?"Use":"Not used") +
                "\n      GaLiLeoSatellite positioning status： " + (gaLiLeo?"Use":"Not used") +
                "\n      GaLiLeoSatellite positioning status： " + (gaLiLeo?"Use":"Not used") +
                "\n      Vehicle driving status： " + (driving?"vehicle driving":"vehicle stopped") +
                "\n       ";
    }
}
