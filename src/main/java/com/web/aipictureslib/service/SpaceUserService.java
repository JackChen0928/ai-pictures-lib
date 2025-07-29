package com.web.aipictureslib.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.web.aipictureslib.model.VO.SpaceUserVO;
import com.web.aipictureslib.model.VO.SpaceVO;
import com.web.aipictureslib.model.dto.space.SpaceAddRequest;
import com.web.aipictureslib.model.dto.space.SpaceQueryRequest;
import com.web.aipictureslib.model.dto.spaceuser.SpaceUserAddRequest;
import com.web.aipictureslib.model.dto.spaceuser.SpaceUserQueryRequest;
import com.web.aipictureslib.model.entity.Space;
import com.web.aipictureslib.model.entity.SpaceUser;
import com.baomidou.mybatisplus.extension.service.IService;
import com.web.aipictureslib.model.entity.User;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
* @author czj24
* @description 针对表【space_user(空间用户关系表)】的数据库操作Service
* @createDate 2025-07-29 13:03:08
*/
public interface SpaceUserService extends IService<SpaceUser> {

    Long addSpaceUser(SpaceUserAddRequest spaceUserAddRequest);

    void validSpaceUser(SpaceUser spaceUser, boolean add);

    /**
     * 获取空间成员VO
     *
     * @param spaceUser
     * @return
     */
    SpaceUserVO getSpaceUserVO(SpaceUser spaceUser);

    /**
     * 获取空间成员VO列表
     * @return
     */
    List<SpaceUserVO> getSpaceUserVOList(List<SpaceUser> spaceUserList);

    /**
     * 获取查询条件
     *
     * @param spaceUserQueryRequest
     * @return
     */
    QueryWrapper<SpaceUser> getQueryWrapper(SpaceUserQueryRequest spaceUserQueryRequest);


}
