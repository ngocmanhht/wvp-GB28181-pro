![logo](doc/_media/logo.png)
# Out of the box national standard 28181 and ministry standard 808+1078 protocol video platform

[![Build Status](https://travis-ci.org/xia-chu/ZLMediaKit.svg?branch=master)](https://travis-ci.org/xia-chu/ZLMediaKit)
[![license](http://img.shields.io/badge/license-MIT-green.svg)](https://github.com/xia-chu/ZLMediaKit/blob/master/LICENSE)
[![JAVA](https://img.shields.io/badge/language-java-red.svg)](https://en.cppreference.com/)
[![platform](https://img.shields.io/badge/platform-linux%20|%20macos%20|%20windows-blue.svg)](https://github.com/xia-chu/ZLMediaKit)
[![PRs Welcome](https://img.shields.io/badge/PRs-welcome-yellow.svg)](https://github.com/xia-chu/ZLMediaKit/pulls)


WEB VIDEO PLATFORM is an out-of-the-box network video platform based on GB28181-2016, Ministry of Industry and Commerce 808, and Ministry of Industry and Commerce 1078 standards. It is responsible for implementing the core signaling and device management backend parts, supports NAT penetration, and supports IPC and NVR access from brands such as Hikvision, Dahua, and Uniview. Supports national standard cascading and supports forwarding cameras/live streams/live push streams without national standard functions to other national standard platforms.

The streaming service is based on @xiachu ZLMediaKit [https://github.com/ZLMediaKit/ZLMediaKit](https://github.com/ZLMediaKit/ZLMediaKit) 
Player using @dexter jessibuca [https://github.com/langhuihui/jessibuca/tree/v3](https://github.com/langhuihui/jessibuca/tree/v3) 
The player uses @Numberwolf-Yanlong h265web.js [https://github.com/numberwolf/h265web.js](https://github.com/numberwolf/h265web.js) 
The front-end page is built based on vue-admin-template [https://github.com/PanJiaChen/vue-admin-template?tab=readme-ov-file](https://github.com/PanJiaChen/vue-admin-template?tab=readme-ov-file) 

#Application scenarios:
- Support browser video playback without plug-in.
- Supports access to national standard equipment (camera, platform, NVR, etc.)
- Supports rtsp, rtmp, live broadcast equipment access, making full use of old equipment.
- Supports national standard cascade. Multi-platform cascading. Cross-network video preview.
- Support cross-network gatekeeper platform interconnection.


# document
wvp usage documentation [https://doc.wvp-pro.cn](https://doc.wvp-pro.cn) 
ZLM usage documentation [https://github.com/ZLMediaKit/ZLMediaKit](https://github.com/ZLMediaKit/ZLMediaKit) 

# gitee warehouse
https://gitee.com/pan648540858/wvp-GB28181-pro.git

# screenshot
<table>
    <tr>
<td ><center><img src="doc/_media/1.png" >Login page </center></td>
<td ><center><img src="doc/_media/2.png" >Homepage</center></td>
    </tr>
    <tr>
<td ><center><img src="doc/_media/3.png" >Split screen playback </center></td>
<td ><center><img src="doc/_media/4.png" >National standard equipment list</center></td>
    </tr>
    <tr>
<td ><center><img src="doc/_media/5.png" >Administrative division management </center></td>
<td ><center><img src="doc/_media/8.png" >Business group management</center></td>
    </tr>
    <tr>
<td ><center><img src="doc/_media/6.png" >Recording plan</center></td>
<td ><center><img src="doc/_media/7.png" >Platform information</center></td>
    </tr>
</table>

# Features
- [X] Integrated web interface
- [X] Good compatibility
- [X] Cross-platform service, once compiled and deployed on multiple terminals, can be used for both x86 and arm architectures
- [X] Access device
- [X] Video preview
- [X] Support main stream and sub-stream switching
- [X] Unlimited access channels, how many devices can be accessed only depends on your server performance
- [X] PTZ control, control device steering, zoom in, zoom out
- [X] Preset position query, use and settings
- [X] Query the recording and playback on NVR/IPC, support playback and download at specified time
- [X] Automatically cut off traffic when no one is watching, saving data traffic
- [X] Video device information synchronization
- [X] Off online monitoring
- [X] Supports direct output of multiple protocol stream addresses for RTSP, RTMP, HTTP-FLV, Websocket-FLV, and HLS
- [X] Supports viewing the camera directly through a stream address without logging in or calling any interface
- [X] Supports two national standard signaling transmission modes: UDP and TCP
- [X] Supports two national standard streaming transmission modes: UDP and TCP
- [X] Support retrieval and channel filtering
- [X] Support channel subdirectory query
- [X] Support audio filtering to prevent noise from affecting viewing
- [X] Support national standard network time adjustment
- [X] Support playing H264 and H265
- [X] Alarm information processing, supports pushing alarm information to the front end
- [X] Voice intercom
- [X] Support custom display of business grouping and administrative division tree and cascade push
- [X] Support subscription and notification methods
- [X] Mobile location subscription
- [X] Mobile location notification handling
- [X] Alarm event subscription
- [X] Alarm event notification processing
- [X] Device Catalog Subscription
- [X] Device catalog notification handling
- [X] Mobile location query and display
- [X] Support manually adding devices and setting individual passwords for devices
- [X] Support platform docking access
- [X] Support national standard cascade
- [X] National standard channel upward cascade
- [X] WEB adds upper level platform
- [X] Register
- [X] Heartbeat Keep Alive
- [X] Channel selection
- [X] Support channel number customization, support the use of different channel numbers for each platform
- [X] Channel push
- [X] On Demand
- [X] PTZ control
- [X] Platform status query
- [X] Platform information query
- [X] Platform remote start
- [X] Customizable virtual directories for each cascade platform
- [X] Directory Subscriptions and Notifications
- [X] Video viewing and playback
- [X] GPS subscription and notification (live streaming)
- [X] Voice intercom
- [X] Support cascading to multiple upper-level platforms at the same time
- [X] Support automatic configuration of ZLM media service to reduce problems caused by configuration issues;
- [X] Support streaming media node cluster and load balancing.
- [X] Support enabling udp multi-port mode to improve media transmission performance in udp mode;
- [X] Support public network deployment;
- [X] Support separate deployment of wvp and zlm to improve platform concurrency capabilities
- [X] Support streaming RTSP/RTMP, distribute to various streaming formats, or push to other national standard platforms
- [X] Support RTSP/RTMP streaming, distribute to various streaming formats, or push to other national standard platforms
- [X] Support push authentication
- [X] Support interface authentication
- [X] Cloud recording, push/proxy/national standard videos can be recorded on the cloud server, support preview and download
- [X] Support packaging executable jar and war
- [X] Support cross-domain requests and separate deployment of front-end and back-end
- [X] Support Mysql, Postgresql, Jincang and other databases
- [X] Support recording plan, record the channel according to the set time. Forwarding the recorded content to the national standard superior is not supported yet.
- [X] Support national standard signaling cluster
- [X] Added support for department standard 808 and department standard 1078. A large number of new features are not listed one by one. Supports being used as a gateway to be called by national standard superiors for ministry-standard equipment
- [X] Support electronic map. Supports displaying channel locations and modifying channel locations on the map. It supports the ability of data layering and thinning, and millions of data can be easily displayed. Provides standard vector tile layers, which can be directly displayed by common map engines.
- [X] Borrowing the new capabilities of the zlm closed-source version, it can support saving video to s3 storage and minio.
- [X] **New virtual thread support greatly improves the platform's concurrency capabilities. LAN stress testing can easily access 50,000+ devices. This is not the service limit. This is the limit of my stress testing tools and hardware test servers. You can test it yourself. Actual performance in production environments depends on server performance and network bandwidth. **
- [X] **Supports alarm subscription and alarm management, supports the display and query of alarm events, and supports automatically obtaining snapshots and playing videos when alarming. **

# Closed source content
- [X] Support all open source functions
- [X] Supports the national standard 28181-2022 protocol and has been certified
- [X] Explicitly support H265 video encoding and AAC audio encoding (already supported by open source)
- [X] Support primary and secondary stream switching (already supported by open source)
- [X] Support GB18030 encoding format. Previously, GB2312 often had garbled characters when encountering uncollected characters. This won't happen anymore
- [X] Supports image capture. The device can capture the image by itself and upload it to the server. It is fast and saves traffic.
- [X] PTZ precise control: including control, query, and subscription to position changes, supporting precise setting of the horizontal angle, vertical angle, and zoom factor of the PTZ
- [X] OSD configuration
- [X] Video screen occlusion configuration
- [X] Supports cruise track query, and the cruise function is perfect ((equivalent to the addition of echo on the basis of open source))
- [X] Memory card management, supports status query, formatting
- [X] Equipment upgrades
- [X] Supports target tracking, supports direct box selection on web pages for manual tracking, and also supports automatic tracking
- [X] Remote configuration of the device's built-in recording plan
- [X] Alarm recording configuration
- [X] Alarm reporting switch
- [X] Video parameter attribute configuration, supports the setting of video parameters: including encoding format, resolution, frame rate, bit rate, video bit rate configuration value (required when the bit rate is fixed)
- [X] Supports screen flip control, supports setting as reference screen, horizontal mirroring (flip left and right), up and down mirroring (flip up and down), center mirroring (flip up and down, left and right)
- [X] Support query of guard position information (equivalent to adding echo on the basis of open source)
- [X] Video playback, reverse dragging
- [X] Supports GB 28181-2022 and GB 28181-2016 dual versions of GB cascade at the same time. You can choose
- [X] ONVIF protocol
- Implemented by self-developed protocols, safe and reliable.
- Device search
- Live image preview
- Video playback and playback speed control
- PTZ control (eight directions), preset position control, absolute positioning, guard position, focus control
- Focus control
-Device restart
- Device time setting and comparison with system time
- Restore factory settings
- Automatically obtain information such as device brand, support display of DNS information, and support display of protocols
- National standard cascade on-demand, automatic on-demand, etc.
- [X] **Haikang-ISUP5.0/ISUP4.0/ISUP2.0/EHome**
- Device registration
- Resource acquisition
- Preview
- Video query and playback
- PTZ control
- Preset position control
- Alarm, supports the analysis and display of a large number of alarm types,
- Tripwire detection
- Area invasion
- Motion detection
- Retrograde detection
- Wandering detection
- Gathering of people
- Abnormal sound
- Equipment abnormalities, etc.
- Snapshot (the device directly uploads snapshot pictures to the server, low traffic consumption, no need for server-side streaming decoding)
- Intercom support
- Device configuration (device name, loop recording, etc. configuration)
- Device information (device serial number, type, etc.)
- Version information (version numbers of software, coding, panels, and hardware)
- Encoding configuration (main and auxiliary stream resolution, bit rate, frame rate, etc. configuration)
- Image parameter configuration (hue, contrast, brightness, saturation configuration)
- [X] Dahua SDK
- LAN device discovery
- Device active registration (used for device registration when the server is deployed on the public network)
- Channel acquisition
- Preview
- Video playback
- Video download
- PTZ control, supports preset position control, cruise group, patrol, horizontal rotation, PTZ speed configuration, power-on action, idle action, PTZ limit, scheduled tasks, PTZ restart
- Snapshot (the device directly uploads snapshot pictures to the server, low traffic consumption, no need for server-side streaming decoding)
- Calling (simplex) and intercom (duplex)
- Camera configuration, including brightness, contrast, contrast, saturation, color suppression, gamma, sharpness, and sharpness suppression; viewing angle configuration, which provides normal, reflection, corridor mode, and mirror configurations; exposure, backlight, white balance, day and night mode, digital zoom, focus, fill light, and fog penetration configurations
- Alarm reception
- [ ] National Standard 35114 Agreement (under development...)
- [X] State Grid B interface protocol
- Device registration
- Resource acquisition
- Preview
- PTZ control
- Preset position control, etc.,
- Free customization to support voice intercom, video playback and image capture.
- [X] Support assigning usable channels according to permissions
- [X] Support table export
- [X] The streaming agent supports splicing URLs according to brands
- [X] For playback authentication, playback cannot be performed on authorized devices, even if the playback address is obtained.


# License Agreement
This project's own code uses the loose MIT license and can be freely used in their own commercial and non-commercial projects while retaining copyright information. However, this project also uses some other open source codes piecemeal. In the case of commercial use, please replace or eliminate them by yourself; any commercial disputes or infringements arising from the use of this project have nothing to do with this project and the developer. Please bear the legal risks yourself. When using the code of this project, the license agreement should also indicate the agreements of the third-party libraries that this project relies on.

# technical support

## Official public account
<img src="doc/_media/gongzhonghao.jpg" width="40%" height="40%">

> Provide everyone with the latest development progress, future plans and other content of WVP. Welcome to pay attention.

## Paid community
<img src="doc/_media/shequ.png" width="50%" height="50%">

> The paid community can provide support to the author and solve problems more quickly for everyone. It also provides a WeChat group for users who have officially joined Planet. If you are not satisfied with the content of Planet, you can withdraw within three days to support automatic refund. If you are unable to join temporarily, giving the project a star is also a great encouragement.

 [knowledge planet](https://t.zsxq.com/0d8VAD3Dm) Column list:
- [WVP Deployment Security Hardening Guide: A must-read for beginners to prevent attacks and vulnerabilities](https://articles.zsxq.com/id_tv8wz4uubx2n.html) 

Paid technical support, one-on-one development coaching, closed source content cooperation, please send an email to 648540858@qq.com for consultation

# Acknowledgments
Thanks to the author [Xia Chu](https://github.com/xia-chu) for providing such a great open source streaming media service framework and providing support and help during the development process.
Thanks to the authors [dexter langhuihui](https://github.com/langhuihui) and [Numberwolf-Yanlong](https://github.com/numberwolf/h265web.js) for open source such a useful WEB player.
Thank you all for your sponsorship and your corrections and help on the project. Including but not limited to code contribution, problem feedback, financial donation and other forms of support! The following rankings are in no particular order:
[lawrencehj](https://github.com/lawrencehj) [Smallwhitepig](https://github.com/Smallwhitepig) [swwhaha](https://github.com/swwheihei)
[hotcoffie](https://github.com/hotcoffie) [xiaomu](https://github.com/nikmu) [TristingChen](https://github.com/TristingChen)
[chenparty](https://github.com/chenparty) [Hotleave](https://github.com/hotleave) [ydwxb](https://github.com/ydwxb)
[ydpd](https://github.com/ydpd) [szy833](https://github.com/szy833) [ydwxb](https://github.com/ydwxb) [Albertzhu666](https://github.com/Albertzhu666)
[mk1990](https://github.com/mk1990) [SaltFish001](https://github.com/SaltFish001)
