package com.genersoft.iot.vmp.jt1078.bean;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * JT terminal type
 */
@Setter
@Getter
@Schema(description = "JTTerminal parameter settings")
public class JTDeviceType {

    /**
     * Applicable to passenger vehicles
     */
    private boolean passengerVehicles;

    /**
     * Suitable for dangerous goods vehicles
     */
    private boolean dangerousGoodsVehicles;

    /**
     * Ordinary freight vehicles
     */
    private boolean freightVehicles;

    /**
     * rental vehicle
     */
    private boolean rentalVehicles;

    /**
     * Support hard disk recording
     */
    private boolean hardDiskRecording;

    /**
     * false：All-in-one machine, true: split machine
     */
    private boolean splittingMachine;

    /**
     * Suitable for trailers
     */
    private boolean trailer;

    public static JTDeviceType getInstance(int content) {
        boolean passengerVehicles = (content & 1) == 1;
        boolean dangerousGoodsVehicles = (content >>> 1 & 1) == 1;
        boolean freightVehicles = (content >>> 2 & 1) == 1;
        boolean rentalVehicles = (content >>> 3 & 1) == 1;
        boolean hardDiskRecording = (content >>> 6 & 1) == 1;
        boolean splittingMachine = (content >>> 7 & 1) == 1;
        boolean trailer = (content >>> 8 & 1) == 1;
        return new JTDeviceType(passengerVehicles, dangerousGoodsVehicles, freightVehicles, rentalVehicles, hardDiskRecording, splittingMachine, trailer);
    }

    public JTDeviceType(boolean passengerVehicles, boolean dangerousGoodsVehicles, boolean freightVehicles, boolean rentalVehicles, boolean hardDiskRecording, boolean splittingMachine, boolean trailer) {
        this.passengerVehicles = passengerVehicles;
        this.dangerousGoodsVehicles = dangerousGoodsVehicles;
        this.freightVehicles = freightVehicles;
        this.rentalVehicles = rentalVehicles;
        this.hardDiskRecording = hardDiskRecording;
        this.splittingMachine = splittingMachine;
        this.trailer = trailer;
    }

    @Override
    public String toString() {
        return "JTDeviceType{" +
                "passengerVehicles=" + passengerVehicles +
                ", dangerousGoodsVehicles=" + dangerousGoodsVehicles +
                ", freightVehicles=" + freightVehicles +
                ", rentalVehicles=" + rentalVehicles +
                ", hardDiskRecording=" + hardDiskRecording +
                ", splittingMachine=" + splittingMachine +
                ", trailer=" + trailer +
                '}';
    }
}
