// @ts-ignore
/* eslint-disable */
import { request } from "@umijs/max";

/** createShare POST /api/board/share/create */
export async function createShareUsingPost(
  body: API.CreateShareDTO,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseString_>("/api/board/share/create", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** verifyShare GET /api/board/share/verify */
export async function verifyShareUsingGet(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.verifyShareUsingGETParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseShareVerifyRespDTO_>(
    "/api/board/share/verify",
    {
      method: "GET",
      params: {
        ...params,
      },
      ...(options || {}),
    }
  );
}

/** verifyPassword POST /api/board/share/verifyPwd */
export async function verifyPasswordUsingPost(
  body: API.SharePwdDTO,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoardDTO_>("/api/board/share/verifyPwd", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}
