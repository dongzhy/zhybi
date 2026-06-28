// @ts-ignore
/* eslint-disable */
import { request } from "@umijs/max";

/** getBoardInfo GET /api/board/${param0} */
export async function getBoardInfoUsingGet(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.getBoardInfoUsingGETParams,
  options?: { [key: string]: any }
) {
  const { id: param0, ...queryParams } = params;
  return request<API.BaseResponseBoardDTO_>(`/api/board/${param0}`, {
    method: "GET",
    params: { ...queryParams },
    ...(options || {}),
  });
}

/** deleteBoard DELETE /api/board/${param0} */
export async function deleteBoardUsingDelete(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.deleteBoardUsingDELETEParams,
  options?: { [key: string]: any }
) {
  const { id: param0, ...queryParams } = params;
  return request<API.BaseResponseVoid_>(`/api/board/${param0}`, {
    method: "DELETE",
    params: { ...queryParams },
    ...(options || {}),
  });
}

/** getBoardList GET /api/board/list */
export async function getBoardListUsingGet(options?: { [key: string]: any }) {
  return request<API.BaseResponseListDataBoard_>("/api/board/list", {
    method: "GET",
    ...(options || {}),
  });
}

/** saveBoard POST /api/board/save */
export async function saveBoardUsingPost(
  body: API.BoardDTO,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseLong_>("/api/board/save", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}
