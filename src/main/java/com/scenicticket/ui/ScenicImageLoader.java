package com.scenicticket.ui;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public final class ScenicImageLoader {
    static final int MAX_IMAGE_BYTES = 10 * 1024 * 1024;
    private static final int CONNECT_TIMEOUT_MS = 4_000;
    private static final int READ_TIMEOUT_MS = 6_000;

    private final int maxWidth;
    private final int maxHeight;

    public ScenicImageLoader(int maxWidth, int maxHeight) {
        if (maxWidth <= 0 || maxHeight <= 0) {
            throw new IllegalArgumentException("图片显示尺寸必须大于 0");
        }
        this.maxWidth = maxWidth;
        this.maxHeight = maxHeight;
    }

    public Optional<ImageIcon> loadFirst(List<?> imageSources) {
        if (imageSources == null || imageSources.isEmpty()) {
            return Optional.empty();
        }
        for (Object source : imageSources) {
            if (source == null || String.valueOf(source).isBlank()) {
                continue;
            }
            try {
                BufferedImage image = download(String.valueOf(source).trim());
                if (image != null) {
                    return Optional.of(scale(image, maxWidth, maxHeight));
                }
            } catch (IOException | IllegalArgumentException ignored) {
                // A broken optional image must not make the scenic introduction fail.
            }
        }
        return Optional.empty();
    }

    private BufferedImage download(String source) throws IOException {
        if (source.matches("^[A-Za-z]:[\\\\/].*")) {
            return readLocal(Path.of(source));
        }
        URI uri;
        try {
            uri = URI.create(source);
        } catch (IllegalArgumentException malformedUri) {
            return readLocal(Path.of(source));
        }
        String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
        if (scheme.isEmpty()) {
            return readLocal(Path.of(source));
        }
        if ("file".equals(scheme)) {
            return readLocal(Path.of(uri));
        }
        if (!"http".equals(scheme) && !"https".equals(scheme)) {
            throw new IllegalArgumentException("不支持的图片来源");
        }

        URLConnection connection = uri.toURL().openConnection();
        connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
        connection.setReadTimeout(READ_TIMEOUT_MS);
        connection.setRequestProperty("User-Agent", "scenic-ticket-desktop/1.0");
        try {
            if (connection instanceof HttpURLConnection httpConnection) {
                httpConnection.setInstanceFollowRedirects(true);
                int status = httpConnection.getResponseCode();
                if (status < 200 || status >= 300) {
                    throw new IOException("图片请求失败，HTTP " + status);
                }
            }

            try (InputStream inputStream = connection.getInputStream()) {
                byte[] bytes = inputStream.readNBytes(MAX_IMAGE_BYTES + 1);
                if (bytes.length > MAX_IMAGE_BYTES) {
                    throw new IOException("图片文件超过 10 MB");
                }
                return ImageIO.read(new ByteArrayInputStream(bytes));
            }
        } finally {
            if (connection instanceof HttpURLConnection httpConnection) {
                httpConnection.disconnect();
            }
        }
    }

    private BufferedImage readLocal(Path path) throws IOException {
        Path normalized = path.toAbsolutePath().normalize();
        if (!Files.isRegularFile(normalized) || Files.size(normalized) > MAX_IMAGE_BYTES) {
            throw new IOException("本地图片不存在或超过 10 MB");
        }
        try (InputStream inputStream = Files.newInputStream(normalized)) {
            return ImageIO.read(inputStream);
        }
    }

    static ImageIcon scale(BufferedImage source, int maxWidth, int maxHeight) {
        double ratio = Math.min(1.0,
                Math.min((double) maxWidth / source.getWidth(), (double) maxHeight / source.getHeight()));
        int width = Math.max(1, (int) Math.round(source.getWidth() * ratio));
        int height = Math.max(1, (int) Math.round(source.getHeight() * ratio));
        if (width == source.getWidth() && height == source.getHeight()) {
            return new ImageIcon(source);
        }
        BufferedImage scaled = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = scaled.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            graphics.drawImage(source, 0, 0, width, height, null);
        } finally {
            graphics.dispose();
        }
        return new ImageIcon(scaled);
    }
}
