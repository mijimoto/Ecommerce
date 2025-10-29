package com.mijimoto.ECommerce.user.rest.services;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.mijimoto.ECommerce.user.persistence.entities.Users;
import com.mijimoto.ECommerce.user.persistence.repositories.UsersRepository;
import com.mijimoto.ECommerce.user.rest.dto.UsersDTO;
import com.mijimoto.ECommerce.common.rest.services.GenericService;
import com.mijimoto.ECommerce.common.mail.MailService;
import com.mijimoto.ECommerce.auth.services.RedisTokenService;

/**
 * REST service for entity "Users"
 *
 * This service provides the necessary operations required by the REST controller.
 * It includes registration (with password hashing), partial updates, activation,
 * password update, basic finders and delete.
 */
@Service
@Transactional
public class UsersService extends GenericService<Users, UsersDTO> {

    private static final Logger logger = LoggerFactory.getLogger(UsersService.class);

    private final UsersRepository repository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final RedisTokenService redisTokenService;
    private final MailService mailService;

    /**
     * TTL for verification codes (seconds). Default 15 minutes.
     * Adjust if required.
     */
    private static final long VERIFICATION_TTL_SECONDS = 15 * 60L;

    public UsersService(UsersRepository repository,
                        BCryptPasswordEncoder passwordEncoder,
                        RedisTokenService redisTokenService,
                        MailService mailService) {
        super(Users.class, UsersDTO.class);
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.redisTokenService = redisTokenService;
        this.mailService = mailService;
    }

    private Integer getEntityId(UsersDTO dto) {
        return dto.getId();
    }

    private void validateForeignKeyReferences(UsersDTO dto) {
        // Implement FK validation if UsersDTO has foreign keys
    }

    public List<UsersDTO> findAll() {
        logger.debug("findAll()");
        Iterable<Users> all = repository.findAll();
        return entityListToDtoList(all);
    }

    public UsersDTO findById(Integer id) {
        logger.debug("findById({})", id);
        Optional<Users> optionalEntity = repository.findById(id);
        return entityToDto(optionalEntity);
    }

    public UsersDTO findByIdOrThrow(Integer id) {
        logger.debug("findByIdOrThrow({})", id);
        Optional<Users> optionalEntity = repository.findById(id);
        if (!optionalEntity.isPresent()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    String.format("Users with id %s not found", id));
        }
        return entityToDto(optionalEntity.get());
    }

    /**
     * Register a new user and send an email verification code.
     *
     * - Creates a user with isActive = false (unless explicitly set).
     * - Hashes password.
     * - Stores a verification code in Redis (ttl = VERIFICATION_TTL_SECONDS).
     * - Delegates email sending to MailService (MailService owns content/template).
     */
    public UsersDTO register(UsersDTO dto) {
        logger.debug("register() - email: {}", dto.getEmail());

        if (dto.getEmail() == null || dto.getEmail().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email is required");
        }
        if (dto.getPasswordHash() == null || dto.getPasswordHash().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password is required");
        }

        Optional<Users> existing = repository.findByEmail(dto.getEmail());
        if (existing.isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "User with email " + dto.getEmail() + " already exists");
        }

        // Hash the password
        String hashedPassword = passwordEncoder.encode(dto.getPasswordHash());
        dto.setPasswordHash(hashedPassword);

        // Default to inactive — require email verification to activate
        if (dto.getIsActive() == null) {
            dto.setIsActive(false);
        }

        Instant now = Instant.now();
        dto.setCreatedAt(Date.from(now));
        dto.setUpdatedAt(Date.from(now));

        validateDto(dto);
        validateForeignKeyReferences(dto);

        Users entity = dtoToEntity(dto);
        Users saved = repository.save(entity);

        // generate verification code and store in redis
        String verificationCode = generateVerificationCode();
        redisTokenService.storeVerificationCode(verificationCode, saved.getEmail(), VERIFICATION_TTL_SECONDS);

        // Delegate mail sending to MailService (MailService knows body/link)
        try {
            mailService.sendVerificationEmail(saved.getEmail(), verificationCode, VERIFICATION_TTL_SECONDS);
            logger.info("Verification email queued for: {}", saved.getEmail());
        } catch (Exception e) {
            // don't reveal internal details to client; log for ops
            logger.error("Failed to send verification email to {}: {}", saved.getEmail(), e.getMessage());
        }

        logger.info("User registered (pending verification) - id: {}, email: {}", saved.getId(), saved.getEmail());
        return entityToDto(saved);
    }

    /**
     * Activate a user account by email (e.g. after email verification).
     */
    public UsersDTO activateUserByEmail(String email) {
        Users u = repository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        if (Boolean.TRUE.equals(u.getIsActive())) return entityToDto(u);
        u.setIsActive(true);
        u.setUpdatedAt(Date.from(Instant.now()));
        repository.save(u);
        logger.info("User activated: {}", email);
        return entityToDto(u);
    }

    public UsersDTO partialUpdate(Integer id, UsersDTO dto) {
        logger.debug("partialUpdate({}, {})", id, dto);

        Optional<Users> optionalEntity = repository.findById(id);
        if (!optionalEntity.isPresent()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    String.format("Users with id %s not found", id));
        }

        if (dto.getPasswordHash() != null && !dto.getPasswordHash().trim().isEmpty()) {
            if (!dto.getPasswordHash().matches("^\\$2[aby]\\$.{56}$")) {
                dto.setPasswordHash(passwordEncoder.encode(dto.getPasswordHash()));
            }
        }

        validateForeignKeyReferences(dto);
        Users entity = optionalEntity.get();
        dto.setUpdatedAt(Date.from(Instant.now()));
        updateEntityFromDto(entity, dto);

        Users updated = repository.save(entity);
        logger.info("User partially updated - id: {}", updated.getId());
        return entityToDto(updated);
    }

    public void updatePassword(String email, String newPlainPassword) {
        Users u = repository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        u.setPasswordHash(passwordEncoder.encode(newPlainPassword));
        u.setUpdatedAt(Date.from(Instant.now()));
        repository.save(u);
        logger.info("Password updated for user: {}", email);
    }

    public void deleteById(Integer id) {
        logger.debug("deleteById({})", id);

        if (!repository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    String.format("Users with id %s not found", id));
        }

        repository.deleteById(id);
        logger.info("User deleted - id: {}", id);
    }

    public boolean existsById(Integer id) {
        return repository.existsById(id);
    }

    // Generate a reasonably random verification code (UUID-based)
    private String generateVerificationCode() {
        return UUID.randomUUID().toString();
    }
}
