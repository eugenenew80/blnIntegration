package bln.integration.gateway.oic;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(of= {"id"})
public class TelemetryDto {
    private String systemCode;
    private Long logPointId;
    private Double val;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime dateTime;

}
