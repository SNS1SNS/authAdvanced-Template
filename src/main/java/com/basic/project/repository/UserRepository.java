package com.basic.project.repository;

import com.basic.project.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;


@Repository
public interface UserRepository  extends JpaRepository<UserEntity, Long> {

    Optional<UserEntity> findByUsername(String username);

    Optional<UserEntity> findByEmail(String email);

        Optional<UserEntity> findByEmailIgnoreCaseOrUsernameIgnoreCase(String email, String username);

    boolean existsByUsername(String username);
    boolean existsByEmail(String email);

    @Query("SELECT u FROM UserEntity u WHERE u.email = :email OR u.username = :username")
    Optional<UserEntity> findByEmailAndUsername(@Param("email") String email, @Param("username") String username);

}
