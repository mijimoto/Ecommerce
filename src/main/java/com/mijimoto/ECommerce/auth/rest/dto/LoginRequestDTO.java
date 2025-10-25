/*
 * Created on 2025-10-25 ( 00:52:00 )
 * Generated manually following Telosys DTO conventions
 */
package com.mijimoto.ECommerce.auth.rest.dto;

import java.io.Serializable;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO class for Login Request
 */
@Data
@NoArgsConstructor
public class LoginRequestDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    //--- DATA FIELDS 
    private String email;
    private String password;
    private String deviceName;
    private String deviceType;
    private String ip;
}
