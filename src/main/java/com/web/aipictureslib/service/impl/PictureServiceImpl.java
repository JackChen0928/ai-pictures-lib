package com.web.aipictureslib.service.impl;

import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.web.aipictureslib.api.aliyunai.AliYunAIApi;
import com.web.aipictureslib.api.aliyunai.model.CreateOutPaintingTaskRequest;
import com.web.aipictureslib.api.aliyunai.model.CreateOutPaintingTaskResponse;
import com.web.aipictureslib.exception.BusinessException;
import com.web.aipictureslib.exception.ErrorCode;
import com.web.aipictureslib.exception.ThrowUtils;
import com.web.aipictureslib.manager.CosManager;
import com.web.aipictureslib.manager.FileManager;
import com.web.aipictureslib.manager.upload.FilePictureUpload;
import com.web.aipictureslib.manager.upload.PictureUploadTemplate;
import com.web.aipictureslib.manager.upload.UrlPictureUpload;
import com.web.aipictureslib.model.VO.PictureVO;
import com.web.aipictureslib.model.VO.UserVO;
import com.web.aipictureslib.model.dto.file.UploadPictureResult;
import com.web.aipictureslib.model.dto.picture.*;
import com.web.aipictureslib.model.entity.Picture;
import com.web.aipictureslib.model.entity.Space;
import com.web.aipictureslib.model.entity.User;
import com.web.aipictureslib.model.enums.PictureReviewStatusEnum;
import com.web.aipictureslib.service.PictureService;
import com.web.aipictureslib.mapper.PictureMapper;
import com.web.aipictureslib.service.SpaceService;
import com.web.aipictureslib.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
* @author czj24
* @description 针对表【picture(图片)】的数据库操作Service实现
* @createDate 2025-07-12 15:09:52
*/
@Service
@Slf4j
public class PictureServiceImpl extends ServiceImpl<PictureMapper, Picture> implements PictureService {

    @Resource
    private FileManager fileManager;

    @Resource
    private UserService userService;

    @Autowired
    private HttpServletRequest request;

    @Resource
    private FilePictureUpload filePictureUpload;

    @Resource
    private UrlPictureUpload urlPictureUpload;
    @Resource
    private CosManager cosManager;
    @Resource
    private SpaceService spaceService;
    @Autowired
    private TransactionTemplate transactionTemplate;
    @Resource
    private AliYunAIApi aliYunAIApi;

    /**
     * 上传图片(使用模板方法，文件上传和url上传合并)
     *
     * @param inputSource
     * @param pictureUploadRequest
     * @param loginUser
     * @return
     */
    @Override
    public PictureVO uploadPicture(Object inputSource, PictureUploadRequest pictureUploadRequest, User loginUser) {
        ThrowUtils.throwIf(inputSource == null, ErrorCode.PARAM_ERROR);
        //没有登录，报错
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);

        //新增空间权限校验
        Long spaceId = pictureUploadRequest.getSpaceId();
        if (spaceId != null) {
            Space space = spaceService.getById(spaceId);
            ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
            //仅本人或管理员可操作
            ThrowUtils.throwIf(!space.getUserId().equals(loginUser.getId()), ErrorCode.NOT_AUTH_ERROR, "您没有权限操作该空间");
            //校验空间是否还有剩余空间条数
            ThrowUtils.throwIf(space.getTotalCount() >= space.getMaxCount(), ErrorCode.OPERATION_ERROR, "空间条数不足");
            //校验空间是否还有剩余空间大小
            ThrowUtils.throwIf(space.getTotalSize() >= space.getMaxSize(), ErrorCode.OPERATION_ERROR, "空间大小不足");
        }

