<!-- Packet capture -->

# Capture packets

If there are any tools that you must know about network programming, I think packet capture must be one of them. As GB/T
28181 The most important method in the debugging process. I think if you are really interested in it, or if the system encounters problems that can be solved as quickly as possible, then you must learn to capture packets.

## Selection of packet capture tools

### 1. Wireshark

On systems with graphical interfaces, such as windows, Linux distributions ubuntu, opensuse, etc., I usually use Wireshark to capture packets directly, which is also convenient for viewing the content.

### 2. Tcpdump

In systems that use the command line, such as Linux servers, I usually use Tcpdump to capture packets. No additional installation is required. The system usually comes with it. The captured files can be opened with Wireshark, and the content can be easily viewed on the graphical interface.

## Tool installation

The installation of Wireshark is very simple. Just click step by step according to the prompts. In Linux, you need to solve the permission problem. If you use a graphical interface Linux distribution like me, you can refer to the following steps;
Windows friends can just skip it.

```shell
# 1. Add wireshark user group
sudo groupadd wireshark
# 2. Change dumpcap to wireshark user group
sudo chgrp wireshark /usr/bin/dumpcap
# 3. Let the wireshark user group have root permissions.dumpcap
sudo chmod 4755 /usr/bin/dumpcap
# 4. Add the username you need to use to the wireshark user group
sudo gpasswd -a $USER wireshark
```

tcpdump generally comes with Linux and does not need to be installed. You can verify it like this; if the version information is displayed, it means it has been installed.

```shell
tcpdump --version
```

## Start capturing packets

### Using Wireshark

In 28181, I usually only focus on sip packets and rtp packets, so I usually filter sip and rtp directly. You can enter `sip or rtp` in the input box. If there are many sources of equipment, you can also add ip and port number filtering.
`(sip or rtp )and ip.addr==192.168.1.3 and udp.port==5060`
Detailed filtering rules can be found on Baidu. I can provide some commonly used ones for your reference.
![img.png](_media/img.png)  
**Filter SIP only:**

```shell
sip
```

**Only get rtp data:**

```shell
rtp
```

**Default mode:**

```shell
sip or rtp
```

**Filter IP:**

```shell
 sip and ip.addr==192.168.1.3
```

**Filter port:**

```shell
 sip and udp.port==5060
```

After entering the command to start packet capture, you can perform operations at this time, such as on-demand playback, video callback, etc. After the operation is completed, return to Wireshark and click the red stop. If you need to save the file, click
 `File->Export specific grouping` exports filtered data, or you can directly save unfiltered data in `File->Save as` .

### Using tcpdump

For server packet capture, in order to obtain sufficiently complete data, I usually require the network card data to be captured directly without filtering, as follows:
To capture the network card, you first need to obtain the network card name. In Linux, I usually use `ip addr` to obtain the network card information, as shown below:
![img_1.png](_media/img_1.png)

```shell
sudo tcpdump -i wlp3s0 -w demo.pcap
```

![img_2.png](_media/img_2.png)  
The command line will stay at this position, and you can perform operations at this time, such as on-demand playback, video playback, etc. After the operation is completed, return to the command line and use `Ctrl+C` 
End the command line, get demo.pcap in the current directory, download this file to the graphical interface operating system, and then use Wireshark to view it.
For more operations, please refer to: [https://www.cnblogs.com/jiujuan/p/9017495.html](https://www.cnblogs.com/jiujuan/p/9017495.html) 
