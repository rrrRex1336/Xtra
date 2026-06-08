package awa.qwq.ovo.Naven.values.impl;

import awa.qwq.ovo.Naven.utils.MathUtils;
import awa.qwq.ovo.Naven.values.HasValue;
import awa.qwq.ovo.Naven.values.Value;
import awa.qwq.ovo.Naven.values.ValueType;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class FloatValue extends Value {
   private final float defaultValue;
   private final float minValue;
   private final float maxValue;
   private final float step;
   private final Consumer<Value> update;
   private float currentValue;
   private FloatValue minBound;
   private FloatValue maxBound;
   private boolean isLinkedAsMin;
   private boolean isLinkedAsMax;

   public FloatValue(
           HasValue key, String name, float defaultValue, float minValue, float maxValue, float step, Consumer<Value> update, Supplier<Boolean> visibility
   ) {
      super(key, name, visibility);
      this.update = update;
      this.currentValue = this.defaultValue = defaultValue;
      this.minValue = minValue;
      this.maxValue = maxValue;
      this.step = step;
      this.minBound = null;
      this.maxBound = null;
      this.isLinkedAsMin = false;
      this.isLinkedAsMax = false;
   }

   @Override
   public ValueType getValueType() {
      return ValueType.FLOAT;
   }

   @Override
   public FloatValue getFloatValue() {
      return this;
   }

   public void setCurrentValue(float currentValue) {
      float clamped = MathUtils.clampValue(currentValue, this.minValue, this.maxValue);
      if (isLinkedAsMin && maxBound != null) {
         clamped = Math.min(clamped, maxBound.getCurrentValue());
      }
      if (isLinkedAsMax && minBound != null) {
         clamped = Math.max(clamped, minBound.getCurrentValue());
      }

      this.currentValue = clamped;

      if (this.update != null) {
         this.update.accept(this);
      }
      if (isLinkedAsMin && minBound != null && minBound.getCurrentValue() > this.currentValue) {
         minBound.setCurrentValue(this.currentValue);
      }
      if (isLinkedAsMax && maxBound != null && maxBound.getCurrentValue() < this.currentValue) {
         maxBound.setCurrentValue(this.currentValue);
      }
   }

   public void linkAsMin(FloatValue bound) {
      this.maxBound = bound;
      this.isLinkedAsMin = true;
      if (this.currentValue > bound.getCurrentValue()) {
         this.setCurrentValue(bound.getCurrentValue());
      }
   }

   public void linkAsMax(FloatValue bound) {
      this.minBound = bound;
      this.isLinkedAsMax = true;
      if (this.currentValue < bound.getCurrentValue()) {
         this.setCurrentValue(bound.getCurrentValue());
      }
   }

   public float getDefaultValue() {
      return this.defaultValue;
   }

   public float getMinValue() {
      return this.minValue;
   }

   public float getMaxValue() {
      return this.maxValue;
   }

   public float getStep() {
      return this.step;
   }

   public Consumer<Value> getUpdate() {
      return this.update;
   }

   public float getCurrentValue() {
      return this.currentValue;
   }
}