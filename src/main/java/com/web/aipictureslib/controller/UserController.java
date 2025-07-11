package com.web.aipictureslib.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.web.aipictureslib.annotation.AuthCheck;
import com.web.aipictureslib.common.BaseResponse;
import com.web.aipictureslib.common.DeleteRequest;
import com.web.aipictureslib.common.ResultUtils;
import com.web.aipictureslib.exception.ErrorCode;
import com.web.aipictureslib.exception.ThrowUtils;
import com.web.aipictureslib.model.VO.LoginUserVO;
import com.web.aipictureslib.model.VO.UserVO;
import com.web.aipictureslib.model.dto.user.*;
import com.web.aipictureslib.model.entity.User;
import com.web.aipictureslib.service.UserService;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("/user")
public class UserController {

    @Resource
    private  UserService userService;
    /**
     * 注册
     * @param userRegisterRequest
     * @return
     */
    @PostMapping("/register")
    public BaseResponse <Long> register(@RequestBody UserRegisterRequest userRegisterRequest){
        // 1.校验
        ThrowUtils.throwIf(userRegisterRequest == null, ErrorCode.PARAM_ERROR);
        //2. 获取参数，传入 服务层service
        String userAccount = userRegisterRequest.getUserAccount();
        String userPassword = userRegisterRequest.getUserPassword();
        String checkPassword = userRegisterRequest.getCheckPassword();

        long result = userService.userRegister(userAccount, userPassword, checkPassword);
        return ResultUtils.success(result);
    }

    /**
     * 登录
     * @param userLoginRequest
     * @param request
     * @return
     */
    @PostMapping("/login")
    public BaseResponse<LoginUserVO> login(@RequestBody UserLoginRequest userLoginRequest, HttpServletRequest request){
        // 1.校验
        ThrowUtils.throwIf(userLoginRequest == null, ErrorCode.PARAM_ERROR);
        String userAccount = userLoginRequest.getUserAccount();
        String userPassword = userLoginRequest.getUserPassword();
        LoginUserVO result = userService.userLogin(userAccount, userPassword, request);
        return ResultUtils.success(result);
    }
    /**
     * 获取当前登录用户
     * @param request
     * @return
     */
    @GetMapping("/get/login")
    public BaseResponse<LoginUserVO> getLoginUser(HttpServletRequest request){
        User loginUser = userService.getLoginUser(request);
        return ResultUtils.success(userService.getLoginUserVO(loginUser));
    }
    /**
     * 登出
     * @param request
     * @return
     */
    @PostMapping("/logout")
    public BaseResponse<Boolean> logout(HttpServletRequest request){
        //先对传来的信息进行校验是否为空
        ThrowUtils.throwIf(request == null, ErrorCode.PARAM_ERROR);
        //调用service层方法，返回一个boolean值
        boolean res = userService.userlogout(request);
        return ResultUtils.success(res);
    }
    /**
     * 添加用户（管理员）
     * @param userAddRequest
     * @return
     */
    @PostMapping("/add")
    @AuthCheck(mustRole = "admin")
    public BaseResponse<Long> addUser(@RequestBody UserAddRequest userAddRequest){
        // 1. 校验
        ThrowUtils.throwIf(userAddRequest == null, ErrorCode.PARAM_ERROR);
        User user = new User();
        BeanUtils.copyProperties(userAddRequest, user);
        //由于新账户没有密码，所以手动添加一个（注意是要加密的）
        String userPassword = "12345678";
        String encryptPassword = userService.getEncryptPassword(userPassword);
        user.setUserPassword(encryptPassword);
        //2. 保存到数据库
        boolean result = userService.save(user);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(user.getId());
    }
    /**
     * 删除用户（管理员）
     * @param deleteRequest
     * @return
     */
    @PostMapping("/delete")
    @AuthCheck(mustRole = "admin")
    public BaseResponse<Boolean> deleteUser(@RequestBody DeleteRequest deleteRequest){
        if (deleteRequest == null || deleteRequest.getId() <= 0){
            throw new RuntimeException("请求参数错误");
        }
        //调用MP中写好的方法
        boolean result = userService.removeById(deleteRequest.getId());
        return ResultUtils.success(result);
    }
    /**
     * 更新用户信息（管理员）
     * @param userUpdateRequest
     * @return
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = "admin")
    public BaseResponse<Boolean> updateUser(@RequestBody UserUpdateRequest userUpdateRequest){
        if (userUpdateRequest == null || userUpdateRequest.getId() == null){
            throw new RuntimeException("请求参数错误");
        }
        User user = new User();
        BeanUtils.copyProperties(userUpdateRequest, user);
        boolean result = userService.updateById(user);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(result);
    }
    /**
     * 根据id查询用户（管理员）
     * @param id
     * @return
     */
    @GetMapping("/get")
    @AuthCheck(mustRole = "admin")
    public BaseResponse<User> getUserById(long id){
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAM_ERROR);
        User user = userService.getById(id);
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_FOUND_ERROR);
        return ResultUtils.success(user);
    }
    /**
     * 根据id查询脱敏后的用户（用户）
     * @param id
     * @return
     */
    @GetMapping("/get/vo")
    public BaseResponse<UserVO> getUserVOById(long id){
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAM_ERROR);
        User user = userService.getById(id);
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_FOUND_ERROR);
        UserVO userVO = userService.getUserVO(user);
        return ResultUtils.success(userVO);
    }
    /**
     * 分页获取脱敏后的用户列表（管理员）
     * @return
     */
    @PostMapping("/list/page/vo")
    @AuthCheck(mustRole = "admin")
    public BaseResponse<Page<UserVO>> listUserVOByPage(@RequestBody UserQueryRequest userQueryRequest){
        ThrowUtils.throwIf(userQueryRequest == null, ErrorCode.PARAM_ERROR);
        long current = userQueryRequest.getCurrent();
        long pageSize = userQueryRequest.getPageSize();
        //用户的分页对象
        Page <User> userPage = userService.page(new Page<>(current, pageSize),
                userService.getQueryWrapper(userQueryRequest));
       //脱敏用户的分页对象total是总数
        Page<UserVO> userVOPage = new Page<>(current, pageSize, userPage.getTotal());
        //根据每一页展示多少个用户，然后获取多少个脱敏后的用户列表，records是一组每一页展示的用户列表集合
        List<UserVO> userVOList = userService.getUserVOList(userPage.getRecords());
        userVOPage.setRecords(userVOList);
        return ResultUtils.success(userVOPage);
    }
}
