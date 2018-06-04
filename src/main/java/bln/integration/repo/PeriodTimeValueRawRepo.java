package bln.integration.repo;

import bln.integration.entity.PeriodTimeValueRaw;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.query.Procedure;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PeriodTimeValueRawRepo extends JpaRepository<PeriodTimeValueRaw, Long> {
    @Procedure(name = "PeriodTimeValueRaw.load")
    void load(@Param("p_batch_id") Long batchId);
}
