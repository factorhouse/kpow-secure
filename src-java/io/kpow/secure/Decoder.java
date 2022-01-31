package io.kpow.secure;

import clojure.java.api.Clojure;
import clojure.lang.IFn;

import java.util.Properties;

public class Decoder {

    public static Properties properties(String key, String payload) {
        IFn require = Clojure.var("clojure.core", "require");
        require.invoke(Clojure.read("kpow.secure"));
        String text = (String) Clojure.var("kpow.secure", "decrypt").invoke(key, payload);
        return (Properties) Clojure.var("kpow.secure", "->props").invoke(text);
    }

    public static Properties loadProperties(String keyPath, String payloadPath) {
        IFn require = Clojure.var("clojure.core", "require");
        require.invoke(Clojure.read("kpow.secure"));
        String text = (String)  Clojure.var("kpow.secure", "decrypt-file").invoke(keyPath, payloadPath);
        return (Properties) Clojure.var("kpow.secure", "->props").invoke(text);
    }

}