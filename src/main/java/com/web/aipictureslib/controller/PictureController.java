package com.web.aipictureslib.controller;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
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
import com.web.aipictureslib.model.entity.Picture;
import com.web.aipictureslib.model.entity.User;
import com.web.aipictureslib.model.enums.PictureReviewStatusEnum;
import com.web.aipictureslib.service.PictureService;
import com.web.aipictureslib.service.UserService;
import org.apache.coyote.Request;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

import java.util.Date;

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

    @PostMapping("/delele")
    public BaseResponse<Boolean> deletePicture(@RequestBody DeleteRequest deleteRequest) {
        User loginUser = userService.getLoginUser(request);
        Long id = deleteRequest.getId();
        if (id == null || id <= 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR);
        }
        //本人和管理员可删
        Picture oldpicture = pictureService.getById(id);
        ThrowUtils.throwIf(oldpicture == null, ErrorCode.NOT_FOUND_ERROR);
        //如果不是管理员或者不是本人的图片，不能删
        if (!userService.isAdmin(loginUser) && !loginUser.getId().equals(oldpicture.getUserId())){
        ThrowUtils.throwIf(!userService.isAdmin(loginUser), ErrorCode.NOT_AUTH_ERROR);
        }
        boolean result = pictureService.removeById(id);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
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
        Long id = pictureEditRequest.getId();
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAM_ERROR);
        //新建一个picture对象，把新的数据存进去这个对象
        Picture picture = new Picture();
        BeanUtils.copyProperties(pictureEditRequest, picture);
        //把list转为string
        picture.setTags(JSONUtil.toJsonStr(pictureEditRequest.getTags()));
        picture.setEditTime(new Date());
        //验证数据是否合法
        pictureService.validPicture(picture);
        //判断是否存在该图片
        Picture oldPicture = pictureService.getById(id);
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);
        //仅本人或管理员可以编辑
        User loginUser = userService.getLoginUser(request);
        if (!oldPicture.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NOT_AUTH_ERROR);
        }
        //填充审核参数
        pictureService.fillReviewParams(picture, loginUser);
        //存在该照片，把之前存起来的新的数据存进去数据库
        boolean result = pictureService.updateById(picture);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
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
        //普通用户默认只能查看已通过审核的照片
        pictureQueryRequest.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());
        //先查询数据库获得不脱敏的数据页
        Page<Picture> picturePage = pictureService.page(new Page<>(current, size),pictureService.getQueryWrapper(pictureQueryRequest));
        //通过不脱敏的数据页，再获得脱敏的数据页
        Page<PictureVO> pictureVOPage = pictureService.getPictureVOPage(picturePage, request);
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
