package com.ilyassan.albaraka.mapper;

import com.ilyassan.albaraka.dto.AccountResponse;
import com.ilyassan.albaraka.entity.Account;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AccountMapper {
    AccountResponse toAccountResponse(Account account);
}
