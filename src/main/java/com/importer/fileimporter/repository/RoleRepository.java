package com.importer.fileimporter.repository;

import com.importer.fileimporter.models.ERole;
import com.importer.fileimporter.models.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface RoleRepository extends JpaRepository <Role, UUID> {

    Optional<Role> findByName(ERole name);

}
