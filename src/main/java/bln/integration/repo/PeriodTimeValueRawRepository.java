package bln.integration.repo;

import bln.integration.entity.PeriodTimeValueRaw;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.query.Procedure;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PeriodTimeValueRawRepository extends JpaRepository<PeriodTimeValueRaw, Long> {
    @Procedure(name = "PeriodTimeValueRaw.updateLastDate")
    void updateLastDate(@Param("p_batch_id") Long batchId);

    @Procedure(name = "PeriodTimeValueRaw.load")
    void load(@Param("p_batch_id") Long batchId);

    default void bulkSave(List<PeriodTimeValueRaw> list) {
        Logger logger = LoggerFactory.getLogger(PeriodTimeValueRawRepository.class);
        long count=0;
        LocalDateTime now = LocalDateTime.now();
        for (PeriodTimeValueRaw pt : list) {
            pt.setCreateDate(now);
            save(pt);

            count++;
            if (count % 1000 == 0) {
                flush();
                logger.info("Saved records: " + count);
            }
        }
        flush();
        logger.info("Saved records: " + count);
    }
}
