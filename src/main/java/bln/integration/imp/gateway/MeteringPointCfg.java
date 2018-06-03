package bln.integration.imp.gateway;

import bln.integration.entity.ParameterConf;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(of= {"sourceMeteringPointCode", "sourceParamCode"})
public class MeteringPointCfg {
	private Long meteringPointId;
	private Long paramId;
	private String paramCode;
	private Integer interval;
	private String sourceMeteringPointCode;
	private String sourceParamCode;
	private String sourceUnitCode;
	private LocalDateTime startTime;
	private LocalDateTime endTime;
	private Double val;

	public static MeteringPointCfg fromParameterConf(ParameterConf parameterConf) {
		MeteringPointCfg point = new MeteringPointCfg();
		point.setMeteringPointId(parameterConf.getMeteringPoint().getId());
		point.setParamId(parameterConf.getParam().getId());
		point.setParamCode(parameterConf.getParam().getCode());
		point.setInterval(parameterConf.getInterval());
		point.setSourceMeteringPointCode(parameterConf.getSourceMeteringPointCode());
		point.setSourceParamCode(parameterConf.getSourceParamCode());
		point.setSourceUnitCode(parameterConf.getSourceUnitCode());
		return point;
	}
}
