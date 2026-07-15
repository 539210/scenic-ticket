package com.scenicticket.ui;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

final class ScenicImageStorage {
    private static final long MAX_BYTES = 10L * 1024L * 1024L;
    private static final Set<String> EXTENSIONS = Set.of("jpg", "jpeg", "png", "gif", "bmp");
    private final Path root;

    ScenicImageStorage() {
        this(Path.of(System.getProperty("user.home"), ".scenic-ticket", "images"));
    }

    ScenicImageStorage(Path root) {
        this.root = root;
    }

    List<String> store(List<File> files) {
        if (files == null || files.isEmpty()) {
            return List.of();
        }
        try {
            Files.createDirectories(root);
            List<String> stored = new ArrayList<>();
            for (File file : files) {
                Path source = file.toPath().toAbsolutePath().normalize();
                if (!Files.isRegularFile(source) || Files.size(source) > MAX_BYTES) {
                    throw new IllegalArgumentException("图片不存在或超过 10MB：" + file.getName());
                }
                String extension = extension(file.getName());
                if (!EXTENSIONS.contains(extension)) {
                    throw new IllegalArgumentException("不支持的图片格式：" + file.getName());
                }
                BufferedImage decoded = ImageIO.read(source.toFile());
                if (decoded == null) {
                    throw new IllegalArgumentException("文件不是有效图片：" + file.getName());
                }
                Path target = root.resolve(UUID.randomUUID() + "." + extension);
                Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
                stored.add(target.toAbsolutePath().normalize().toString());
            }
            return stored;
        } catch (IOException exception) {
            throw new IllegalStateException("保存景点图片失败", exception);
        }
    }

    private String extension(String name) {
        int dot = name.lastIndexOf('.');
        return dot < 0 ? "" : name.substring(dot + 1).toLowerCase(Locale.ROOT);
    }
}
