package com.clutch.app.controller;

import com.clutch.app.config.TenantContext;
import com.clutch.app.service.importdata.StreamingImportService;
import com.clutch.app.service.notification.NotificationService;
import com.opencsv.exceptions.CsvValidationException;
import jakarta.xml.bind.ValidationException;
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
import java.io.InputStream;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/clutch")
@RequiredArgsConstructor
public class ImportController {

    private final NotificationService notificationService;
    private final StreamingImportService streamingImportService;

    // notification subscription
    @GetMapping(value = "/import/{importId}/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(@PathVariable String importId) {
        return notificationService.createEmitter(importId);
    }

    @PostMapping(value = "/import/form/{formId}/upload", consumes = "multipart/form-data")
    public String importFile(@PathVariable UUID formId,
                             @RequestParam("file") MultipartFile file) throws IOException {
        log.info("Import :: started. File: " + file.getOriginalFilename());

        String importId = UUID.randomUUID().toString();
        UUID companyId = TenantContext.get();

        InputStream inputStream = file.getInputStream();

        Thread.ofVirtual().start(() -> {
            try {
                ScopedValue.where(TenantContext.COMPANY_UUID, companyId).run(() -> {
                    if (Objects.requireNonNull(file.getOriginalFilename()).endsWith(".csv")) {
                        try {
                            streamingImportService.processCsv(formId, importId, inputStream);
                        } catch (ValidationException | CsvValidationException | IOException e) {
                            throw new RuntimeException(e);
                        }
                    } else {
                        streamingImportService.processExcel(formId, importId, inputStream);
                    }
                });
                log.info("Import :: completed. File: " + file.getOriginalFilename());
            } catch (Exception e) {
                notificationService.sendError(importId, "Import error: " + e.getMessage());
            }
        });

        return importId; // id for SSE subscription
    }

}
