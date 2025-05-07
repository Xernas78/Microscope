package dev.xernas.format.ttf;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public record NameTable(List<NameRecord> nameRecords) {

    public List<NameRecord> getNames(int nameID) {
        List<NameRecord> names = new ArrayList<>();
        for (NameRecord nameRecord : nameRecords) {
            if (nameRecord.nameID() == nameID) {
                names.add(nameRecord);
            }
        }
        return names;
    }

    public record NameRecord(int platformID, int encodingID, int languageID, int nameID, String nameString) {
    }
}
