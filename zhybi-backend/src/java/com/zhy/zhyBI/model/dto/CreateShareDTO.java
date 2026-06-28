package com.zhy.zhyBI.model.dto;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class CreateShareDTO {
    private Long boardId;
    private String sharePassword;
    private LocalDateTime expireTime;
}