package com.zhy.zhyBI.common;

import java.io.Serializable;
import lombok.Data;

/**
 * 删除请求
* @author <a href="https://github.com/dongzhy">程序员zhy</a>
 */
@Data
public class DeleteRequest implements Serializable {

    /**
     * id
     */
    private Long id;

    private static final long serialVersionUID = 1L;
}