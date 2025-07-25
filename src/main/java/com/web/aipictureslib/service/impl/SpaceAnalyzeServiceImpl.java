package com.web.aipictureslib.service.impl;


import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.web.aipictureslib.exception.BusinessException;
import com.web.aipictureslib.exception.ErrorCode;
import com.web.aipictureslib.exception.ThrowUtils;
import com.web.aipictureslib.model.VO.space.analyze.SpaceCategoryAnalyzeResponse;
import com.web.aipictureslib.model.VO.space.analyze.SpaceTagAnalyzeResponse;
import com.web.aipictureslib.model.VO.space.analyze.SpaceUsageAnalyzeResponse;
import com.web.aipictureslib.model.dto.space.analyze.SpaceAnalyzeRequest;
import com.web.aipictureslib.model.dto.space.analyze.SpaceCategoryAnalyzeRequest;
import com.web.aipictureslib.model.dto.space.analyze.SpaceTagAnalyzeRequest;
import com.web.aipictureslib.model.dto.space.analyze.SpaceUsageAnalyzeRequest;
import com.web.aipictureslib.model.entity.Picture;
import com.web.aipictureslib.model.entity.Space;
import com.web.aipictureslib.model.entity.User;
import com.web.aipictureslib.service.PictureService;
import com.web.aipictureslib.service.SpaceAnalyzeService;
import com.web.aipictureslib.mapper.SpaceMapper;
import com.web.aipictureslib.service.SpaceService;
import com.web.aipictureslib.service.UserService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


/**
 * @author czj24
 */
@Service
public class SpaceAnalyzeServiceImpl extends ServiceImpl<SpaceMapper, Space> implements SpaceAnalyzeService {

    @Resource
    private UserService userService;

    @Resource
    private SpaceService spaceService;

    @Resource
    private PictureService pictureService;

    @Resource
    private SpaceAnalyzeService spaceAnalyzeService;

    private void checkSpaceAnalyzeAuth(SpaceAnalyzeRequest spaceAnalyzeRequest, User loginUser) {
        //检查权限
        //1.分析需要查询全部空间或者公共图库，只允许管理员分析
        if (spaceAnalyzeRequest.isQueryAll() || spaceAnalyzeRequest.isQueryPublic()) {
            ThrowUtils.throwIf(!userService.isAdmin(loginUser), ErrorCode.NOT_AUTH_ERROR, "您不是管理员，无法分析全部空间或公共图库");
        } else {
            //私有空间分析，只允许空间创建者和管理员分析
            Long spaceId = spaceAnalyzeRequest.getSpaceId();
            ThrowUtils.throwIf(spaceId == null, ErrorCode.PARAM_ERROR);
            Space space = this.getById(spaceId);
            ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
            spaceService.checkSpaceAuth(loginUser, space);
        }
    }

    /**
     * 填充分析查询条件
     *
     * @param spaceAnalyzeRequest
     * @param queryWrapper
     */
    private static void fillAnalyzeQueryWrapper(SpaceAnalyzeRequest spaceAnalyzeRequest, QueryWrapper<Picture> queryWrapper) {
        //查询所有空间，查询范围为全部，故不需要填充查询条件（spaceId为空，不为空的照片都查询）
        if (spaceAnalyzeRequest.isQueryAll()) {
            return;
        }
        //查询公共图库，查询范围为公共，故只需要查询spaceId为空的图片
        if (spaceAnalyzeRequest.isQueryPublic()) {
            queryWrapper.isNull("spaceId");
            return;
        }
        //查询指定空间，查询范围为指定空间，故只需要填充spaceId条件
        Long spaceId = spaceAnalyzeRequest.getSpaceId();
        if (spaceId != null) {
            queryWrapper.eq("spaceId", spaceId);
            return;
        }
        throw new BusinessException(ErrorCode.PARAM_ERROR, "未指定查询范围");
    }

