package com.mijimoto.ECommerce.user.rest.controllers;

import java.util.List;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.mijimoto.ECommerce.user.rest.dto.UsersDTO;
import com.mijimoto.ECommerce.user.rest.services.UsersService;
import com.mijimoto.ECommerce.auth.services.RedisTokenService;

/**
 * REST Controller for Users
 *
 * Provides CRUD operations via HTTP endpoints and a verification endpoint.
 */
@CrossOrigin(origins = "*")
@RestController
@RequestMapping(value = "/api/v1/users", produces = MediaType.APPLICATION_JSON_VALUE)
public class UsersRestController {
    private static final Logger logger = LoggerFactory.getLogger(UsersRestController.class);

    private final UsersService service;
    private final RedisTokenService redisTokenService;

    @Autowired
    public UsersRestController(UsersService service, RedisTokenService redisTokenService) {
        this.service = service;
        this.redisTokenService = redisTokenService;
    }

    @GetMapping
    public ResponseEntity<List<UsersDTO>> findAll() {
        logger.debug("GET - findAll");
        List<UsersDTO> list = service.findAll();
        list.forEach(dto -> dto.setPasswordHash(null));
        return ResponseEntity.ok(list);
    }

    @GetMapping("/{id}")
    public ResponseEntity<UsersDTO> findById(@PathVariable Integer id) {
        logger.debug("GET - findById: {}", id);
        UsersDTO dto = service.findById(id);
        if (dto != null) {
            dto.setPasswordHash(null);
            return ResponseEntity.ok(dto);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<UsersDTO> register(@Valid @RequestBody UsersDTO usersDTO) {
        logger.debug("POST - register: {}", usersDTO.getEmail());
        UsersDTO created = service.register(usersDTO);
        created.setPasswordHash(null);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PatchMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<UsersDTO> partialUpdate(
            @PathVariable Integer id,
            @RequestBody UsersDTO usersDTO) {
        logger.debug("PATCH - partialUpdate: id={}, dto={}", id, usersDTO);
        UsersDTO updated = service.partialUpdate(id, usersDTO);
        updated.setPasswordHash(null);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteById(@PathVariable Integer id) {
        logger.debug("DELETE - deleteById: {}", id);
        service.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Verify email using code previously sent by MailService.
     * On success: activates user (isActive=true) and returns 200.
     * On failure: 400 Bad Request
     */
    @GetMapping("/verify")
    public ResponseEntity<String> verifyEmail(@RequestParam("code") String code) {
        logger.debug("GET - verifyEmail: code={}", code);
        if (code == null || code.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Invalid verification code");
        }

        String email = redisTokenService.consumeVerificationCode(code);
        if (email == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Verification code expired or invalid");
        }

        service.activateUserByEmail(email);
        return ResponseEntity.ok("Email verified");
    }
}
