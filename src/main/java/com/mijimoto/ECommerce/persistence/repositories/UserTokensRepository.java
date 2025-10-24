/*
 * Created on 2025-10-24 ( 23:12:45 )
 * Generated manually following Telosys conventions
 */
package com.mijimoto.ECommerce.persistence.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.List;

import com.mijimoto.ECommerce.persistence.entities.UserTokens;

/**
 * Spring Data JPA repository for entity "UserTokens"
 *
 * Provides CRUD and token-specific finders.
 */
public interface UserTokensRepository extends JpaRepository<UserTokens, Integer> {

    Optional<UserTokens> findByTokenHash(String tokenHash);

    List<UserTokens> findByUsers_IdAndIsRevokedFalse(Integer userId);

    List<UserTokens> findByUserSessions_Id(Integer sessionId);
}
