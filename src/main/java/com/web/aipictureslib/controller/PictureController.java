package com.web.aipictureslib.controller;

import com.web.aipictureslib.annotation.AuthCheck;
import com.web.aipictureslib.common.BaseResponse;
import com.web.aipictureslib.common.ResultUtils;
import com.web.aipictureslib.constant.UserConstant;
import com.web.aipictureslib.exception.ErrorCode;
import com.web.aipictureslib.exception.ThrowUtils;
import com.web.aipictureslib.manager.FileManager;
import com.web.aipictureslib.model.VO.PictureVO;
import com.web.aipictureslib.model.dto.file.UploadPictureResult;
import com.web.aipictureslib.model.dto.picture.PictureUploadRequest;
import com.web.aipictureslib.model.entity.User;
import com.web.aipictureslib.service.PictureService;
import com.web.aipictureslib.service.UserService;
import org.apache.coyote.Request;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

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
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<PictureVO> uploadPicture(@RequestPart("file") MultipartFile multipartFile, PictureUploadRequest pictureUploadRequest) {
        ThrowUtils.throwIf(multipartFile == null, ErrorCode.PARAM_ERROR);
        User user = userService.getLoginUser(request);
        PictureVO pictureVO = pictureService.uploadPicture(multipartFile, pictureUploadRequest,user);
        return ResultUtils.success(pictureVO);
    }

}
