package com.web.aipictureslib.controller;

import com.web.aipictureslib.common.BaseResponse;
import com.web.aipictureslib.common.ResultUtils;
import com.web.aipictureslib.exception.ErrorCode;
import com.web.aipictureslib.exception.ThrowUtils;
import com.web.aipictureslib.model.VO.space.analyze.SpaceCategoryAnalyzeResponse;
import com.web.aipictureslib.model.VO.space.analyze.SpaceTagAnalyzeResponse;
import com.web.aipictureslib.model.VO.space.analyze.SpaceUsageAnalyzeResponse;
import com.web.aipictureslib.model.dto.space.analyze.SpaceCategoryAnalyzeRequest;
import com.web.aipictureslib.model.dto.space.analyze.SpaceTagAnalyzeRequest;
import com.web.aipictureslib.model.dto.space.analyze.SpaceUsageAnalyzeRequest;
import com.web.aipictureslib.model.entity.User;
import com.web.aipictureslib.service.SpaceAnalyzeService;
import com.web.aipictureslib.service.SpaceService;
import com.web.aipictureslib.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("/space/analyze")
public class SpaceAnalyzeController {
    @Resource
    private SpaceAnalyzeService spaceAnalyzeService;
    @Resource
    private UserService userService;
    @Autowired
    private HttpServletRequest request;


    /**
     * 获取空间使用情况
     * @param spaceUsageAnalyzeRequest
     * @return
     */
    @PostMapping("/usage")
    public BaseResponse<SpaceUsageAnalyzeResponse> getSpaceUsage(@RequestBody SpaceUsageAnalyzeRequest spaceUsageAnalyzeRequest) {
        ThrowUtils.throwIf(spaceUsageAnalyzeRequest == null, ErrorCode.PARAM_ERROR);
        User loginUser = userService.getLoginUser(request);
        SpaceUsageAnalyzeResponse response = spaceAnalyzeService.getSpaceUsageAnalyze(spaceUsageAnalyzeRequest, loginUser);
        return ResultUtils.success(response);
    }

    @PostMapping("/category")
    public BaseResponse<List<SpaceCategoryAnalyzeResponse>> getSpaceCategory(@RequestBody SpaceCategoryAnalyzeRequest spaceCategoryAnalyzeRequest) {
        ThrowUtils.throwIf(spaceCategoryAnalyzeRequest ==null, ErrorCode.PARAM_ERROR);
        User loginUser = userService.getLoginUser(request);
        List<SpaceCategoryAnalyzeResponse> responses = spaceAnalyzeService.getSpaceCategoryAnalyze(spaceCategoryAnalyzeRequest,loginUser);
        return ResultUtils.success(responses);
    }
    @PostMapping("/tags")
    public BaseResponse<List<SpaceTagAnalyzeResponse>> getSpaceCategory(@RequestBody SpaceTagAnalyzeRequest spaceTagAnalyzeRequest) {
        ThrowUtils.throwIf(spaceTagAnalyzeRequest ==null, ErrorCode.PARAM_ERROR);
        User loginUser = userService.getLoginUser(request);
        List<SpaceTagAnalyzeResponse> responses = spaceAnalyzeService.getSpaceTagAnalyze(spaceTagAnalyzeRequest,loginUser);
        return ResultUtils.success(responses);
    }
}