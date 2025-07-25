package com.web.aipictureslib.model.VO.space.analyze;

import lombok.Data;

import java.io.Serializable;

/**
 * 空间使用情况分析响应（将分析结果返回给前端）
 */
@Data
public class SpaceUsageAnalyzeResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 已使用的空间大小
     */
    private Long usedSize;

    /**
     * 空间最大容量
     */
    private Long maxSize;

    /**
     * 空间使用比例
     */
    private Double sizeUsageRatio;

    /**
     * 已使用的图片数量
     */
    private Long usedCount;

    /**
     * 图片最大数量
     */
    private Long maxCount;

    /**
     * 图片数量占比
     */
    private Double countUsageRatio;

}