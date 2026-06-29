package com.clutch.app.controller;

import com.clutch.app.dto.response.AuditLogDto;
import com.clutch.app.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/")
@RequiredArgsConstructor
public class AuditLogController {

    private final AuditLogService auditLogService;

    /**
     * Gets history of data changes for row
     *
     * @param rowUuid row uuid
     * @param page    page number
     * @return history data for the row
     */
    @GetMapping("rows/{rowUuid}/history")
    public Page<AuditLogDto> getRowHistory(@PathVariable UUID rowUuid,
                                           @RequestParam(defaultValue = "0") int page) {

        return auditLogService.getRowHistory(rowUuid, page);
    }

    /**
     * Gets history of data changes for row by user
     *
     * @param rowUuid row uuid
     * @param page    page number
     * @return history data for the row
     */
    @GetMapping("rows/{rowUuid}/history/user")
    public Page<AuditLogDto> getRowHistoryByUser(@PathVariable UUID rowUuid,
                                                 @RequestParam String userEmail,    // todo: hide user email from url
                                                 @RequestParam(defaultValue = "0") int page) {
        return auditLogService.getRowHistoryByUser(rowUuid, userEmail, page);
    }

}
