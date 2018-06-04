package bln.integration.entity;

import lombok.*;
import org.hibernate.annotations.Immutable;
import javax.persistence.*;

@Data
@EqualsAndHashCode(of= {"id"})
@Entity
@Table(name = "dict_metering_points")
@Immutable
public class MeteringPoint  {
    @Id
    private Long id;

    @Column(name = "external_code")
    private String externalCode;
}
