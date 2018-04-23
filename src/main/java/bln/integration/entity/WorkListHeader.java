package bln.integration.entity;

import bln.integration.entity.enums.BatchStatusEnum;
import bln.integration.entity.enums.DirectionEnum;
import bln.integration.entity.enums.SourceSystemEnum;
import bln.integration.entity.enums.WorkListTypeEnum;
import bln.integration.jpa.BooleanToIntConverter;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.DynamicUpdate;
import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

@Data
@EqualsAndHashCode(of= {"id"})
@Entity
@Table(name = "media_work_list_headers")
@DynamicUpdate
@NamedEntityGraph(name="WorkListHeader.allJoins", attributeNodes = {
    @NamedAttributeNode("config"),
    @NamedAttributeNode("atBatch"),
    @NamedAttributeNode("ptBatch")
})
public class WorkListHeader {
    @Id
    @SequenceGenerator(name="media_work_list_headers_s", sequenceName = "media_work_list_headers_s", allocationSize=1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "media_work_list_headers_s")
    private Long id;

    @Column
    private String name;

    @Column(name = "work_list_type")
    @Enumerated(EnumType.STRING)
    private WorkListTypeEnum workListType;

    @Column(name="source_system_code")
    @Enumerated(EnumType.STRING)
    private SourceSystemEnum sourceSystemCode;

    @Column(name="direction")
    @Enumerated(EnumType.STRING)
    private DirectionEnum direction;

    @ManyToOne
    @JoinColumn(name="config_id")
    private ConnectionConfig config;

    @Column(name="at_status")
    @Enumerated(EnumType.STRING)
    private BatchStatusEnum atStatus;

    @Column(name="pt_status")
    @Enumerated(EnumType.STRING)
    private BatchStatusEnum ptStatus;

    @Column(name = "is_active")
    @Convert(converter = BooleanToIntConverter.class)
    private Boolean active;

    @OneToMany(mappedBy = "header", fetch = FetchType.LAZY)
    private List<WorkListLine> lines;

    @ManyToOne
    @JoinColumn(name="at_batch_id")
    private Batch atBatch;

    @ManyToOne
    @JoinColumn(name="pt_batch_id")
    private Batch ptBatch;

    @Column(name = "start_date")
    private LocalDateTime startDate;

    @Column(name = "end_date")
    private LocalDateTime endDate;

    @Column(name = "create_date")
    private LocalDateTime createDate;

    @Column(name = "last_update_date")
    private LocalDateTime lastUpdateDate;
}
