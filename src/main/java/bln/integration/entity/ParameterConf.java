package bln.integration.entity;

import bln.integration.entity.enums.ParamTypeEnum;
import bln.integration.entity.enums.SourceSystemEnum;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.Immutable;
import javax.persistence.*;

@Data
@EqualsAndHashCode(of= {"id"})
@Entity
@Table(name = "media_parameter_conf")
@Immutable
public class ParameterConf {
    @Id
    @SequenceGenerator(name="media_parameter_conf_s", sequenceName = "media_parameter_conf_s", allocationSize=1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "media_parameter_conf_s")
    private Long id;

    @Column(name="source_system_code")
    @Enumerated(EnumType.STRING)
    private SourceSystemEnum sourceSystemCode;

    @Column(name = "source_param_code")
    private String sourceParamCode;

    @Column(name = "source_unit_code")
    private String sourceUnitCode;

    @ManyToOne
    @JoinColumn(name="source_unit_id")
    private Unit sourceUnit;

    @ManyToOne
    @JoinColumn(name="param_id")
    private Parameter param;

    @Column
    private Integer interval;

    @Column(name="param_type")
    @Enumerated(EnumType.STRING)
    private ParamTypeEnum paramType;

    @ManyToOne
    @JoinColumn(name="metering_point_id")
    private MeteringPoint meteringPoint;

    @Column(name = "source_metering_point_code")
    private String sourceMeteringPointCode;
}
