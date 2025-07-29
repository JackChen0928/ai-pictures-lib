package com.web.aipictureslib.model.dto.space;

import com.web.aipictureslib.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 分页查询空间请求
 */
@EqualsAndHashCode
@Data
public class SpaceQueryRequest extends PageRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    /**
     * 空间id
     */
    private Long id;
    /**
     * 用户id
     */
    private Long userId;

    /**
     * 空间等级 0-普通版 1-专业版 2-旗舰版
     */
    private Integer spaceLevel;

    /**
     * 空间类型：0-私有 1-团队
     */
    private Integer spaceType;


    /**
     * 空间名称
     */
    private String spaceName;

}
