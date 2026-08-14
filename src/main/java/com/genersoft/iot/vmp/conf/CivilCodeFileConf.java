package com.genersoft.iot.vmp.conf;

import com.genersoft.iot.vmp.common.CivilCodePo;
import com.genersoft.iot.vmp.utils.CivilCodeUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.ObjectUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/**
 * Read the administrative division table at startup
 */
@Slf4j
@Configuration
public class CivilCodeFileConf implements CommandLineRunner {

    @Autowired
    @Lazy
    private UserSetting userSetting;

    @Override
    public void run(String... args) throws Exception {
        if (ObjectUtils.isEmpty(userSetting.getCivilCodeFile())) {
            log.warn("[Administrative division] The file is not set, which may result in incomplete directory refresh results.");
            return;
        }
        InputStream inputStream;
        if (userSetting.getCivilCodeFile().startsWith("classpath:")){
            String filePath = userSetting.getCivilCodeFile().substring("classpath:".length());
            ClassPathResource civilCodeFile = new ClassPathResource(filePath);
            if (!civilCodeFile.exists()) {
                log.warn("[Administrative division] File<{}>Does not exist, which may result in incomplete directory refresh results.", userSetting.getCivilCodeFile());
                return;
            }
            inputStream = civilCodeFile.getInputStream();

        }else {
            File civilCodeFile = new File(userSetting.getCivilCodeFile());
            if (!civilCodeFile.exists()) {
                log.warn("[Administrative division] File<{}>Does not exist, which may result in incomplete directory refresh results.", userSetting.getCivilCodeFile());
                return;
            }
            inputStream = Files.newInputStream(civilCodeFile.toPath());
        }

        BufferedReader inputStreamReader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
        int index = -1;
        String line;
        while ((line = inputStreamReader.readLine()) != null) {
            index ++;
            if (index == 0) {
                continue;
            }
            String[] infoArray = line.split(",");
            CivilCodePo civilCodePo = CivilCodePo.getInstance(infoArray);
            CivilCodeUtil.INSTANCE.add(civilCodePo);
        }
        inputStreamReader.close();
        inputStream.close();
        if (CivilCodeUtil.INSTANCE.isEmpty()) {
            log.warn("[Administrative division] The file content is empty, which may cause the directory refresh result to be incomplete.");
        }else {
            log.info("[Administrative division] Loading successful, total data loaded{}Article", CivilCodeUtil.INSTANCE.size());
        }
    }
}
