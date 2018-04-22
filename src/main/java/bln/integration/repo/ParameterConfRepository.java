package bln.integration.repo;

import bln.integration.entity.ParameterConf;
import bln.integration.entity.enums.ParamTypeEnum;
import bln.integration.entity.enums.SourceSystemEnum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ParameterConfRepository extends JpaRepository<ParameterConf, Long> {
    List<ParameterConf> findAllBySourceSystemCodeAndParamType(
        SourceSystemEnum sourceSystemCode,
        ParamTypeEnum paramType
    );
}
