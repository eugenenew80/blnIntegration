package bln.integration.entity;

import bln.integration.entity.enums.SourceSystemEnum;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.Immutable;

import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(of= {"id"})
@Entity
@Table(name = "media_connection_config")
@Immutable
public class ConnectionConfig {
    @Id
    @SequenceGenerator(name="media_connection_config_s", sequenceName = "media_connection_config_s", allocationSize=1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "media_connection_config_s")
    private Long id;

    @Column
    private String name;

    @Column(name="source_system_code")
    @Enumerated(EnumType.STRING)
    private SourceSystemEnum sourceSystemCode;

    @Column
    private String protocol;

    @Column
    private String url;

    @Column(name = "user_name")
    private String userName;

    @Column
    private String pwd;

    @Column(name = "create_date")
    private LocalDateTime createDate;

    @Column(name = "last_update_date")
    private LocalDateTime lastUpdateDate;
}
