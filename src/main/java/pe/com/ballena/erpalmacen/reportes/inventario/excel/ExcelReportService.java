package pe.com.ballena.erpalmacen.reportes.inventario.excel;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ExcelReportService {

    public byte[] generar(String sheetName, List<String> headers, List<List<Object>> rows) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet(sanitizeSheetName(sheetName));
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.size(); i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers.get(i));
                cell.setCellStyle(headerStyle);
            }

            for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
                Row row = sheet.createRow(rowIndex + 1);
                List<Object> values = rows.get(rowIndex);
                for (int columnIndex = 0; columnIndex < values.size(); columnIndex++) {
                    writeCell(row.createCell(columnIndex), values.get(columnIndex));
                }
            }

            for (int i = 0; i < headers.size(); i++) {
                sheet.autoSizeColumn(i);
            }
            workbook.write(outputStream);
            return outputStream.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo generar el archivo Excel", e);
        }
    }

    private void writeCell(Cell cell, Object value) {
        if (value == null) {
            cell.setBlank();
        } else if (value instanceof BigDecimal number) {
            cell.setCellValue(number.toPlainString());
        } else if (value instanceof Number number) {
            cell.setCellValue(number.doubleValue());
        } else if (value instanceof LocalDate date) {
            cell.setCellValue(date.toString());
        } else if (value instanceof LocalDateTime dateTime) {
            cell.setCellValue(dateTime.toString());
        } else {
            cell.setCellValue(String.valueOf(value));
        }
    }

    private String sanitizeSheetName(String sheetName) {
        String sanitized = sheetName.replaceAll("[\\\\/?*\\[\\]:]", " ").trim();
        if (sanitized.isBlank()) {
            return "Reporte";
        }
        return sanitized.length() > 31 ? sanitized.substring(0, 31) : sanitized;
    }
}
