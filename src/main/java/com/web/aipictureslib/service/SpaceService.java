package com.web.aipictureslib.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.web.aipictureslib.model.VO.SpaceVO;
import com.web.aipictureslib.model.dto.space.SpaceQueryRequest;
import com.web.aipictureslib.model.dto.space.SpaceAddRequest;
import com.web.aipictureslib.model.entity.Picture;
import com.web.aipictureslib.model.entity.Space;
import com.web.aipictureslib.model.entity.User;

import javax.servlet.http.HttpServletRequest;

/**
* @author czj24
* @description 针对表【space(空间)】的数据库操作Service
* @createDate 2025-07-21 13:13:01
*/
public interface SpaceService extends IService<Space> {
    /**
     * 添加空间
     *
     * @param spaceAddRequest
     * @param loginUser
     * @return
     */
    long addSpace(SpaceAddRequest spaceAddRequest, User loginUser);

    /**
     * 填充空间信息
     *
     * @param space
     */
    void fillSpaceBySpaceLevel(Space space);

    /**
     * 校验
     *
     * @param space
     * @param add
     */
    void validSpace(Space space, boolean add);
    /**
     * 获取空间VO
     *
     * @param space
     * @return
     */
    SpaceVO getSpaceVO(Space space);

    /**
     * 获取空间VO分页
     * @return
     */
    Page<SpaceVO> getSpaceVOPage(Page<Space> spacePage, HttpServletRequest request);
    
    /**
     * 获取查询条件
     *
     * @param spaceQueryRequest
     * @return
     */
    QueryWrapper<Space> getQueryWrapper(SpaceQueryRequest spaceQueryRequest);

    /**
     * 校验权限
     *
     * @param loginUser
     * @param space
     */
    void checkSpaceAuth(User loginUser, Space space);
}
