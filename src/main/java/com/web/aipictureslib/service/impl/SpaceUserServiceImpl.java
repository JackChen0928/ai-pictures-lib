package com.web.aipictureslib.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.web.aipictureslib.exception.ErrorCode;
import com.web.aipictureslib.exception.ThrowUtils;
import com.web.aipictureslib.model.VO.SpaceUserVO;
import com.web.aipictureslib.model.VO.SpaceVO;
import com.web.aipictureslib.model.VO.UserVO;
import com.web.aipictureslib.model.dto.spaceuser.SpaceUserAddRequest;
import com.web.aipictureslib.model.dto.spaceuser.SpaceUserQueryRequest;
import com.web.aipictureslib.model.entity.Space;
import com.web.aipictureslib.model.entity.User;
import com.web.aipictureslib.model.enums.SpaceRoleEnum;
import com.web.aipictureslib.service.SpaceService;
import com.web.aipictureslib.service.SpaceUserService;
import com.web.aipictureslib.model.entity.SpaceUser;
import com.web.aipictureslib.mapper.SpaceUserMapper;
import com.web.aipictureslib.service.UserService;
import org.springframework.beans.BeanUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
* @author czj24
* @description 针对表【space_user(空间用户关系表)】的数据库操作Service实现
* @createDate 2025-07-29 13:03:08
*/
@Service
public class SpaceUserServiceImpl extends ServiceImpl<SpaceUserMapper, SpaceUser> implements SpaceUserService {

    @Resource
    private UserService userService;
    @Resource
    @Lazy
    private SpaceService spaceService;


    @Override
    public Long addSpaceUser(SpaceUserAddRequest spaceUserAddRequest){
        ThrowUtils.throwIf(spaceUserAddRequest == null, ErrorCode.PARAM_ERROR);
        SpaceUser spaceUser = new SpaceUser();
        BeanUtils.copyProperties(spaceUserAddRequest, spaceUser);
        validSpaceUser(spaceUser,true);

        //插入数据库
        boolean result = this.save(spaceUser);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return spaceUser.getId();
    }

    @Override
    public void validSpaceUser(SpaceUser spaceUser, boolean add) {
        ThrowUtils.throwIf(ObjUtil.isEmpty(spaceUser), ErrorCode.PARAM_ERROR);
        //创建时，参数不能为空,与编辑时作出区分
        Long spaceId = spaceUser.getSpaceId();
        Long userId = spaceUser.getUserId();
        //如果是创建，则参数不能为空，为空就报错
        if(add){
            ThrowUtils.throwIf(spaceId == null||userId == null, ErrorCode.PARAM_ERROR, "空间id或用户id不能为空");
            User user = userService.getById(userId);
            ThrowUtils.throwIf(user == null, ErrorCode.NOT_FOUND_ERROR, "用户不存在");
            Space space = spaceService.getById(spaceId);
            ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
        }
        //校验空间角色
        String spaceRole = spaceUser.getSpaceRole();
        SpaceRoleEnum spaceRoleEnum = SpaceRoleEnum.getEnumByValue(spaceRole);
        ThrowUtils.throwIf(spaceRole != null && spaceRoleEnum == null, ErrorCode.NOT_FOUND_ERROR, "空间角色不存在");
    }

    @Override
    public SpaceUserVO getSpaceUserVO(SpaceUser spaceUser) {
        ThrowUtils.throwIf(ObjUtil.isEmpty(spaceUser), ErrorCode.PARAM_ERROR);
        SpaceUserVO spaceUserVO = SpaceUserVO.objToVo(spaceUser);

        User user = userService.getById(spaceUser.getUserId());
        if (ObjUtil.isNotEmpty( user) ){
            UserVO userVO = userService.getUserVO(user);
            spaceUserVO.setUser(userVO);
        }
        Space space = spaceService.getById(spaceUser.getSpaceId());
        if (ObjUtil.isNotEmpty( space)){
            SpaceVO spaceVO = spaceService.getSpaceVO(space);
            spaceUserVO.setSpace(spaceVO);
        }
        return spaceUserVO;
    }

    @Override
    public List<SpaceUserVO> getSpaceUserVOList(List<SpaceUser> spaceUserList) {
        // 判断输入列表是否为空
        if (CollUtil.isEmpty(spaceUserList)) {
            return Collections.emptyList();
        }
        // 对象列表 => 封装对象列表
        List<SpaceUserVO> spaceUserVOList = spaceUserList.stream().map(SpaceUserVO::objToVo).collect(Collectors.toList());
        // 1. 收集需要关联查询的用户 ID 和空间 ID
        Set<Long> userIdSet = spaceUserList.stream().map(SpaceUser::getUserId).collect(Collectors.toSet());
        Set<Long> spaceIdSet = spaceUserList.stream().map(SpaceUser::getSpaceId).collect(Collectors.toSet());
        // 2. 批量查询用户和空间
        Map<Long, List<User>> userIdUserListMap = userService.listByIds(userIdSet).stream()
                .collect(Collectors.groupingBy(User::getId));
        Map<Long, List<Space>> spaceIdSpaceListMap = spaceService.listByIds(spaceIdSet).stream()
                .collect(Collectors.groupingBy(Space::getId));
        // 3. 填充 SpaceUserVO 的用户和空间信息
        spaceUserVOList.forEach(spaceUserVO -> {
            Long userId = spaceUserVO.getUserId();
            Long spaceId = spaceUserVO.getSpaceId();
            // 填充用户信息
            User user = null;
            if (userIdUserListMap.containsKey(userId)) {
                user = userIdUserListMap.get(userId).get(0);
            }
            spaceUserVO.setUser(userService.getUserVO(user));
            // 填充空间信息
            Space space = null;
            if (spaceIdSpaceListMap.containsKey(spaceId)) {
                space = spaceIdSpaceListMap.get(spaceId).get(0);
            }
            spaceUserVO.setSpace(SpaceVO.objToVo(space));
        });
        return spaceUserVOList;
    }



    @Override
    public QueryWrapper<SpaceUser> getQueryWrapper(SpaceUserQueryRequest spaceUserQueryRequest) {
        QueryWrapper<SpaceUser> queryWrapper = new QueryWrapper<>();
        if (spaceUserQueryRequest == null) {
            return queryWrapper;
        }
        Long id = spaceUserQueryRequest.getId();
        Long spaceId = spaceUserQueryRequest.getSpaceId();
        Long userId = spaceUserQueryRequest.getUserId();
        String spaceRole = spaceUserQueryRequest.getSpaceRole();
        queryWrapper.eq(ObjUtil.isNotEmpty(id),"id", spaceUserQueryRequest.getId()) ;
        queryWrapper.eq(ObjUtil.isNotEmpty(spaceId),"spaceId", spaceUserQueryRequest.getSpaceId());
        queryWrapper.eq(ObjUtil.isNotEmpty(userId),"userId", spaceUserQueryRequest.getUserId()) ;
        queryWrapper.eq(StrUtil.isNotBlank(spaceRole),"spaceRole", spaceUserQueryRequest.getSpaceId());
        return queryWrapper;
    }
}




