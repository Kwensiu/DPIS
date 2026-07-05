package com.dpis.module.runtime.systemserver;

import com.dpis.module.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

public final class SystemServerHookSpec {
    final String entryName;
    final String[] classNames;
    final String[] methodNames;
    final String hookIdPrefix;

    private SystemServerHookSpec(String entryName,
                                 String[] classNames,
                                 String[] methodNames,
                                 String hookIdPrefix) {
        this.entryName = entryName;
        this.classNames = classNames;
        this.methodNames = methodNames;
        this.hookIdPrefix = hookIdPrefix;
    }

    static SystemServerHookSpec method(String entryName,
                                       String hookIdPrefix,
                                       String[] classNames,
                                       String[] methodNames) {
        return new SystemServerHookSpec(
                entryName,
                classNames,
                methodNames,
                hookIdPrefix);
    }

    static SystemServerHookSpec constructor(String entryName,
                                            String hookIdPrefix,
                                            String[] classNames) {
        return new SystemServerHookSpec(
                entryName,
                classNames,
                new String[0],
                hookIdPrefix);
    }

    String describeClassNames() {
        return String.join("|", classNames);
    }

    String describeMethodNames() {
        return methodNames.length == 0 ? "<constructor>" : String.join("|", methodNames);
    }

    String hookIdFor(Method method) {
        return hookIdPrefix + "_" + executableSuffix(method.getName(), method.getParameterTypes());
    }

    String hookIdFor(Constructor<?> constructor) {
        return hookIdPrefix + "_" + executableSuffix("ctor", constructor.getParameterTypes());
    }

    private static String executableSuffix(String name, Class<?>[] parameterTypes) {
        StringBuilder builder = new StringBuilder(name);
        builder.append("__");
        if (parameterTypes == null || parameterTypes.length == 0) {
            builder.append("noargs");
            return builder.toString();
        }
        for (int i = 0; i < parameterTypes.length; i++) {
            if (i > 0) {
                builder.append('_');
            }
            Class<?> parameterType = parameterTypes[i];
            String typeName = parameterType != null ? parameterType.getSimpleName() : "unknown";
            for (int j = 0; j < typeName.length(); j++) {
                char c = typeName.charAt(j);
                builder.append(Character.isLetterOrDigit(c) ? Character.toLowerCase(c) : '_');
            }
        }
        return builder.toString();
    }
}
