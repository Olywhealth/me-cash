package com.mecash.repository;

import java.util.Optional;

import com.mecash.entity.AccountUser;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<AccountUser, Long> {

    Optional<AccountUser> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);
}
