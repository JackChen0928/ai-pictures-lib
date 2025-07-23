package com.web.aipictureslib.api.aliyunai.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class CreateOutPaintingTaskResponse {
    private Output output;
    private String request_id; // 请求唯一标识
    private String code; // 请求失败的错误码
    private String message; // 请求失败的详细信息

    @Data
    public static class Output {
        private String task_id; // 任务ID
        private String task_status; // 任务状态

    }
}

