package cc.quarkus.vm.api;

/**
 * A Java object handle.
 */
public interface JavaObject {
    JavaClass getJavaClass();

    default boolean isClass() {
        return false;
    }

    default JavaClass asClass() {
        throw new ClassCastException();
    }

    default boolean isArray() {
        return false;
    }

    default JavaArray asArray() {
        throw new ClassCastException();
    }

    default boolean isPrimitiveClass() {
        return false;
    }

    default JavaPrimitiveClass asPrimitiveClass() {
        throw new ClassCastException();
    }
}