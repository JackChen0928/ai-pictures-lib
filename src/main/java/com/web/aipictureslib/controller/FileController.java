package com.web.aipictureslib.controller;

import com.qcloud.cos.model.COSObject;
import com.qcloud.cos.model.COSObjectInputStream;
import com.qcloud.cos.utils.IOUtils;
import com.web.aipictureslib.annotation.AuthCheck;
import com.web.aipictureslib.common.BaseResponse;
import com.web.aipictureslib.common.ResultUtils;
import com.web.aipictureslib.constant.UserConstant;
import com.web.aipictureslib.exception.BusinessException;
import com.web.aipictureslib.exception.ErrorCode;
import com.web.aipictureslib.manager.CosManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;

@RestController
@RequestMapping("/file")
@Slf4j
public class FileController {
    @Resource
    private CosManager cosManager;
    /**
     * 测试文件上传（管理员）
     *
     * @param multipartFile
     * @return
     */
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @PostMapping("/test/upload")
    public BaseResponse<String> testUploadFile(@RequestPart("file") MultipartFile multipartFile) {
        // 先得到文件的原始名字，然后加上前缀/test变成文件目录
        String filename = multipartFile.getOriginalFilename();
        String filepath = String.format("/test/%s", filename);
        //新建一个临时 文件
        File file = null;
        try {
            //新建一个临时文件，把刚刚生成的文件目录作为临时文件的名字，null 表示不指定后缀（suffix），此时系统会默认使用 .tmp 作为后缀
            file = File.createTempFile(filepath, null);
            //将用户上传的 MultipartFile 文件内容写入到刚刚创建的临时文件 file 中。
            multipartFile.transferTo(file);
            //调用 cosManager 上传文件到 COS
            cosManager.putObject(filepath, file);
            // 返回可访问地址
            return ResultUtils.success(filepath);
        } catch (Exception e) {
            log.error("file upload error, filepath = " + filepath, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "上传失败");
        } finally {
            if (file != null) {
                // 删除创建的临时文件
                boolean delete = file.delete();
                if (!delete) {
                    log.error("file delete error, filepath = {}", filepath);
                }
            }
        }
    }

    /**
     * 测试文件下载（管理员）
     *
     * @param filepath 文件路径
     * @param response 响应对象
     */
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @GetMapping("/test/download/")
    public void testDownloadFile(String filepath, HttpServletResponse response) throws IOException {
        // 初始化COS对象输入流为null，用于后续获取和处理对象内容
        COSObjectInputStream cosObjectInput = null;
        try {
            // 通过CosManager的下载图片方法，获取指定文件目录的图片对象
            COSObject cosObject = cosManager.getObject(filepath);
            // 获取对象的内容流
            cosObjectInput = cosObject.getObjectContent();
            // 处理下载到的流
            byte[] bytes = IOUtils.toByteArray(cosObjectInput);
            // 设置响应头
            response.setContentType("application/octet-stream;charset=UTF-8");
            response.setHeader("Content-Disposition", "attachment; filename=" + filepath);
            // 写入响应
            response.getOutputStream().write(bytes);
            response.getOutputStream().flush();
        } catch (Exception e) {
            // 记录文件下载错误日志
            log.error("file download error, filepath = " + filepath, e);
            // 抛出业务异常，指示系统错误
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "下载失败");
        } finally {
            // 在finally块中确保COS对象输入流被关闭
            if (cosObjectInput != null) {
                cosObjectInput.close();
            }
        }

    }


}
