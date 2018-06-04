package bln.integration.repo;

import bln.integration.entity.WorkListHeader;
import bln.integration.entity.enums.DirectionEnum;
import bln.integration.entity.enums.SourceSystemEnum;
import bln.integration.entity.enums.WorkListTypeEnum;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface WorkListHeaderRepo extends JpaRepository<WorkListHeader, Long> {
    @EntityGraph(value = "WorkListHeader.allJoins" , type= EntityGraph.EntityGraphType.FETCH)
    List<WorkListHeader> findAllBySourceSystemCodeAndDirectionAndWorkListType(
        SourceSystemEnum sourceSystemCode,
        DirectionEnum direction,
        WorkListTypeEnum workListType
    );
}
