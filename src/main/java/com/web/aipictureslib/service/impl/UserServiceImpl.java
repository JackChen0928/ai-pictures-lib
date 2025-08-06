package com.web.aipictureslib.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import com.web.aipictureslib.constant.UserConstant;
import com.web.aipictureslib.exception.BusinessException;
import com.web.aipictureslib.exception.ErrorCode;
import com.web.aipictureslib.manager.auth.StpKit;
import com.web.aipictureslib.model.VO.LoginUserVO;
import com.web.aipictureslib.model.VO.UserVO;
import com.web.aipictureslib.model.dto.user.UserQueryRequest;
import com.web.aipictureslib.model.entity.User;
import com.web.aipictureslib.model.enums.UserRoleEnum;
import com.web.aipictureslib.mapper.UserMapper;
import com.web.aipictureslib.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import javax.servlet.http.HttpServletRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.web.aipictureslib.constant.UserConstant.USER_LOGIN_STATE;

/**
* @author czj24
* @description 针对表【user(用户)】的数据库操作Service实现
* @createDate 2025-07-09 11:46:09
*/
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    public long userRegister(String userAccount, String userPassword, String checkPassword) {
        // 1. 校验
        //空值校验
        if (StrUtil.hasBlank(userAccount, userPassword, checkPassword)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "参数为空");
        }
        //长度校验
        if (userAccount.length() < 4) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "用户账号过短");
        }
        //密码校验
        if (userPassword.length() < 8 || checkPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "用户密码过短");
        }
        //密码一致性校验
        if (!userPassword.equals(checkPassword)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "两次输入的密码不一致");
        }

        //2.检查是否重复注册
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        //账号重复校验
        queryWrapper.eq("userAccount", userAccount);
        //调用mybatisplus的方法查询数据库中是否有该账号,并返回计数器
        long count = this.baseMapper.selectCount(queryWrapper);
        //如果计数器大于0，则说明该账号已存在，抛出异常
        if (count > 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "账号重复");
        }

        //到这一步之后，说明账号没有重复，可以注册
        //3. 对密码加密，然后插入数据库
        String encryptPassword = getEncryptPassword(userPassword);
        //4. 插入数据
        User user = new User();
        //账号，密码，用户名，用户角色      注册时默认用户名是none，用户角色是user
        user.setUserAccount(userAccount);
        user.setUserPassword(encryptPassword);
        user.setUserName("none");
        user.setUserRole(UserRoleEnum.User.getValue());
        //save保存到数据库
        boolean save = this.save(user);
        //插入失败，抛出异常
        if (!save) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "注册失败");
        }
        return user.getId();
    }

    @Override
    public LoginUserVO userLogin(String userAccount, String userPassword, HttpServletRequest request) {
        //1.空值校验
        if (StrUtil.hasBlank(userAccount, userPassword)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "参数为空");
        }
        //长度校验
        if (userAccount.length() < 4) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "账号错误");
        }
        //密码校验
        if (userPassword.length() < 8 ) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "密码错误");
        }
        //2.密码加密
        String encryptpassword = getEncryptPassword(userPassword);
        //3.查询用户是否存在
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        //查询条件：账号，密码，相当于SELECT * FROM user WHERE password = '加密后的密码' AND userAccount = '输入的账号';
        queryWrapper.eq("userPassword", encryptpassword).eq("userAccount", userAccount);
        //根据查询条件select一个用户
        User user = baseMapper.selectOne(queryWrapper);
        if (user == null) throw new BusinessException(ErrorCode.PARAM_ERROR, "用户不存在");
        //拿到请求，获取session，将user存入session
        request.getSession().setAttribute(USER_LOGIN_STATE, user);

        //4.记录用户登录态到Sa-Token 便于空间鉴权时使用，注意保证该用户信息与SpringSession中的用户过期信息一致
        StpKit.SPACE.login(user.getId());
        StpKit.SPACE.getSession().set(USER_LOGIN_STATE, user);

        return this.getLoginUserVO(user);
    }

    @Override
    public User getLoginUser(HttpServletRequest request) {
        //判断现在的账号是否登录了
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        User currentUser = (User) userObj;
        //没登录，抛出异常
        if (currentUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        //以下是已登录的情况
        //根据id查询用户
        long userId = currentUser.getId();
        currentUser = this.getById(userId);
        if (currentUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        return currentUser;
    }

    /**
     * 获取脱敏后的用户信息
     *
     * @param user
     * @return
     */
    public LoginUserVO getLoginUserVO(User user) {
        if (user == null)
            return null;
        LoginUserVO loginUserVO = new LoginUserVO();
        //返回脱敏后的用户信息,把user中的属性复制到VO，不存在的就过滤了
        BeanUtil.copyProperties(user, loginUserVO);
        return loginUserVO;
    }

    @Override
    public boolean userlogout(HttpServletRequest request) {
        //判断是否已登录（没登录就不用注销）
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        if (userObj == null) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "未登录");
        }
        //删除session中的用户信息
        request.getSession().removeAttribute(USER_LOGIN_STATE);
        return true;
    }

    @Override
    public UserVO getUserVO(User user) {
        if (user == null) return null;
        UserVO userVO = new UserVO();
        BeanUtil.copyProperties(user, userVO);
        return userVO;
    }

    @Override
    public List<UserVO> getUserVOList(List<User> userList) {
        if (userList == null) return new ArrayList<>();
        return userList.stream().map(this::getUserVO).collect(Collectors.toList());
    }

    @Override
    public QueryWrapper<User> getQueryWrapper(UserQueryRequest userQueryRequest) {
        //条件为空的话，直接返回空的QueryWrapper
        if (userQueryRequest == null) throw new BusinessException(ErrorCode.PARAM_ERROR, "参数为空");
        //把请求中的参数拿出来
        Long id = userQueryRequest.getId();
        String userName = userQueryRequest.getUserName();
        String userAccount = userQueryRequest.getUserAccount();
        String userProfile = userQueryRequest.getUserProfile();
        String userRole = userQueryRequest.getUserRole();
        String sortField = userQueryRequest.getSortField();
        String sortOrder = userQueryRequest.getSortOrder();
        //创建QueryWrapper
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        //添加条件,id不为空，则添加id条件，数据库中id那一列的值等于我们传入的id
        queryWrapper.eq(ObjUtil.isNotNull(id), "id", id);
        queryWrapper.eq(StrUtil.isNotBlank(userName), "userName", userName);
        queryWrapper.eq(StrUtil.isNotBlank(userAccount), "userAccount", userAccount);
        queryWrapper.eq(StrUtil.isNotBlank(userProfile), "userProfile", userProfile);
        queryWrapper.eq(StrUtil.isNotBlank(userRole), "userRole", userRole);
        //调整以下排序顺序给前端发过去
        //如果sortField不为空，则添加排序条件，排序字段为sortField，排序顺序为sortOrder
        queryWrapper.orderBy(StrUtil.isNotEmpty(sortField), sortOrder.equals("ascend"), sortField);
        return queryWrapper;
    }

    /**
     * 获取加密后的密码
     * @param userPassword
     * @return
     */
    public String getEncryptPassword(String userPassword) {
        //1.加盐
        String salt = "czj";
        //2.md5加密
        String encryptPassword = DigestUtils.md5DigestAsHex((salt + userPassword).getBytes());
        return encryptPassword;
    }

    @Override
    public boolean isAdmin(User user) {
        return user != null && UserRoleEnum.Admin.getValue().equals(user.getUserRole());
    }

}




