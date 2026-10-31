package tr.sonumut.core;

/** Her modül bu arayüzü uygular. ModuleManager sırayla başlatır. */
public interface Module {
    String id();
    void enable();
    void disable();
    default void reload() {}
}
