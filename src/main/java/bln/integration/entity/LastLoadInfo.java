package bln.integration.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(of= {"id"})
@Entity
@Table(name = "media_last_load_info")
public class LastLoadInfo {
    @Id
    @SequenceGenerator(name="media_last_load_info_s", sequenceName = "media_last_load_info_s", allocationSize=1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "media_last_load_info_s")
    private Long id;

    @Column(name = "source_system_code")
    private String sourceSystemCode;

    @Column(name = "source_metering_point_code")
    private String sourceMeteringPointCode;

    @Column(name = "source_param_code")
    private String sourceParamCode;

    @Column(name = "last_load_date")
    private LocalDateTime lastLoadDate;

    @ManyToOne
    @JoinColumn(name="last_batch_id")
    private Batch lastBatch;
}
