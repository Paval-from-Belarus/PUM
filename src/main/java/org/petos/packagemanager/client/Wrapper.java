package org.petos.packagemanager.client;

import org.jetbrains.annotations.Nullable;

public class Wrapper<T> {
      private T value = null;
      public void set(T value){
	    this.value = value;
      }
      public @Nullable T get(){
	    return value;
      }
      public boolean isEmpty(){
	    return value == null;
      }
}
