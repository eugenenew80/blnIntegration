package bln.integration.entity;

import bln.integration.entity.enums.*;
import lombok.*;
import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(of= {"id"})
@Entity
@Table(name = "media_last_load_info")

@NamedEntityGraph(name="LastLoadInfo.allJoins", attributeNodes = {
    @NamedAttributeNode("meteringPoint"),
    @NamedAttributeNode("param")
})

@NamedStoredProcedureQueries({
    @NamedStoredProcedureQuery(
        name = "LastLoadInfo.updateAtLastDate",
        procedureName = "media_raw_data_proc.at_last_load_info",
        parameters = { @StoredProcedureParameter(mode = ParameterMode.IN, name = "p_batch_id", type = Long.class) }
    ),

    @NamedStoredProcedureQuery(
        name = "LastLoadInfo.updatePtLastDate",
        procedureName = "media_raw_data_proc.pt_last_load_info",
        parameters = { @StoredProcedureParameter(mode = ParameterMode.IN, name = "p_batch_id", type = Long.class) }
    )
})
public class LastLoadInfo {
    @Id
    @SequenceGenerator(name="media_last_load_info_s", sequenceName = "media_last_load_info_s", allocationSize=1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "media_last_load_info_s")
    private Long id;

    @Column(name="source_system_code")
    @Enumerated(EnumType.STRING)
    private SourceSystemEnum sourceSystemCode;

    @ManyToOne
    @JoinColumn(name="metering_point_id")
    private MeteringPoint meteringPoint;

    @ManyToOne
    @JoinColumn(name="param_id")
    private Parameter param;

    @Column(name="param_type")
    @Enumerated(EnumType.STRING)
    private ParamTypeEnum paramType;

    @Column
    private Integer interval;

    @Column(name = "last_load_date")
    private LocalDateTime lastLoadDate;
}
