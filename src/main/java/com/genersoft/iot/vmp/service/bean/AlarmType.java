package com.genersoft.iot.vmp.service.bean;

public enum AlarmType {

    // Video loss alarm
    VideoLoss("Video loss alarm"),
    // Equipment anti-tamper alarm
    DeviceTamper("Equipment anti-tamper alarm"),
    // Storage device disk full alarm
    StorageFull("Storage device disk full alarm"),
    // Equipment high temperature alarm
    DeviceHighTemperature("Equipment high temperature alarm"),
    // Equipment low temperature alarm
    DeviceLowTemperature("Equipment low temperature alarm"),
    // Manual video alarm
    ManualVideo("Manual video alarm"),
    // Moving target detection alarm
    MotionDetection("Moving target detection alarm"),
    // Remaining object detection alarm
    LeftObjectDetection("Remaining object detection alarm"),
    // Object removal detection alarm
    ObjectRemovalDetection("Object removal detection alarm"),
    // Tripwire detection alarm
    TripwireDetection("Tripwire detection alarm"),
    // Intrusion detection alarm
    IntrusionDetection("Intrusion detection alarm"),
    // Motion detection alarm
    MobileDetection("Motion detection alarm"),
    // Video occlusion alarm
    VideoOcclusion("Video occlusion alarm"),
    // Retrograde detection alarm
    ReverseDetection("Retrograde detection alarm"),
    // Wandering detection alarm
    LoiteringDetection("Wandering detection alarm"),
    // Traffic statistics alarm
    FlowStatistics("Traffic statistics alarm"),
    // Density detection alarm
    DensityDetection("Density detection alarm"),
    // Video anomaly detection and alarm
    VideoAbnormal("Video anomaly detection and alarm"),
    // Fast moving alarm
    RapidMovement("Fast moving alarm"),
    // Storage device disk failure alarm
    StorageFault("Storage device disk failure alarm"),
    // Storage device fan failure alarm
    StorageFanFault("Storage device fan failure alarm"),
    // Abnormal sound alarm
    SoundAbnormal("Abnormal sound alarm"),
    // Semaphore exception alarm
    SignalAbnormal("Semaphore exception alarm"),
    // Illegal access alarm
    IllegalAccess("Illegal access alarm"),
    // Virtual focus alarm
    Defocus("Virtual focus alarm"),
    // Scene change alarm
    SceneChange("Scene change alarm"),
    // Call the police when people gather
    CrowdGathering("Call the police when people gather"),
    // Parking detection alarm
    ParkingDetection("Parking detection alarm"),
    // Other alarms
    Other("Other alarms");

    private String description;

    AlarmType(String description) {
        this.description = description;
    }
}
