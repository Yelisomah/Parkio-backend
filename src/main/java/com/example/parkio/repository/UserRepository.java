package com.example.parkio.repository;

import com.example.parkio.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findByEmailOrPhone(String email, String phone);

    /** findFirst, not findBy — `phone` has no unique constraint, so duplicates are tolerated rather than throwing. */
    Optional<User> findFirstByPhone(String phone);

    boolean existsByEmail(String email);

    boolean existsByPhone(String phone);

    @Query("SELECT u FROM User u JOIN FETCH u.roles WHERE u.email = :email")
    Optional<User> findByEmailWithRoles(String email);

    @Query("SELECT u FROM User u JOIN FETCH u.roles WHERE u.email = :identifier OR u.phone = :identifier")
    Optional<User> findByEmailOrPhoneWithRoles(@org.springframework.data.repository.query.Param("identifier") String identifier);
}
