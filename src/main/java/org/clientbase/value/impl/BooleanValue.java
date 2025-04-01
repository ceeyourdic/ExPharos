package org.clientbase.value.impl;

import org.clientbase.value.Value;

import java.util.function.Supplier;

/**
 * @author LangYa466
 * @since 2025/3/22
 */
public class BooleanValue extends Value<Boolean> {
    public BooleanValue(String name, Boolean value) {
        super(name, value);
    }

    public BooleanValue(String name, Boolean value, Supplier<Boolean> booleanSupplier) {
        super(name, value, booleanSupplier);
    }
}
