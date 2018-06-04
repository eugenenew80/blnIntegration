package bln.integration.repo;

import bln.integration.entity.Batch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BatchRepo extends JpaRepository<Batch, Long> { }
