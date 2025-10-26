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

/**
 * REST Controller for Users
 * 
 * Provides CRUD operations via HTTP endpoints
 * All exceptions are handled by GlobalExceptionHandler
 * 
 * @author Telosys
 */
@CrossOrigin(origins = "*")
@RestController
@RequestMapping(value = "/api/v1/users", produces = MediaType.APPLICATION_JSON_VALUE)
public class UsersRestController {
    private static final Logger logger = LoggerFactory.getLogger(UsersRestController.class);
    
    private final UsersService service;

    @Autowired
    public UsersRestController(UsersService service) {
        this.service = service;
    }

    /**
     * GET all Users
     *
     * @return list of Users DTOs
     */
    @GetMapping
    public ResponseEntity<List<UsersDTO>> findAll() {
        logger.debug("GET - findAll");
        List<UsersDTO> list = service.findAll();
        
        // Remove password hashes from response for security
        list.forEach(dto -> dto.setPasswordHash(null));
        
        return ResponseEntity.ok(list);
    }

    /**
     * GET Users by ID
     * 
     * @param id
     * @return Users DTO or 404 if not found
     */
    @GetMapping("/{id}")
    public ResponseEntity<UsersDTO> findById(@PathVariable Integer id) {
        logger.debug("GET - findById: {}", id);
        
        UsersDTO dto = service.findById(id);
        
        if (dto != null) {
            // Remove password hash from response for security
            dto.setPasswordHash(null);
            return ResponseEntity.ok(dto);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * POST - Register new user
     * 
     * @param usersDTO the DTO to register (passwordHash field contains plain password)
     * @return 201 Created with the created DTO, or 400/409 on error
     */
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<UsersDTO> register(@Valid @RequestBody UsersDTO usersDTO) {
        logger.debug("POST - register: {}", usersDTO.getEmail());
        
        UsersDTO created = service.register(usersDTO);
        
        // Remove password hash from response for security
        created.setPasswordHash(null);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * PATCH - Partial update of Users
     * Only updates fields that are present (non-null) in the request body
     * 
     * @param id
     * @param usersDTO the DTO with fields to update
     * @return 200 OK with updated DTO, or 404 if not found
     */
    @PatchMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<UsersDTO> partialUpdate(
            @PathVariable Integer id,
            @RequestBody UsersDTO usersDTO) {
        logger.debug("PATCH - partialUpdate: id={}, dto={}", id, usersDTO);
        
        UsersDTO updated = service.partialUpdate(id, usersDTO);
        
        // Remove password hash from response for security
        updated.setPasswordHash(null);
        
        return ResponseEntity.ok(updated);
    }

    /**
     * DELETE Users by ID
     * 
     * @param id
     * @return 204 No Content if deleted, or 404 if not found
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteById(@PathVariable Integer id) {
        logger.debug("DELETE - deleteById: {}", id);
        
        service.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}