package org.qbicc.runtime.main;

import org.qbicc.runtime.CNative;

import static org.qbicc.runtime.CNative.*;
import static org.qbicc.runtime.posix.PThread.*;
import static org.qbicc.runtime.stdc.Stdint.*;

/**
 * Intrinsics for accessing implementation-dependent object header fields
 * and compiler-generated global object model structures.
 *
 * These APIs are primarily intended for use in methods of VMHelper
 * and are subject to change as the runtime object model evolves.
 */
@SuppressWarnings("unused")
public class ObjectModel {

    /**
     * Get the dimensionality for the represented type from a java.lang.Class instance.
     * Classes and interfaces have dimensionality 0.
     */
    public static native uint8_t get_dimensions_from_class(Class<?> cls);

    /**
     * Get the concrete type ID for the represented type from a java.lang.Class instance.
     */
    public static native type_id get_type_id_from_class(Class<?> cls);

    /**
     * Get the java.lang.Class instance for the type ID.
     */
    public static native Class<?> get_class_from_type_id(type_id typeId, uint8_t dimensions);

    /**
     * Get the java.lang.Class instance for the type ID for non-array classes.
     */
    public static native Class<?> get_class_from_type_id_simple(type_id typeId);

    /**
     * Returns java.lang.Class instance representing array class of a given class
     *
     * @param componentClass
     * @return instance of java.lang.Class
     */
    public static native Class<?> get_array_class_of(Class<?> componentClass);

    /**
     * Tries to atomically set the java.lang.Class#arrayClass field of a component class
     * to a given array class
     *
     * @param componentClass
     * @param arrayClass
     * @return boolean true if atomic operation succeeds, false otherwise
     */
    public static native boolean set_array_class(Class<?> componentClass, Class<?> arrayClass);

    /**
     * Checks if the java.lang.Class instance represents reference array class.
     * Reference array class have dimension greater than 0.
     *
     * @param cls
     * @return boolean
     */
    public static boolean is_reference_array_class(Class<?> cls) {
        type_id typeId = get_type_id_from_class(cls);
        return is_reference_array(typeId);
    }

    /**
     * Returns java.lang.Class instance representing the array class of a given component class
     *
     * @param componentClass
     * @return instance of java.lang.Class
     */
    public static Class<?> get_or_create_array_class(Class<?> componentClass, uint8_t dimensions) {
        Class<?> arrayClass = get_array_class_of(componentClass);
        if (arrayClass == null) {
            String className;
            type_id componentTypeId = get_type_id_from_class(componentClass);
            if (is_reference_array(componentTypeId) || is_prim_array(componentTypeId)) {
                className = "[" + componentClass.getName();
                arrayClass = create_class(className, get_reference_array_typeid(), dimensions);
            } else {
                className = "[L" + componentClass.getName() + ";";
                arrayClass = create_class(className, get_reference_array_typeid(), dimensions);
            }
            if (!set_array_class(componentClass, arrayClass)) {
                arrayClass = get_array_class_of(componentClass);
            }
        }
        return arrayClass;
    }

    /**
     * Helper method to create java.lang.Class instance for array class
     *
     * @param leafClass leaf element of the array
     * @param dimensions dimensions of the array
     * @return instance of java.lang.Class
     */
    public static Class<?> get_array_class_of_dimension(Class<?> leafClass, uint8_t dimensions) {
        if (dimensions.intValue() == 1) {
            return get_or_create_array_class(leafClass, dimensions);
        }
        Class<?> cls = get_array_class_of_dimension(leafClass, CNative.word(dimensions.intValue()-1));
        return get_or_create_array_class(cls, dimensions);
    }

    /**
     * Creates java.lang.Class instance for array class
     *
     * @param leafClass leaf element of the array
     * @param dimensions dimensions of the array
     * @return instance of java.lang.Class
     */
    public static Class<?> get_or_create_class_for_refarray(Class<?> leafClass, uint8_t dimensions) {
        return get_array_class_of_dimension(leafClass, dimensions);
    }

    /**
     * Allocates an instance of java.lang.Class in the runtime heap
     *
     * @param name class name
     * @param id class's type id
     * @param dimension array dimension if the class is an array class, 0 otherwise
     * @return instance of java.lang.Class
     */
    public static native Class<?> create_class(String name, type_id id, uint8_t dimension);

    /**
     * Get the concrete type ID value from the referenced object.  Note that all reference arrays will have the same
     * type ID, which does not reflect the element type.
     *
     * @param reference the object reference (must not be {@code null})
     * @return the type ID of the object
     */
    public static native type_id type_id_of(Object reference);

    /**
     * Get the dimensionality of the referenced array.
     *
     * @param arrayReference the array reference (must not be {@code null} and must be an Object[])
     * @return the dimensionality of the array
     */
    public static native uint8_t dimensions_of(Object arrayReference); // Object not Object[] because we use this in the impl of cast

