package org.clientbase.value.impl;

import org.clientbase.value.Value;

import java.util.function.Supplier;

/**
 * @author LangYa466
 * @since 2025/3/22
 */
public class ModeValue extends Value<String> {
    public final String[] modes;

    public ModeValue(String name, String value, String... modes) {
        super(name, value, null);
        this.modes = modes;
    }

    public ModeValue(String name, String value, Supplier<Boolean> supplier, String... modes) {
        super(name, value, supplier);
        this.modes = modes;
    }

    public boolean isMode(String mode) {
        return value.equalsIgnoreCase(mode);
    }
}
