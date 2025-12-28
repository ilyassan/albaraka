package com.ilyassan.albaraka.repository;

import com.ilyassan.albaraka.entity.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {

    Optional<Document> findByTransactionId(Long transactionId);

    boolean existsByTransactionId(Long transactionId);
}
