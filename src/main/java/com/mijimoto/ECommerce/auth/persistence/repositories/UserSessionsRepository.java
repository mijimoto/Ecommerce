/*
 * Created on 2025-10-24 ( 23:12:32 )
 * Generated manually following Telosys conventions
 */
package com.mijimoto.ECommerce.auth.persistence.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

import com.mijimoto.ECommerce.auth.persistence.entities.UserSessions;

/**
 * Spring Data JPA repository for entity "UserSessions"
 *
 * Provides basic CRUD and custom finders for sessions.
 */
public interface UserSessionsRepository extends JpaRepository<UserSessions, Integer> {

    Optional<UserSessions> findBySessionUuid(String sessionUuid);

    List<UserSessions> findByUsers_IdAndIsActiveTrue(Integer userId);
}
