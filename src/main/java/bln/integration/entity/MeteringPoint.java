package bln.integration.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.Immutable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Data
@EqualsAndHashCode(of= {"id"})
@Entity
@Table(name = "dict_metering_points")
@Immutable
public class MeteringPoint  {
    @Id
    private Long id;

    @Column
    private String name;

    @Column
    private String code;

    @Column(name = "external_code")
    private String externalCode;
}
