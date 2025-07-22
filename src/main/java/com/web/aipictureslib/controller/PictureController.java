package com.web.aipictureslib.controller;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.web.aipictureslib.annotation.AuthCheck;
import com.web.aipictureslib.common.BaseResponse;
import com.web.aipictureslib.common.DeleteRequest;
import com.web.aipictureslib.common.ResultUtils;
import com.web.aipictureslib.constant.UserConstant;
import com.web.aipictureslib.exception.BusinessException;
import com.web.aipictureslib.exception.ErrorCode;
import com.web.aipictureslib.exception.ThrowUtils;
import com.web.aipictureslib.manager.FileManager;
import com.web.aipictureslib.model.VO.PictureVO;
import com.web.aipictureslib.model.dto.file.UploadPictureResult;
import com.web.aipictureslib.model.dto.picture.*;
import com.web.aipictureslib.model.dto.space.SpaceUpdateRequest;
import com.web.aipictureslib.model.entity.Picture;
import com.web.aipictureslib.model.entity.Space;
import com.web.aipictureslib.model.entity.User;
import com.web.aipictureslib.model.enums.PictureReviewStatusEnum;
import com.web.aipictureslib.service.PictureService;
import com.web.aipictureslib.service.SpaceService;
import com.web.aipictureslib.service.UserService;
import org.apache.commons.codec.cli.Digest;
import org.apache.coyote.Request;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

import java.util.Date;
import java.util.concurrent.TimeUnit;

import static com.web.aipictureslib.constant.UserConstant.USER_LOGIN_STATE;

@RestController
@RequestMapping("/picture")
public class PictureController {
    @Resource
    private FileManager fileManager;
    @Resource
    private PictureService pictureService;
    @Resource
    private UserService userService;
    @Autowired
    private HttpServletRequest request;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private SpaceService spaceService;

    //构造本地缓存
    private final Cache<String,String> LOCAL_CACHE =
            Caffeine.newBuilder()
                    .maximumSize(10000L)
                    .expireAfterWrite(10, TimeUnit.MINUTES)
                    .build();


    /**
     * 上传图片
     *
     * @param multipartFile
     * @param pictureUploadRequest
     * @param
     * @return
     */
    @PostMapping("/upload")
    public BaseResponse<PictureVO> uploadPicture(@RequestPart("file") MultipartFile multipartFile, PictureUploadRequest pictureUploadRequest) {
        ThrowUtils.throwIf(multipartFile == null, ErrorCode.PARAM_ERROR);
        User user = userService.getLoginUser(request);
        PictureVO pictureVO = pictureService.uploadPicture(multipartFile, pictureUploadRequest,user);
        return ResultUtils.success(pictureVO);
    }

    /**
     * 上传图片（url）
     *
     * @param pictureUploadRequest
     * @param
     * @return
     */
    @PostMapping("/upload/url")
    public BaseResponse<PictureVO> uploadPictureByurl(@RequestBody PictureUploadRequest pictureUploadRequest) {
        ThrowUtils.throwIf(pictureUploadRequest == null, ErrorCode.PARAM_ERROR);
        User user = userService.getLoginUser(request);
        String url = pictureUploadRequest.getFileUrl();
        PictureVO pictureVO = pictureService.uploadPicture(url, pictureUploadRequest,user);
        return ResultUtils.success(pictureVO);
    }

