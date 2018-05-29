package bln.integration.gateway.oic;

import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(of= {"logTi"})
public class LogPointCfg {
    private Long meteringPointId;
    private Long logPointId;
    private String paramCode;
    private String unitCode;
    private LocalDateTime start;
    private LocalDateTime end;
}
