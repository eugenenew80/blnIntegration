package bln.integration.entity;

import lombok.*;
import org.hibernate.annotations.Immutable;
import javax.persistence.*;

@Data
@EqualsAndHashCode(of= {"id"})
@Entity
@Table(name = "media_work_list_lines")
@Immutable
public class WorkListLine  {
    @Id
    private Long id;

    @ManyToOne
    @JoinColumn(name="work_list_header_id")
    private WorkListHeader header;

    @ManyToOne
    @JoinColumn(name="metering_point_id")
    private MeteringPoint meteringPoint;
}
