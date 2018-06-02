package bln.integration.entity;

import bln.integration.entity.enums.*;
import lombok.*;
import org.hibernate.annotations.Immutable;
import javax.persistence.*;

@Data
@EqualsAndHashCode(of= {"id"})
@Entity
@Table(name = "media_source_types")
@Immutable
public class SourceType {
    @Id
    @SequenceGenerator(name="media_source_types_s", sequenceName = "media_source_types_s", allocationSize=1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "media_source_types_s")
    private Long id;

    @Column(name = "source_system_code")
    @Enumerated(value = EnumType.STRING)
    private SourceSystemEnum sourceSystemCode;

    @Column(name="receiving_method")
    @Enumerated(EnumType.STRING)
    private ReceivingMethodEnum receivingMethod;

    @Column(name="input_method")
    @Enumerated(EnumType.STRING)
    private InputMethodEnum inputMethod;
}
