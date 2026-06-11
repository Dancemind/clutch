package com.clutch.app.controller;

import com.clutch.app.service.importdata.StreamingImportService;
import com.clutch.app.service.notification.SseNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/clutch")
@RequiredArgsConstructor
public class ImportController {

    private final SseNotificationService sseNotificationService;
    private final StreamingImportService streamingImportService;

    /**
     * Import progress notification subscription
     *
     * @param importId import id
     * @return  SSE
     */
    @GetMapping(value = "/import/{importId}/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(@PathVariable String importId) {
        return sseNotificationService.createEmitter(importId);
    }

    /**
     * Import CSV or Excel file
     *
     * @param formId    form id
     * @param file      multipart file
     *
     * @return      import id for SSE
     * @throws IOException  exception
     */
    @PostMapping(value = "/import/form/{formId}/upload", consumes = "multipart/form-data")
    public String importFile(@PathVariable UUID formId,
                             @RequestParam("file") MultipartFile file) throws IOException {
        return streamingImportService.importFile(formId, file);
    }

}
