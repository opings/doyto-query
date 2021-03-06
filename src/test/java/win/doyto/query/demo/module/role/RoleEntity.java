package win.doyto.query.demo.module.role;

import lombok.Getter;
import lombok.Setter;
import win.doyto.query.entity.CommonEntity;

import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * RoleEntity
 *
 * @author f0rb on 2019-05-28
 */
@Getter
@Setter
@Entity
@Table(name = "role")
public class RoleEntity extends CommonEntity<Long, Long> {
    private String roleName;
    private String roleCode;
    private Boolean valid;
}