    /**
     * 获取空间使用情况
     *
     * @param spaceUsageAnalyzeRequest
     * @param loginUser
     * @return
     */
    @Override
    public SpaceUsageAnalyzeResponse getSpaceUsageAnalyze(SpaceUsageAnalyzeRequest spaceUsageAnalyzeRequest, User loginUser) {
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);
        ThrowUtils.throwIf(spaceUsageAnalyzeRequest == null, ErrorCode.PARAM_ERROR);
        //查询全部空间或公共图库
        if (spaceUsageAnalyzeRequest.isQueryAll() || spaceUsageAnalyzeRequest.isQueryPublic()) {
            //权限校验
            boolean isAdmin = userService.isAdmin(loginUser);
            ThrowUtils.throwIf(!isAdmin, ErrorCode.NOT_AUTH_ERROR);
            //统计公共空间的资源使用情况
            QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
            queryWrapper.select("picSize");
            //统计公共图库，不是全部空间，加上条件
            if (!spaceUsageAnalyzeRequest.isQueryAll()) {
                queryWrapper.isNull("spaceId");
            }
            List<Object> pictureObjList = pictureService.getBaseMapper().selectObjs(queryWrapper);
            long usedSize = pictureObjList.stream().mapToLong(result -> result instanceof Long ? (Long) result : 0).sum();
            long usedCount = pictureObjList.size();
            //封装返回结果
            SpaceUsageAnalyzeResponse spaceUsageAnalyzeResponse = new SpaceUsageAnalyzeResponse();
            spaceUsageAnalyzeResponse.setUsedSize(usedSize);
            spaceUsageAnalyzeResponse.setUsedCount(usedCount);
            //公共图库无上限
            spaceUsageAnalyzeResponse.setMaxSize(null);
            spaceUsageAnalyzeResponse.setSizeUsageRatio(null);
            spaceUsageAnalyzeResponse.setMaxCount(null);
            spaceUsageAnalyzeResponse.setCountUsageRatio(null);
            return spaceUsageAnalyzeResponse;
        } else {
            //查询私有空间
            //查询空间ID是否存在
            Long spaceId = spaceUsageAnalyzeRequest.getSpaceId();
            ThrowUtils.throwIf(spaceId == null, ErrorCode.PARAM_ERROR);
            //查询空间信息是否存在
            Space space = spaceService.getById(spaceId);
            ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");

            //判断权限
            spaceService.checkSpaceAuth(loginUser, space);

            //通过权限后，查询并构造返回结果
            SpaceUsageAnalyzeResponse response = new SpaceUsageAnalyzeResponse();
            //数据库已有的数据，直接赋值使用
            response.setUsedSize(space.getTotalCount());
            response.setMaxSize(space.getMaxSize());
            response.setUsedCount(space.getTotalCount());
            response.setMaxCount(space.getMaxCount());


            double sizeUsageRatio = NumberUtil.round(space.getTotalSize() * 100.0 / space.getMaxSize(), 2).doubleValue();
            response.setSizeUsageRatio(sizeUsageRatio);
            double countUsageRatio = NumberUtil.round(space.getTotalCount() * 100.0 / space.getMaxCount(), 2).doubleValue();
            response.setCountUsageRatio(countUsageRatio);

            return response;
        }
    }

    @Override
    public List<SpaceCategoryAnalyzeResponse> getSpaceCategoryAnalyze(SpaceCategoryAnalyzeRequest spaceCategoryAnalyzeRequest, User loginUser) {
        ThrowUtils.throwIf(spaceCategoryAnalyzeRequest == null, ErrorCode.PARAM_ERROR);
        //权限校验
        checkSpaceAnalyzeAuth(spaceCategoryAnalyzeRequest,loginUser);

        //处理请求
         QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();

         fillAnalyzeQueryWrapper(spaceCategoryAnalyzeRequest,queryWrapper);

         //使用mp分组查询
        queryWrapper.select("category AS category",
                "COUNT(*) AS count)",
                "SUM(picSize) AS totalSize")
                .groupBy("category");

        //封装返回结果
        return pictureService.getBaseMapper().selectMaps(queryWrapper)
                .stream()
                .map(result->{
                    String category = result.get("category") != null ? result.get("category").toString():"未分类";
                    Long count = ((Number) result.get("count")).longValue();
                    Long totalSize = ((Number) result.get("totalSize")).longValue();
                    return new SpaceCategoryAnalyzeResponse(category,count,totalSize);
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<SpaceTagAnalyzeResponse> getSpaceTagAnalyze(SpaceTagAnalyzeRequest spaceTagAnalyzeRequest, User loginUser) {
        ThrowUtils.throwIf(spaceTagAnalyzeRequest == null, ErrorCode.PARAM_ERROR);
        //权限校验
        checkSpaceAnalyzeAuth(spaceTagAnalyzeRequest,loginUser);

        //处理请求
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        fillAnalyzeQueryWrapper(spaceTagAnalyzeRequest,queryWrapper);

        //查询所有符合条件的标签
        queryWrapper.select("tags");
        //tags在数据库是以Json存储的
        List<String> tagsJsonList = pictureService.getBaseMapper().selectObjs(queryWrapper)
                .stream()
                .filter(ObjUtil::isNotNull)
                .map(Object::toString)
                .collect(Collectors.toList());

        //合并所有标签并统计使用次数
        Map<String,Long > tagCountMap = tagsJsonList.stream()
                .flatMap(tagsJson -> JSONUtil.toList(tagsJson,String.class).stream())
                .collect(Collectors.groupingBy(tag->tag, Collectors.counting()));

        //转换为响应对象，按使用次数降序排序
        return tagCountMap.entrySet().stream()
                .sorted((e1,e2) ->Long.compare(e2.getValue(),e1.getValue()))//降序排列
                .map(entry ->new SpaceTagAnalyzeResponse(entry.getKey(),entry.getValue()))
                .collect(Collectors.toList());

    }

}




