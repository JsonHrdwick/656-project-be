package com.example.springbootjava.repository;

import com.example.springbootjava.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    Optional<User> findByEmail(String email);
    
    boolean existsByEmail(String email);
    
    @Query("SELECT u FROM User u WHERE u.email = :email AND u.enabled = true")
    Optional<User> findActiveUserByEmail(@Param("email") String email);
    
    @Query("SELECT COUNT(u) FROM User u WHERE u.enabled = true")
    long countActiveUsers();

    @Query("SELECT COUNT(u) FROM User u WHERE u.role = :role")
    long countByRole(@Param("role") User.Role role);

    // Native helpers (PostgreSQL)
    @Modifying
    @Query(value = "INSERT INTO users (id, first_name, last_name, email, password, role, enabled, created_at, updated_at) " +
                   "VALUES (:id, :firstName, :lastName, :email, :password, :role, :enabled, :createdAt, :updatedAt)", nativeQuery = true)
    void insertWithId(@Param("id") Long id,
                      @Param("firstName") String firstName,
                      @Param("lastName") String lastName,
                      @Param("email") String email,
                      @Param("password") String password,
                      @Param("role") String role,
                      @Param("enabled") Boolean enabled,
                      @Param("createdAt") java.time.LocalDateTime createdAt,
                      @Param("updatedAt") java.time.LocalDateTime updatedAt);

    @Query(value = "SELECT setval(pg_get_serial_sequence('users','id'), (SELECT COALESCE(MAX(id),1) FROM users))", nativeQuery = true)
    Long syncUserIdSequence();

    @Query(value = "SELECT setval(pg_get_serial_sequence('users','id'), 1, false)", nativeQuery = true)
    Long resetUserIdSequenceToStart();
}
