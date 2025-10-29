
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

import com.mijimoto.ECommerce.user.rest.dto.UserPhoneNumbersDTO;
import com.mijimoto.ECommerce.user.rest.services.UserPhoneNumbersService;


/**
 * REST Controller for UserPhoneNumbers
 * 
 * Provides CRUD operations via HTTP endpoints
 * All exceptions are handled by GlobalExceptionHandler
 * 
 * @author Telosys
 */
@CrossOrigin(origins = "*")
@RestController
@RequestMapping(value = "/api/v1/userphonenumbers", produces = MediaType.APPLICATION_JSON_VALUE)
public class UserPhoneNumbersRestController {

    private static final Logger logger = LoggerFactory.getLogger(UserPhoneNumbersRestController.class);

    private final UserPhoneNumbersService service;

    @Autowired
    public UserPhoneNumbersRestController(UserPhoneNumbersService service) {
        this.service = service;
    }

    /**
     * GET all UserPhoneNumbers
     *
     * @return list of UserPhoneNumbers DTOs
     */
    @GetMapping
    public ResponseEntity<List<UserPhoneNumbersDTO>> findAll() {
        logger.debug("GET - findAll");
        List<UserPhoneNumbersDTO> list = service.findAll();
        return ResponseEntity.ok(list);
    }

    /**
     * GET UserPhoneNumbers by ID
     * 
     * @param id
     * @return UserPhoneNumbers DTO or 404 if not found
     */
    @GetMapping("/{id}")
    public ResponseEntity<UserPhoneNumbersDTO> findById(@PathVariable Integer id) {
        logger.debug("GET - findById: {}", id);
        
        UserPhoneNumbersDTO dto = service.findById(id);
        
        if (dto != null) {
            return ResponseEntity.ok(dto);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * POST - Create new UserPhoneNumbers
     * 
     * @param userPhoneNumbersDTO the DTO to create
     * @return 201 Created with the created DTO, or 400/422 on validation error
     */
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<UserPhoneNumbersDTO> create(@Valid @RequestBody UserPhoneNumbersDTO userPhoneNumbersDTO) {
        logger.debug("POST - create: {}", userPhoneNumbersDTO);
        
        UserPhoneNumbersDTO created = service.create(userPhoneNumbersDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * PATCH - Partial update of UserPhoneNumbers
     * Only updates fields that are present (non-null) in the request body
     * 
     * @param id
     * @param userPhoneNumbersDTO the DTO with fields to update
     * @return 200 OK with updated DTO, or 404 if not found
     */
    @PatchMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<UserPhoneNumbersDTO> partialUpdate(
            @PathVariable Integer id,
            @RequestBody UserPhoneNumbersDTO userPhoneNumbersDTO) {
        logger.debug("PATCH - partialUpdate: id={}, dto={}", id, userPhoneNumbersDTO);
        
        UserPhoneNumbersDTO updated = service.partialUpdate(id, userPhoneNumbersDTO);
        return ResponseEntity.ok(updated);
    }

    /**
     * DELETE UserPhoneNumbers by ID
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