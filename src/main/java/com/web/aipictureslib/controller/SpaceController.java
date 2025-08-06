package com.web.aipictureslib.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.web.aipictureslib.annotation.AuthCheck;
import com.web.aipictureslib.common.BaseResponse;
import com.web.aipictureslib.common.DeleteRequest;
import com.web.aipictureslib.common.ResultUtils;
import com.web.aipictureslib.constant.UserConstant;
import com.web.aipictureslib.exception.BusinessException;
import com.web.aipictureslib.exception.ErrorCode;
import com.web.aipictureslib.exception.ThrowUtils;
import com.web.aipictureslib.manager.auth.SpaceUserAuthManager;
import com.web.aipictureslib.model.dto.space.SpaceLevel;
import com.web.aipictureslib.model.VO.SpaceVO;
import com.web.aipictureslib.model.dto.space.*;
import com.web.aipictureslib.model.dto.space.SpaceUpdateRequest;
import com.web.aipictureslib.model.entity.Space;
import com.web.aipictureslib.model.entity.User;
import com.web.aipictureslib.model.enums.SpaceLevelEnum;
import com.web.aipictureslib.service.SpaceService;
import com.web.aipictureslib.service.UserService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/space")
public class SpaceController {
    @Resource
    private SpaceService spaceService;
    @Resource
    private UserService userService;
    @Autowired
    private HttpServletRequest request;
    @Resource
    private SpaceUserAuthManager spaceUserAuthManager;

