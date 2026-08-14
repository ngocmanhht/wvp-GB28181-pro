<!-- Error when starting -->

# Error when starting

Most of the time, errors at startup are caused by problems with your configuration. For example, mysql is not connected, redis is not connected, and port 18080/15060 is occupied. These will cause errors at startup, which can be solved by modifying the configuration;
I have compiled some common mistakes below. You can check them briefly first.
> **Common Mistakes**

![_media/img.png](_media/img.png)
**Error reason:** redis configuration error, possible reasons: redis is not started/ip error/port error/network unavailable
---
![_media/img_1.png](_media/img_1.png)
**Error reason:** redis configuration error, possible reasons: wrong password
---
![_media/img_2.png](_media/img_2.png)
**Error reason:** mysql configuration error, possible reasons: mysql is not started/ip error/port error/network unavailable
---
![_media/img_3.png](_media/img_3.png)
**Error reason:** mysql configuration error, possible reasons: wrong username/password
---
![_media/img_4.png](_media/img_4.png)
**Error reason:** SIP configuration error, possible reasons: SIP port is occupied
---
![_media/img_5.png](_media/img_5.png)
**Error reason:** WVP Tomcat port configuration error, possible reasons: server.port port is occupied
---