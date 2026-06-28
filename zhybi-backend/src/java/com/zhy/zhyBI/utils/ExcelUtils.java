package com.zhy.zhyBI.utils;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.support.ExcelTypeEnum;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class ExcelUtils {

    /**
     * 将Excel文件转换为CSV格式字符串
     * @param multipartFile 上传的Excel文件
     * @return CSV格式的字符串（含表头）
     */
    public static String excelToCsv(MultipartFile multipartFile) {
        try {
            // 1. 读取表头（第一行）
            List<Object> headerList = EasyExcel.read(multipartFile.getInputStream())
                    .excelType(ExcelTypeEnum.XLSX)
                    .sheet()
                    .headRowNumber(0) // 指定第一行为表头
                    .doReadSync();

            if (CollectionUtils.isEmpty(headerList)) {
                return "";
            }

            // 强制转换表头为Map<Integer, String>
            Map<Integer, String> headerMap = (Map<Integer, String>) headerList.get(0);
            List<String> headers = headerMap.values().stream()
                    .filter(ObjectUtils::isNotEmpty)
                    .collect(Collectors.toList());

            // 2. 读取数据行（从第二行开始）
            List<Object> dataList = EasyExcel.read(multipartFile.getInputStream())
                    .excelType(ExcelTypeEnum.XLSX)
                    .sheet()
                    .headRowNumber(1) // 数据行从第1行开始（跳过表头）
                    .doReadSync();

            // 3. 构建CSV字符串
            StringBuilder csvBuilder = new StringBuilder();
            csvBuilder.append(StringUtils.join(headers, ",")).append("\n");

            for (Object dataObj : dataList) {
                // 强制转换数据行为Map<Integer, String>
                Map<Integer, String> dataMap = (Map<Integer, String>) dataObj;
                List<String> rowData = dataMap.values().stream()
                        .filter(ObjectUtils::isNotEmpty)
                        .map(str -> str.contains(",") ? "\"" + str + "\"" : str) // 处理含逗号的字段
                        .collect(Collectors.toList());
                csvBuilder.append(StringUtils.join(rowData, ",")).append("\n");
            }

            return csvBuilder.toString();
        } catch (IOException e) {
            log.error("Excel转CSV失败", e);
            throw new RuntimeException("Excel转CSV失败", e);
        }
    }

    /**
     * 读取Excel表头（第一行）
     */
    public static Set<String> readHeaders(MultipartFile file) {
        try {
            List<Object> headMaps = EasyExcel.read(file.getInputStream())
                    .sheet()
                    .headRowNumber(0) // 第一行为表头
                    .doReadSync()
                    .stream()
                    .limit(1) // 只取第一行
                    .collect(Collectors.toList());

            if (headMaps.isEmpty()) return Collections.emptySet();

            Set<String> headers = new LinkedHashSet<>();
            Map<Integer, String> headerMap = (Map<Integer, String>) headMaps.get(0);
            headerMap.values().stream().filter(Objects::nonNull).forEach(headers::add);
            return headers;
        } catch (IOException e) {
            log.error("读取Excel表头失败", e);
            throw new RuntimeException("解析Excel表头失败");
        }
    }

    /**
     * 将字符串按逗号切分为表头集合
     */
    public static Set<String> parseHeadersFromString(String headersStr) {
        if (StringUtils.isBlank(headersStr)) return Collections.emptySet();
        return Arrays.stream(headersStr.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());
    }
}
