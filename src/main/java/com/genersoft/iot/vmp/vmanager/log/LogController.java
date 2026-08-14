package com.genersoft.iot.vmp.vmanager.log;

import com.genersoft.iot.vmp.conf.exception.ControllerException;
import com.genersoft.iot.vmp.conf.security.JwtUtils;
import com.genersoft.iot.vmp.service.ILogService;
import com.genersoft.iot.vmp.service.bean.LogFileInfo;
import com.genersoft.iot.vmp.vmanager.bean.ErrorCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.List;

@SuppressWarnings("rawtypes")
@Tag(name = "Log file query interface")
@Slf4j
@RestController
@RequestMapping("/api/log")
public class LogController {

    @Autowired
    private ILogService logService;


    @ResponseBody
    @GetMapping("/list")
    @Operation(summary = "Query log files by page", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "query", description = "Search content", required = false)
    @Parameter(name = "startTime", description = "start time(yyyy-MM-dd HH:mm:ss)", required = false)
    @Parameter(name = "endTime", description = "end time(yyyy-MM-dd HH:mm:ss)", required = false)
    public List<LogFileInfo> queryList(@RequestParam(required = false) String query, @RequestParam(required = false) String startTime, @RequestParam(required = false) String endTime

    ) {
        if (ObjectUtils.isEmpty(query)) {
            query = null;
        }
        if (ObjectUtils.isEmpty(startTime)) {
            startTime = null;
        }
        if (ObjectUtils.isEmpty(endTime)) {
            endTime = null;
        }
        return logService.queryList(query, startTime, endTime);
    }

    /**
     * Download the specified log file
     */
    @ResponseBody
    @GetMapping("/file/{fileName}")
    public void downloadFile(HttpServletResponse response, @PathVariable  String fileName) {
        try {
            File file = logService.getFileByName(fileName);
            if (file == null || !file.exists() || !file.isFile()) {
                throw new ControllerException(ErrorCode.ERROR400);
            }
            final InputStream in = Files.newInputStream(file.toPath());
            response.setContentType(MediaType.TEXT_PLAIN_VALUE);
            ServletOutputStream outputStream = response.getOutputStream();
            IOUtils.copy(in, response.getOutputStream());
            in.close();
            outputStream.close();
        } catch (IOException e) {
            response.setStatus(HttpServletResponse.SC_NO_CONTENT);
        }
    }

}
