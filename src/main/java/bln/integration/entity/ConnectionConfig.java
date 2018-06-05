package bln.integration.entity;

import lombok.*;
import org.hibernate.annotations.Immutable;
import javax.persistence.*;

@Data
@EqualsAndHashCode(of= {"id"})
@Entity
@Table(name = "media_connection_config")
@Immutable
public class ConnectionConfig {
    @Id
    private Long id;

    @Column
    private String protocol;

    @Column
    private String url;

    @Column(name = "user_name")
    private String userName;

    @Column
    private String pwd;

    @Column(name = "time_zone")
    private String timeZone;
}
