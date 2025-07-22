package com.web.aipictureslib.model.dto.picture;

import lombok.Data;

import java.io.Serializable;

@Data
public class PictureUploadRequest implements Serializable {
  
    /**  
     * 图片 id（用于修改）
     */  
    private Long id;
    /**
     * 图片上传url
     */
    private String fileUrl;
    /**
     * 图片上传名称
     */
    private String picName;

    /**
     * 图片空间id
     */
    private Long spaceId;

    private static final long serialVersionUID = 1L;  
}