    /**
     * 删除图片
     *
     * @param deleteRequest
     * @return
     */
    @PostMapping("/delele")
    public BaseResponse<Boolean> deletePicture(@RequestBody DeleteRequest deleteRequest) {
        User loginUser = userService.getLoginUser(request);
        Long id = deleteRequest.getId();
        pictureService.deletePicture(id, loginUser);
        return ResultUtils.success(true);
    }
    /**
     * 更新（管理员）
     *
     * @param pictureUpdateRequest
     * @return
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updatePicture(@RequestBody PictureUpdateRequest pictureUpdateRequest){
       ThrowUtils.throwIf(pictureUpdateRequest == null, ErrorCode.PARAM_ERROR);
       Long id = pictureUpdateRequest.getId();
       ThrowUtils.throwIf(id <= 0, ErrorCode.PARAM_ERROR);
       //新建一个picture对象，把新的数据存进去这个对象
       Picture picture = new Picture();
       BeanUtils.copyProperties(pictureUpdateRequest, picture);
       picture.setTags(JSONUtil.toJsonStr(pictureUpdateRequest.getTags()));
       //验证数据是否合法
       pictureService.validPicture(picture);
       //判断是否存在该图片
       Picture oldPicture = pictureService.getById(id);
       ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);
       //填充审核参数
       User loginUser = userService.getLoginUser(request);
       pictureService.fillReviewParams(picture, loginUser);
       //存在该照片，把之前存起来的新的数据存进去数据库（操作数据库）
       boolean result = pictureService.updateById(picture);
       ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
       return ResultUtils.success(true);
    }

    @PostMapping("/edit")
    public BaseResponse<Boolean> editPicture(@RequestBody PictureEditRequest pictureEditRequest){
        ThrowUtils.throwIf(pictureEditRequest == null, ErrorCode.PARAM_ERROR);
        pictureService.editPicture(pictureEditRequest);
        return ResultUtils.success(true);
        }

    /**
     * 根据id获取单张图片（管理员）（数据不脱敏）
     * @param id
     * @return
     */
    @GetMapping("/get")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Picture> getPictureById(Long id) {
       ThrowUtils.throwIf(id <= 0, ErrorCode.PARAM_ERROR);
       Picture picture = pictureService.getById(id);
       ThrowUtils.throwIf(picture == null, ErrorCode.NOT_FOUND_ERROR);
       return ResultUtils.success(picture);
    }
    /**
     * 根据id获取单张图片（用户）（数据脱敏）
     * @param id
     * @return
     */
    @GetMapping("/get/vo")
    public BaseResponse<PictureVO> getPictureVOById(Long id) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAM_ERROR);
        Picture picture = pictureService.getById(id);
        ThrowUtils.throwIf(picture == null, ErrorCode.NOT_FOUND_ERROR);

        //新增空间校验
        Long spaceId = picture.getSpaceId();
        if (spaceId != null)
        {
            User loginUser = userService.getLoginUser(request);
            pictureService.checkPictureAuth(loginUser, picture);
        }
        // 脱敏
        return ResultUtils.success(pictureService.getPictureVO(picture));
    }

    /**
     * 分页获取图片列表（仅管理员）
     * @param pictureQueryRequest
     * @return
     */
    @PostMapping("/list/page")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<Picture>> listPictureByPage(@RequestBody PictureQueryRequest pictureQueryRequest) {
        long current = pictureQueryRequest.getCurrent();
        long size = pictureQueryRequest.getPageSize();
        Page<Picture> picturePage = pictureService.page(new Page<>(current, size), pictureService.getQueryWrapper(pictureQueryRequest));
        return ResultUtils.success(picturePage);
    }

    /**
     * 分页获取图片列表（用户）（脱敏）
     * @param pictureQueryRequest
     * @return
     */
    @PostMapping("/list/page/vo")
    public BaseResponse<Page<PictureVO>> listPictureVOByPage(@RequestBody PictureQueryRequest pictureQueryRequest) {
        long current = pictureQueryRequest.getCurrent();
        long size = pictureQueryRequest.getPageSize();
        //限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAM_ERROR);
        Long spaceId = pictureQueryRequest.getSpaceId();
        if (spaceId==null){
            //普通用户默认只能查看已通过审核的照片
            pictureQueryRequest.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());
            pictureQueryRequest.setNullSpaceId(true);
        }else {
            //私有空间
            User loginUser = userService.getLoginUser(request);
            Space space = spaceService.getById(spaceId);
            ThrowUtils.throwIf(space==null,ErrorCode.NOT_FOUND_ERROR,"空间不存在");
            if (!space.getUserId().equals(loginUser.getId())){
                throw new BusinessException(ErrorCode.NOT_AUTH_ERROR,"您没有权限查看该空间");
            }
        }
        //先查询数据库获得不脱敏的数据页
        Page<Picture> picturePage = pictureService.page(new Page<>(current, size),pictureService.getQueryWrapper(pictureQueryRequest));
        //通过不脱敏的数据页，再获得脱敏的数据页
        Page<PictureVO> pictureVOPage = pictureService.getPictureVOPage(picturePage, request);
        return ResultUtils.success(pictureVOPage);
    }

