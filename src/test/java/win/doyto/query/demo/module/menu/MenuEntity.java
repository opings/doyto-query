package win.doyto.query.demo.module.menu;

import lombok.Getter;
import lombok.Setter;
import win.doyto.query.entity.CommonEntity;

import java.util.Objects;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Transient;

/**
 * MenuEntity
 *
 * @author f0rb
 */
@Getter
@Setter
@Entity
@Table(name = MenuEntity.TABLE)
public class MenuEntity extends CommonEntity<Integer, Long> {

    public static final String TABLE = "menu_${platform}";

    @Transient
    private String platform;

    private Integer parentId;

    private String menuName;

    private String memo;

    private Boolean valid;

    @Override
    public MenuIdWrapper toIdWrapper() {
        Objects.requireNonNull(id);
        Objects.requireNonNull(platform);
        return new MenuIdWrapper(id, platform);
    }
}
