package com.zhy.zhyBI.utils;

import com.zhy.zhyBI.common.ErrorCode;
import com.zhy.zhyBI.exception.BusinessException;
import com.zhy.zhyBI.model.dto.chart.FileTablePreviewResp;
import com.zhy.zhyBI.model.dto.chart.TableSelectConfig;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class FileParseUtils {

    // 字节数组转输入流
    private static InputStream getStream(byte[] bytes) {
        return new ByteArrayInputStream(bytes);
    }

    // 预览解析（字节数组入参）
    public static FileTablePreviewResp parseExcelTablePreview(byte[] fileBytes, int maxPreviewRows) {
        FileTablePreviewResp resp = new FileTablePreviewResp();
        try (InputStream is = getStream(fileBytes);
             Workbook workbook = WorkbookFactory.create(is)) {

            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null) return resp;

            Row headerRow = sheet.getRow(0);
            List<String> headerList = new ArrayList<>();
            if (headerRow != null) {
                for (Cell cell : headerRow) {
                    headerList.add(getCellValue(cell));
                }
            }
            resp.setHeaderList(headerList);
            resp.setTotalRows(sheet.getLastRowNum() + 1);

            List<List<Object>> rowDataList = new ArrayList<>();
            int endRow = Math.min(sheet.getLastRowNum(), maxPreviewRows);
            for (int i = 1; i <= endRow; i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                List<Object> rowData = new ArrayList<>();
                for (Cell cell : row) {
                    rowData.add(getCellValue(cell));
                }
                rowDataList.add(rowData);
            }
            resp.setRowDataList(rowDataList);
            return resp;
        } catch (Exception e) {
            log.error("Excel预览解析失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "Excel解析失败");
        }
    }

    // 按勾选配置解析（字节数组入参）
    public static String parseFileBySelectConfig(byte[] fileBytes, TableSelectConfig config) {
        StringBuilder sb = new StringBuilder();
        try (InputStream is = getStream(fileBytes);
             Workbook workbook = WorkbookFactory.create(is)) {

            Sheet sheet = workbook.getSheetAt(0);
            Row headerRow = sheet.getRow(0);
            if (headerRow == null) return "";

            Map<String, Integer> columnIndexMap = new HashMap<>();
            for (int i = 0; i < headerRow.getLastCellNum(); i++) {
                Cell cell = headerRow.getCell(i);
                if (cell != null) {
                    columnIndexMap.put(getCellValue(cell), i);
                }
            }

            List<Integer> targetColumnIndexes = new ArrayList<>();
            if (config.isSelectAll()) {
                for (int i = 0; i < headerRow.getLastCellNum(); i++) {
                    targetColumnIndexes.add(i);
                }
            } else {
                for (String column : config.getSelectColumns()) {
                    Integer idx = columnIndexMap.get(column);
                    if (idx != null) {
                        targetColumnIndexes.add(idx);
                    }
                }
            }

            // 拼接表头
            for (int i = 0; i < targetColumnIndexes.size(); i++) {
                Cell cell = headerRow.getCell(targetColumnIndexes.get(i));
                sb.append(getCellValue(cell));
                if (i < targetColumnIndexes.size() - 1) sb.append(",");
            }
            sb.append("\n");

            // 拼接数据行
            int endRow = sheet.getLastRowNum();
            List<Integer> selectRows = config.getSelectRowIndexes();
            boolean allRow = config.isSelectAll() || (selectRows == null || selectRows.isEmpty());

            for (int i = 1; i <= endRow; i++) {
                if (!allRow && !selectRows.contains(i)) continue;
                Row row = sheet.getRow(i);
                if (row == null) continue;

                for (int j = 0; j < targetColumnIndexes.size(); j++) {
                    Cell cell = row.getCell(targetColumnIndexes.get(j));
                    sb.append(getCellValue(cell));
                    if (j < targetColumnIndexes.size() - 1) sb.append(",");
                }
                sb.append("\n");
            }
            return sb.toString();
        } catch (Exception e) {
            log.error("Excel数据解析失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "文件解析失败");
        }
    }

    // 单元格取值
    private static String getCellValue(Cell cell) {
        if (cell == null) return "";
        DataFormatter formatter = new DataFormatter();
        return formatter.formatCellValue(cell);
    }
}