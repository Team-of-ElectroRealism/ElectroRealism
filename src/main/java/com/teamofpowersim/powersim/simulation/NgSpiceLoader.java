package com.teamofpowersim.powersim.simulation;

import com.sun.jna.Native;
import java.io.*;

public class NgSpiceLoader {
    private static NgSpiceLibrary INSTANCE;

    public static synchronized NgSpiceLibrary get() throws IOException {
        if (INSTANCE == null) {
            // 1) pick the correct resource
            String resource = System.getProperty("os.name").toLowerCase().contains("win")
                    ? "/native/win32-x86-64/ngspice.dll"
                    : "/native/linux/libngspice.so";
            InputStream in = NgSpiceLoader.class.getResourceAsStream(resource);
            if (in == null) throw new FileNotFoundException("Failed to find " + resource);
            File tmp = File.createTempFile("ngspice", resource.substring(resource.lastIndexOf('.')));
            tmp.deleteOnExit();
            try (OutputStream out = new FileOutputStream(tmp)) {
                in.transferTo(out);
            }
            // 2) load the library by its full absolute path
            INSTANCE = Native.load(tmp.getAbsolutePath(), NgSpiceLibrary.class);
        }
        return INSTANCE;
    }
}

