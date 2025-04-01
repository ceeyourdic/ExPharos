package org.clientbase.value;

import lombok.Getter;
import lombok.Setter;

import java.util.function.Supplier;

/**
 * @author LangYa466
 * @since 2025/3/22
 */
@Getter
@Setter
public class Value<V> {
    protected String name;
    protected V value;
    protected Supplier<Boolean> supplier;

    public Value(String name, V value) {
        this(name, value, null);
    }

    public Value(String name, V value, Supplier<Boolean> supplier) {
        this.name = name;
        this.value = value;
        this.supplier = supplier;
    }
}

