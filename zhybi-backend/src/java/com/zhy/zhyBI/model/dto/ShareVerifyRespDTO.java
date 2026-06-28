package com.zhy.zhyBI.model.dto;
import lombok.Data;

@Data
public class ShareVerifyRespDTO {
    private Boolean needPassword;
    private BoardDTO boardInfo;
}