    /**
     * Get the element type ID value of the referenced array.
     *
     * @param arrayReference the array reference (must not be {@code null} and must be an Object[])
     * @return the array element type ID
     */
    public static native type_id element_type_id_of(Object arrayReference); // Object not Object[] because we use this in the impl of cast

    /**
     * Get the maxTypeId assigned to subclasses of the argument typeId
     */
    public static native type_id max_subclass_type_id_of(type_id typeId);

    /**
     * Is the argument typeId the typeId for java.lang.Object?
     */
    public static native boolean is_java_lang_object(type_id typeId);

    /**
     * Is the argument typeId the typeId for java.lang.Cloneable?
     */
    public static native boolean is_java_lang_cloneable(type_id typeId);

    /**
     * Is the argument typeId the typeId for java.io.Serializable?
     */
    public static native boolean is_java_io_serializable(type_id typeId);

    /**
     * Is the argument typeId the typeId of a Class?
     */
    public static native boolean is_class(type_id typeId);

    /**
     * Is the argument typeId the typeId of an Interface?
     */
    public static native boolean is_interface(type_id typeId);

    /**
     * Is the argument typeId the typeId of a primitive array?
     */
    public static native boolean is_prim_array(type_id typeId);

    /**
     * Is the argument typeId the typeId of a primitive?
     */
    public static native boolean is_primitive(type_id typeId);

    /**
     * Is the argument typeId the typeId use for reference arrays?
     */
    public static native boolean is_reference_array(type_id typeId);

    /**
     * Returns the typeId used for reference arrays
     */
    public static native type_id get_reference_array_typeid();

    /**
     * Does a typeId implement the argument interface?
     */
    public static native boolean does_implement(type_id valueTypeId, type_id interfaceTypeId);

    /**
     * Get the number of typeIds in the system.
     * This will be 1 higher than the highest typeid
     */
    public static native type_id get_number_of_typeids();

    /**
     * Call the class initializer for this class if it hasn't already been
     * called.
     * 
     * This operation is racy as the locking is managed by the ClinitState
     * object in VMHelpers#initialize_class and should only be called by
     * that method.
     * 
     * @param typeId the class to initialize
     */
    public static native void call_class_initializer(type_id typeId);

    static final int Flag_typeid_has_clinit = 1;
    static final int Flag_typeid_declares_default_methods = 2;
    static final int Flag_typeid_has_default_methods = 4;

    /**
     * Get the `flags` field from the qbicc_typeid_array for the given
     * typeid.
     * 
     * Flags are:
     * 1 - has clinit method
     * 2 - declares default methods
     * 4 - has default methods
     * See SupersDisplayTables.calculateTypeIdFlags() for definitive list.
     * 
     * @param typeId the class to read the flags for
     * @return the flags value
     */
    public static native int get_typeid_flags(type_id typeId);

    public static boolean has_class_initializer(type_id typeId) {
        return (get_typeid_flags(typeId) & Flag_typeid_has_clinit) == Flag_typeid_has_clinit;
    }

    public static boolean declares_default_methods(type_id typeId) {
        return (get_typeid_flags(typeId) & Flag_typeid_declares_default_methods) == Flag_typeid_declares_default_methods;
    }

    public static boolean has_default_methods(type_id typeId) {
        return (get_typeid_flags(typeId) & Flag_typeid_has_default_methods) == Flag_typeid_has_default_methods;
    }

    /** 
     * Fetch the superclass `type_id` from the current `type_id`
     * @param typeId an existing type_id, don't call this on Object's typeid
     * @return superclass's type_id
     */
    public static native type_id get_superclass_typeid(type_id typeId);

    public static native type_id get_first_interface_typeid();

    public static native int get_number_of_bytes_in_interface_bits_array();

    public static native byte get_byte_of_interface_bits(type_id typeId, int index);

    /**
     * Check the `clinit_states` native structure to see if this typeid is initialized.
     * 
     * This is a fast check reading a bit in the structure.  A "true" value can be trusted
     * as a fast path check while a "false" value requires the state-machine defined in
     * VMHelpers.initialize_class() to validate the result and handle the transition.
     * 
     * @return true if initialized.  False if the state machine needs to validate.
     */
    public static native boolean is_initialized(type_id typdId);

    /**
     * Set the class initialized.  
     * 
     * This should only be done by the MHelpers.initialize_class() statemachine
     * @param typdId the class to mark initialized
     */
    public static native void set_initialized(type_id typdId);

    /**
     * Get the native object monitor (mutex) slot from the referenced object. These are intended for object monitor synchronization.
     *
     * @param reference the object reference (must not be {@code null})
     * @return the pthread mutex of the object
     */
    public static native pthread_mutex_t_ptr get_nativeObjectMonitor(Object reference);

    /**
     * Set the native object monitor (mutex) for the referenced object. This method is atomic and will return true on success.
     *
     * @param reference the object reference (must not be {@code null})
     * @param nom mutex for the referenced object (must not be {@code null})
     * @return true if successful
     */
    public static native boolean set_nativeObjectMonitor(Object reference, pthread_mutex_t_ptr nom);
}
