package bln.integration.repo;

import bln.integration.entity.LastLoadInfo;
import bln.integration.entity.enums.SourceSystemEnum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LastLoadInfoRepository extends JpaRepository<LastLoadInfo, Long> {
    LastLoadInfo findBySourceSystemCodeAndSourceMeteringPointCodeAndSourceParamCode(
        SourceSystemEnum sourceSystemCode,
        String sourceMeteringPointCode,
        String sourceParamCode
    );
}
