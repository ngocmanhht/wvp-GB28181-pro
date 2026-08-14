<!-- Deployment -->

# deploy

**Please read the following carefully**

1. WVP-PRO and ZLM support separate deployment;
2. Ports that need to be opened
| Service | Port | Type | Required |
   |-----|:-------------------------|-------------|-------|
| wvp | server.port | tcp | yes |
| wvp | sip.port | udp and tcp | yes |
| zlm | http.port | tcp | yes |
| zlm | http.sslport | tcp | no |
| zlm | rtmp.port | tcp | no |
| zlm | rtmp.sslport | tcp | no |
| zlm | rtsp.port | udp and tcp | no |
| zlm | rtsp.sslport | udp and tcp | no |
| zlm | rtp_proxy.port | udp and tcp | single port open |
| zlm | rtp.port-range (configured in wvp) | udp and tcp | multi-port opening |

3. For test environment deployment, it is recommended that all services be deployed on one host and the firewall is turned off to reduce the possibility of network problems;
4. Open ports on demand in the production environment, but it is recommended to modify the default port, especially port 5060, which is vulnerable to attacks;
5. When zlm is deployed using docker, please use host mode, or the port mapping is consistent, such as mapping 5060, the external port should also be mapped to port 5060;
6. ZLM and WVP will maintain high-frequency communication, so do not separate WVP and ZLM into two networks. For example, WVP is on the internal network and ZLM is on the public network.
7. Start the service, taking linux as an example
**Start WVP-PRO**

```shell
nohup java -jar wvp-pro-*.jar &
```

**war package:**
After downloading Tomcat, put the war package into webapps, start Tomcat to decompress the war package, stop Tomcat, delete the ROOT directory and war package, rename the decompressed war package directory to ROOT, and configure the Server.port in the configuration file to be consistent with the Tomcat port
Then start Tomcat.
**Start ZLM**

```shell
nohup ./MediaServer -d -m 3 &
```

### Separate deployment of front-end and back-end

The front-end is built based on [vue-admin-template](https://github.com/PanJiaChen/vue-admin-template/blob/master/README-zh.md) , please refer to here.

###Default account and password

After deployment, you can access WVP by accessing IP and port. The default login account and password of WVP are both admin.



