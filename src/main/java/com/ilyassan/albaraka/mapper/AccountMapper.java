package com.ilyassan.albaraka.mapper;

import com.ilyassan.albaraka.entity.Account;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.Map;

@Mapper(componentModel = "spring")
public interface AccountMapper {

    @Mapping(target = "id", source = "id")
    @Mapping(target = "accountNumber", source = "accountNumber")
    @Mapping(target = "balance", source = "balance")
    @Mapping(target = "createdAt", source = "createdAt")
    @Mapping(target = "updatedAt", source = "updatedAt")
    Map<String, Object> toAccountResponse(Account account);
}
