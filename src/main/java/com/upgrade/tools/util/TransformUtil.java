package com.upgrade.tools.util;

import com.upgrade.tools.util.function.UnsafeFunction;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/// @author Albert Gomes Cabral
/// Reference from Liferay
/// See: <a href="https://github.com/liferay/liferay-portal/blob/master/">...</a>
public class TransformUtil {

    public static <T, E extends Throwable> boolean anyMatch(
            Collection<T> collection,
            UnsafeFunction<T, Boolean, E> unsafeFunction)
        throws E {

        for (T item : collection) {
            Boolean result = unsafeFunction.apply(item);

            if (Boolean.TRUE.equals(result)) {
                return true;
            }
        }

        return false;
    }

    public static <T, R, E extends Throwable> List<R> transform(
        Collection<T> collection, UnsafeFunction<T, R, E> unsafeFunction) {

        try {
            return unsafeTransform(collection, unsafeFunction);
        }
        catch (Throwable throwable) {
            throw new RuntimeException(throwable);
        }
    }

    public static <S, R, E extends Throwable> Set<R> transformToSet(
            Set<S> collection, UnsafeFunction<S, R, E> unsafeFunction) {

        try {
            return unsafeTransformToSet(collection, unsafeFunction);
        }
        catch (Throwable throwable) {
            throw new RuntimeException(throwable);
        }
    }

    public static <T, R, E extends Throwable> List<R> unsafeTransform(
            Collection<T> collection, UnsafeFunction<T, R, E> unsafeFunction)
        throws E {

        if (collection == null) {
            return new ArrayList<>();
        }

        List<R> list = new ArrayList<>(collection.size());

        for (T item : collection) {
            R newItem = unsafeFunction.apply(item);

            if (newItem != null) {
                list.add(newItem);
            }
        }

        return list;
    }

    public static <S, R, E extends Throwable> Set<R> unsafeTransformToSet(
            Set<S> collection, UnsafeFunction<S, R, E> unsafeFunction)
        throws E {

        if (collection == null) {
            return new HashSet<>();
        }

        Set<R> set = new HashSet<>(collection.size());

        for (S item : collection) {
            R newItem = unsafeFunction.apply(item);

            if (newItem != null) {
                set.add(newItem);
            }
        }

        return set;
    }

}
