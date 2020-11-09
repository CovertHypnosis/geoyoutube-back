package com.geotube.repositories;

import com.geotube.model.UserInformation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserInformationRepository extends JpaRepository<UserInformation, UUID> {
    Optional<UserInformation> findFirstByUserId(UUID uuid);
}
