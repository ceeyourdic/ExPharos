package org.clientbase.module;

import lombok.Getter;
import lombok.Setter;
import org.clientbase.ClientBase;
import org.clientbase.Wrapper;
import org.clientbase.value.Value;

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
            ClientBase.INSTANCE.getEventManager().register(this);
        } else {
            ClientBase.INSTANCE.getEventManager().unregister(this);
        }
    }
}
