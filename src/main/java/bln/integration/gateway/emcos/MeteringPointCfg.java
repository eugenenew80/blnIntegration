package bln.integration.gateway.emcos;

import bln.integration.entity.ParameterConf;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(of= {"sourceMeteringPointCode", "sourceParamCode"})
public class MeteringPointCfg {
	private Long meteringPointId;
	private String sourceMeteringPointCode;
	private String sourceParamCode;
	private String sourceUnitCode;
	private String paramCode;
	private Integer interval;
	private LocalDateTime startTime;
	private LocalDateTime endTime;
	private Double val;

	public static MeteringPointCfg fromLine(ParameterConf parameterConf) {
		MeteringPointCfg mpc = new MeteringPointCfg();
		mpc.setSourceMeteringPointCode(parameterConf.getSourceMeteringPointCode());
		mpc.setSourceParamCode(parameterConf.getSourceParamCode());
		mpc.setSourceUnitCode(parameterConf.getSourceUnitCode());
		mpc.setMeteringPointId(parameterConf.getMeteringPoint().getId());
		mpc.setInterval(parameterConf.getInterval());
		mpc.setParamCode(parameterConf.getParam().getCode());
		return mpc;
	}
}
