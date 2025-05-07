package dev.xernas.format;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

public interface IFileFormat {

    void readFile(File file) throws IOException;

}
