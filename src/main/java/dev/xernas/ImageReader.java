package dev.xernas;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

public class ImageReader implements AutoCloseable{

    private final InputStream in;
    private final BufferedImage image;
    private final int width;
    private final int height;
    private final ByteBuffer data;

    public ImageReader(InputStream in) throws IOException {
        this.in = in;
        this.image = ImageIO.read(in);
        this.width = image.getWidth();
        this.height = image.getHeight();

        int[] pixelsRaw = new int[width * height];
        image.getRGB(0, 0, width, height, pixelsRaw, 0, width);

        ByteBuffer pixels = ByteBuffer.allocate(width * height * Integer.BYTES);
        for (int i = 0; i < width * height; i++) {
            int pixel = pixelsRaw[i];
            pixels.put((byte) ((pixel >> 16) & 0xFF)); // Red
            pixels.put((byte) ((pixel >> 8) & 0xFF));  // Green
            pixels.put((byte) (pixel & 0xFF));         // Blue
            pixels.put((byte) ((pixel >> 24) & 0xFF)); // Alpha
        }
        pixels.flip();
        this.data = pixels;
    }

    public BufferedImage getBufferedImage() {
        return image;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public ByteBuffer getData() {
        return data;
    }

    @Override
    public void close() throws Exception {
        in.close();
    }

}
