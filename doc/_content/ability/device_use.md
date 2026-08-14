<!-- Equipment usage -->

# National standard equipment

### Update device channel

Click the "Refresh" button at the end of the list, and you will see a circular progress bar. The update will be completed after the progress is completed and the prompt is successful. If the number of channels has changed, you can click on the upper left corner! [Refresh](_media/img_14.png) 
You can see the change in the number of channels; if the number of channels is still not 0, it is possible that the other party has not pushed the channel to you yet.

### View device channel

Click the "Channel" button at the end of the list,

### Edit device

Click the "Edit" button at the end of the list to modify the device functions in the pop-up window that opens.

-Device name
If the device name cannot be read from the device or you need to rename it yourself, you can modify this option.
- password
Supports configuring independent passwords for devices.
- Receive IP
If you need the device to access the video stream from a specified network address, you can configure this IP and the device will send the stream to this IP, such as a server with multiple network cards connected. Or there is network mapping.
- Streaming ID
The streaming media ID used by the fixed device is automatically assigned based on the load by default.
- character set
Modify the character set used when reading device data. The default is GB2312. However, GB2312 does not contain complete Chinese characters, so sometimes you encounter garbled characters. You can change it to UTF-8 to solve the problem.
- Catalog subscription
Fill in the subscription period to enable directory subscription for the device. If the device supports directory subscription, the device will notify WVP of which channels have changed when the channel information changes, including channel addition/deletion/update/online/offline/video loss/fault. 0 means unsubscribe.
Generally, this option can be turned on when the NVR is connected to the platform. It does not make much sense to turn on this option when directly connecting the camera.
- Mobile location subscription
Enable mobile location subscription for the device. If the device supports directory subscription, WVP will be notified when the device location changes. Generally, law enforcement recorders can enable this option, but it is of little significance for devices with fixed locations.
- SSRC check
To resolve streaming issues on some devices, this option can be turned on. ZLM will process the video stream strictly according to the given ssrc. The streaming information of some devices is not standard, and turning it on may result in the inability to play on-demand.
- as a message channel
wvp supports pushing messages to each other through alarm messages to subordinate WVPs. The message content is sent to wvp through redis messages, and wvp edits it into alarm messages and sends them to subordinates.
- Send stream after receiving ACK
Voice intercom strategy: Different devices have different requirements for the timing of voice intercom reception. If checked, the stream will start after receiving the ack sent by the device. If not checked, the stream will start after replying with 200OK. Currently, it is known that Dahua equipment does not check, and Hikvision needs to check.

### Delete device

The device information in WVP can be deleted. If the device 28181 configuration has not been changed, the device will still be registered after the next registration.

### Video on demand

After entering the channel list, click the "Play" button at the end of the list, and the play page will pop up after a moment.

### Equipment recording

After entering the channel list, click the "Device Recording" button at the end of the list. You can also click Recording Query on the playback page to enter the video viewing page. Select the date you want to view to play and download the video.

### PTZ control

Devices that support the PTZ function can be rotated up, down, left, right, and zoomed in or out.

### Get the player address of the video

After the video on demand is successfully played, on the real-time video page, click "More Addresses" to see all the playback addresses. Whether the address can be played depends on whether you have fully compiled and enabled the zlm function, and it is also related to the network.

### Voice intercom

 [Voice intercom](_content/ability/continuous_broadcast.md) 