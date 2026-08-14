<!-- On-demand process -->

#On-demand process

> The following is the WVP-PRO cascade voice call process.

```plantuml
@startuml
""Upper-level platform" -> "Subordinate platform": 1. Initiate a voice call request
"Upper-level platform" <-- "Subordinate platform": 2. 200OK
"Upper-level platform" <- "lower-level platform": 3. ReplyResult OK
"Upper-level platform" --> "Subordinate platform": 4. 200OK

""Subordinate Platform" -> "Device": 5. Initiate a voice call request
"Lower-level platform" <-- "Device": 6. 200OK
"Subordinate platform" <- "device": 7. ReplyResult OK
"Lower-level platform" --> "Equipment": 8. 200OK

"Lower-level platform" <- "Device": 9. invite(broadcast)
"Lower-level platform" --> "Equipment": 10. 100 trying
"Lower-level platform" --> "Equipment": 11. 200OK SDP
"Lower-level platform" <-- "Device": 12. ack

"Upper-level platform" <- "Subordinate platform": 13. invite(broadcast)
"Upper-level platform" --> "Subordinate platform": 14. 100 trying
"Upper-level platform" --> "Subordinate platform": 15. 200OK SDP
"Upper-level platform" <-- "Subordinate platform": 16. ack

"Upper-level platform" -> "Lower-level platform": 17. PushRTP
"Lower-level platform" -> "Device": 18. PushRTP

@enduml
```

## The registration process is described as follows:

1. The user initiates an on-demand request from the web page or calling interface;
2. WVP-PRO sends an Invite message to the camera. The message header field carries the Subject field, indicating the on-demand video source ID, the sender's media stream serial number, the IP and port number used by ZLMediaKit to receive the stream,
Receiver media stream sequence number and other parameters, the s field in the SDP message body is "Play" which represents real-time on-demand, the y field describes the SSRC value, and the f field describes the media parameters.
3. The camera replies 200OK to WVP-PRO. The message body describes the IP, port, media format, SSRC field, etc. of the media stream sent by the media stream sender.
4. WVP-PRO replies Ack to the device, and the session is established successfully.
5. The device sends a real-time stream to ZLMediaKit.
6. ZLMediaKit sends stream change events to WVP-PRO.
7. WVP-PRO replies to the WEB user with the playback address.
8. ZLMediaKit sends the stream unwatched event to WVP.
9. WVP-PRO replies Bye to the device and ends the session.
10. The device replies 200OK and the session ends successfully.
