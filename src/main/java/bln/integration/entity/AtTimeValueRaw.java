package bln.integration.entity;

import bln.integration.entity.enums.InputMethodEnum;
import bln.integration.entity.enums.ProcessingStatusEnum;
import bln.integration.entity.enums.ReceivingMethodEnum;
import bln.integration.entity.enums.SourceSystemEnum;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(of= {"id"})
@Entity
@Table(name = "media_at_time_values_raw")
@NamedStoredProcedureQueries({
	@NamedStoredProcedureQuery(
		name = "AtTimeValueRaw.load",
		procedureName = "media_raw_data_proc.at_proc",
		parameters = { @StoredProcedureParameter(mode = ParameterMode.IN, name = "p_batch_id", type = Long.class) }
	)
})
public class AtTimeValueRaw {
	@Id
	@SequenceGenerator(name="media_at_time_values_raw_s", sequenceName = "media_at_time_values_raw_s", allocationSize=1)
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "media_at_time_values_raw_s")
	private Long id;

	@Column(name = "source_system_code")
	@Enumerated(value = EnumType.STRING)
	private SourceSystemEnum sourceSystemCode;

	@Column(name = "source_metering_point_code")
	private String sourceMeteringPointCode;

	@Column(name = "source_param_code")
	private String sourceParamCode;

	@Column(name = "source_unit_code")
	private String sourceUnitCode;

	@Column(name = "metering_date")
	private LocalDateTime meteringDate;

	@Column(name="receiving_method")
	@Enumerated(EnumType.STRING)
	private ReceivingMethodEnum receivingMethod;

	@Column(name="input_method")
	@Enumerated(EnumType.STRING)
	private InputMethodEnum inputMethod;

	@Column
	@Enumerated(value = EnumType.STRING)
	private ProcessingStatusEnum status;

	@Column
	private Double val;

	@ManyToOne
	@JoinColumn(name="batch_id")
	private Batch batch;

	@Column(name = "create_date")
	private LocalDateTime createDate;

	@Column(name = "last_update_date")
	private LocalDateTime lastUpdateDate;
}
