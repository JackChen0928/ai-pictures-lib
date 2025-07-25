package com.web.aipictureslib.model.VO.space.analyze;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 空间标签情况分析响应（将分析结果返回给前端）
 */
@NoArgsConstructor
@AllArgsConstructor
@Data
public class SpaceTagAnalyzeResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 标签分类
     */
    private String tag;

    /**
     * 标签数量
     */
    private Long count;



}