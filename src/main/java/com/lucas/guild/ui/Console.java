package com.lucas.guild.ui;

import javafx.scene.control.TextArea;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

public class Console extends OutputStream {
    private TextArea output;

    public Console(TextArea ta) {
        this.output = ta;
    }

    @Override
    public void write(int i) throws IOException {
        // Redirige les données vers le TextArea
        output.appendText(String.valueOf((char) i));
    }

    public static void redirectSystemOut(TextArea textArea) {
        Console console = new Console(textArea);
        PrintStream ps = new PrintStream(console, true);
        System.setOut(ps);
        System.setErr(ps);
    }
}