        Long pictureId = null;
        //判断是更新还是新增图片（id空就是新增，不为空就是更新）
        //如果这个图片的id不为空（前端传来了id），且大于0，说明是更新图片
        if (pictureUploadRequest != null) {
            pictureId = pictureUploadRequest.getId();
        }
        //id不为空，说明是更新图片，校验一下图片是否存在
        // 如果是更新图片，需要校验图片是否存在
        if (pictureId != null) {
            Picture oldpicture = this.getById(pictureId);
            ThrowUtils.throwIf(oldpicture == null, ErrorCode.NOT_FOUND_ERROR);
            //仅本人或管理员可编辑
            if (!oldpicture.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
                throw new BusinessException(ErrorCode.NOT_AUTH_ERROR);
            }
            //校验空间是否一致
            //没传spaceId,复用原来的spaceId
            if (spaceId == null) {
                if (oldpicture.getSpaceId() != null) {
                    spaceId = oldpicture.getSpaceId();
                }
            } else {
                //传了spaceId,校验空间是否和原图片一致
                if (!spaceId.equals(oldpicture.getSpaceId())) {
                    throw new BusinessException(ErrorCode.PARAM_ERROR, "空间id不一致");
                }
            }
        }
        //上传图片，得到信息
        //按照用户id划分目录, 例如：user/123/xxx.jpg==>>按照空间划分目录
        String uploadPathPrefix;
        if (spaceId == null) {
            uploadPathPrefix = String.format("public/%s", loginUser.getId());
        } else {
            uploadPathPrefix = String.format("space/%s", spaceId);
        }

        //根据inputSource判断是上传文件还是上传url
        PictureUploadTemplate pictureUploadTemplate = filePictureUpload;
        if (inputSource instanceof String) {
            pictureUploadTemplate = urlPictureUpload;
        }
        UploadPictureResult uploadPictureResult = pictureUploadTemplate.uploadPicture(inputSource, uploadPathPrefix);
        //封装picture对象，将数据保存到数据库中
        Picture picture = new Picture();
        //补充spaceId
        picture.setSpaceId(spaceId);

        //把结果中的图片信息全部设置到picture对象中
        picture.setUrl(uploadPictureResult.getUrl());


        //填充缩略图url
        picture.setThumbnailUrl(uploadPictureResult.getThumbnailUrl());

//        picture.setName(uploadPictureResult.getPicName());
        picture.setPicSize(uploadPictureResult.getPicSize());
        picture.setPicWidth(uploadPictureResult.getPicWidth());
        picture.setPicScale(uploadPictureResult.getPicScale());
        picture.setPicFormat(uploadPictureResult.getPicFormat());
        picture.setPicHeight(uploadPictureResult.getPicHeight());
        //填充审核参数
        fillReviewParams(picture, loginUser);
        //设置用户id
        picture.setUserId(loginUser.getId());
        //填充图片名称(爬虫获取的照片需要自定义命名)
        String picName = uploadPictureResult.getPicName();
        if (pictureUploadRequest != null && StrUtil.isNotBlank(pictureUploadRequest.getPicName())) {
            picName = pictureUploadRequest.getPicName();
        }
        picture.setName(picName);