//    @PostMapping("/list/page/vo/cache")
//    public BaseResponse<Page<PictureVO>> listPictureVOByPageWithCache(@RequestBody PictureQueryRequest pictureQueryRequest) {
//        long current = pictureQueryRequest.getCurrent();
//        long size = pictureQueryRequest.getPageSize();
//        //限制爬虫
//        ThrowUtils.throwIf(size > 20, ErrorCode.PARAM_ERROR);
//        //普通用户默认只能查看已通过审核的照片
//        pictureQueryRequest.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());
//
//        //1.查询缓存(先构造key，再通过key查询是否有缓存)
//        //构造key
//        String queryCondition = JSONUtil.toJsonStr(pictureQueryRequest);
//        String hashKey = DigestUtils.md5DigestAsHex(queryCondition.getBytes());
//        String redisKey = String.format("ai_pictures_lib:listPictureVOByPage:%s", hashKey);
//
//        //查询redis缓存
//        //拿到操作对象
//        ValueOperations<String, String> valueOps = stringRedisTemplate.opsForValue();
//        //通过操作对象拿到数据
//        String cachedValue = valueOps.get(redisKey);
//        //若缓存有，直接返回
//        if (StringUtils.isNotBlank(cachedValue)) {
//            //从缓存中获得数据(json形式，需要转成java对象)
//            Page<PictureVO> pictureVOPage = JSONUtil.toBean(cachedValue, Page.class);
//            return ResultUtils.success(pictureVOPage);
//        }
//
//        //2.若缓存没有，查询数据库
//        //查询数据库获得不脱敏的数据页
//        Page<Picture> picturePage = pictureService.page(new Page<>(current, size),pictureService.getQueryWrapper(pictureQueryRequest));
//        //通过不脱敏的数据页，再获得脱敏的数据页
//        Page<PictureVO> pictureVOPage = pictureService.getPictureVOPage(picturePage, request);
//
//
//        //3.把数据库查出来的，写入缓存
//        //将数据页转成json字符串
//        String cacheValue = JSONUtil.toJsonStr(pictureVOPage);
//        //5-10分钟随机过期，防止缓存雪崩
//        int expireSeconds = RandomUtil.randomInt(5 * 60, 10 * 60);
//        valueOps.set(redisKey, cacheValue, expireSeconds, TimeUnit.SECONDS);
//
//        return ResultUtils.success(pictureVOPage);
//    }


