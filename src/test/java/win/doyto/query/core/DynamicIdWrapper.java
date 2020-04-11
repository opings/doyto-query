package win.doyto.query.core;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.Transient;

/**
 * DynamicIdWrapper
 *
 * @author forb.yuan@baozun.com
 * @date 2020-04-11
 */
@Getter
@Setter
@AllArgsConstructor
public class DynamicIdWrapper implements IdWrapper<Integer> {

    private Integer id;

    @Transient
    private String user;

    @Transient
    private String project;

}
