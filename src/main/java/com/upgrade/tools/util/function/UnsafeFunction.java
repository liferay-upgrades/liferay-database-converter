package com.upgrade.tools.util.function;

/// @author Albert Gomes Cabral
/// Reference from Liferay
/// See: <a href="https://github.com/liferay/liferay-portal/blob/master/">...</a>
@FunctionalInterface
public interface UnsafeFunction <T, R, E extends Throwable> {

    R apply(T t) throws E;

}
