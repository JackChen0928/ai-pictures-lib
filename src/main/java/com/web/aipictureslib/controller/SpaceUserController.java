package com.web.aipictureslib.controller;

import cn.hutool.core.util.ObjUtil;
import cn.hutool.http.HttpRequest;
import com.web.aipictureslib.common.BaseResponse;
import com.web.aipictureslib.common.DeleteRequest;
import com.web.aipictureslib.common.ResultUtils;
import com.web.aipictureslib.exception.ErrorCode;
import com.web.aipictureslib.exception.ThrowUtils;
import com.web.aipictureslib.model.VO.SpaceUserVO;
import com.web.aipictureslib.model.VO.SpaceVO;
import com.web.aipictureslib.model.dto.spaceuser.SpaceUserAddRequest;
import com.web.aipictureslib.model.dto.spaceuser.SpaceUserEditRequest;
import com.web.aipictureslib.model.dto.spaceuser.SpaceUserQueryRequest;
import com.web.aipictureslib.model.entity.SpaceUser;
import com.web.aipictureslib.model.entity.User;
import com.web.aipictureslib.service.SpaceUserService;
import com.web.aipictureslib.service.UserService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.prefs.BackingStoreException;

@RestController
@RequestMapping("/spaceUser")
public class SpaceUserController {

    @Resource
    private SpaceUserService spaceUserService;
    @Resource
    private UserService userService;

    @Autowired
    HttpServletRequest request;


    /**
     * 创建团队空间成员
     *
     * @param spaceUserAddRequest
     * @return
     */
    @PostMapping("/add")
    public BaseResponse<Long> add(@RequestBody SpaceUserAddRequest spaceUserAddRequest) {
        Long id = spaceUserService.addSpaceUser(spaceUserAddRequest);
        return ResultUtils.success(id);
    }

    /**
     * 删除团队空间成员
     *
     * @param deleteRequest
     * @return
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> delete(@RequestBody DeleteRequest deleteRequest) {
        ThrowUtils.throwIf(deleteRequest.getId() <= 0 || ObjUtil.isEmpty(deleteRequest), ErrorCode.PARAM_ERROR);
        //确认用户确实存在
        Long id = deleteRequest.getId();
        SpaceUser spaceUser = spaceUserService.getById(id);
        ThrowUtils.throwIf(ObjUtil.isEmpty(spaceUser), ErrorCode.NOT_FOUND_ERROR);
        //删除
        boolean delete = spaceUserService.removeById(deleteRequest.getId());
        ThrowUtils.throwIf(!delete, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }


    /**
     * 查询某个成员在某个空间的信息
     *
     * @param spaceUserQueryRequest
     * @return
     */
    @PostMapping("/get")
    public BaseResponse<SpaceUser> getSpaceUser(@RequestBody SpaceUserQueryRequest spaceUserQueryRequest) {
        ThrowUtils.throwIf(ObjUtil.isNull(spaceUserQueryRequest), ErrorCode.PARAM_ERROR);
        Long userId = spaceUserQueryRequest.getId();
        Long spaceId = spaceUserQueryRequest.getSpaceId();
        ThrowUtils.throwIf(ObjUtil.isNull(userId) || ObjUtil.isNull(spaceId), ErrorCode.PARAM_ERROR);

        SpaceUser spaceUser = spaceUserService.getOne(spaceUserService.getQueryWrapper(spaceUserQueryRequest));
        ThrowUtils.throwIf(ObjUtil.isNull(spaceUser), ErrorCode.NOT_FOUND_ERROR);
        return ResultUtils.success(spaceUser);
    }

    /**
     * 获取空间成员列表
     *
     * @param spaceUserQueryRequest
     * @return
     */
    @PostMapping("/list")
    public BaseResponse<List<SpaceUserVO>> listSpaceUser(@RequestBody SpaceUserQueryRequest spaceUserQueryRequest) {
        ThrowUtils.throwIf(ObjUtil.isNull(spaceUserQueryRequest), ErrorCode.PARAM_ERROR);
        //先拿到未脱敏列表
        List<SpaceUser> spaceUserList = spaceUserService.list(spaceUserService.getQueryWrapper(spaceUserQueryRequest));
        //对列表脱敏
        List<SpaceUserVO> spaceUserVOList = spaceUserService.getSpaceUserVOList(spaceUserList);

        return ResultUtils.success(spaceUserVOList);
    }

    /**
     * 编辑团队空间用户信息
     *
     * @param spaceUserEditRequest
     * @return
     */
    @PostMapping("/edit")
    public BaseResponse<Boolean> editSpaceUser(@RequestBody SpaceUserEditRequest spaceUserEditRequest) {
        ThrowUtils.throwIf(ObjUtil.isNull(spaceUserEditRequest) || spaceUserEditRequest.getId() < 0, ErrorCode.PARAM_ERROR);
        //判断是否存在该用户
        Long id = spaceUserEditRequest.getId();
        SpaceUser spaceUser = spaceUserService.getById(id);
        ThrowUtils.throwIf(ObjUtil.isNull(spaceUser), ErrorCode.NOT_FOUND_ERROR);
        //更新用户的数据
        BeanUtils.copyProperties(spaceUserEditRequest, spaceUser);
        //校验（就是看一下各个参数是不是为空，为空就报错）
        spaceUserService.validSpaceUser(spaceUser, false);
        //更新数据库
        boolean result = spaceUserService.updateById(spaceUser);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 查询我加入的团队空间列表
     */
    @PostMapping("/list/my")
    public BaseResponse<List<SpaceUserVO>> listMyTeamSpace() {
        User loginUser = userService.getLoginUser(request);
        SpaceUserQueryRequest spaceUserQueryRequest = new SpaceUserQueryRequest();
        spaceUserQueryRequest.setUserId(loginUser.getId());
        // 查询条件只有当前登录用户id，查询所有加入的团队空间，其实就在space_user表中的userId那一列查询我的id
        List<SpaceUser> spaceUserList = spaceUserService.list(spaceUserService.getQueryWrapper(spaceUserQueryRequest));
        //返回脱敏列表
        return ResultUtils.success(spaceUserService.getSpaceUserVOList(spaceUserList));
    }
}
