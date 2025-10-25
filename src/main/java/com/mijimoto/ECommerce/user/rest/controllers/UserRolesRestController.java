
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

import com.mijimoto.ECommerce.user.rest.dto.UserRolesDTO;
import com.mijimoto.ECommerce.user.rest.services.UserRolesService;


/**
 * REST Controller for UserRoles
 * 
 * Provides CRUD operations via HTTP endpoints
 * All exceptions are handled by GlobalExceptionHandler
 * 
 * @author Telosys
 */
@CrossOrigin(origins = "*")
@RestController
@RequestMapping(value = "/api/v1/userroles", produces = MediaType.APPLICATION_JSON_VALUE)
public class UserRolesRestController {

    private static final Logger logger = LoggerFactory.getLogger(UserRolesRestController.class);

    private final UserRolesService service;

    @Autowired
    public UserRolesRestController(UserRolesService service) {
        this.service = service;
    }

    /**
     * GET all UserRoles
     *
     * @return list of UserRoles DTOs
     */
    @GetMapping
    public ResponseEntity<List<UserRolesDTO>> findAll() {
        logger.debug("GET - findAll");
        List<UserRolesDTO> list = service.findAll();
        return ResponseEntity.ok(list);
    }

    /**
     * GET UserRoles by ID
     * 
     * @param id
     * @return UserRoles DTO or 404 if not found
     */
    @GetMapping("/{id}")
    public ResponseEntity<UserRolesDTO> findById(@PathVariable Integer id) {
        logger.debug("GET - findById: {}", id);
        
        UserRolesDTO dto = service.findById(id);
        
        if (dto != null) {
            return ResponseEntity.ok(dto);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * POST - Create new UserRoles
     * 
     * @param userRolesDTO the DTO to create
     * @return 201 Created with the created DTO, or 400/422 on validation error
     */
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<UserRolesDTO> create(@Valid @RequestBody UserRolesDTO userRolesDTO) {
        logger.debug("POST - create: {}", userRolesDTO);
        
        UserRolesDTO created = service.create(userRolesDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * PATCH - Partial update of UserRoles
     * Only updates fields that are present (non-null) in the request body
     * 
     * @param id
     * @param userRolesDTO the DTO with fields to update
     * @return 200 OK with updated DTO, or 404 if not found
     */
    @PatchMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<UserRolesDTO> partialUpdate(
            @PathVariable Integer id,
            @RequestBody UserRolesDTO userRolesDTO) {
        logger.debug("PATCH - partialUpdate: id={}, dto={}", id, userRolesDTO);
        
        UserRolesDTO updated = service.partialUpdate(id, userRolesDTO);
        return ResponseEntity.ok(updated);
    }

    /**
     * DELETE UserRoles by ID
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