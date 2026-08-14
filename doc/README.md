# introduce

> Out-of-the-box 28181 protocol video platform

# Overview

- WVP-PRO is based on GB/T
28181-2016 standard-implemented streaming media platform, relying on excellent open source streaming media services [ZLMediaKit](https://github.com/ZLMediaKit/ZLMediaKit) 
, providing complete and rich functions.
- GB/T 28181-2016 The Chinese standard name is "Technical Requirements for Information Transmission, Exchange, and Control of Public Security Video Surveillance Networking Systems", which is a national standard in the field of surveillance. It is widely used in government video platforms.
- Through the 28181 protocol, you can connect the IPC camera to the platform, and you can watch or use 28181/rtsp/rtmp/flv and other protocols to distribute the video stream to other platforms.

# Features

- Implements standard 28181 signaling and is compatible with common brand equipment, such as IPC, NVR and platforms of Hikvision, Dahua, Uniview and other brands.
- Supports cascading national standard devices to other national standard platforms, and also supports pushing images or live broadcasts of devices that do not support national standards to other national standard platforms
- The front-end is perfect and comes with a complete front-end page, which can be deployed and used directly without secondary development.
- Completely open source and using the MIT license. Can be used for commercial projects while retaining copyright.
- Supports load balancing of multiple streaming media nodes.

# Paid community

 [![Community](_media/shequ.png "shequ") ](https://t.zsxq.com/0d8VAD3Dm)
> Charging is to provide better services and is also a greater incentive for authors. Users who join Planet can send me a private message and leave their WeChat ID three days later, and I will recruit everyone into the group. If you are not satisfied within three days of joining, you can get a refund directly. You don’t need to worry. It’s not impossible to come and have sex for free for three days.

# What national standard functions have we implemented?

**As a superior platform**

- [X] Register
- [X] Logout
- [X] Real-time video and audio on demand
- [X] Device Control
- [X] PTZ control
- [X] Remote start
- [X] Video control
- [X] Alarm arm/disarm
- [X] Alarm reset
- [X] Force keyframe
- [X] Scroll down to zoom in
- [X] Zoom out
- [X] Guard position control
- [X] Device configuration
- [X] Alarm event notification and distribution
- [X] Device Catalog Subscription
- [X] Network device information query
- [X] Device directory query
- [X] Device status query
- [X] Device configuration query
- [X] Equipment preset position query
- [X] Status information reporting
- [X] Device video and audio file retrieval
- [X] Playback of historical video and audio
- [X] Play
- [X] Pause
- [X] Forward/Back
- [X] Stop
- [X] Video and audio file download
- [X] School time
- [X] Subscriptions and notifications
- [X] Event subscription
- [X] Mobile Device Location Subscription
- [X] Alarm subscription
- [X] Directory Subscription
- [X] Voice Broadcast
- [X] Voice call

**National Standard Cascade**

- [X] Register
- [X] Logout
- [X] Real-time video and audio on demand
- [X] Device Control
- [X] PTZ control
- [ ] Remote start
- [X] Video control
- [X] Alarm arm/disarm
- [X] Alarm reset
- [X] Force keyframe
- [X] Scroll down to zoom in
- [X] Zoom out
- [X] Guard position control
- [ ] Device Configuration
- [ ] Alarm event notification and distribution
- [X] Device Catalog Subscription
- [X] Network device information query
- [X] Device directory query
- [X] Device status query
- [ ] Device configuration query
- [X] Equipment preset position query
- [X] Status information reporting
- [X] Device video and audio file retrieval
- [X] Playback of historical video and audio
- [X] Play
- [x] Pause
- [x] Forward/Return
- [x] Stop
- [X] Video and audio file download
- [ ] ~~school time~~
- [X] Subscriptions and notifications
- [X] Event subscription
- [X] Mobile Device Location Subscription
- [ ] Alarm subscription
- [X] Directory Subscription
- [X] Voice Broadcast
- [X] Voice call

**Closed source version**
- [X] National Standard 28181-2022
Compared with the 28181-2016 standard, the 28181-2022 standard adds the following functions
- [X] PTZ control precise position query and control, supports setting specific horizontal angles, vertical angles and zoom multiples, and supports subscriptions and notifications.
- [X] Support standard H265 video encoding and AAC audio encoding formats
- [X] Support primary and auxiliary code streams
- [X] Support image capture, you can obtain the current image without pulling the stream, and the inference image acquisition is friendly.
- [X] Supports GB18030 encoding, which does not lose the UTF-8 encoding format. The GB2312 encoding used in 2016 has been upgraded, basically saying goodbye to Chinese garbled characters.
- [X] Support memory card formatting
- [X] Support device software upgrade
- [X] Support OSD configuration
- [X] Support video playback function
- [X] Support target tracking control
- [X] Support video parameter attribute configuration
- [X] Support video blocking configuration
- [X] Support device software upgrade
# Community

The code is currently hosted on GitHub and Gitee. Gitee is currently used as an acceleration warehouse and does not accept issues.
GitHub： [https://github.com/648540858/wvp-GB28181-pro](https://github.com/648540858/wvp-GB28181-pro)  
Gitee： [https://gitee.com/pan648540858/wvp-GB28181-pro](https://gitee.com/pan648540858/wvp-GB28181-pro)
