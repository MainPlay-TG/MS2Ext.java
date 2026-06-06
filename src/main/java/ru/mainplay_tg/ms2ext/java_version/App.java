package ru.mainplay_tg.ms2ext.java_version;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import ru.mainplay_tg.ms2ext.Util;

public class App {
  public static void main(String[] argv) {
    // Словарь с результатом
    Map<String, Object> result = new HashMap<>();
    // Свойства системы
    result.put("home", System.getProperty("java.home"));
    result.put("os_name", System.getProperty("os.name"));
    result.put("vendor", System.getProperty("java.vendor"));
    result.put("version", System.getProperty("java.version"));
    // Номер версии
    Runtime.Version version = Runtime.version();
    result.put("feature", version.feature()); // Самое важное
    result.put("interim", version.interim());
    result.put("patch", version.patch());
    result.put("update", version.update());
    // Модули
    Set<Module> modules = ModuleLayer.boot().modules();
    Set<String> module_names = new HashSet<>();
    modules.forEach(m -> module_names.add(m.getName()));
    result.put("modules", module_names);
    // Вывод результата
    System.err.println(Util.to_json(result));
  }
}
