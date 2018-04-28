package bln.integration.repo;

import bln.integration.entity.LastLoadInfo;
import bln.integration.entity.enums.SourceSystemEnum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.query.Procedure;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface LastLoadInfoRepository extends JpaRepository<LastLoadInfo, Long> {
    //@Query(value = "select * from apps.media_last_load_info l where l.source_system_code=:sourceSystemCode", nativeQuery = true)
    List<LastLoadInfo> findAllBySourceSystemCode(SourceSystemEnum sourceSystemCode);

    @Procedure(name = "LastLoadInfo.updatePtLastDate")
    void updatePtLastDate(@Param("p_batch_id") Long batchId);

    @Procedure(name = "LastLoadInfo.updateAtLastDate")
    void updateAtLastDate(@Param("p_batch_id") Long batchId);
}
