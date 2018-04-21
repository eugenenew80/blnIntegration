package bln.integration.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(of= {"id"})
public class TelemetryRaw {
    private Long id;
    private Long logPoint;
    private  String systemCode;
    private  String unitCode;
    private  String paramCode;
    private String dateTime;
    private Double val;
}
