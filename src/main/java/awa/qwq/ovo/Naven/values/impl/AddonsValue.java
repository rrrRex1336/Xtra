package awa.qwq.ovo.Naven.values.impl;

import awa.qwq.ovo.Naven.values.HasValue;
import awa.qwq.ovo.Naven.values.Value;
import awa.qwq.ovo.Naven.values.ValueType;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class AddonsValue extends Value {
    private final String[] values;
    private final Consumer<Value> update;
    private final Set<Integer> selectedIndices;
    private final boolean[] selected;
    private int currentValue;

    public AddonsValue(HasValue key, String name, String[] values, boolean[] defaultSelected, Consumer<Value> update, Supplier<Boolean> visibility) {
        super(key, name, visibility);
        this.update = update;
        this.values = values;
        this.selected = new boolean[values.length];
        this.selectedIndices = new HashSet<>();

        for (int i = 0; i < values.length && i < defaultSelected.length; i++) {
            this.selected[i] = defaultSelected[i];
            if (defaultSelected[i]) {
                this.selectedIndices.add(i);
            }
        }
    }

    public AddonsValue(HasValue key, String name, String[] values, Consumer<Value> update, Supplier<Boolean> visibility) {
        this(key, name, values, new boolean[values.length], update, visibility);
    }

    public boolean isSelected(String value) {
        for (int i = 0; i < values.length; i++) {
            if (values[i].equalsIgnoreCase(value)) {
                return selected[i];
            }
        }
        return false;
    }

    public boolean isSelected(int index) {
        if (index < 0 || index >= selected.length) return false;
        return selected[index];
    }

    @Override
    public ValueType getValueType() {
        return ValueType.ADDONS;
    }

    @Override
    public AddonsValue getAddonsValue() {
        return this;
    }

    public String[] getValues() {
        return this.values;
    }

    public boolean[] getSelected() {
        return this.selected.clone();
    }

    public Set<Integer> getSelectedIndices() {
        return new HashSet<>(selectedIndices);
    }

    public List<String> getSelectedValues() {
        List<String> result = new ArrayList<>();
        for (int index : selectedIndices) {
            result.add(values[index]);
        }
        return result;
    }

    public void setSelected(int index, boolean value) {
        if (index < 0 || index >= selected.length) return;
        if (selected[index] == value) return;

        selected[index] = value;
        if (value) {
            selectedIndices.add(index);
        } else {
            selectedIndices.remove(index);
        }

        if (this.update != null) {
            this.update.accept(this);
        }
    }

    public void toggleSelected(int index) {
        setSelected(index, !selected[index]);
    }

    public void clearSelection() {
        Arrays.fill(selected, false);
        selectedIndices.clear();
        if (this.update != null) {
            this.update.accept(this);
        }
    }

    public void selectAll() {
        Arrays.fill(selected, true);
        selectedIndices.clear();
        for (int i = 0; i < values.length; i++) {
            selectedIndices.add(i);
        }
        if (this.update != null) {
            this.update.accept(this);
        }
    }

    public void setCurrentValue(int currentValue) {
        this.currentValue = currentValue;
        if (this.update != null) {
            this.update.accept(this);
        }
    }

    public Consumer<Value> getUpdate() {
        return this.update;
    }

    public int getSelectedCount() {
        return selectedIndices.size();
    }

    public boolean isEmpty() {
        return selectedIndices.isEmpty();
    }
}