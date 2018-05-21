package bln.integration.entity;

import bln.integration.entity.enums.ParamTypeEnum;
import lombok.Data;
import lombok.EqualsAndHashCode;
import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(of= {"id"})
@Entity
@Table(name = "media_last_requested_dates")
public class LastRequestedDate {
    @Id
    @SequenceGenerator(name="media_last_requested_dates_s", sequenceName = "media_last_requested_dates_s", allocationSize=1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "media_last_requested_dates_s")
    private Long id;

    @ManyToOne
    @JoinColumn(name="work_list_header_id")
    private WorkListHeader workListHeader;

    @Column(name="param_type")
    @Enumerated(EnumType.STRING)
    private ParamTypeEnum paramType;

    @Column(name = "last_requested_date")
    private LocalDateTime lastRequestedDate;
}
