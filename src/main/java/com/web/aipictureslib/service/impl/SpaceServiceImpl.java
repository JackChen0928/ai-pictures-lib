package com.web.aipictureslib.service.impl;

import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.web.aipictureslib.exception.BusinessException;
import com.web.aipictureslib.exception.ErrorCode;
import com.web.aipictureslib.exception.ThrowUtils;
import com.web.aipictureslib.model.VO.SpaceVO;
import com.web.aipictureslib.model.VO.UserVO;
import com.web.aipictureslib.model.dto.space.SpaceAddRequest;
import com.web.aipictureslib.model.dto.space.SpaceQueryRequest;
import com.web.aipictureslib.model.entity.Picture;
import com.web.aipictureslib.model.entity.Space;
import com.web.aipictureslib.model.entity.User;
import com.web.aipictureslib.model.enums.SpaceLevelEnum;
import com.web.aipictureslib.service.SpaceService;
import com.web.aipictureslib.mapper.SpaceMapper;
import com.web.aipictureslib.service.UserService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
* @author czj24
* @description 针对表【space(空间)】的数据库操作Service实现
* @createDate 2025-07-21 13:13:01
*/
@Service
public class SpaceServiceImpl extends ServiceImpl<SpaceMapper, Space> implements SpaceService{

    @Autowired
    private UserService userService;
    @Autowired
    private TransactionTemplate transactionTemplate;


