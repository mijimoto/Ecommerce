
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

import com.mijimoto.ECommerce.user.rest.dto.RolesDTO;
import com.mijimoto.ECommerce.user.rest.services.RolesService;


/**
 * REST Controller for Roles
 * 
 * Provides CRUD operations via HTTP endpoints
 * All exceptions are handled by GlobalExceptionHandler
 * 
 * @author Telosys
 */
@CrossOrigin(origins = "*")
@RestController
@RequestMapping(value = "/api/v1/roles", produces = MediaType.APPLICATION_JSON_VALUE)
public class RolesRestController {

    private static final Logger logger = LoggerFactory.getLogger(RolesRestController.class);

    private final RolesService service;

    @Autowired
    public RolesRestController(RolesService service) {
        this.service = service;
    }

    /**
     * GET all Roles
     *
     * @return list of Roles DTOs
     */
    @GetMapping
    public ResponseEntity<List<RolesDTO>> findAll() {
        logger.debug("GET - findAll");
        List<RolesDTO> list = service.findAll();
        return ResponseEntity.ok(list);
    }

    /**
     * GET Roles by ID
     * 
     * @param id
     * @return Roles DTO or 404 if not found
     */
    @GetMapping("/{id}")
    public ResponseEntity<RolesDTO> findById(@PathVariable Integer id) {
        logger.debug("GET - findById: {}", id);
        
        RolesDTO dto = service.findById(id);
        
        if (dto != null) {
            return ResponseEntity.ok(dto);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * POST - Create new Roles
     * 
     * @param rolesDTO the DTO to create
     * @return 201 Created with the created DTO, or 400/422 on validation error
     */
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<RolesDTO> create(@Valid @RequestBody RolesDTO rolesDTO) {
        logger.debug("POST - create: {}", rolesDTO);
        
        RolesDTO created = service.create(rolesDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * PATCH - Partial update of Roles
     * Only updates fields that are present (non-null) in the request body
     * 
     * @param id
     * @param rolesDTO the DTO with fields to update
     * @return 200 OK with updated DTO, or 404 if not found
     */
    @PatchMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<RolesDTO> partialUpdate(
            @PathVariable Integer id,
            @RequestBody RolesDTO rolesDTO) {
        logger.debug("PATCH - partialUpdate: id={}, dto={}", id, rolesDTO);
        
        RolesDTO updated = service.partialUpdate(id, rolesDTO);
        return ResponseEntity.ok(updated);
    }

    /**
     * DELETE Roles by ID
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