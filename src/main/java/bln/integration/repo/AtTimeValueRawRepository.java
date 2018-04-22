package bln.integration.repo;

import bln.integration.entity.AtTimeValueRaw;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.query.Procedure;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AtTimeValueRawRepository extends JpaRepository<AtTimeValueRaw, Long> {

    @Procedure(name = "AtTimeValueRaw.updateLastDate")
    void updateLastDate(@Param("p_batch_id") Long batchId);

    @Procedure(name = "AtTimeValueRaw.load")
    void load(@Param("p_batch_id") Long batchId);

    default void bulkSave(List<AtTimeValueRaw> list) {
        Logger logger = LoggerFactory.getLogger(AtTimeValueRawRepository.class);
        long count=0;
        LocalDateTime now = LocalDateTime.now();
        for (AtTimeValueRaw item : list) {
            item.setCreateDate(now);
            save(item);

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
