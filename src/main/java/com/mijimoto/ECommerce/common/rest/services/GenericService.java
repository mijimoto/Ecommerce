package com.mijimoto.ECommerce.common.rest.services;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;

import java.beans.PropertyDescriptor;
import java.util.HashSet;
import java.util.Set;

/**
 * Abstract class for REST services <br>
 * Enhanced with better entity-DTO conversion and error handling <br>
 * 
 * @author Telosys
 *
 * @param <ENTITY> JPA entity class
 * @param <DTO> Data Transfer Object class
 */
public abstract class GenericService<ENTITY, DTO> {

	private static final Logger logger = LoggerFactory.getLogger(GenericService.class);
	
	private final ModelMapper strictMapper = new ModelMapper();
	private final ModelMapper updateMapper = new ModelMapper();

	private final Class<ENTITY> entityClass;
	private final Class<DTO> dtoClass;

	/**
	 * Constructor
	 * 
	 * @param entityClass the entity class
	 * @param dtoClass the DTO class
	 */
	protected GenericService(Class<ENTITY> entityClass, Class<DTO> dtoClass) {
		super();
		this.entityClass = entityClass;
		this.dtoClass = dtoClass;
		configureMappers();
	}

	/**
	 * Configure ModelMappers for different use cases
	 */
	private void configureMappers() {
		// Strict mapper for full conversions (create/save)
		strictMapper.getConfiguration()
			.setMatchingStrategy(MatchingStrategies.STRICT)
			.setFieldMatchingEnabled(true)
			.setFieldAccessLevel(org.modelmapper.config.Configuration.AccessLevel.PRIVATE)
			.setSkipNullEnabled(false); // Map nulls explicitly
		
		// Update mapper for partial updates (skips nulls)
		updateMapper.getConfiguration()
			.setMatchingStrategy(MatchingStrategies.STRICT)
			.setFieldMatchingEnabled(true)
			.setFieldAccessLevel(org.modelmapper.config.Configuration.AccessLevel.PRIVATE)
			.setSkipNullEnabled(true); // Skip null values during update
	}

	/**
	 * Converts DTO to JPA ENTITY
	 * 
	 * @param dto the DTO to convert
	 * @return the entity
	 */
	protected ENTITY dtoToEntity(DTO dto) {
		if (dto == null) {
			return null;
		}
		try {
			return strictMapper.map(dto, entityClass);
		} catch (Exception e) {
			logger.error("Error converting DTO to Entity using ModelMapper: {}", e.getMessage());
			// Fallback to BeanUtils if ModelMapper fails
			try {
				ENTITY entity = entityClass.getDeclaredConstructor().newInstance();
				BeanUtils.copyProperties(dto, entity);
				return entity;
			} catch (Exception fallbackException) {
				logger.error("Fallback conversion also failed: {}", fallbackException.getMessage());
				throw new RuntimeException("Failed to convert DTO to Entity", fallbackException);
			}
		}
	}

	/**
	 * Converts JPA ENTITY to DTO
	 * 
	 * @param entity the entity to convert
	 * @return the DTO
	 */
	protected DTO entityToDto(ENTITY entity) {
		if (entity == null) {
			return null;
		}
		try {
			return strictMapper.map(entity, dtoClass);
		} catch (Exception e) {
			logger.error("Error converting Entity to DTO using ModelMapper: {}", e.getMessage());
			// Fallback to BeanUtils if ModelMapper fails
			try {
				DTO dto = dtoClass.getDeclaredConstructor().newInstance();
				BeanUtils.copyProperties(entity, dto);
				return dto;
			} catch (Exception fallbackException) {
				logger.error("Fallback conversion also failed: {}", fallbackException.getMessage());
				throw new RuntimeException("Failed to convert Entity to DTO", fallbackException);
			}
		}
	}

	/**
	 * Converts an Optional JPA ENTITY to DTO
	 * 
	 * @param optionalEntity the optional entity
	 * @return the DTO or null if nothing in optional
	 */
	protected DTO entityToDto(Optional<ENTITY> optionalEntity) {
		return optionalEntity.map(this::entityToDto).orElse(null);
	}

