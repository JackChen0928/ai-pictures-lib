package com.web.aipictureslib.model.dto.picture;

import com.web.aipictureslib.api.aliyunai.model.CreateOutPaintingTaskRequest;
import lombok.Data;

import java.io.Serializable;

/**
 * 接受前端传来的创建ai图片扩图任务请求
 *
 * @author chenzhou
 * @date 2023/05/05
 */
@Data
public class CreatePictureOutPaintingTaskRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 图片id
     */
    private Long pictureId;

    /**
     * 扩图扩图参数
     */
    private CreateOutPaintingTaskRequest.Parameters parameters;
}
