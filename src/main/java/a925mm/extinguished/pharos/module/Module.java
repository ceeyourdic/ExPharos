package a925mm.extinguished.pharos.module;

import lombok.Getter;
import lombok.Setter;
import a925mm.extinguished.pharos.ExPharos;
import a925mm.extinguished.pharos.Wrapper;
import a925mm.extinguished.pharos.value.Value;

import java.util.ArrayList;
import java.util.List;


/**
 * @author LangYa466
 * @since 4/2/2025 12:46 AM
 */
@Getter
@Setter
public class Module implements Wrapper {
    private String name;
    private Category category;
    public List<Value<?>> values = new ArrayList<>();

    private boolean enabled;

    public Module(String name, Category category) {
        this.name = name;
        this.category = category;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;

        if (enabled) {
            ExPharos.INSTANCE.getEventManager().register(this);
        } else {
            ExPharos.INSTANCE.getEventManager().unregister(this);
        }
    }
}
