/*
 * Created on 2025-10-24 ( 23:12:17 )
 * Generated manually following Telosys conventions
 */

package com.mijimoto.ECommerce.persistence.entities;

import java.io.Serializable;
import java.util.Date;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Entity
@Table(name = "user_tokens", schema = "dbo", catalog = "EcommerceDB")
public class UserTokens implements Serializable {

    private static final long serialVersionUID = 1L;

    // --- PRIMARY KEY
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", nullable = false)
    private Integer id;

    // --- FOREIGN KEYS
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private Users users;

    @ManyToOne
    @JoinColumn(name = "session_id", nullable = false)
    private UserSessions userSessions;

    // --- OTHER DATA FIELDS
    @Column(name = "token_hash", nullable = false, length = 255, unique = true)
    private String tokenHash;

    @Column(name = "token_type", length = 50, nullable = false)
    private String tokenType; // e.g., REFRESH, API_KEY

    @Column(name = "is_revoked", nullable = false)
    private Boolean isRevoked;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "created_at", nullable = false)
    private Date createdAt;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "expires_at", nullable = false)
    private Date expiresAt;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "revoked_at")
    private Date revokedAt;
}
