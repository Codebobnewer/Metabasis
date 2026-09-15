package com.goga221.metabasis.util;

import java.util.Locale;

public final class Names {

    private Names() {
    }

    public static String normalize(String name) {
        return name.toLowerCase(Locale.ROOT);
    }
}
