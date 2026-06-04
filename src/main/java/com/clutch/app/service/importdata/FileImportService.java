package com.clutch.app.service.importdata;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class FileImportService {


    public List<Map<String, Object>> parseCsv(MultipartFile file) throws IOException, CsvException {
        try (CSVReader reader = new CSVReader(new InputStreamReader(file.getInputStream()))) {
            List<String[]> lines = reader.readAll();
            if (lines.isEmpty()) {
                throw new IllegalArgumentException("Empty file");
            }

            List<Map<String, Object>> result = new ArrayList<>();
            List<String> headers = new ArrayList<>();
            int headerRowIndex = -1;
            int firstDataColumn = -1;

            // 1. Ищем строку с заголовками
            for (int i = 0; i < lines.size(); i++) {
                String[] row = lines.get(i);
                // Ищем первый непустой элемент в строке
                for (int j = 0; j < row.length; j++) {
                    if (row[j] != null && !row[j].trim().isEmpty()) {
                        headerRowIndex = i;
                        firstDataColumn = j; // Запоминаем, где начались данные
                        break;
                    }
                }
                if (headerRowIndex != -1) break;
            }

            if (headerRowIndex == -1) return result; // Файл пуст

            // 2. Формируем список заголовков (начиная с первой значащей колонки)
            String[] headerRow = lines.get(headerRowIndex);
            for (int j = firstDataColumn; j < headerRow.length; j++) {
                headers.add(headerRow[j].trim());
            }

            // 3. Парсим данные
            for (int i = headerRowIndex + 1; i < lines.size(); i++) {
                String[] row = lines.get(i);
                Map<String, Object> dataMap = new LinkedHashMap<>();

                for (int j = 0; j < headers.size(); j++) {
                    int currentColumn = firstDataColumn + j;
                    if (currentColumn < row.length) {
                        String value = row[currentColumn].trim();
                        if (!value.isEmpty()) {
                            dataMap.put(headers.get(j), value);
                        }
                    }
                }

                if (!dataMap.isEmpty()) {
                    result.add(dataMap);
                }
            }

            return result;
        }
    }

    public List<Map<String, Object>> parseExcel(MultipartFile file) throws Exception {
        List<Map<String, Object>> result = new ArrayList<>();
        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {

            Sheet sheet = workbook.getSheetAt(0); // Берем первый лист
            DataFormatter formatter = new DataFormatter(); // Для корректного чтения чисел и дат

            int headerRowIndex = -1;
            int firstDataColumn = -1;

            // 1. Поиск начала таблицы (заголовков)
            for (Row row : sheet) {
                for (Cell cell : row) {
                    String val = formatter.formatCellValue(cell).trim();
                    if (!val.isEmpty()) {
                        headerRowIndex = row.getRowNum();
                        firstDataColumn = cell.getColumnIndex();
                        break;
                    }
                }
                if (headerRowIndex != -1) break;
            }

            if (headerRowIndex == -1) return result;

            // 2. Чтение заголовков
            Row headerRow = sheet.getRow(headerRowIndex);
            List<String> headers = new ArrayList<>();
            for (int j = firstDataColumn; j < headerRow.getLastCellNum(); j++) {
                headers.add(formatter.formatCellValue(headerRow.getCell(j)).trim());
            }

            // 3. Чтение данных
            for (int i = headerRowIndex + 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                Map<String, Object> dataMap = new LinkedHashMap<>();
                for (int j = 0; j < headers.size(); j++) {
                    Cell cell = row.getCell(firstDataColumn + j);
                    String value = formatter.formatCellValue(cell).trim();

                    // Отбрасываем пустые ячейки, как в условии
                    if (!value.isEmpty()) {
                        dataMap.put(headers.get(j), value);
                    }
                }

                if (!dataMap.isEmpty()) {
                    result.add(dataMap);
                }
            }

            workbook.close();
            return result;
        }
    }

}
