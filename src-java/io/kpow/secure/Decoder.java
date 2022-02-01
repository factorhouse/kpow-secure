package io.kpow.secure;

import clojure.java.api.Clojure;
import clojure.lang.IFn;

import java.util.Properties;

public class Decoder {

    /**
     * Decode payload > text with key taken from environment variable KPOW_SECURE_KEY
     **/
    public static String text(String payload) {
        IFn require = Clojure.var("clojure.core", "require");
        require.invoke(Clojure.read("kpow.secure"));
        return (String) Clojure.var("kpow.secure", "decrypted").invoke(payload);
    }

    /**
     * Decode payload > text with key provided
     **/
    public static String text(String key, String payload) {
        IFn require = Clojure.var("clojure.core", "require");
        require.invoke(Clojure.read("kpow.secure"));
        return (String) Clojure.var("kpow.secure", "decrypted").invoke(key, payload);
    }

    /**
     * Decode payload > properties with key taken from environment variable KPOW_SECURE_KEY
     **/
    public static Properties properties(String payload) {
        IFn require = Clojure.var("clojure.core", "require");
        require.invoke(Clojure.read("kpow.secure"));
        String text = (String) Clojure.var("kpow.secure", "decrypted").invoke(payload);
        return (Properties) Clojure.var("kpow.secure", "->props").invoke(text);
    }

    /**
     * Decode payload > properties with key provided
     **/
    public static Properties properties(String key, String payload) {
        IFn require = Clojure.var("clojure.core", "require");
        require.invoke(Clojure.read("kpow.secure"));
        String text = (String) Clojure.var("kpow.secure", "decrypted").invoke(key, payload);
        return (Properties) Clojure.var("kpow.secure", "->props").invoke(text);
    }

}