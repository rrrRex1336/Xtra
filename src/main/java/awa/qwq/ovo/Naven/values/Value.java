package awa.qwq.ovo.Naven.values;

import awa.qwq.ovo.Naven.Naven;
import awa.qwq.ovo.Naven.exceptions.BadValueTypeException;
import awa.qwq.ovo.Naven.values.impl.*;

import java.util.function.Supplier;

public abstract class Value {
   private final HasValue key;
   private final String name;
   private final Supplier<Boolean> visibility;

   protected Value(HasValue key, String name, Supplier<Boolean> visibility) {
      this.key = key;
      this.name = name;
      this.visibility = visibility;
      Naven.getInstance().getValueManager().addValue(this);
   }

   public abstract ValueType getValueType();

   public BooleanValue getBooleanValue() {
      throw new BadValueTypeException();
   }

   public FloatValue getFloatValue() {
      throw new BadValueTypeException();
   }

   public StringValue getStringValue() {
      throw new BadValueTypeException();
   }

   public ModeValue getModeValue() {
      throw new BadValueTypeException();
   }

   public AddonsValue getAddonsValue() {
      throw new BadValueTypeException();
   }

   public boolean isVisible() {
      return this.visibility == null || this.visibility.get();
   }

   public HasValue getKey() {
      return this.key;
   }

   public String getName() {
      return this.name;
   }

   public Supplier<Boolean> getVisibility() {
      return this.visibility;
   }
}
