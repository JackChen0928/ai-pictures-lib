package com.web.aipictureslib.api.aliyunai.model;

import lombok.Data;

import java.io.Serializable;

@Data
public class CreateOutPaintingTaskRequest implements Serializable {
    private String model = "image-out-painting"; // 模型名称，示例值：image-out-painting
    private Input input; // 输入图像信息
    private Parameters parameters; // 输出图像处理参数

    @Data
    public static class Input {
        private String image_url; // 图像URL地址或Base64数据
    }

    @Data
    public static class Parameters {
        private Integer angle; // 逆时针旋转角度，默认0
        private String output_ratio; // 图像宽高比，默认空
        private Float x_scale; // 水平扩展比例，默认1.0
        private Float y_scale; // 垂直扩展比例，默认1.0
        private Integer top_offset; // 上方添加像素，默认0
        private Integer bottom_offset; // 下方添加像素，默认0
        private Integer left_offset; // 左侧添加像素，默认0
        private Integer right_offset; // 右侧添加像素，默认0
        private Boolean best_quality; // 最佳质量模式，默认false
        private Boolean limit_image_size; // 限制图像大小，默认true
        private Boolean add_watermark; // 添加水印，默认true
    }
}
