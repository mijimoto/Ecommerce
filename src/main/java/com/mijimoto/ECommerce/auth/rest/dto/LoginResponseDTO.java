/*
 * Created on 2025-10-25 ( 00:52:00 )
 * Generated manually following Telosys DTO conventions
 */
package com.mijimoto.ECommerce.auth.rest.dto;

import java.io.Serializable;
import java.util.Date;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO class for Login Response
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponseDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    //--- DATA FIELDS 
    private String accessToken;
    private String refreshToken;
    private Date expiresAt;
    private String sessionUuid;
}
