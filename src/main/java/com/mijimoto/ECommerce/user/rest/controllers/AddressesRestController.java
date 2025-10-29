
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

import com.mijimoto.ECommerce.user.rest.dto.AddressesDTO;
import com.mijimoto.ECommerce.user.rest.services.AddressesService;


/**
 * REST Controller for Addresses
 * 
 * Provides CRUD operations via HTTP endpoints
 * All exceptions are handled by GlobalExceptionHandler
 * 
 * @author Telosys
 */
@CrossOrigin(origins = "*")
@RestController
@RequestMapping(value = "/api/v1/addresses", produces = MediaType.APPLICATION_JSON_VALUE)
public class AddressesRestController {

    private static final Logger logger = LoggerFactory.getLogger(AddressesRestController.class);

    private final AddressesService service;

    @Autowired
    public AddressesRestController(AddressesService service) {
        this.service = service;
    }

    /**
     * GET all Addresses
     *
     * @return list of Addresses DTOs
     */
    @GetMapping
    public ResponseEntity<List<AddressesDTO>> findAll() {
        logger.debug("GET - findAll");
        List<AddressesDTO> list = service.findAll();
        return ResponseEntity.ok(list);
    }

    /**
     * GET Addresses by ID
     * 
     * @param id
     * @return Addresses DTO or 404 if not found
     */
    @GetMapping("/{id}")
    public ResponseEntity<AddressesDTO> findById(@PathVariable Integer id) {
        logger.debug("GET - findById: {}", id);
        
        AddressesDTO dto = service.findById(id);
        
        if (dto != null) {
            return ResponseEntity.ok(dto);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * POST - Create new Addresses
     * 
     * @param addressesDTO the DTO to create
     * @return 201 Created with the created DTO, or 400/422 on validation error
     */
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AddressesDTO> create(@Valid @RequestBody AddressesDTO addressesDTO) {
        logger.debug("POST - create: {}", addressesDTO);
        
        AddressesDTO created = service.create(addressesDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * PATCH - Partial update of Addresses
     * Only updates fields that are present (non-null) in the request body
     * 
     * @param id
     * @param addressesDTO the DTO with fields to update
     * @return 200 OK with updated DTO, or 404 if not found
     */
    @PatchMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AddressesDTO> partialUpdate(
            @PathVariable Integer id,
            @RequestBody AddressesDTO addressesDTO) {
        logger.debug("PATCH - partialUpdate: id={}, dto={}", id, addressesDTO);
        
        AddressesDTO updated = service.partialUpdate(id, addressesDTO);
        return ResponseEntity.ok(updated);
    }

    /**
     * DELETE Addresses by ID
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