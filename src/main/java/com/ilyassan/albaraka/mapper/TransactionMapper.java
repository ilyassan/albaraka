package com.ilyassan.albaraka.mapper;

import com.ilyassan.albaraka.dto.TransactionResponse;
import com.ilyassan.albaraka.entity.Transaction;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface TransactionMapper {

    @Mapping(target = "status", expression = "java(transaction.getStatus().name())")
    TransactionResponse toTransactionResponse(Transaction transaction);
}