    @Override
    public long addSpace(SpaceAddRequest spaceAddRequest, User loginUser) {
        Space space = new Space();
        BeanUtils.copyProperties(spaceAddRequest, space);
        if (StrUtil.isBlank(spaceAddRequest.getSpaceName())) {
            space.setSpaceName("默认空间");
        }
        if (spaceAddRequest.getSpaceLevel() == null) {
            space.setSpaceLevel(SpaceLevelEnum.COMMON.getValue());
        }
        //填充数据
        this.fillSpaceBySpaceLevel(space);
        //校验数据
        this.validSpace(space, true);
        long userId = loginUser.getId();
        space.setUserId(userId);

        //权限校验
        if(SpaceLevelEnum.COMMON.getValue() != spaceAddRequest.getSpaceLevel() && !userService.isAdmin(loginUser))
        {
            throw new BusinessException(ErrorCode.NOT_AUTH_ERROR,"您不是管理员，无法创建专业版或旗舰版空间");
        }

        //针对用户进行加锁（一人一个空间）
//        String lock = String.valueOf(userId).intern();

        //创建一个map，key为用户ID，value为锁对象
        Map<Long, Object> userLocks = new ConcurrentHashMap<>();
        // 针对用户进行加锁（一人一个空间）（concurrenthashmap）
        //从map中根据用户ID获取锁对象，如果不存在则创建一个锁对象
        Object lock = userLocks.computeIfAbsent(userId, key -> new Object());

        synchronized (lock) {
            try {
                Long newSpaceId = transactionTemplate.execute(status -> {
                    boolean exist = this.lambdaQuery().eq(Space::getUserId, userId).exists();
                    ThrowUtils.throwIf(exist, ErrorCode.OPERATION_ERROR, "您已创建过空间");

                    //写入数据库
                    boolean result = this.save(space);
                    ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "创建空间失败");
                    return space.getId();
                });
                //返回结果是包装类，可以做一些处理（只是为了防止报错，无实际意义）
                return Optional.ofNullable(newSpaceId).orElse(-1L);
            } finally {
                // 移除用户锁
                userLocks.remove(userId);
            }
        }
    }

    /**
     * 根据空间级别，自动填充容量大小等信息
     * @param space
     */
    @Override
    public void fillSpaceBySpaceLevel(Space space) {
        //根据空间级别，自动填充容量大小等信息
        SpaceLevelEnum spaceLevelEnum = SpaceLevelEnum.getEnumByValue(space.getSpaceLevel());
        if (spaceLevelEnum != null) {
            long maxCount = spaceLevelEnum.getMaxCount();
            if (space.getMaxCount() == null) {
                space.setMaxCount(maxCount);
            }

            long maxSize = spaceLevelEnum.getMaxSize();
            if (space.getMaxSize() == null) {
                space.setMaxSize(maxSize);
            }
        }
    }
    /**
     * 校验空间
     * @param space
     * @param add 区分是创建还是更新
     */
    @Override
    public void validSpace(Space space, boolean add) {
        ThrowUtils.throwIf(space == null, ErrorCode.PARAM_ERROR);
        //从对象中取值
        String spaceName = space.getSpaceName();
        Integer spaceLevel = space.getSpaceLevel();
        SpaceLevelEnum spaceLevelEnum = SpaceLevelEnum.getEnumByValue(spaceLevel);
        //创建
        if (add) {
            if (StrUtil.isBlank(spaceName))
                throw new BusinessException(ErrorCode.PARAM_ERROR, "空间名称不能为空");
            if (spaceLevel == null)
                throw new BusinessException(ErrorCode.PARAM_ERROR, "空间级别不能为空");
        }
        //修改数据时，如果需要改空间级别
        if (spaceLevel != null && spaceLevelEnum == null)
            throw new BusinessException(ErrorCode.PARAM_ERROR, "空间级别错误");
        if (spaceName.length() > 30 && StrUtil.isNotBlank(spaceName))
            throw new BusinessException(ErrorCode.PARAM_ERROR, "空间名称过长");
    }

    @Override
    public SpaceVO getSpaceVO(Space space) {
        //转换为VO
        SpaceVO spaceVO = SpaceVO.objToVo(space);
        //关联用户信息
        Long userId = space.getUserId();
        if (userId != null && userId > 0) {
            User user = userService.getById(userId);
            UserVO userVO = userService.getUserVO(user);
            spaceVO.setUser(userVO);
        }
        return spaceVO;
    }

    @Override
    public Page<SpaceVO> getSpaceVOPage(Page<Space> spacePage, HttpServletRequest request) {
        //获取空间列表
        List<Space> spaceList = spacePage.getRecords();
        //创建一个新的空间VO分页对象，继承自spacePage的当前页码、大小和总记录数
        Page<SpaceVO> spaceVOPage = new Page<>(spacePage.getCurrent(), spacePage.getSize(), spacePage.getTotal());
        //如果spaceList为空，返回空的spaceVOPage
        if (ObjUtil.isEmpty(spaceList)) {
            return spaceVOPage;
        }
        //获取spaceList的VO；-->>spaceVOList
        List<SpaceVO> spaceVOList = spaceList.stream().map(SpaceVO::objToVo).collect(Collectors.toList());
        //1.关联查询用户信息
        //收集所有空间的用户ID，去重，组成set
        Set<Long> userIdSet = spaceVOList.stream().map(SpaceVO::getUserId).collect(Collectors.toSet());
        //通过用户ID列表，用刚刚组成的set，查询用户信息，并按用户ID分组，组成map
        Map<Long, List<User>> userIdUserListMap = userService.listByIds(userIdSet).stream()
                .collect(Collectors.groupingBy(User::getId));
        //2.每一个VO都要填充用户信息
        //遍历spaceVOList，为每个空间VO填充对应的用户信息
        spaceVOList.forEach(spaceVO -> {
            Long userId = spaceVO.getUserId();
            User user = null;
            //如果用户ID存在于查询用户信息的结果map中，获取第一个用户对象
            if (userIdUserListMap.containsKey(userId)) {
                user = userIdUserListMap.get(userId).get(0);
            }
            //使用获取的用户信息设置spaceVO的用户属性
            spaceVO.setUser(userService.getUserVO(user));
        });
        //将转换后的spaceVOList设置为spaceVOPage的记录
        spaceVOPage.setRecords(spaceVOList);

        //返回填充了用户信息的spaceVOPage
        return spaceVOPage;
    }

    @Override
    public QueryWrapper<Space> getQueryWrapper(SpaceQueryRequest spaceQueryRequest) {
        if (spaceQueryRequest == null) {
            return new QueryWrapper<>();
        }
        Long id = spaceQueryRequest.getId();
        Long userId = spaceQueryRequest.getUserId();
        Integer spaceLevel = spaceQueryRequest.getSpaceLevel();
        String spaceName = spaceQueryRequest.getSpaceName();
        String sortField = spaceQueryRequest.getSortField();
        String sortOrder = spaceQueryRequest.getSortOrder();

        //下面开写querywrapper
        QueryWrapper<Space> queryWrapper = new QueryWrapper<>();

        queryWrapper.eq(ObjUtil.isNotEmpty(id), "id", id);
        queryWrapper.eq(ObjUtil.isNotEmpty(userId), "userId", userId);
        queryWrapper.eq(ObjUtil.isNotEmpty(spaceLevel), "spaceLevel", spaceLevel);
        queryWrapper.like(StrUtil.isNotBlank(spaceName), "spaceName", spaceName);

        //排序
        queryWrapper.orderBy(StrUtil.isNotBlank(sortField), sortOrder.equals("ascend"), sortField);
        return queryWrapper;
    }

    @Override
    public void checkSpaceAuth(User loginUser, Space space) {
        //仅本人或管理员可访问
        if (!loginUser.getId().equals(space.getUserId()) && !userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NOT_AUTH_ERROR);
        }
    }
}




