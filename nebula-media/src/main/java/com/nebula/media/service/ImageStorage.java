package com.nebula.media.service;

import com.nebula.common.exception.BusinessException;
import com.nebula.media.MediaError;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import javax.imageio.ImageIO;
import java.io.*;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Pattern;

@Component
public class ImageStorage {
    public static final long MAX_SIZE = 10L * 1024 * 1024;
    public static final Pattern PATH = Pattern.compile("upload/(avatar|article)/[A-Za-z0-9_-]{1,64}/[0-9]{8}/[a-f0-9]{32}\\.(jpg|png|gif|webp)");
    private final Path root;
    private final MediaLimitProperties limits;
    private final com.nebula.common.security.ResourceGuard guard;
    public ImageStorage(@Value("${file.upload-root:upload}") String directory, MediaLimitProperties limits,
            com.nebula.common.security.ResourceGuard guard) {
        this.limits = limits; this.guard = guard;
        Path configured = Path.of(directory);
        Path working = Path.of(System.getProperty("user.dir"));
        // IDEA and Maven may start from either the repository or nebula-start.
        if (!configured.isAbsolute() && working.getFileName().toString().equals("nebula-start")) working = working.getParent();
        root = (configured.isAbsolute() ? configured : working.resolve(configured)).toAbsolutePath().normalize();
    }
    public record Stored(String path, String name, String contentType, long size, int width, int height) {}

    /** 检查真实图片类型、容量和像素后写入业务/NID/日期目录，文件名由服务端生成。 */
    public Stored store(String business, String nid, MultipartFile file) throws IOException {
        try (var permit = guard.acquire(com.nebula.common.security.ResourceGuard.Work.DECODE)) {
            return storeBounded(business, nid, file);
        }
    }

    /** 在已领取解码名额的前提下验证图片并落盘，写入失败不保留半成品文件。 */
    private Stored storeBounded(String business, String nid, MultipartFile file) throws IOException {
        if (!Set.of("avatar", "article").contains(business) || nid == null || !nid.matches("[A-Za-z0-9_-]{1,64}"))
            throw new BusinessException(MediaError.INVALID_PATH);
        if (file == null || file.isEmpty() || file.getSize() > MAX_SIZE || file.getContentType() == null
                || !file.getContentType().toLowerCase(Locale.ROOT).startsWith("image/")) invalid();
        requireSpace(file.getSize());
        byte[] bytes;
        try (var input = file.getInputStream()) { bytes = input.readNBytes((int) MAX_SIZE + 1); }
        if (bytes.length == 0 || bytes.length > MAX_SIZE || bytes.length != file.getSize()) invalid();
        String format;
        int width, height;
        try (var input = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            var readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) throw new BusinessException(MediaError.INVALID_IMAGE);
            var reader = readers.next();
            try {
                reader.setInput(input, true, true);
                format = reader.getFormatName().toLowerCase(Locale.ROOT);
                if (format.equals("jpeg")) format = "jpg";
                if (!Set.of("jpg", "png", "gif", "webp").contains(format)) invalid();
                width = reader.getWidth(0); height = reader.getHeight(0);
                if (width < 1 || height < 1 || width > 12000 || height > 12000 || (long) width * height > 25_000_000) invalid();
                if (reader.read(0) == null) invalid();
            } finally { reader.dispose(); }
        } catch (IOException | IllegalArgumentException e) { throw new BusinessException(MediaError.INVALID_IMAGE); }
        String date = LocalDate.now(ZoneId.of("Asia/Shanghai")).format(DateTimeFormatter.BASIC_ISO_DATE);
        String path = "upload/" + business + "/" + nid + "/" + date + "/" + UUID.randomUUID().toString().replace("-", "") + "." + format;
        Path target = resolve(path);
        writeWithReserve(target, bytes);
        String name = Objects.toString(file.getOriginalFilename(), "图片").replace('\\', '/');
        name = name.substring(name.lastIndexOf('/') + 1).replaceAll("\\p{Cntrl}", "");
        if (name.isBlank()) name = "图片." + format;
        if (name.length() > 200) name = name.substring(0, 200);
        return new Stored(path, name, "image/" + (format.equals("jpg") ? "jpeg" : format), bytes.length, width, height);
    }
    /** 在本实例内串行检查余量并写盘，失败后删除半成品，保留配置的磁盘余量。 */
    private synchronized void writeWithReserve(Path target, byte[] bytes) throws IOException {
        requireSpace(bytes.length);
        Files.createDirectories(target.getParent());
        checkLinks(target);
        boolean opened = false;
        try (var output = Files.newOutputStream(target, StandardOpenOption.CREATE_NEW)) {
            opened = true;
            output.write(bytes);
        } catch (IOException e) {
            if (opened) Files.deleteIfExists(target);
            throw e;
        }
    }
    /** 上传目录尚未创建时从最近的已有父目录取得可用容量，不写入探测文件。 */
    private void requireSpace(long incoming) throws IOException {
        Path existing = root;
        while (existing != null && !Files.exists(existing)) existing = existing.getParent();
        if (existing == null || Files.getFileStore(existing).getUsableSpace() - incoming < limits.getMinFreeDiskBytes())
            throw new BusinessException(MediaError.STORAGE_LOW);
    }
    /** 仅将规范的资源相对路径映射到上传根目录，并拒绝目录穿越及符号链接。 */
    public Path resolve(String path) {
        if (!PATH.matcher(path).matches()) throw new BusinessException(MediaError.INVALID_PATH);
        Path target = root.resolve(path.substring("upload/".length())).normalize();
        if (!target.startsWith(root)) throw new BusinessException(MediaError.INVALID_PATH);
        checkLinks(target);
        return target;
    }
    /** 解析历史头像文件名，兼容旧链接且不允许离开头像目录。 */
    public Path legacyAvatar(String filename) {
        if (!filename.matches("[A-Za-z0-9_-]+\\.(?i:jpg|jpeg|png|gif|webp)")) throw new BusinessException(MediaError.NOT_FOUND);
        Path target = root.resolve("avatar").resolve(filename);
        checkLinks(target);
        return target;
    }
    /** 拒绝上传根目录到目标之间的符号链接，防止读写跳出指定目录。 */
    private void checkLinks(Path target) {
        for (Path part = target; part != null && part.startsWith(root); part = part.getParent())
            if (Files.isSymbolicLink(part)) throw new BusinessException(MediaError.INVALID_PATH);
    }
    /** 统一抛出图片格式或大小不符合要求的业务异常。 */
    private static void invalid() { throw new BusinessException(MediaError.INVALID_IMAGE); }
}
