package org.clientbase.module;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.clientbase.module.impl.combat.KillAura;
import org.clientbase.value.Value;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

/**
 * @author LangYa466
 * @since 4/2/2025 12:46 AM
 */
public class ModuleManager {
    public Map<String, Module> moduleMap;

    private final Logger logger = LogManager.getLogger();

    public ModuleManager() {
        moduleMap = new HashMap<>();
        registerModule(KillAura.INSTANCE);
    }

    public void registerModule(Module module) {
        moduleMap.put(module.getName(), module);

        for (final Field field : module.getClass().getDeclaredFields()) {
            try {
                field.setAccessible(true);
                final Object obj = field.get(module);
                if (obj instanceof Value<?>) module.values.add((Value<?>) obj);
            } catch (IllegalAccessException e) {
                logger.error(e.getMessage());
            }
        }

        // test
        module.setEnabled(true);
    }

    public Module getModule(String moduleName) {
        return moduleMap.get(moduleName);
    }
}
