package com.web.aipictureslib.api.aliyunai;

import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.URLUtil;
import cn.hutool.http.ContentType;
import cn.hutool.http.Header;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONUtil;
import com.web.aipictureslib.api.aliyunai.model.CreateOutPaintingTaskRequest;
import com.web.aipictureslib.api.aliyunai.model.CreateOutPaintingTaskResponse;
import com.web.aipictureslib.api.aliyunai.model.GetOutPaintingTaskResponse;
import com.web.aipictureslib.exception.BusinessException;
import com.web.aipictureslib.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class AliYunAIApi {
    @Value("${aliYunAi.apiKey}")
    private String apikey;

    // 创建扩图任务 POST请求
    public static final String CREATE_OUT_PAINTING_TASK_URL = "https://dashscope.aliyuncs.com/api/v1/services/aigc/image2image/out-painting";
    // 获取扩图任务 GET请求
    public static final String GET_OUT_PAINTING_TASK_URL = "https://dashscope.aliyuncs.com/api/v1/tasks/%s";

    //步骤1：首先发送一个请求创建扩图任务，该请求会返回任务ID
    public CreateOutPaintingTaskResponse createOutPaintingTask(CreateOutPaintingTaskRequest request) {
        if (request == null)throw new RuntimeException("参数为空");
        //发送请求
        HttpRequest httpRequest = HttpRequest.post(CREATE_OUT_PAINTING_TASK_URL)
                .header(Header.AUTHORIZATION,"Bearer " + apikey)
                .header("X-DashScope-Async","enable")
                .header(Header.CONTENT_TYPE, "application/json")
                .body(JSONUtil.toJsonStr(request));
        //执行请求，获取响应
        try(HttpResponse httpResponse = httpRequest.execute()){
            if(!httpResponse.isOk()){
                log.error("AI扩图请求失败:{}", httpResponse.body());
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "AI扩图请求失败");
            }
            CreateOutPaintingTaskResponse response = JSONUtil.toBean(httpResponse.body(), CreateOutPaintingTaskResponse.class);
            String errorCode = response.getCode();
            if (StrUtil.isNotBlank(errorCode)){
                log.error("AI扩图请求失败errorCode:{},errorMessage:{}", errorCode, response.getMessage());
                throw new BusinessException(ErrorCode.OPERATION_ERROR,"AI扩图接口响应异常");
            }
            return response;
        }
    };

    //步骤2：根据任务ID查询结果
    public GetOutPaintingTaskResponse getOutPaintingTask(String taskId) {
        if (StrUtil.isBlank(taskId)) throw new BusinessException(ErrorCode.PARAM_ERROR,"参数不能为空");

        // 对 taskId 进行 URL 编码
        String url = String.format(GET_OUT_PAINTING_TASK_URL,taskId);
        //发送请求
        HttpRequest httpRequest = HttpRequest.get(url)
                .header(Header.AUTHORIZATION,"Bearer " + apikey);
        //执行请求，获取响应
        try(HttpResponse httpResponse = httpRequest.execute()){
            if(!httpResponse.isOk()){
                log.error("AI扩图查询任务失败:{}", httpResponse.body());
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "AI扩图查询任务失败");
            }
            return JSONUtil.toBean(httpResponse.body(), GetOutPaintingTaskResponse.class);
        }
    };
}
