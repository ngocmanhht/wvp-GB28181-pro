package com.genersoft.iot.vmp;

import com.genersoft.iot.vmp.gb28181.event.EventPublisher;
import com.genersoft.iot.vmp.jt1078.util.ClassUtil;
import com.genersoft.iot.vmp.utils.GitUtil;
import com.genersoft.iot.vmp.utils.SpringBeanFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.scheduling.annotation.EnableScheduling;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;    
import jakarta.servlet.SessionCookieConfig;
import jakarta.servlet.SessionTrackingMode;
import java.util.Collections;

/**
 * Startup class
 */
@ServletComponentScan("com.genersoft.iot.vmp.conf")
@SpringBootApplication
@EnableScheduling
@EnableCaching
@Slf4j
public class VManageBootstrap extends SpringBootServletInitializer {

	private static String[] args;
	private static ConfigurableApplicationContext context;
	public static void main(String[] args) {
		VManageBootstrap.args = args;
		VManageBootstrap.context = SpringApplication.run(VManageBootstrap.class, args);
		ClassUtil.context = VManageBootstrap.context;
		GitUtil gitUtil = SpringBeanFactory.getBean("gitUtil");
		if (gitUtil == null) {
			log.info("Failed to obtain version information");
		}else {
			log.info("build version： {}", gitUtil.getBuildVersion());
			log.info("Build time： {}", gitUtil.getBuildDate());
			log.info("GITInformation: Branch: {}, ID: {},  time: {}", gitUtil.getBranch(), gitUtil.getCommitIdShort(), gitUtil.getCommitTime());
		}

	}
	// Project restart
	public static void restart() {
		context.close();
		VManageBootstrap.context = SpringApplication.run(VManageBootstrap.class, args);
	}

	@Override
	protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
		return application.sources(VManageBootstrap.class);
	}

	@Override
	public void onStartup(ServletContext servletContext) throws ServletException {
		super.onStartup(servletContext);

		servletContext.setSessionTrackingModes(
				Collections.singleton(SessionTrackingMode.COOKIE)
		);
		SessionCookieConfig sessionCookieConfig = servletContext.getSessionCookieConfig();
		sessionCookieConfig.setHttpOnly(true);
	}
}
