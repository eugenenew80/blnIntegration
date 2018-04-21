package bln.integration.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.Immutable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

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