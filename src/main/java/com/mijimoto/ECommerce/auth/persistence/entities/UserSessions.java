/*
 * Created on 2025-10-24 ( 23:12:01 )
 * Generated manually following Telosys conventions
 */

package com.mijimoto.ECommerce.auth.persistence.entities;

import java.io.Serializable;
import java.util.Date;

import com.mijimoto.ECommerce.user.persistence.entities.Users;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@Entity
@Table(name = "user_sessions", schema = "dbo", catalog = "EcommerceDB")
public class UserSessions implements Serializable {

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

    // --- OTHER DATA FIELDS
    @Column(name = "session_uuid", nullable = false, length = 64, unique = true)
    private String sessionUuid;

    @Column(name = "device_name", length = 255)
    private String deviceName;

    @Column(name = "device_type", length = 100)
    private String deviceType;

    @Column(name = "ip_address", length = 100)
    private String ipAddress;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

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
