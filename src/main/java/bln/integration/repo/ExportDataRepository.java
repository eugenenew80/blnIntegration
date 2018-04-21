package bln.integration.repo;

import bln.integration.entity.ExportData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ExportDataRepository extends JpaRepository<ExportData, Long> {
    List<ExportData> findAllBySourceMeteringPointCodeAndMeteringDateBetween(
        String sourceMeteringPointCode,
        LocalDateTime startDate,
        LocalDateTime endDate
    );
}
