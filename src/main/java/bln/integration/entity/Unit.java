package bln.integration.entity;

import lombok.*;
import org.hibernate.annotations.Immutable;
import javax.persistence.*;

@Data
@EqualsAndHashCode(of= {"id"})
@Entity
@Table(name = "dict_units")
@Immutable
public class Unit  {
	@Id
	private Long id;

	@Column
	private String code;

	@Column
	private String name;

	@Column(name = "short_name")
	private String shortName;
}
