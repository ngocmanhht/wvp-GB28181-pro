<!-- The use of national standard cascade -->

# Use of national standard cascade

National standard 28181 supports two connection methods between different platforms, horizontal and upper-level. WVP currently supports upward cascading.

## Add superior platform

Click the "Add" button on the national standard cascade page, taking pushing to the superior WVP as an example, see [Access device](./_content/ability/device.md) 
![cascade17](_media/img_17.png)

1. Name
The name of the lower-level platform seen by the upper-level platform;
2. Local IP
Which specific network card is used for local connection to the superior;
3. SIP authentication username
It can be set to be consistent with the "Equipment National Standard Number";
4. Registration period
How often to initiate registration, in seconds;
5. Heartbeat cycle
How often to send a heartbeat. Generally, if the upper-level platform fails to receive the heartbeat three times, it will consider the lower-level platform to be offline, so it is recommended that {heartbeat period}x3 < registration period;
6. SDP streaming IP
The local IP used when calling the media node to send the video stream to the superior;
7. Signaling transmission
Signaling transmission mode supports UDP and TCP. There are no special requirements. The default is UDP;
8. Directory grouping
The superior sends a "CATALOG" message to query channel information. Each message carries several channel information. The default is 1. Increasing this value can speed up the channel sending speed;
9. Character set
The encoding format used for the message body in the "MESSAGE" message sent to the superior, the national standard 28181-2016 defaults to GB2312;
10. Administrative divisions
If you check the "Other options/Push platform information" option, the platform information will be pushed to the superior. Here is the administrative division information of the platform.
11. Platform vendors
If you check the "Other options/Push platform information" option, the platform information will be pushed to the superior. Here is the platform manufacturer information of the platform.
12. Platform model
If you check the "Other options/Push platform information" option, the platform information will be pushed to the superior. Here is the platform model information of the platform.
13. Platform installation address
If you check the "Other options/Push platform information" option, the platform information will be pushed to the superior. Here is the platform installation address information of the platform.
14. Other options
- RTCP keepalive
When the superior stream transmission mode is UDP, due to the stateless nature of UDP, it is impossible to know whether the superior is collecting traffic normally. When RTCP keepalive is enabled, you can actively send RTCP messages to confirm whether the superior is collecting traffic normally.
Under abnormal circumstances, the subordinate can actively stop the flow;
- Message channel
Supports pushing messages to the superior WVP through alarm messages. The message content is sent to wvp by redis message, and wvp edits it into an alarm message and sends it to the superior;
- Active push channel
WVP simulates a catalog subscription information, and then when the shared channel changes, it sends a CATALOG event to the superior to notify the specific channel change.
Currently supported states are: state change events ON: online, OFF: offline, VLOST: video loss, DEFECT: failure, ADD: add, DEL: delete, UPDATE: update;
- Push platform information
If this option is checked, there will be an additional platform information channel in the channel information received by the superior. The content can be modified in the platform editor;
- Push group information
Check this option. If the channel you share is assigned a specific business group and virtual organization, the channel received by the superior will include business group and virtual organization node information;
- Push administrative divisions
Check this option. If the channel you share is assigned a specific administrative division, the channel received by the superior will include administrative division information;

The cascaded platform appears in the national standard cascade list; at the same time, the status is displayed as online. If the status is offline, it may be that your service information is configured incorrectly or the network is unavailable.
There are three icons in the subscription information column, indicating that the superior has enabled the subscription. From left to right, they are: alarm subscription, directory subscription, and mobile location subscription.

## Channel sharing

Click the "Channel Share" button for the platform you want to push to.
![cascade18](_media/img_18.png)

1. Add status and select "Unshared" to share specific channels with superiors;
2. Add status and select "Shared" to see the shared channel, and support the special name and number of this channel on this platform device;
3. Click "Add by device" to share all channels under a certain national standard device with superiors;
4. Click "Remove by Device" to unshare all channels under a certain national standard device to the superior;
5. Click "Add All" to share all channels with superiors;
6. Click "Remove All" to share all channels with superiors;

## Push channel

WVP will send ADD events to superiors in the form of directory subscription message notifications for all channel information.
