package com.web.aipictureslib.annotation;

import com.web.aipictureslib.model.entity.User;
import com.web.aipictureslib.model.enums.UserRoleEnum;
import com.web.aipictureslib.service.UserService;
import org.apache.coyote.Request;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

@Aspect
@Component
public class AuthInterceptor {
    @Resource
    private UserService userService;

    /**
     * 执行拦截
     *
     * @param joinPoint
     * @param authCheck 权限校验注解
     * @return
     */
    @Around("@annotation(authCheck)")
    public Object doInterceptor(ProceedingJoinPoint joinPoint, AuthCheck authCheck) throws Throwable {
        String mustRole = authCheck.mustRole();
        //拿到请求中的信息
        RequestAttributes requestAttributes = RequestContextHolder.currentRequestAttributes();
        HttpServletRequest request = ((ServletRequestAttributes)requestAttributes).getRequest();
        //从请求信息中拿到当前登录用户
        User loginUser = userService.getLoginUser(request);
        //从写的注解中拿到当前接口must需要的权限，从枚举类中拿到枚举，然后下面就可以比较权限了。
        UserRoleEnum mustRoleEnum = UserRoleEnum.getEnumByValue(mustRole);
        //不需要权限，放行
        if (mustRoleEnum == null) {
            return joinPoint.proceed();
        }
        //下面是需要权限的情况
        //先拿到用户的权限，并转成枚举类型，这样userRoleEnum和mustRoleEnum就可以比较了
        String userRole = loginUser.getUserRole();
        UserRoleEnum loginUserRoleEnum = UserRoleEnum.getEnumByValue(userRole);
        //当前用户没有权限
        if (loginUserRoleEnum == null ) {
            throw new RuntimeException("无权限");
        }
        //要求有管理员权限，当前用户没有管理员权限
        //需要的权限等于管理员，但是当前用户权限不是管理员，就抛出异常
        if (UserRoleEnum.Admin.equals(mustRoleEnum) && !UserRoleEnum.Admin.equals(loginUserRoleEnum)) {
            throw new RuntimeException("无权限");
        }
        //当前用户权限正常，放行
        return joinPoint.proceed();
    }

}
