package win.doyto.query.demo.module.menu;

import lombok.AllArgsConstructor;
import lombok.Getter;
import win.doyto.query.core.IdWrapper;

@Getter
@AllArgsConstructor
public class MenuIdWrapper implements IdWrapper<Integer> {
    private Integer id;
    private String platform;

    @Override
    public String toCacheKey() {
        return id + "-" + platform;
    }
}
