package com.web.aipictureslib.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.web.aipictureslib.model.VO.space.analyze.SpaceCategoryAnalyzeResponse;
import com.web.aipictureslib.model.VO.space.analyze.SpaceTagAnalyzeResponse;
import com.web.aipictureslib.model.VO.space.analyze.SpaceUsageAnalyzeResponse;
import com.web.aipictureslib.model.dto.space.analyze.SpaceAnalyzeRequest;
import com.web.aipictureslib.model.dto.space.analyze.SpaceCategoryAnalyzeRequest;
import com.web.aipictureslib.model.dto.space.analyze.SpaceTagAnalyzeRequest;
import com.web.aipictureslib.model.dto.space.analyze.SpaceUsageAnalyzeRequest;
import com.web.aipictureslib.model.entity.Space;
import com.web.aipictureslib.model.entity.User;

import java.util.List;


/**
 * @author czj24
 */
public interface SpaceAnalyzeService extends IService<Space> {

    SpaceUsageAnalyzeResponse getSpaceUsageAnalyze(SpaceUsageAnalyzeRequest spaceUsageAnalyzeRequest, User loginUser);

    List<SpaceCategoryAnalyzeResponse> getSpaceCategoryAnalyze(SpaceCategoryAnalyzeRequest spaceCategoryAnalyzeRequest, User loginUser);

    List<SpaceTagAnalyzeResponse> getSpaceTagAnalyze(SpaceTagAnalyzeRequest spaceTagAnalyzeRequest, User loginUser);
}
