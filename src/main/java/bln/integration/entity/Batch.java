package bln.integration.entity;

import bln.integration.entity.enums.BatchStatusEnum;
import bln.integration.entity.enums.DirectionEnum;
import bln.integration.entity.enums.ParamTypeEnum;
import bln.integration.entity.enums.SourceSystemEnum;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(of= {"id"})
@Entity
@Table(name = "media_batches")
@NoArgsConstructor
public class Batch  {
    public Batch(WorkListHeader header, ParamTypeEnum paramTypeEnum) {
        this.workListHeader = header;
        this.sourceSystemCode = header.getSourceSystemCode();
        this.direction = header.getDirection();
        this.paramType = paramTypeEnum;
        this.status = BatchStatusEnum.P;
        this.startDate = LocalDateTime.now();
    }

    @Id
    @SequenceGenerator(name="media_batches_s", sequenceName = "media_batches_s", allocationSize=1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "media_batches_s")
    private Long id;

    @ManyToOne
    @JoinColumn(name="work_list_header_id")
    private WorkListHeader workListHeader;

    @Column(name="source_system_code")
    @Enumerated(EnumType.STRING)
    private SourceSystemEnum sourceSystemCode;

    @Column(name="direction")
    @Enumerated(EnumType.STRING)
    private DirectionEnum direction;

    @Column(name="param_type")
    @Enumerated(EnumType.STRING)
    private ParamTypeEnum paramType;

    @Column(name = "start_date")
    private LocalDateTime startDate;

    @Column(name = "end_date")
    private LocalDateTime endDate;

    @Column(name = "rec_count")
    private Long recCount;

    @Column(name="status")
    @Enumerated(EnumType.STRING)
    private BatchStatusEnum status;

    @Column(name = "err_msg")
    private String errMsg;

    @Column(name = "create_date")
    private LocalDateTime createDate;

    @Column(name = "last_update_date")
    private LocalDateTime lastUpdateDate;
}
