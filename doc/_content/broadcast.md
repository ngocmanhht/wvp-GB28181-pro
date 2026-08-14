# Schematic

## Use ffmpeg to test the principle of voice intercom

```plantuml
@startuml
"FFMPEG" -> "ZLMediaKit": Push tozlm
"WVP-PRO" <- "ZLMediaKit": Notification of receipt of voice intercom push stream, carrying device and channel information
"WVP-PRO" -> "Device": Start voice intercom
"WVP-PRO" <-- ""Device": Voice intercom established successfully, carrying traffic collection port
"WVP-PRO" -> "ZLMediaKit": Notify zlm to push the stream to the device's stream port
"ZLMediaKit" -> ""Device": push stream to device
@enduml
```

## Use the web page to test the principle of voice intercom

```plantuml
@startuml
"Front-end page" -> "WVP-PRO": Request push address
"Front-end page" <-- "WVP-PRO": Return push address
"Front-end page" -> "ZLMediaKit": Use webrtc to push to zlm, the following process is the same
"WVP-PRO" <- "ZLMediaKit": Notification of receipt of voice intercom push stream, carrying device and channel information
"WVP-PRO" -> "Device": Start voice intercom
"WVP-PRO" <-- ""Device": Voice intercom established successfully, carrying traffic collection port
"WVP-PRO" -> "ZLMediaKit": Notify zlm to push the stream to the device's stream port
"ZLMediaKit" -> ""Device": push stream to device
@enduml
```