package bln.integration.entity;

import bln.integration.entity.enums.*;
import lombok.*;
import org.hibernate.annotations.Immutable;
import javax.persistence.*;

@Data
@EqualsAndHashCode(of= {"id"})
@Entity
@Table(name = "media_parameter_conf")
@Immutable
@NamedEntityGraph(name="ParameterConf.allJoins", attributeNodes = {
    @NamedAttributeNode("meteringPoint"),
    @NamedAttributeNode("param")
})
public class ParameterConf {
    @Id
    private Long id;

    @Column(name="source_system_code")
    @Enumerated(EnumType.STRING)
    private SourceSystemEnum sourceSystemCode;

    @Column(name = "source_metering_point_code")
    private String sourceMeteringPointCode;

    @Column(name = "source_param_code")
    private String sourceParamCode;

    @Column(name = "source_unit_code")
    private String sourceUnitCode;

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
}
