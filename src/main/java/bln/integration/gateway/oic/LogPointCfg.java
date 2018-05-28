package bln.integration.gateway.oic;

import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(of= {"logTi"})
public class LogPointCfg {
    private Long logPointId;
    private LocalDateTime start;
    private LocalDateTime end;
}
