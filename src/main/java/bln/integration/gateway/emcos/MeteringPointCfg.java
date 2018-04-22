package bln.integration.gateway.emcos;

import bln.integration.entity.ParameterConf;
import bln.integration.entity.WorkListLine;
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

	public static MeteringPointCfg fromLine(WorkListLine line, ParameterConf parameterConf, LocalDateTime startTime, LocalDateTime endTime) {
		MeteringPointCfg mpc = new MeteringPointCfg();
		mpc.setSourceParamCode(parameterConf.getSourceParamCode());
		mpc.setSourceUnitCode(parameterConf.getSourceUnitCode());
		mpc.setInterval(parameterConf.getInterval());
		mpc.setSourceMeteringPointCode(line.getMeteringPoint().getExternalCode());
		mpc.setParamCode(line.getParam().getCode());
		mpc.setStartTime(startTime);
		mpc.setEndTime(endTime);
		return mpc;
	}
}
