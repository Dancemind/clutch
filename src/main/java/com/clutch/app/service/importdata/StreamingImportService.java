package com.clutch.app.service.importdata;

import com.clutch.app.service.notification.NotificationService;
import com.github.pjfanning.xlsx.StreamingReader;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;
import jakarta.xml.bind.ValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class StreamingImportService {

    private final BulkImportService bulkImportService;
    private final NotificationService notificationService;

    public void processCsv(UUID formId, String importId, MultipartFile file) throws ValidationException, CsvValidationException, IOException {
        try (var reader = new CSVReader(new InputStreamReader(file.getInputStream()))) {

            List<String> columnNames = new ArrayList<>();
            String[] headers;
            boolean headersFound = false;
            int initColumn = -1;

            while (!headersFound && (headers = reader.readNext()) != null) {
                for (int i = 0; i< headers.length; i++) {
                    if (headers[i] != null && !headers[i].isEmpty()) {
                        if (initColumn == -1) {
                            initColumn = i;
                        }
                        columnNames.add(headers[i].trim());
                        headersFound = true;
                    }
                }
            }
            if (!headersFound) {
                notificationService.sendError(importId, "Файл пуст");
                return;
            }
            processInBatches(formId, importId, columnNames, initColumn, reader.iterator());
        } catch (Exception e) {
            // Логируем для админа
            log.error("Ошибка при импорте CSV (ID: {}): {}", importId, e.getMessage());

            // Отправляем пользователю
            notificationService.sendError(importId, "Ошибка парсинга: " + e.getMessage());
            throw e;
        }
    }

    public void processExcel(UUID formId, String importId, MultipartFile file) {
        try (var is = file.getInputStream();
             var workbook = StreamingReader.builder().rowCacheSize(100).open(is)) {

            var sheet = workbook.getSheetAt(0);
            var rowIterator = sheet.iterator();
            if (!rowIterator.hasNext()) return;

            // Читаем заголовки из первой строки
            Row headerRow = rowIterator.next();
            int initColumn = 0;
//            String[] headers = new String[headerRow.getLastCellNum()];
            List<String> headers = new ArrayList<>();
            for (int i = 0; i < headerRow.getLastCellNum(); i++) {
                headers.add(headerRow.getCell(i).getStringCellValue());
            }

            // todo: fix initColumn value, find where headers line starts
            processInBatches(formId, importId, headers, initColumn, rowIterator);
        } catch (Exception e) {
            notificationService.sendError(importId, e.getMessage());
        }
    }

    private void processInBatches(UUID formId, String importId, List<String> headers, int initColumn, Iterator<?> iterator)
            throws ValidationException {
        List<Map<String, Object>> batch = new ArrayList<>();
        int processed = 0;

        while (iterator.hasNext()) {
            Object rowRaw = iterator.next();
            Map<String, Object> rowMap = (rowRaw instanceof String[] strings)
                    ? mapCsvToMap(headers, initColumn, strings)
                    : mapExcelToMap(headers, initColumn, (Row) rowRaw);

            batch.add(rowMap);
            processed++;

            if (batch.size() >= 500) {
                bulkImportService.importData(formId, batch); // Вставка в БД
                notificationService.sendProgress(importId, processed, 0); // 0 если total неизвестен
                batch.clear();
            }
        }

        if (!batch.isEmpty()) {
            bulkImportService.importData(formId, batch);
        }
        notificationService.sendComplete(importId);
    }

    private Map<String, Object> mapCsvToMap(List<String> headers, int initColumn, String[] row) {
        Map<String, Object> map = new HashMap<>(headers.size());
        for (int i = 0; i < headers.size(); i++) {
            map.put(headers.get(i), i < row.length ? row[i + initColumn] : null);
        }
        return map;
    }

    private Map<String, Object> mapExcelToMap(List<String> headers, int initColumn, Row row) {
        Map<String, Object> map = new HashMap<>(headers.size());
        for (int i = initColumn; i < headers.size(); i++) {
            Cell cell = row.getCell(i + initColumn);
            map.put(headers.get(i), cell == null ? null : cell.getStringCellValue());
        }
        return map;
    }
}

