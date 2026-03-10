package com.renda.merchantops.infra.repository;

import com.renda.merchantops.infra.persistence.entity.UserRoleEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRoleRepository extends JpaRepository<UserRoleEntity, Long> {

    void deleteByUserId(Long userId);
}