    @PostMapping("/add")
    public BaseResponse<Long> addSpace(@RequestBody SpaceAddRequest spaceAddRequest) {
        ThrowUtils.throwIf(spaceAddRequest == null, ErrorCode.PARAM_ERROR);
        User loginUser = userService.getLoginUser(request);
        long newId = spaceService.addSpace(spaceAddRequest, loginUser);
        return ResultUtils.success(newId);
    }
    /**
     * 删除（管理员）
     *
     * @param deleteRequest
     * @return
     */
    @PostMapping("/delele")
    public BaseResponse<Boolean> deleteSpace(@RequestBody DeleteRequest deleteRequest) {
        User loginUser = userService.getLoginUser(request);
        Long id = deleteRequest.getId();
        if (id == null || id <= 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR);
        }
        //本人和管理员可删
        Space oldspace = spaceService.getById(id);
        ThrowUtils.throwIf(oldspace == null, ErrorCode.NOT_FOUND_ERROR);
        //如果不是管理员或者不是本人的空间，不能删
        spaceService.checkSpaceAuth(loginUser, oldspace);
        boolean result = spaceService.removeById(id);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }
    /**
     * 更新（管理员）
     *
     * @param spaceUpdateRequest
     * @return
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateSpace(@RequestBody SpaceUpdateRequest spaceUpdateRequest){
       ThrowUtils.throwIf(spaceUpdateRequest == null, ErrorCode.PARAM_ERROR);
       Long id = spaceUpdateRequest.getId();
       ThrowUtils.throwIf(id <= 0, ErrorCode.PARAM_ERROR);
       //新建一个space对象，把新的数据存进去这个对象
       Space space = new Space();
       BeanUtils.copyProperties(spaceUpdateRequest, space);
       //自动填充数据
        spaceService.fillSpaceBySpaceLevel(space);
       //验证数据是否合法
       spaceService.validSpace(space,false);
       //判断是否存在该空间
       Space oldSpace = spaceService.getById(id);
       ThrowUtils.throwIf(oldSpace == null, ErrorCode.NOT_FOUND_ERROR);
       //存在该照片，把之前存起来的新的数据存进去数据库（操作数据库）
       boolean result = spaceService.updateById(space);
       ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
       return ResultUtils.success(true);
    }

    @PostMapping("/edit")
    public BaseResponse<Boolean> editSpace(@RequestBody SpaceEditRequest spaceEditRequest){
        ThrowUtils.throwIf(spaceEditRequest == null, ErrorCode.PARAM_ERROR);
        Long id = spaceEditRequest.getId();
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAM_ERROR);
        //新建一个space对象，把新的数据存进去这个对象
        Space space = new Space();
        BeanUtils.copyProperties(spaceEditRequest, space);
        //自动填充数据
        spaceService.fillSpaceBySpaceLevel(space);
        space.setEditTime(new Date());
        //验证数据是否合法
        spaceService.validSpace(space,false);
        //判断是否存在该空间
        Space oldSpace = spaceService.getById(id);
        ThrowUtils.throwIf(oldSpace == null, ErrorCode.NOT_FOUND_ERROR);
        //仅本人或管理员可以编辑
        User loginUser = userService.getLoginUser(request);
        spaceService.checkSpaceAuth(loginUser, oldSpace);
        //存在该照片，把之前存起来的新的数据存进去数据库
        boolean result = spaceService.updateById(space);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);

        }

    /**
     * 根据id获取单张空间（管理员）（数据不脱敏）
     * @param id
     * @return
     */
    @GetMapping("/get")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Space> getSpaceById(Long id) {
       ThrowUtils.throwIf(id <= 0, ErrorCode.PARAM_ERROR);
       Space space = spaceService.getById(id);
       ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR);
       return ResultUtils.success(space);
    }
    /**
     * 根据id获取单张空间（用户）（数据脱敏）
     * @param id
     * @return
     */
    @GetMapping("/get/vo")
    public BaseResponse<SpaceVO> getSpaceVOById(long id) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAM_ERROR);
        // 查询数据库
        Space space = spaceService.getById(id);
        ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR);
        SpaceVO spaceVO = spaceService.getSpaceVO(space);
        User loginUser = userService.getLoginUser(request);
        List<String> permissionList = spaceUserAuthManager.getPermissionList(space, loginUser);
        spaceVO.setPermissionList(permissionList);
        // 获取封装类
        return ResultUtils.success(spaceVO);
    }


    /**
     * 分页获取空间列表（仅管理员）
     * @param spaceQueryRequest
     * @return
     */
    @PostMapping("/list/page")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<Space>> listSpaceByPage(@RequestBody SpaceQueryRequest spaceQueryRequest) {
        long current = spaceQueryRequest.getCurrent();
        long size = spaceQueryRequest.getPageSize();
        Page<Space> spacePage = spaceService.page(new Page<>(current, size), spaceService.getQueryWrapper(spaceQueryRequest));
        return ResultUtils.success(spacePage);
    }

    /**
     * 分页获取空间列表（用户）（脱敏）
     * @param spaceQueryRequest
     * @return
     */
    @PostMapping("/list/page/vo")
    public BaseResponse<Page<SpaceVO>> listSpaceVOByPage(@RequestBody SpaceQueryRequest spaceQueryRequest) {
        long current = spaceQueryRequest.getCurrent();
        long size = spaceQueryRequest.getPageSize();
        //限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAM_ERROR);
        //先查询数据库获得不脱敏的数据页
        Page<Space> spacePage = spaceService.page(new Page<>(current, size),spaceService.getQueryWrapper(spaceQueryRequest));
        //通过不脱敏的数据页，再获得脱敏的数据页
        Page<SpaceVO> spaceVOPage = spaceService.getSpaceVOPage(spacePage, request);
        return ResultUtils.success(spaceVOPage);
    }
    /**
     * 列出空间级别信息
     *
     * 本方法通过遍历SpaceLevelEnum枚举，来构建一个包含所有空间级别信息的列表
     * 使用Java 8的Stream API对枚举进行处理，以简化集合的构建过程
     *
     * @return BaseResponse<List<SpaceLevel>> 返回一个包含空间级别列表的响应对象
     */
    @GetMapping("/list/level")
    public BaseResponse<List<SpaceLevel>> listSpaceLevel() {
        // 使用Java 8 Stream API获取并处理所有空间级别枚举
        List<SpaceLevel> spaceLevelList = Arrays.stream(SpaceLevelEnum.values()) // 获取所有枚举
                .map(spaceLevelEnum -> new SpaceLevel(spaceLevelEnum.getValue(), spaceLevelEnum.getText(), spaceLevelEnum.getMaxCount(), spaceLevelEnum.getMaxSize()))
                .collect(Collectors.toList());
        // 返回成功响应，包含空间级别列表
        return ResultUtils.success(spaceLevelList);
    }


}