	/**
	 * Converts a collection of JPA ENTITIES to a collection of DTOs
	 * 
	 * @param entities the entities to convert
	 * @return list of DTOs
	 */
	protected List<DTO> entityListToDtoList(Iterable<ENTITY> entities) {
		List<DTO> dtoList = new LinkedList<>();
		if (entities != null) {
			for (ENTITY e : entities) {
				DTO dto = entityToDto(e);
				if (dto != null) {
					dtoList.add(dto);
				}
			}
		}
		return dtoList;
	}

	/**
	 * Converts a collection of DTOs to a collection of JPA ENTITIES
	 * 
	 * @param dtos the DTOs to convert
	 * @return list of entities
	 */
	protected List<ENTITY> dtoListToEntityList(Iterable<DTO> dtos) {
		List<ENTITY> entityList = new LinkedList<>();
		if (dtos != null) {
			for (DTO dto : dtos) {
				ENTITY entity = dtoToEntity(dto);
				if (entity != null) {
					entityList.add(entity);
				}
			}
		}
		return entityList;
	}

	/**
	 * Updates an existing entity with values from DTO, ignoring null values
	 * 
	 * @param existingEntity the entity to update
	 * @param dto the DTO containing new values
	 * @return the updated entity
	 */
	protected ENTITY updateEntityFromDto(ENTITY existingEntity, DTO dto) {
		if (existingEntity == null || dto == null) {
			return existingEntity;
		}
		
		try {
			// Use update mapper that skips nulls
			updateMapper.map(dto, existingEntity);
			return existingEntity;
		} catch (Exception e) {
			logger.error("Error updating entity from DTO using ModelMapper: {}", e.getMessage());
			// Fallback to BeanUtils with null-aware copying
			try {
				BeanUtils.copyProperties(dto, existingEntity, getNullPropertyNames(dto));
				return existingEntity;
			} catch (Exception fallbackException) {
				logger.error("Fallback update also failed: {}", fallbackException.getMessage());
				throw new RuntimeException("Failed to update entity from DTO", fallbackException);
			}
		}
	}

	/**
	 * Helper method to get property names that are null in the source object
	 * Used for excluding null properties during BeanUtils.copyProperties
	 * 
	 * @param source the source object
	 * @return array of null property names
	 */
	private String[] getNullPropertyNames(Object source) {
		final BeanWrapper src = new BeanWrapperImpl(source);
		PropertyDescriptor[] pds = src.getPropertyDescriptors();

		Set<String> emptyNames = new HashSet<>();
		for (PropertyDescriptor pd : pds) {
			Object srcValue = src.getPropertyValue(pd.getName());
			if (srcValue == null) {
				emptyNames.add(pd.getName());
			}
		}
		String[] result = new String[emptyNames.size()];
		return emptyNames.toArray(result);
	}

	/**
	 * Validates that all required fields in DTO are not null/empty
	 * Override this method in concrete services to add specific validation
	 * 
	 * @param dto the DTO to validate
	 * @throws IllegalArgumentException if validation fails
	 */
	protected void validateDto(DTO dto) {
		if (dto == null) {
			throw new IllegalArgumentException("DTO cannot be null");
		}
		// Override in concrete services for specific validation
	}

	/**
	 * Logs the operation being performed for debugging
	 * 
	 * @param operation the operation name
	 * @param entityClass the entity class
	 * @param identifier the entity identifier
	 */
	protected void logOperation(String operation, Class<?> entityClass, Object identifier) {
		logger.debug("Operation: {} on {} with identifier: {}", 
			operation, entityClass.getSimpleName(), identifier);
	}

	/**
	 * Creates a standardized error message for foreign key validation failures
	 * 
	 * @param entityName the entity name
	 * @param entityId the entity ID
	 * @return formatted error message
	 */
	protected String createForeignKeyErrorMessage(String entityName, Object entityId) {
		return String.format("Referenced %s with ID '%s' does not exist", entityName, entityId);
	}

	/**
	 * Creates a standardized error message for entity not found scenarios
	 * 
	 * @param entityName the entity name
	 * @param entityId the entity ID
	 * @return formatted error message
	 */
	protected String createEntityNotFoundErrorMessage(String entityName, Object entityId) {
		return String.format("%s with ID '%s' was not found", entityName, entityId);
	}
}