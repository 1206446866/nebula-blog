package com.nebula.media.service;

import com.nebula.common.exception.BusinessException;
import com.nebula.common.security.ResourceGuard;
import com.nebula.media.MediaError;
import com.nebula.media.entity.MediaAsset;
import org.springframework.stereotype.Service;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;

/** 有字节和条目上限的缩略图缓存；调用者必须先校验当前业务可见性。 */
@Service
public class ThumbnailService {
    private final ImageStorage storage;
    private final ResourceGuard guard;
    private final MediaLimitProperties limits;
    private final LinkedHashMap<String, byte[]> cache = new LinkedHashMap<>(16, .75f, true);
    private long bytes;
    public ThumbnailService(ImageStorage storage, ResourceGuard guard, MediaLimitProperties limits) {
        this.storage = storage; this.guard = guard; this.limits = limits;
    }

    /**
     * 生成或复用受限大小的PNG缩略图，图片解码与上传共用本实例并发预算。
     * @param asset 已通过本次请求权限检查、文件内容不可变的媒体资源
     * @return 最长边不超过320像素的PNG数据
     * @throws IOException 读取或编码失败时抛出
     */
    public byte[] render(MediaAsset asset) throws IOException {
        byte[] hit = cached(asset.getPath());
        if (hit != null) return hit;
        try (var permit = guard.acquire(ResourceGuard.Work.DECODE)) {
            hit = cached(asset.getPath());
            if (hit != null) return hit;
            var original = readSmall(asset.getPath());
            double scale = Math.min(1, 320.0 / Math.max(original.getWidth(), original.getHeight()));
            var small = new BufferedImage(Math.max(1, (int) (original.getWidth() * scale)),
                    Math.max(1, (int) (original.getHeight() * scale)), BufferedImage.TYPE_INT_ARGB);
            var graphics = small.createGraphics();
            try { graphics.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION, java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                graphics.drawImage(original, 0, 0, small.getWidth(), small.getHeight(), null);
            } finally { graphics.dispose(); }
            var output = new ByteArrayOutputStream();
            if (!ImageIO.write(small, "png", output)) throw new IOException("无法编码缩略图");
            byte[] image = output.toByteArray();
            remember(asset.getPath(), image);
            return image;
        }
    }

    /** 删除资源后移除其缓存，缓存从不替代后续请求的权限检查。 */
    public synchronized void evict(String path) {
        byte[] removed = cache.remove(path);
        if (removed != null) bytes -= removed.length;
    }
    /** 只读取缓存，不保存账号权限或业务可见性结果。 */
    private synchronized byte[] cached(String path) { return cache.get(path); }
    /** 以最近使用顺序同时约束总字节数和条目数，避免小图无限占用条目。 */
    private synchronized void remember(String path, byte[] image) {
        evict(path);
        if (image.length > limits.getThumbnailCacheBytes()) return;
        while (!cache.isEmpty() && (bytes + image.length > limits.getThumbnailCacheBytes() || cache.size() >= limits.getThumbnailCacheEntries()))
            evict(cache.keySet().iterator().next());
        cache.put(path, image); bytes += image.length;
    }
    /** 校验尺寸并使用解码器子采样，避免为320像素缩略图无条件分配完整原图。 */
    private BufferedImage readSmall(String path) throws IOException {
        try (var input = ImageIO.createImageInputStream(storage.resolve(path).toFile())) {
            if (input == null) throw new BusinessException(MediaError.NOT_FOUND);
            var readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) throw new BusinessException(MediaError.INVALID_IMAGE);
            var reader = readers.next();
            try {
                reader.setInput(input, true, true);
                int width = reader.getWidth(0), height = reader.getHeight(0);
                if (width < 1 || height < 1 || width > 12000 || height > 12000 || (long) width * height > 25_000_000)
                    throw new BusinessException(MediaError.INVALID_IMAGE);
                var params = reader.getDefaultReadParam();
                int sample = Math.max(1, Math.max(width, height) / 640);
                params.setSourceSubsampling(sample, sample, 0, 0);
                var image = reader.read(0, params);
                if (image == null) throw new BusinessException(MediaError.INVALID_IMAGE);
                return image;
            } finally { reader.dispose(); }
        }
    }
}
