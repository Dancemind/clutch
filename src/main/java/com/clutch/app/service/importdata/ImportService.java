package com.clutch.app.service.importdata;

import com.clutch.app.config.TenantContext;
import jakarta.xml.bind.ValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImportService {

    private final FileImportService fileImportService;
    private final BulkImportService bulkImportService;

    public void importFileData(MultipartFile file, UUID formUuid) throws Exception {
        log.info("Import :: started. File: " + file.getOriginalFilename());

        List<Map<String, Object>> data =
                Objects.requireNonNull(
                        file.getOriginalFilename()).endsWith(".csv") // todo: supported excel extensions?
                        ? fileImportService.parseCsv(file)
                        : fileImportService.parseExcel(file);

        log.info("Import :: processing. File: " + file.getOriginalFilename() + " Lines: " + data.size());

        // Используем наш BulkImportService для сохранения
        UUID companyUuid = TenantContext.get();
        Thread.ofVirtual().start(() ->
                ScopedValue.where(TenantContext.COMPANY_UUID, companyUuid).run(() -> {
                    try {
                        bulkImportService.importData(formUuid, data);
                        log.info("Import :: completed. File: " + file.getOriginalFilename());
                    } catch (ValidationException e) {
                        throw new RuntimeException("Import failed", e);
                    }
                })
        );
    }
}
