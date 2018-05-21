package bln.integration.repo;

import bln.integration.entity.LastRequestedDate;
import bln.integration.entity.enums.ParamTypeEnum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface LastRequestedDateRepository extends JpaRepository<LastRequestedDate, Long> {
    List<LastRequestedDate> findAllByWorkListHeaderIdAndParamType(Long workListHeaderId, ParamTypeEnum paramType);
}
