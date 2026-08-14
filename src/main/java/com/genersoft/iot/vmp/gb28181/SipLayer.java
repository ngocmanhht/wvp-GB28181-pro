package com.genersoft.iot.vmp.gb28181;

import com.genersoft.iot.vmp.conf.SipConfig;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.gb28181.bean.GbStringMsgParserFactory;
import com.genersoft.iot.vmp.gb28181.conf.DefaultProperties;
import com.genersoft.iot.vmp.gb28181.transmit.ISIPProcessorObserver;
import com.genersoft.iot.vmp.utils.EnvUtil;
import gov.nist.javax.sip.SipProviderImpl;
import gov.nist.javax.sip.SipStackImpl;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

import javax.sip.*;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class SipLayer{

	@Autowired
	private SipConfig sipConfig;

	@Autowired
	private ISIPProcessorObserver sipProcessorObserver;

	@Autowired
	private UserSetting userSetting;

	private final Map<String, SipProviderImpl> tcpSipProviderMap = new ConcurrentHashMap<>();
	private final Map<String, SipProviderImpl> udpSipProviderMap = new ConcurrentHashMap<>();
	private final List<String> monitorIps = new ArrayList<>();

	@PostConstruct
	public void onApplicationReady(){
		if (ObjectUtils.isEmpty(sipConfig.getIp())) {
			try {
				// Get all network interfaces of this machine
				Enumeration<NetworkInterface> nifs = NetworkInterface.getNetworkInterfaces();
				while (nifs.hasMoreElements()) {
					NetworkInterface nif = nifs.nextElement();
					// Obtain the IP address bound to the network interface, generally there is only one
					Enumeration<InetAddress> addresses = nif.getInetAddresses();
					while (addresses.hasMoreElements()) {
						InetAddress addr = addresses.nextElement();
						if (addr instanceof Inet4Address) {
							if (addr.getHostAddress().equals("127.0.0.1")){
								continue;
							}
							if (nif.getName().startsWith("docker")) {
								continue;
							}
							log.info("[Automatically configure SIP listening network card] Network card interface address： {}", addr.getHostAddress());// Only care about IPv4 addresses
							monitorIps.add(addr.getHostAddress());
						}
					}
				}
			}catch (Exception e) {
				log.error("[Failed to read network card information]", e);
			}
			if (monitorIps.isEmpty()) {
				log.error("[Automatic configuration of SIP monitoring network card information failed]， Please manually configure SIP.IP and then restart");
				System.exit(1);
			}
		}else {
			// Use commas to separate multipleip
			String separator = ",";
			if (sipConfig.getIp().indexOf(separator) > 0) {
				String[] split = sipConfig.getIp().split(separator);
				monitorIps.addAll(Arrays.asList(split));
			}else {
				monitorIps.add(sipConfig.getIp());
			}
		}
		sipConfig.setMonitorIps(monitorIps);
		if (ObjectUtils.isEmpty(sipConfig.getShowIp())){
			sipConfig.setShowIp(String.join(",", monitorIps));
		}
		SipFactory.getInstance().setPathName("gov.nist");
		if (!monitorIps.isEmpty()) {
			for (String monitorIp : monitorIps) {
				addListeningPoint(monitorIp, sipConfig.getPort());
			}
			if (udpSipProviderMap.size() + tcpSipProviderMap.size() == 0) {
				System.exit(1);
			}
		}
	}

	private void addListeningPoint(String monitorIp, int port){
		SipStackImpl sipStack;
		try {
			sipStack = (SipStackImpl)SipFactory.getInstance().createSipStack(DefaultProperties.getProperties("GB28181_SIP", userSetting.getSipLog(), userSetting.isSipCacheServerConnections()));
			sipStack.setMessageParserFactory(new GbStringMsgParserFactory());
		} catch (PeerUnavailableException e) {
			log.error("[SIP SERVER] SIPService startup failed, listening address{}Failed, please check if the IP is correct", monitorIp);
			return;
		}

		try {
			ListeningPoint tcpListeningPoint = sipStack.createListeningPoint(monitorIp, port, "TCP");
			SipProviderImpl tcpSipProvider = (SipProviderImpl)sipStack.createSipProvider(tcpListeningPoint);

			tcpSipProvider.setDialogErrorsAutomaticallyHandled();
			tcpSipProvider.addSipListener(sipProcessorObserver);
			tcpSipProviderMap.put(monitorIp, tcpSipProvider);
			log.info("[SIP SERVER] tcp://{}:{} Started successfully", monitorIp, port);
		} catch (TransportNotSupportedException
				 | TooManyListenersException
				 | ObjectInUseException
				 | InvalidArgumentException e) {
			log.error("[SIP SERVER] tcp://{}:{} SIPThe service failed to start. Please check whether the port is occupied or whether the IP address is correct."
					, monitorIp, port);
		}

		try {
			ListeningPoint udpListeningPoint = sipStack.createListeningPoint(monitorIp, port, "UDP");

			SipProviderImpl udpSipProvider = (SipProviderImpl)sipStack.createSipProvider(udpListeningPoint);
			udpSipProvider.addSipListener(sipProcessorObserver);
			udpSipProvider.setDialogErrorsAutomaticallyHandled();
			udpSipProviderMap.put(monitorIp, udpSipProvider);

			log.info("[SIP SERVER] udp://{}:{} Started successfully", monitorIp, port);
		} catch (TransportNotSupportedException
				 | TooManyListenersException
				 | ObjectInUseException
				 | InvalidArgumentException e) {
			log.error("[SIP SERVER] udp://{}:{} SIPThe service failed to start. Please check whether the port is occupied or whether the IP address is correct."
					, monitorIp, port);
		}
	}

	public SipProviderImpl getUdpSipProvider(String ip) {
		if (udpSipProviderMap.size() == 1) {
			return udpSipProviderMap.values().stream().findFirst().get();
		}
		if (ObjectUtils.isEmpty(ip)) {
			return null;
		}
		return udpSipProviderMap.get(ip);
	}

	public SipProviderImpl getUdpSipProvider() {
		if (udpSipProviderMap.size() != 1) {
			return null;
		}
		return udpSipProviderMap.values().stream().findFirst().get();
	}

	public SipProviderImpl getTcpSipProvider() {
		if (tcpSipProviderMap.size() != 1) {
			return null;
		}
		return tcpSipProviderMap.values().stream().findFirst().get();
	}

	public SipProviderImpl getTcpSipProvider(String ip) {
		if (tcpSipProviderMap.size() == 1) {
			return tcpSipProviderMap.values().stream().findFirst().get();
		}
		if (ObjectUtils.isEmpty(ip)) {
			return null;
		}
		return tcpSipProviderMap.get(ip);
	}

	public String getLocalIp(String deviceLocalIp) {
		if(EnvUtil.isDockerEnv()){
			return sipConfig.getShowIp();
		}
		if (monitorIps.size() == 1) {
			return monitorIps.get(0);
		}
		if (!ObjectUtils.isEmpty(deviceLocalIp)) {
			return deviceLocalIp;
		}
		return getUdpSipProvider().getListeningPoint().getIPAddress();
	}
}
