package bln.integration.repo;

import bln.integration.entity.ConnectionConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ConnectionConfigRepository extends JpaRepository<ConnectionConfig, Long> {
}
