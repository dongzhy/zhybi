declare namespace API {
  type AiChartSimple = {
    goal?: string;
    id?: number;
    name?: string;
  };

  type BaseResponseBiResponse_ = {
    code?: number;
    data?: BiResponse;
    message?: string;
  };

  type BaseResponseBoardDTO_ = {
    code?: number;
    data?: BoardDTO;
    message?: string;
  };

  type BaseResponseBoolean_ = {
    code?: number;
    data?: boolean;
    message?: string;
  };

  type BaseResponseChart_ = {
    code?: number;
    data?: Chart;
    message?: string;
  };

  type BaseResponseListAiChartSimple_ = {
    code?: number;
    data?: AiChartSimple[];
    message?: string;
  };

  type BaseResponseListDataBoard_ = {
    code?: number;
    data?: DataBoard[];
    message?: string;
  };

  type BaseResponseListFileTablePreviewResp_ = {
    code?: number;
    data?: FileTablePreviewResp[];
    message?: string;
  };

  type BaseResponseListMapStringObject_ = {
    code?: number;
    data?: MapStringObject_[];
    message?: string;
  };

  type BaseResponseLoginUserVO_ = {
    code?: number;
    data?: LoginUserVO;
    message?: string;
  };

  type BaseResponseLong_ = {
    code?: number;
    data?: number;
    message?: string;
  };

  type BaseResponseMapStringObject_ = {
    code?: number;
    data?: Record<string, any>;
    message?: string;
  };

  type BaseResponsePageChart_ = {
    code?: number;
    data?: PageChart_;
    message?: string;
  };

  type BaseResponsePageUser_ = {
    code?: number;
    data?: PageUser_;
    message?: string;
  };

  type BaseResponsePageUserVO_ = {
    code?: number;
    data?: PageUserVO_;
    message?: string;
  };

  type BaseResponseShareVerifyRespDTO_ = {
    code?: number;
    data?: ShareVerifyRespDTO;
    message?: string;
  };

  type BaseResponseString_ = {
    code?: number;
    data?: string;
    message?: string;
  };

  type BaseResponseUser_ = {
    code?: number;
    data?: User;
    message?: string;
  };

  type BaseResponseUserVO_ = {
    code?: number;
    data?: UserVO;
    message?: string;
  };

  type BaseResponseVoid_ = {
    code?: number;
    message?: string;
  };

  type BiResponse = {
    chartId?: number;
    genChart?: string;
    genResult?: string;
  };

  type BoardChartDTO = {
    chartId?: number;
    chartTitle?: string;
    chartType?: string;
    genChart?: string;
    h?: number;
    itemKey?: string;
    minH?: number;
    minW?: number;
    w?: number;
    x?: number;
    y?: number;
  };

  type BoardDTO = {
    boardId?: number;
    boardName?: string;
    chartList?: BoardChartDTO[];
    isShared?: number;
  };

  type Chart = {
    chartData?: string;
    chartType?: string;
    createTime?: string;
    execMessage?: string;
    genChart?: string;
    genResult?: string;
    goal?: string;
    id?: number;
    isDelete?: number;
    isStar?: number;
    isTop?: number;
    name?: string;
    status?: string;
    updateTime?: string;
    userId?: number;
  };

  type ChartAddRequest = {
    chartData?: string;
    chartType?: string;
    goal?: string;
    name?: string;
  };

  type ChartEditRequest = {
    chartData?: string;
    chartType?: string;
    goal?: string;
    id?: number;
    name?: string;
  };

  type ChartQueryRequest = {
    chartType?: string;
    current?: number;
    goal?: string;
    id?: number;
    isStar?: number;
    isTop?: number;
    name?: string;
    pageSize?: number;
    sortField?: string;
    sortOrder?: string;
    userId?: number;
  };

  type ChartStarRequest = {
    id?: number;
  };

  type ChartTopRequest = {
    id?: number;
  };

  type ChartUpdateRequest = {
    id?: number;
    name?: string;
  };

  type CreateShareDTO = {
    boardId?: number;
    expireTime?: string;
    sharePassword?: string;
  };

  type DataBoard = {
    boardName?: string;
    createTime?: string;
    id?: number;
    isDelete?: number;
    isShared?: number;
    updateTime?: string;
    userId?: number;
  };

  type deleteBoardUsingDELETEParams = {
    /** id */
    id: number;
  };

  type DeleteRequest = {
    id?: number;
  };

  type FileTablePreviewResp = {
    fileName?: string;
    headerList?: string[];
    rowDataList?: Record<string, any>[][];
    totalRows?: number;
  };

  type genChartAiAsyncMqUsingPOSTParams = {
    chartType?: string;
    goal?: string;
    name?: string;
  };

  type genChartAiAsyncUsingPOSTParams = {
    chartType?: string;
    goal?: string;
    name?: string;
  };

  type genChartAiUsingPOSTParams = {
    chartType?: string;
    goal?: string;
    name?: string;
  };

  type getBoardInfoUsingGETParams = {
    /** id */
    id: number;
  };

  type getChartUsingGETParams = {
    /** id */
    id: number;
  };

  type getChartVOByIdUsingGETParams = {
    /** id */
    id?: number;
  };

  type getUserByIdUsingGETParams = {
    /** id */
    id?: number;
  };

  type getUserVOByIdUsingGETParams = {
    /** id */
    id?: number;
  };

  type LoginUserVO = {
    createTime?: string;
    id?: number;
    updateTime?: string;
    userAvatar?: string;
    userName?: string;
    userProfile?: string;
    userRole?: string;
  };

  type MapStringObject_ = true;

  type multiAnalyzeUsingPOSTParams = {
    creationTime?: number;
    id?: string;
    lastAccessedTime?: number;
    maxInactiveInterval?: number;
    new?: boolean;
    "servletContext.classLoader"?: any;
    "servletContext.contextPath"?: string;
    "servletContext.defaultSessionTrackingModes"?: "COOKIE" | "URL" | "SSL";
    "servletContext.effectiveMajorVersion"?: number;
    "servletContext.effectiveMinorVersion"?: number;
    "servletContext.effectiveSessionTrackingModes"?: "COOKIE" | "URL" | "SSL";
    "servletContext.jspConfigDescriptor.jspPropertyGroups[0].buffer"?: string;
    "servletContext.jspConfigDescriptor.jspPropertyGroups[0].defaultContentType"?: string;
    "servletContext.jspConfigDescriptor.jspPropertyGroups[0].deferredSyntaxAllowedAsLiteral"?: string;
    "servletContext.jspConfigDescriptor.jspPropertyGroups[0].elIgnored"?: string;
    "servletContext.jspConfigDescriptor.jspPropertyGroups[0].errorOnUndeclaredNamespace"?: string;
    "servletContext.jspConfigDescriptor.jspPropertyGroups[0].includeCodas"?: string[];
    "servletContext.jspConfigDescriptor.jspPropertyGroups[0].includePreludes"?: string[];
    "servletContext.jspConfigDescriptor.jspPropertyGroups[0].isXml"?: string;
    "servletContext.jspConfigDescriptor.jspPropertyGroups[0].pageEncoding"?: string;
    "servletContext.jspConfigDescriptor.jspPropertyGroups[0].scriptingInvalid"?: string;
    "servletContext.jspConfigDescriptor.jspPropertyGroups[0].trimDirectiveWhitespaces"?: string;
    "servletContext.jspConfigDescriptor.jspPropertyGroups[0].urlPatterns"?: string[];
    "servletContext.jspConfigDescriptor.taglibs[0].taglibLocation"?: string;
    "servletContext.jspConfigDescriptor.taglibs[0].taglibURI"?: string;
    "servletContext.majorVersion"?: number;
    "servletContext.minorVersion"?: number;
    "servletContext.requestCharacterEncoding"?: string;
    "servletContext.responseCharacterEncoding"?: string;
    "servletContext.serverInfo"?: string;
    "servletContext.servletContextName"?: string;
    "servletContext.sessionCookieConfig.comment"?: string;
    "servletContext.sessionCookieConfig.domain"?: string;
    "servletContext.sessionCookieConfig.httpOnly"?: boolean;
    "servletContext.sessionCookieConfig.maxAge"?: number;
    "servletContext.sessionCookieConfig.name"?: string;
    "servletContext.sessionCookieConfig.path"?: string;
    "servletContext.sessionCookieConfig.secure"?: boolean;
    "servletContext.sessionTimeout"?: number;
    "servletContext.virtualServerName"?: string;
    valueNames?: string[];
  };

  type MultiFileAnalyzeDTO = {
    chartName?: string;
    goal?: string;
    selectConfigMap?: Record<string, any>;
  };

  type OrderItem = {
    asc?: boolean;
    column?: string;
  };

  type PageChart_ = {
    countId?: string;
    current?: number;
    maxLimit?: number;
    optimizeCountSql?: boolean;
    orders?: OrderItem[];
    pages?: number;
    records?: Chart[];
    searchCount?: boolean;
    size?: number;
    total?: number;
  };

  type PageUser_ = {
    countId?: string;
    current?: number;
    maxLimit?: number;
    optimizeCountSql?: boolean;
    orders?: OrderItem[];
    pages?: number;
    records?: User[];
    searchCount?: boolean;
    size?: number;
    total?: number;
  };

  type PageUserVO_ = {
    countId?: string;
    current?: number;
    maxLimit?: number;
    optimizeCountSql?: boolean;
    orders?: OrderItem[];
    pages?: number;
    records?: UserVO[];
    searchCount?: boolean;
    size?: number;
    total?: number;
  };

  type previewTableUsingPOSTParams = {
    creationTime?: number;
    id?: string;
    lastAccessedTime?: number;
    maxInactiveInterval?: number;
    new?: boolean;
    "servletContext.classLoader"?: any;
    "servletContext.contextPath"?: string;
    "servletContext.defaultSessionTrackingModes"?: "COOKIE" | "URL" | "SSL";
    "servletContext.effectiveMajorVersion"?: number;
    "servletContext.effectiveMinorVersion"?: number;
    "servletContext.effectiveSessionTrackingModes"?: "COOKIE" | "URL" | "SSL";
    "servletContext.jspConfigDescriptor.jspPropertyGroups[0].buffer"?: string;
    "servletContext.jspConfigDescriptor.jspPropertyGroups[0].defaultContentType"?: string;
    "servletContext.jspConfigDescriptor.jspPropertyGroups[0].deferredSyntaxAllowedAsLiteral"?: string;
    "servletContext.jspConfigDescriptor.jspPropertyGroups[0].elIgnored"?: string;
    "servletContext.jspConfigDescriptor.jspPropertyGroups[0].errorOnUndeclaredNamespace"?: string;
    "servletContext.jspConfigDescriptor.jspPropertyGroups[0].includeCodas"?: string[];
    "servletContext.jspConfigDescriptor.jspPropertyGroups[0].includePreludes"?: string[];
    "servletContext.jspConfigDescriptor.jspPropertyGroups[0].isXml"?: string;
    "servletContext.jspConfigDescriptor.jspPropertyGroups[0].pageEncoding"?: string;
    "servletContext.jspConfigDescriptor.jspPropertyGroups[0].scriptingInvalid"?: string;
    "servletContext.jspConfigDescriptor.jspPropertyGroups[0].trimDirectiveWhitespaces"?: string;
    "servletContext.jspConfigDescriptor.jspPropertyGroups[0].urlPatterns"?: string[];
    "servletContext.jspConfigDescriptor.taglibs[0].taglibLocation"?: string;
    "servletContext.jspConfigDescriptor.taglibs[0].taglibURI"?: string;
    "servletContext.majorVersion"?: number;
    "servletContext.minorVersion"?: number;
    "servletContext.requestCharacterEncoding"?: string;
    "servletContext.responseCharacterEncoding"?: string;
    "servletContext.serverInfo"?: string;
    "servletContext.servletContextName"?: string;
    "servletContext.sessionCookieConfig.comment"?: string;
    "servletContext.sessionCookieConfig.domain"?: string;
    "servletContext.sessionCookieConfig.httpOnly"?: boolean;
    "servletContext.sessionCookieConfig.maxAge"?: number;
    "servletContext.sessionCookieConfig.name"?: string;
    "servletContext.sessionCookieConfig.path"?: string;
    "servletContext.sessionCookieConfig.secure"?: boolean;
    "servletContext.sessionTimeout"?: number;
    "servletContext.virtualServerName"?: string;
    valueNames?: string[];
  };

  type reGenChartUsingPOSTParams = {
    /** id */
    id: number;
  };

  type SharePwdDTO = {
    inputPwd?: string;
    shareCode?: string;
  };

  type ShareVerifyRespDTO = {
    boardInfo?: BoardDTO;
    needPassword?: boolean;
  };

  type TableSelectConfig = {
    selectAll?: boolean;
    selectColumns?: string[];
    selectRowIndexes?: number[];
  };

  type uploadFileUsingPOSTParams = {
    biz?: string;
  };

  type User = {
    createTime?: string;
    id?: number;
    isDelete?: number;
    updateTime?: string;
    userAccount?: string;
    userAvatar?: string;
    userName?: string;
    userPassword?: string;
    userRole?: string;
  };

  type UserAddRequest = {
    userAccount?: string;
    userAvatar?: string;
    userName?: string;
    userRole?: string;
  };

  type UserLoginRequest = {
    userAccount?: string;
    userPassword?: string;
  };

  type UserQueryRequest = {
    current?: number;
    id?: number;
    mpOpenId?: string;
    pageSize?: number;
    sortField?: string;
    sortOrder?: string;
    unionId?: string;
    userName?: string;
    userProfile?: string;
    userRole?: string;
  };

  type UserRegisterRequest = {
    checkPassword?: string;
    userAccount?: string;
    userPassword?: string;
  };

  type UserUpdateMyRequest = {
    userAvatar?: string;
    userName?: string;
    userProfile?: string;
  };

  type UserUpdateRequest = {
    id?: number;
    userAvatar?: string;
    userName?: string;
    userProfile?: string;
    userRole?: string;
  };

  type UserVO = {
    createTime?: string;
    id?: number;
    userAvatar?: string;
    userName?: string;
    userProfile?: string;
    userRole?: string;
  };

  type verifyShareUsingGETParams = {
    /** shareCode */
    shareCode: string;
  };
}
