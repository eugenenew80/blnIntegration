package bln.integration.entity;

import lombok.*;
import org.hibernate.annotations.Immutable;
import javax.persistence.*;

@Data
@EqualsAndHashCode(of= {"id"})
@Entity
@Table(name = "media_parameters")
@Immutable
public class Parameter {
    @Id
    private Long id;

    @Column
    private String code;
}
