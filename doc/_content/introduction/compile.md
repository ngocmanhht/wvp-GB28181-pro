<!-- Compile -->

# compile

WVP-PRO not only implements the national standard 28181 protocol, it is also a complete video platform itself. So for newbies, you may need some patience to complete. Don’t be anxious when you encounter problems;

1. Baidu
2. Add planetary questions; [knowledge planet](https://t.zsxq.com/0d8VAD3Dm) 
3. Send an email to the author at 648540858@qq.com for technical support (paid);

WVP-PRO is developed using Spring boot, and maven manages dependencies. For friends who are familiar with spring development, it is easy to compile, deploy and run.
The following will provide a general method to facilitate everyone to run the project.

## 1 Service introduction

| Service | Function | Is it necessary |
|------------|------------------------------------------|------|
| WVP-PRO | Implements GB 28181 signaling and video platform related functions | Yes |
| ZLMediaKit | Provides WVP-PRO with the implementation of the media part of the national standard 28181, as well as distribution support for various video stream formats | Yes |

## 2 Install dependencies

| Dependencies | Version | Purpose | Development environment requirements | Production environment requirements |
|--------|-------|-------------|--------|--------|
| jdk | >=21 | Run and compile java code | Yes | Yes |
| maven | >=3.3 | Manage java code dependencies | No | No |
| git | | Download/update/commit code | No | No |
| nodejs | | Compile before running front-end file | No | No |
| npm | | Manage front-end file dependencies | No | No |

If you are a newbie, it is recommended that you use the linux or macOS platform. Windows is not recommended.

Ubuntu environment, taking Ubuntu 18 as an example:

``` bash
apt-get install -y openjdk-21-jdk git maven nodejs npm
```

window environment, taking windows10 as an example:

```bash
I won’t go into details here. You can search a lot on Baidu or Google. Basically, the next step is the next step, and then configure the environment variables.。
```

## 3 Install mysql and redis

Here we still refer to the online tutorial and install it by yourself.

## 4 Compile ZLMediaKit

> Now zlm provides the latest version of the package for direct download, the address is: [Download binary packages for each platform](https://github.com/ZLMediaKit/ZLMediaKit/issues/483) 

ReferenceZLMediaKit [WIKI](https://github.com/ZLMediaKit/ZLMediaKit/wiki) 
, if you need to use the voice intercom function, please refer to [zlm enable webrtc compilation guide](https://github.com/ZLMediaKit/ZLMediaKit/wiki/zlm%E5%90%AF%E7%94%A8webrtc%E7%BC%96%E8%AF%91%E6%8C%87%E5%8D%97) 
, enable the webrtc function of zlm. Intercept the key steps:

```bash
# Domestic users recommend downloading from the synchronization mirror website gitee 
git clone --depth 1 https://gitee.com/xia-chu/ZLMediaKit
cd ZLMediaKit
# Don't forget to execute this command
git submodule update --init
```

## 5 Compile WVP-PRO

### 5.1 You can clone through git, or click to download in the project download

! [Click to download](_media/img_1.png) 
! [Click to download](_media/img_2.png) 
Clone from gitee

```bash
git clone https://gitee.com/pan648540858/wvp-GB28181-pro.git
```

Clone from github

```bash
git clone https://github.com/648540858/wvp-GB28181-pro.git
```

### 5.2 Compile front-end page

```shell script
cd wvp-GB28181-pro/web/
npm --registry=https://registry.npmmirror.com install
npm run build:prod
```

If an error is reported during compilation, it is usually due to network problems, resulting in failure to download dependent packages.
After compilation is completed, the static directory appears under src/main/resources
**The compilation process usually looks like this, with no red error message in the middle**
! [Compiled successfully](_media/img.png) 

### 5.3 Generate executable jar

```bash
cd wvp-GB28181-pro
mvn package
```

### 5.4 Generate war

```bash
cd wvp-GB28181-pro
mvn package -P war
```

If an error is reported during compilation, it is usually due to network problems, resulting in failure to download dependent packages.
After compilation is completed, `wvp-pro-VERSION.jar` and `wvp-pro-VERSION.war` files will appear in the target directory.
Next [Configuration service](./_content/introduction/config.md) 

  






