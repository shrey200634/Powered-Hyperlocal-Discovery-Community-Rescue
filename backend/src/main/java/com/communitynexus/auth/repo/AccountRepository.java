package com.communitynexus.auth.repo;

import com.communitynexus.auth.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Long> {

    Optional<Account> findByPhone(String phone);

    boolean existsByPhone(String phone);
}