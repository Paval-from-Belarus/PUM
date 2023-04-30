package org.petos.packagemanager.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
class InstanceInfoTest {

@Test
void testToString() {
      InstanceInfo info = new InstanceInfo(new String[]{"kernel", "cd-rom", "driver"}, "/home");
      System.out.println(info);
}

@Test
void valueOf() {
      String[] sources = {
          "[kernel, cd-rom, driver][/home]\n[edit][C:\\]"
      };
      for (String source : sources){
            var list = InstanceInfo.valueOf(source);
            list.forEach(System.out::println);
      }
}
}