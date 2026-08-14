<!-- Access device -->

# Access device

## National standard 28181 equipment

Device access mainly requires configuring the 28181 superior information, that is, WVP-PRO, on the device. Only when the information is consistent can the registration be successful. After the device is successfully registered, open WVP->
National standard equipment, you can see the newly added equipment; [Equipment usage](./_content/ability/device_use.md) ,
There are mainly the following fields that need to be configured:

- sip->port  
28181 service listening port

- sip->domain  
The domain should use the first ten digits of the unified encoding of the ID.

- sip->id  
28181 Service ID

- sip->password  
28181 service password

- The configuration information is in the following location

![_media/img_16.png](_media/img_16.png)
***

### 1. Dahua camera

![_media/img_10.png](_media/img_10.png)

### 2. Dahua NVR

![_media/img_11.png](_media/img_11.png)

### 3. Uniview Technology

![_media/img_25.png](_media/img_25.png)

### 3. Ecovision camera

![_media/img_15.png](_media/img_15.png)

### 4. Mercury Camera

![_media/img_12.png](_media/img_12.png)

### 5. Hikvision camera

![_media/img_9.png](_media/img_9.png)

## Live streaming equipment

Here we take obs push streaming as an example. Many drones are the same. You can access it by setting the push streaming address.

1. Obtain the push address from wvp, select the node management menu, and view the node to be pushed;
   ![_media/img_19.png](_media/img_19.png)
2. Splice push address
The obtained rtsp address is: rtsp://{stream IP}:{RTSP PORT}/{app}/{stream}
The obtained rtmp address is: rtmp://{stream IP}:{RTMP PORT}/{app}/{stream}
The stream IP is the IP that the device can connect to zlm, and the port is the port number of the corresponding protocol. App and stream can be defined by themselves.
3. Add push authentication information
WVP enables push authentication by default. The spliced ​​address cannot be pushed directly and will be returned as authentication failure. Please refer to [Push rules](_content/ability/push?id=Push rules) 
4. After the push is successful, you can see the push device in the push list and can play it.
This method only supports the playback of the device's real-time stream and has no other functions. The push information will be automatically removed after the push is completed and will not be visible in the list. If you need to push information, you need to configure the national standard number for the device so that it can exist as a permanent channel of WVP.

## Access non-national standard IPC devices or other devices with flow addresses.

This type of device is mainly accessed through a streaming proxy. The principle is that zlm actively pulls the stream like a player and caches it on its own server for others to play. This can solve the problem of poor concurrent access capabilities of the source device.
You can play directly after pulling the streaming agent/adding the proxy. The streaming proxy also only supports playing the currently configured stream.

 [Equipment usage](_content/ability/device_use.md) 
