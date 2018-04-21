package bln.integration.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(of= {"id"})
@Entity
@Table(name = "media_work_list_lines")
public class WorkListLine  {
    @Id
    @SequenceGenerator(name="media_work_list_lines_s", sequenceName = "media_work_list_lines_s", allocationSize=1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "media_work_list_lines_s")
    private Long id;

    @ManyToOne
    @JoinColumn(name="work_list_header_id")
    private WorkListHeader header;

    @ManyToOne
    @JoinColumn(name="metering_point_id")
    private MeteringPoint meteringPoint;

    @ManyToOne
    @JoinColumn(name="param_id")
    private Parameter param;

    @Column(name = "start_date")
    private LocalDateTime startDate;

    @Column(name = "end_date")
    private LocalDateTime endDate;

    @Column(name = "create_date")
    private LocalDateTime createDate;

    @Column(name = "last_update_date")
    private LocalDateTime lastUpdateDate;
}
