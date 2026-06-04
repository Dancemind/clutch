package com.clutch.app.service.importdata;

import com.clutch.app.service.notification.NotificationService;
import com.opencsv.CSVReader;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class FileCsvExcelImportService {

    private final BulkImportService bulkImportService;
    private final NotificationService notificationService;

    public void streamImportCsv(Long formId, String importId, MultipartFile file) {
        try (var reader = new CSVReader(new InputStreamReader(file.getInputStream()))) {
            String[] headers = reader.readNext(); // Заголовки
            int totalApprox = (int) (file.getSize() / 100); // Грубая оценка строк для прогресса

            List<Map<String, Object>> batch = new ArrayList<>();
            int processed = 0;

            for (String[] row : reader) {
                Map<String, Object> rowMap = mapToMap(headers, row);
                batch.add(rowMap);
                processed++;

                if (batch.size() >= 500) {
                    bulkImportService.importData(formId, importId, batch);
                    notificationService.sendProgress(importId, processed, totalApprox);
                    batch.clear();
                }
            }
            // Доливаем остаток
            if (!batch.isEmpty()) {
                bulkImportService.importData(formId, importId, batch);
            }
            notificationService.sendComplete(importId);
        } catch (Exception e) {
            notificationService.sendError(importId, e.getMessage());
        }
    }

//    public void streamImportExcel(Long formId, String importId, MultipartFile file) throws IOException {
//        try (InputStream is = file.getInputStream();
//             Workbook workbook = StreamingReader.builder()
//                     .rowCacheSize(100)
//                     .open(is)) {
//
//            Sheet sheet = workbook.getSheetAt(0);
//            Iterator<Row> rowIterator = sheet.iterator();
//
//            // Читаем заголовки
//            if (!rowIterator.hasNext()) return;
//            Row headerRow = rowIterator.next();
//            String[] headers = getHeadersFromRow(headerRow);
//
//            // Погнали по строкам...
//            while (rowIterator.hasNext()) {
//                Row row = rowIterator.next();
//                // Превращаем Row в Map и кидаем в батч
//            }
//        }
//    }

    private Map<String, Object> mapToMap(String[] headers, String[] row) {
        // Используем HashMap с начальной емкостью, чтобы избежать ресайзинга
        Map<String, Object> rowMap = new HashMap<>(headers.length);

        for (int i = 0; i < headers.length; i++) {
            // Защита от коротких строк в файле (если в строке меньше колонок, чем в заголовке)
            String value = (i < row.length) ? row[i] : null;

            // Кладем в мапу: ключ — имя колонки из файла (например, "price"),
            // значение — строка из файла
            rowMap.put(headers[i], value);
        }
        return rowMap;
    }

}
