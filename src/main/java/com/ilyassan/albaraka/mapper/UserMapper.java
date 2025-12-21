package com.ilyassan.albaraka.mapper;

import com.ilyassan.albaraka.dto.UserResponse;
import com.ilyassan.albaraka.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "role", expression = "java(user.getRole().name())")
    UserResponse toUserResponse(User user);
}
