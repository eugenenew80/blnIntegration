package bln.integration.repo;

import bln.integration.entity.LastRequestedDate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface LastRequestedDateRepo extends JpaRepository<LastRequestedDate, Long> {
    List<LastRequestedDate> findAllByWorkListHeaderId(Long workListHeaderId);
}
