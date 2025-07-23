package com.web.aipictureslib.api.aliyunai.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class GetOutPaintingTaskResponse {
    private String request_id;

    private Output output;

    @Data
    public static class Output {
        private String task_id; // 任务ID
        private String task_status; // 任务状态
        private TaskMetrics task_metrics; // 任务结果统计
        private String submit_time; // 任务提交时间
        private String end_time; // 任务完成时间
        private String output_image_url; // 输出图像URL地址
        private String code; // 请求失败的错误码
        private String message; // 请求失败的详细信息
        private String request_id; // 请求唯一标识
    }

    @Data
    public static class TaskMetrics {
        private Integer TOTAL; // 总任务数
        private Integer SUCCEEDED; // 成功任务数
        private Integer FAILED; // 失败任务数
    }

    @Data
    public static class Usage {
        private Integer image_count; // 生成图片数量
    }
}