//    @PostMapping("/list/page/vo/cache")
//    public BaseResponse<Page<PictureVO>> listPictureVOByPageWithCache(@RequestBody PictureQueryRequest pictureQueryRequest) {
//        long current = pictureQueryRequest.getCurrent();
//        long size = pictureQueryRequest.getPageSize();
//        //限制爬虫
//        ThrowUtils.throwIf(size > 20, ErrorCode.PARAM_ERROR);
//        //普通用户默认只能查看已通过审核的照片
//        pictureQueryRequest.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());
//
//        //1.查询缓存(先构造key，再通过key查询是否有缓存)
//        //构造key
//        String queryCondition = JSONUtil.toJsonStr(pictureQueryRequest);
//        String hashKey = DigestUtils.md5DigestAsHex(queryCondition.getBytes());
//        String cacheKey = String.format("ai_pictures_lib:listPictureVOByPage:%s", hashKey);
//
//        //查询本地缓存
//        //拿到操作对象
//        String cachedValue = LOCAL_CACHE.getIfPresent(cacheKey);
//        //若缓存有，直接返回
//        if (cachedValue != null) {
//            //从缓存中获得数据(json形式，需要转成java对象)
//            Page<PictureVO> pictureVOPage = JSONUtil.toBean(cachedValue, Page.class);
//            return ResultUtils.success(pictureVOPage);
//        }
//
//    //2.若缓存没有，查询数据库
//    //查询数据库获得不脱敏的数据页
//    Page<Picture> picturePage = pictureService.page(new Page<>(current, size),pictureService.getQueryWrapper(pictureQueryRequest));
//    //通过不脱敏的数据页，再获得脱敏的数据页
//    Page<PictureVO> pictureVOPage = pictureService.getPictureVOPage(picturePage, request);
//
//
//    //3.把数据库查出来的，写入本地缓存
//    //将数据页转成json字符串
//    String cacheValue = JSONUtil.toJsonStr(pictureVOPage);
//    LOCAL_CACHE.put(cacheKey, cacheValue);
//
//
//    return ResultUtils.success(pictureVOPage);
//    }
    @PostMapping("/list/page/vo/cache")
    public BaseResponse<Page<PictureVO>> listPictureVOByPageWithCache(@RequestBody PictureQueryRequest pictureQueryRequest) {
        long current = pictureQueryRequest.getCurrent();
        long size = pictureQueryRequest.getPageSize();
        //限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAM_ERROR);
        //普通用户默认只能查看已通过审核的照片
        pictureQueryRequest.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());

        //1.查询缓存(先构造key，再通过key查询是否有缓存)
        //构造key
        String queryCondition = JSONUtil.toJsonStr(pictureQueryRequest);
        String hashKey = DigestUtils.md5DigestAsHex(queryCondition.getBytes());
        String cacheKey = String.format("ai_pictures_lib:listPictureVOByPage:%s", hashKey);

        //1.先查询本地缓存
        String cachedValue = LOCAL_CACHE.getIfPresent(cacheKey);
        //若本地缓存有，直接返回
        if (cachedValue != null) {
            //从本地缓存中获得数据(json形式，需要转成java对象)
            Page<PictureVO> pictureVOPage = JSONUtil.toBean(cachedValue, Page.class);
            //返回本地缓存数据
            return ResultUtils.success(pictureVOPage);
        }
        //2.若本地缓存没有，查询Redis缓存
        //拿到操作对象
        ValueOperations<String, String> valueOps = stringRedisTemplate.opsForValue();
        //通过操作对象拿到数据
        cachedValue = valueOps.get(cacheKey);
        //若redis缓存有，直接返回
        if (StringUtils.isNotBlank(cachedValue)) {
            //写入本地缓存
            LOCAL_CACHE.put(cacheKey, cachedValue);
            //从redis缓存中获得数据(json形式，需要转成java对象)
            Page<PictureVO> pictureVOPage = JSONUtil.toBean(cachedValue, Page.class);
            //返回redis缓存数据
            return ResultUtils.success(pictureVOPage);
        }
        //3.若Redis缓存没有，查询数据库
        //查询数据库获得不脱敏的数据页
        Page<Picture> picturePage = pictureService.page(new Page<>(current, size),
                pictureService.getQueryWrapper(pictureQueryRequest));
        //通过不脱敏的数据页，再获得脱敏的数据页
        Page<PictureVO> pictureVOPage = pictureService.getPictureVOPage(picturePage, request);


        //4.把数据库查出来的，写入本地缓存以及redis缓存
        //将数据页转成json字符串
        String cacheValue = JSONUtil.toJsonStr(pictureVOPage);
        //更新本地缓存
        LOCAL_CACHE.put(cacheKey, cacheValue);
        //更新redis缓存
        valueOps.set(cacheKey, cacheValue, 5, TimeUnit.MINUTES);

        return ResultUtils.success(pictureVOPage);
    }

    @PostMapping("/review")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> doPictureReview(@RequestBody PictureReviewRequest pictureReviewRequest) {
        ThrowUtils.throwIf(pictureReviewRequest == null, ErrorCode.PARAM_ERROR);
        User loginUser = userService.getLoginUser(request);
        pictureService.doPictureReview(pictureReviewRequest,loginUser);
        return ResultUtils.success(true);
    }

    @PostMapping("/upload/batch")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Integer> uploadPictureByBatch(@RequestBody PictureUploadByBatchRequest pictureUploadByBatchRequest) {
        ThrowUtils.throwIf(pictureUploadByBatchRequest == null, ErrorCode.PARAM_ERROR);
        User loginUser = userService.getLoginUser(request);
        int count = pictureService.uploadPictureByBatch(pictureUploadByBatchRequest, loginUser);
        return ResultUtils.success(count);
    }

//    /**
//     * 获取所有标签和分类
//     *
//     * @return
//     */
//    @GetMapping("/tag_category")
//    public BaseResponse<PictureTagCategory> listPictureTagCategory() {
//        PictureTagCategory pictureTagCategory = new PictureTagCategory();
//        List<String> tagList = Arrays.asList("热门", "搞笑", "生活", "高清", "艺术", "校园", "背景", "简历", "创意");
//        List<String> categoryList = Arrays.asList("模板", "电商", "表情包", "素材", "海报");
//        pictureTagCategory.setTagList(tagList);
//        pictureTagCategory.setCategoryList(categoryList);
//        return ResultUtils.success(pictureTagCategory);
//    }

}