        //保存到数据库中
        //如果id不为空，说明是更新图片，更新编辑时间
        if (pictureId != null) {
            //设置空间id
            picture.setId(pictureId);
            picture.setEditTime(new Date());
        }
        //开启事务更新额度
        Long finalSpaceId = spaceId;
        transactionTemplate.execute(status -> {
            boolean result = this.saveOrUpdate(picture);
            ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "图片上传失败");
            if (finalSpaceId != null) {
                boolean update = spaceService.lambdaUpdate()
                        .eq(Space::getId, finalSpaceId)
                        .setSql("totalCount = totalCount + 1")
                        .setSql("totalSize = totalSize + " + uploadPictureResult.getPicSize())
                        .update();
                ThrowUtils.throwIf(!update, ErrorCode.OPERATION_ERROR, "空间额度更新失败");
            }
            return picture;
        });

        return PictureVO.objToVo(picture);
    }

    /**
     * 获取查询包装类
     *
     * @param pictureQueryRequest
     * @return
     */
    public QueryWrapper<Picture> getQueryWrapper(PictureQueryRequest pictureQueryRequest) {
        if (pictureQueryRequest == null) {
            return new QueryWrapper<>();
        }
        //从请求中获取参数(看着请求类写就好了，别忘了还有继承pageRequest的参数)
        Long id = pictureQueryRequest.getId();
        String name = pictureQueryRequest.getName();
        String introduction = pictureQueryRequest.getIntroduction();
        String category = pictureQueryRequest.getCategory();
        List<String> tags = pictureQueryRequest.getTags();
        Long picSize = pictureQueryRequest.getPicSize();
        Integer picWidth = pictureQueryRequest.getPicWidth();
        Integer picHeight = pictureQueryRequest.getPicHeight();
        Double picScale = pictureQueryRequest.getPicScale();
        String picFormat = pictureQueryRequest.getPicFormat();
        Long userId = pictureQueryRequest.getUserId();
        String searchText = pictureQueryRequest.getSearchText();
        //别忘了还有继承pageRequest的参数
        String sortField = pictureQueryRequest.getSortField();
        String sortOrder = pictureQueryRequest.getSortOrder();
        //补充审核参数
        Integer reviewStatus = pictureQueryRequest.getReviewStatus();
        String reviewMessage = pictureQueryRequest.getReviewMessage();
        Long reviewerId = pictureQueryRequest.getReviewerId();

        //空间id
        Long spaceId = pictureQueryRequest.getSpaceId();
        boolean nullSpaceId = pictureQueryRequest.isNullSpaceId();

        Date startEditTime = pictureQueryRequest.getStartEditTime();
        Date endEditTime = pictureQueryRequest.getEndEditTime();


        //下面开写querywrapper
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();

        queryWrapper.ge(ObjUtil.isNotEmpty(startEditTime), "editTime", startEditTime);
        queryWrapper.lt(ObjUtil.isNotEmpty(endEditTime), "editTime", endEditTime);

        //多字段中查询,模糊查询，从名称和简介中查询是否有搜索文本
        if (StrUtil.isNotBlank(searchText)) {
            queryWrapper.and(qw -> qw.like("name", searchText).or().like("introduction", searchText));
        }
        queryWrapper.eq(ObjUtil.isNotEmpty(id), "id", id);
        queryWrapper.eq(ObjUtil.isNotEmpty(name), "name", name);
        queryWrapper.eq(ObjUtil.isNotEmpty(introduction), "introduction", introduction);
        queryWrapper.eq(ObjUtil.isNotEmpty(category), "category", category);
        queryWrapper.eq(ObjUtil.isNotEmpty(picSize), "picSize", picSize);
        queryWrapper.eq(ObjUtil.isNotEmpty(picWidth), "picWidth", picWidth);
        queryWrapper.eq(ObjUtil.isNotEmpty(picHeight), "picHeight", picHeight);
        queryWrapper.eq(ObjUtil.isNotEmpty(picScale), "picScale", picScale);
        queryWrapper.eq(ObjUtil.isNotEmpty(picFormat), "picFormat", picFormat);
        queryWrapper.eq(ObjUtil.isNotEmpty(userId), "userId", userId);
        //补充审核参数
        queryWrapper.eq(ObjUtil.isNotEmpty(reviewStatus), "reviewStatus", reviewStatus);
        queryWrapper.like(StrUtil.isNotBlank(reviewMessage), "reviewMessage", reviewMessage);
        queryWrapper.eq(ObjUtil.isNotEmpty(reviewerId), "reviewerId", reviewerId);

        //新增空间id校验
        queryWrapper.eq(ObjUtil.isNotEmpty(spaceId), "spaceId", spaceId);
        queryWrapper.isNull(nullSpaceId, "spaceId");

        //Json数组查询
        if (ObjUtil.isNotEmpty(tags)) {
            for (String tag : tags) {
                //为了模糊匹配标签字段中的某个完整标签
                queryWrapper.like("tags", "/" + tag + "/");
            }
        }
        //排序
        queryWrapper.orderBy(StrUtil.isNotBlank(sortField), sortOrder.equals("ascend"), sortField);
        return queryWrapper;
    }

    @Override
    public void doPictureReview(PictureReviewRequest pictureReviewRequest, User loginUser) {
        //1.接受参数以及校验参数
        Long id = pictureReviewRequest.getId();
        Integer reviewStatus = pictureReviewRequest.getReviewStatus();
        //因为这里的状态是数字,我们需要转换成枚举类型
        PictureReviewStatusEnum reviewStatusEnum = PictureReviewStatusEnum.getEnumByValue(reviewStatus);
        //校验参数
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAM_ERROR, "图片id不能为空");
        //状态不能为空，并且请求要修改状态不能是审核中
        ThrowUtils.throwIf(reviewStatusEnum == null || PictureReviewStatusEnum.REVIEWING.equals(reviewStatusEnum), ErrorCode.PARAM_ERROR, "审核状态不能为空");
        //2.获取图片，是否为空
        Picture oldpicture = this.getById(id);
        ThrowUtils.throwIf(oldpicture == null, ErrorCode.NOT_FOUND_ERROR);
        if (oldpicture.getReviewStatus().equals(reviewStatusEnum)) {
            ThrowUtils.throwIf(!PictureReviewStatusEnum.REVIEWING.equals(reviewStatusEnum), ErrorCode.PARAM_ERROR, "审核状态不能重复");
        }
        //3.审核（更新库表）
        Picture newpicture = new Picture();
        BeanUtils.copyProperties(pictureReviewRequest, newpicture);
        newpicture.setReviewerId(loginUser.getId());
        newpicture.setReviewTime(new Date());
        //更新
        boolean result = this.updateById(newpicture);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "审核失败");
        //4.返回结果
    }

    /**
     * 获取图片VO
     *
     * @param picture
     * @return
     */
    public PictureVO getPictureVO(Picture picture) {
        //转换为VO
        PictureVO pictureVO = PictureVO.objToVo(picture);
        //关联用户信息
        Long userId = picture.getUserId();
        if (userId != null && userId > 0) {
            User user = userService.getById(userId);
            UserVO userVO = userService.getUserVO(user);
            pictureVO.setUser(userVO);
        }
        return pictureVO;
    }

    /**
     * 分页获取图片VO
     *
     * @param picturePage
     * @param request
     * @return
     */
    @Override
    public Page<PictureVO> getPictureVOPage(Page<Picture> picturePage, HttpServletRequest request) {
        //获取图片列表
        List<Picture> pictureList = picturePage.getRecords();
        //创建一个新的图片VO分页对象，继承自picturePage的当前页码、大小和总记录数
        Page<PictureVO> pictureVOPage = new Page<>(picturePage.getCurrent(), picturePage.getSize(), picturePage.getTotal());
        //如果pictureList为空，返回空的pictureVOPage
        if (ObjUtil.isEmpty(pictureList)) {
            return pictureVOPage;
        }
        //获取pictureList的VO；-->>pictureVOList
        List<PictureVO> pictureVOList = pictureList.stream().map(PictureVO::objToVo).collect(Collectors.toList());
        //1.关联查询用户信息
        //收集所有图片的用户ID，去重，组成set
        Set<Long> userIdSet = pictureVOList.stream().map(PictureVO::getUserId).collect(Collectors.toSet());
        //通过用户ID列表，用刚刚组成的set，查询用户信息，并按用户ID分组，组成map
        Map<Long, List<User>> userIdUserListMap = userService.listByIds(userIdSet).stream()
                .collect(Collectors.groupingBy(User::getId));
        //2.每一个VO都要填充用户信息
        //遍历pictureVOList，为每个图片VO填充对应的用户信息
        pictureVOList.forEach(pictureVO -> {
            Long userId = pictureVO.getUserId();
            User user = null;
            //如果用户ID存在于查询用户信息的结果map中，获取第一个用户对象
            if (userIdUserListMap.containsKey(userId)) {
                user = userIdUserListMap.get(userId).get(0);
            }
            //使用获取的用户信息设置pictureVO的用户属性
            pictureVO.setUser(userService.getUserVO(user));
        });
        //将转换后的pictureVOList设置为pictureVOPage的记录
        pictureVOPage.setRecords(pictureVOList);

        //返回填充了用户信息的pictureVOPage
        return pictureVOPage;
    }


    /**
     * 校验图片数据
     *
     * @param picture
     */
    public void validPicture(Picture picture) {
        ThrowUtils.throwIf(picture == null, ErrorCode.PARAM_ERROR, "图片不能为空");
        Long id = picture.getId();
        String name = picture.getName();
        ThrowUtils.throwIf(ObjUtil.isNull(id) && ObjUtil.isEmpty(name), ErrorCode.PARAM_ERROR, "id和name不能同时为空");
        String introduction = picture.getIntroduction();
        ThrowUtils.throwIf(introduction == null, ErrorCode.PARAM_ERROR, "简介不能为空");
    }

    @Override
    public void fillReviewParams(Picture picture, User loginUser) {
        //管理员自动过审
        if (userService.isAdmin(loginUser)) {
            picture.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());
            picture.setReviewerId(loginUser.getId());
            picture.setReviewTime(new Date());
            picture.setReviewMessage("管理员自动审核通过");
        } else {
            picture.setReviewStatus(PictureReviewStatusEnum.REVIEWING.getValue());
        }
        //非管理员需审核，审核参数改为待审核
    }

    @Override
    public int uploadPictureByBatch(PictureUploadByBatchRequest pictureUploadByBatchRequest, User loginUser) {
        //1.接受参数以及校验参数
        String searchText = pictureUploadByBatchRequest.getSearchText();
        Integer count = pictureUploadByBatchRequest.getCount();
        String namePerfix = pictureUploadByBatchRequest.getNamePerfix();
        if (StrUtil.isBlank(namePerfix)) {
            namePerfix = searchText;
        }
        //校验参数
        ThrowUtils.throwIf(ObjUtil.isEmpty(searchText), ErrorCode.PARAM_ERROR, "搜索文本不能为空");
        ThrowUtils.throwIf(count == null || count <= 0, ErrorCode.PARAM_ERROR, "抓取数量不能为空");
        ThrowUtils.throwIf(count > 30, ErrorCode.PARAM_ERROR, "抓取数量不能超过30");
        //2.要抓取的地址
        String fetchUrl = String.format("https://cn.bing.com/images/async?q=%s&mmasync=1", searchText);
        Document document;
        try {
            document = Jsoup.connect(fetchUrl).get();
        } catch (IOException e) {
            log.error("图片批量上传失败", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "图片批量上传失败");
        }
        Element div = document.getElementsByClass("dgControl").first();
        if (ObjUtil.isNull(div)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "图片批量上传失败");
        }
        Elements imgElementList = div.select("img.mimg");
        int uploadCount = 0;
        for (Element imgElement : imgElementList) {
            String fileUrl = imgElement.attr("src");
            if (StrUtil.isBlank(fileUrl)) {
                log.info("图片批量上传失败，图片地址为空:{}", fileUrl);
                continue;
            }
            //处理图片上传地址，防止出现转义问题
            int questionMarkIndex = fileUrl.indexOf("?");
            if (questionMarkIndex != -1) {
                fileUrl = fileUrl.substring(0, questionMarkIndex);
            }
            PictureUploadRequest pictureUploadRequest = new PictureUploadRequest();
            String picName = namePerfix + (uploadCount + 1);
            //设置图片名称，序号连续递增，如namePerfix为"bing_"，则图片名称为"bing_1"
            pictureUploadRequest.setPicName(picName);

            try {
//                pictureUploadRequest.setPicName(namePerfix);
                PictureVO pictureVO = uploadPicture(fileUrl, pictureUploadRequest, loginUser);
                log.info("图片批量上传成功,id:{}", pictureVO.getId());
                uploadCount++;
            } catch (BusinessException e) {
                log.error("图片批量上传失败", e);
                continue;
            }
            if (uploadCount >= count) {
                break;
            }
        }
        return uploadCount;
    }

    @Async
    @Override
    public void clearPictureFile(Picture oldPicture) {
        // 判断该图片是否被多条记录使用
        String pictureUrl = oldPicture.getUrl();
        long count = this.lambdaQuery()
                .eq(Picture::getUrl, pictureUrl)
                .count();
        // 有不止一条记录用到了该图片，不清理
        if (count > 1) {
            return;
        }
        // FIXME 注意，这里的 url 包含了域名，实际上只要传 key 值（存储路径）就够了
        cosManager.deleteObject(oldPicture.getUrl());
        // 清理缩略图
        String thumbnailUrl = oldPicture.getThumbnailUrl();
        if (StrUtil.isNotBlank(thumbnailUrl)) {
            cosManager.deleteObject(thumbnailUrl);
        }
    }

    @Override
    public void checkPictureAuth(User loginUser, Picture picture) {
        Long spaceId = picture.getSpaceId();
        if (spaceId == null) {
            // 公共图库，仅本人或管理员可操作
            if (!picture.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
                throw new BusinessException(ErrorCode.NOT_AUTH_ERROR);
            }
        } else {
            // 私有空间，仅空间管理员可操作
            if (!picture.getUserId().equals(loginUser.getId())) {
                throw new BusinessException(ErrorCode.NOT_AUTH_ERROR);
            }
        }
    }

    @Override
    public void deletePicture(long pictureId, User loginUser) {
        ThrowUtils.throwIf(pictureId <= 0, ErrorCode.PARAM_ERROR);
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_AUTH_ERROR);
        // 判断是否存在
        Picture oldPicture = this.getById(pictureId);
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);
        // 校验权限
        checkPictureAuth(loginUser, oldPicture);
        // 操作数据库
        boolean result = this.removeById(pictureId);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        // 异步清理文件
        this.clearPictureFile(oldPicture);
    }

    @Override
    public void editPicture(PictureEditRequest pictureEditRequest) {
        Long id = pictureEditRequest.getId();
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAM_ERROR);
        //新建一个picture对象，把新的数据存进去这个对象
        Picture picture = new Picture();
        BeanUtils.copyProperties(pictureEditRequest, picture);
        //把list转为string
        picture.setTags(JSONUtil.toJsonStr(pictureEditRequest.getTags()));
        picture.setEditTime(new Date());
        //验证数据是否合法
        this.validPicture(picture);
        //判断是否存在该图片
        Picture oldPicture = this.getById(id);
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);
//        //仅本人或管理员可以编辑
//        User loginUser = userService.getLoginUser(request);
//        if (!oldPicture.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
//            throw new BusinessException(ErrorCode.NOT_AUTH_ERROR);
//        }
        User loginUser = userService.getLoginUser(request);
        //校验权限
        checkPictureAuth(loginUser, oldPicture);
        //填充审核参数
        this.fillReviewParams(picture, loginUser);
        //存在该照片，把之前存起来的新的数据存进去数据库
        boolean result = this.updateById(picture);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
    }

    @Override
    public CreateOutPaintingTaskResponse createPictureOutPaintingTask(CreatePictureOutPaintingTaskRequest createPictureOutPaintingTaskRequest,User loginUser) {
        ThrowUtils.throwIf(createPictureOutPaintingTaskRequest == null, ErrorCode.PARAM_ERROR);
        Long pictureId = createPictureOutPaintingTaskRequest.getPictureId();
        Picture picture = this.getById(pictureId);
        ThrowUtils.throwIf(picture == null, ErrorCode.NOT_FOUND_ERROR);
        //校验权限
        checkPictureAuth(loginUser, picture);
        //新建一个创建ai扩图任务请求，然后把请求参数填满（填Input和Parameters）
        CreateOutPaintingTaskRequest taskRequest = new CreateOutPaintingTaskRequest();
        //补充Input
        CreateOutPaintingTaskRequest.Input input = new CreateOutPaintingTaskRequest.Input();
        input.setImage_url(picture.getUrl());
        taskRequest.setInput(input);
        //补充Parameters
        BeanUtils.copyProperties(createPictureOutPaintingTaskRequest, taskRequest);
        return aliYunAIApi.createOutPaintingTask(taskRequest);
    };
}



