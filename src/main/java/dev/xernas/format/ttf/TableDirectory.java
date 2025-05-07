package dev.xernas.format.ttf;

public record TableDirectory(String tag, long checkSum, long offset, long length) {

    @Override
    public String toString() {
        return "TableDirectory{" +
                "tag='" + tag + '\'' +
                ", checkSum=" + checkSum +
                ", offset=" + offset +
                ", length=" + length +
                '}';
    }
}
