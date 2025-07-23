package com.web.aipictureslib.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.web.aipictureslib.model.entity.User;
import org.apache.ibatis.annotations.Mapper;

/**
* @author czj24
* @description 针对表【user(用户)】的数据库操作Mapper
* @createDate 2025-07-09 11:46:09
* @Entity generator.domain.User
*/
@Mapper
public interface UserMapper extends BaseMapper<User> {

}




