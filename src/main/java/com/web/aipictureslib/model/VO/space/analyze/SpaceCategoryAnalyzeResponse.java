package com.web.aipictureslib.model.VO.space.analyze;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 空间分类情况分析响应（将分析结果返回给前端）
 */
@NoArgsConstructor
@AllArgsConstructor
@Data
public class SpaceCategoryAnalyzeResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 图片分类
     */
    private String category;

    /**
     * 分类数量
     */
    private Long count;

    /**
     * 分类图片总大小
     */
    private Long totalSize;


}