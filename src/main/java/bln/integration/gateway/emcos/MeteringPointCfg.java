package bln.integration.gateway.emcos;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(of= {"sourceMeteringPointCode", "sourceParamCode"})
public class MeteringPointCfg {
	private String sourceMeteringPointCode;
	private String sourceParamCode;
	private String sourceUnitCode;
	private String paramCode;
	private Integer interval;
	private LocalDateTime startTime;
	private LocalDateTime endTime;
	private Double val;
}
