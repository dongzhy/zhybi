// @ts-ignore
/* eslint-disable */
import { request } from "@umijs/max";

/** getChart GET /api/smart/get/${param0} */
export async function getChartUsingGet(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.getChartUsingGETParams,
  options?: { [key: string]: any }
) {
  const { id: param0, ...queryParams } = params;
  return request<API.BaseResponseChart_>(`/api/smart/get/${param0}`, {
    method: "GET",
    params: { ...queryParams },
    ...(options || {}),
  });
}

/** multiAnalyze POST /api/smart/multi-analyze */
export async function multiAnalyzeUsingPost(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.multiAnalyzeUsingPOSTParams,
  body: API.MultiFileAnalyzeDTO,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseLong_>("/api/smart/multi-analyze", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    params: {
      ...params,
    },
    data: body,
    ...(options || {}),
  });
}

/** previewTable POST /api/smart/previewTable */
export async function previewTableUsingPost(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.previewTableUsingPOSTParams,
  body: {
    /** files */
    files: any[];
  },
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseListFileTablePreviewResp_>(
    "/api/smart/previewTable",
    {
      method: "POST",
      headers: {
        "Content-Type": "application/x-www-form-urlencoded",
      },
      params: {
        ...params,
      },
      data: body,
      ...(options || {}),
    }
  );
}
