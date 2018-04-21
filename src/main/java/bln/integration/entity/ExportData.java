package bln.integration.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.Immutable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(of= {"id"})
@Entity
@Table(name ="media_export_data")
@Immutable
public class ExportData {
    @Id
    @Column(name = "id")
    private Long id;

    @Column(name = "source_metering_point_code")
    private String sourceMeteringPointCode;

    @Column(name = "metering_date")
    private LocalDateTime meteringDate;

    @Column(name = "val_ap")
    private Double valAp;

    @Column(name = "val_am")
    private Double valAm;
}
