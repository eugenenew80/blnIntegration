package bln.integration.entity;

import bln.integration.jpa.BooleanToIntConverter;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.Immutable;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

@Data
@EqualsAndHashCode(of= {"id"})
@Entity
@Table(name = "media_parameters")
@Immutable
public class Parameter {
    @Id
    @SequenceGenerator(name="media_parameters_s", sequenceName = "media_parameters_s", allocationSize=1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "media_parameters_s")
    private Long id;

    @Column
    private String code;

    @Column
    private String name;

    @Column(name = "short_name")
    private String shortName;

    @ManyToOne
    @JoinColumn(name="unit_id")
    private Unit unit;

    @Column(name = "is_at")
    @Convert(converter = BooleanToIntConverter.class)
    private Boolean isAt;

    @Column(name = "is_pt")
    @Convert(converter = BooleanToIntConverter.class)
    private Boolean isPt;

    @Column(name = "create_date")
    private LocalDateTime createDate;

    @Column(name = "last_update_date")
    private LocalDateTime lastUpdateDate;

    @OneToMany(mappedBy = "param")
    @Fetch(FetchMode.SUBSELECT)
    private List<ParameterConf> confs;
}
