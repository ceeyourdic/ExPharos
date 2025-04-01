package org.clientbase.value.impl;

import org.clientbase.value.Value;
import java.util.function.Supplier;

/**
 * @author LangYa466
 * @since 2025/3/22
 */
public class NumberValue extends Value<Double> {
    public double min, max, inc;

    public NumberValue(String name, Double value, double min, double max, double inc) {
        super(name, value, null);
        this.min = min;
        this.max = max;
        this.inc = inc;
    }

    public NumberValue(String name, Double value, double min, double max, double inc, Supplier<Boolean> supplier) {
        super(name, value, supplier);
        this.min = min;
        this.max = max;
        this.inc = inc;
    }
}
