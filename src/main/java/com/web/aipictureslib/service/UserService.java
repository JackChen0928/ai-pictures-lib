package com.web.aipictureslib.service;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.web.aipictureslib.model.VO.LoginUserVO;
import com.web.aipictureslib.model.VO.UserVO;
import com.web.aipictureslib.model.dto.user.UserQueryRequest;
import com.web.aipictureslib.model.entity.User;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * @author czj24
 * @description 针对表【user(用户)】的数据库操作Service
 * @createDate 2025-07-09 13:05:50
 */
public interface UserService extends IService<User> {

    long userRegister(String userAccount, String userPassword, String checkPassword);

    /**
     * 用户登录
     *
     * @param userAccount  用户账户
     * @param userPassword 用户密码
     * @param request
     * @return 脱敏后的用户信息
     */
    LoginUserVO userLogin(String userAccount, String userPassword, HttpServletRequest request);

    /**
     * 获取当前登录用户
     *
     * @param request
     * @return
     */
    User getLoginUser(HttpServletRequest request);

    /**
     * 获取脱敏后登录用户的信息
     *
     * @param user
     * @return
     */
    LoginUserVO getLoginUserVO(User user);

    /**
     * 用户注销
     *
     * @param request
     * @return
     */
    boolean userlogout(HttpServletRequest request);

    /**
     * 获取脱敏后的单个用户信息
     *
     * @param user
     * @return
     */
    UserVO getUserVO(User user);

    /**
     * 批量获取脱敏后的用户列表
     *
     * @param userList
     * @return
     */
    List<UserVO> getUserVOList(List<User> userList);

    /**
     * 获取查询条件
     *
     * @param userQueryRequest
     * @return
     */
    QueryWrapper<User> getQueryWrapper(UserQueryRequest userQueryRequest);

    /**
     * 获取加密密码
     *
     * @param userPassword
     * @return
     */
    String getEncryptPassword(String userPassword);
}
