package com.nebula.article.service.impl;

import com.nebula.article.service.ArticleExportService;
import com.nebula.article.service.ArticleReadAccess;
import com.nebula.common.exception.BusinessException;
import com.nebula.common.util.SecurityUtils;
import com.nebula.media.service.ImageStorage;
import com.nebula.media.service.MediaService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.regex.Pattern;
import java.util.zip.*;
import static com.nebula.common.exception.code.ArticleErrorCode.*;

@Service
@RequiredArgsConstructor
public class ArticleExportServiceImpl implements ArticleExportService {
    private final ArticleReadAccess access;
    private final MediaService media;
    private final ImageStorage storage;
    private static final Pattern IMAGE = Pattern.compile("(?<![\\w/.:?=&%-])/?upload/article/[A-Za-z0-9_-]{1,64}/[0-9]{8}/[a-f0-9]{32}\\.(?:jpg|png|gif|webp)(?![A-Za-z0-9_.-])");
    private static final long MAX_BYTES = 40L * 1024 * 1024;

    @Override
    public File export(long articleId, boolean images) {
        if (!SecurityUtils.hasPermission("article:list")) throw new AccessDeniedException("需要文章阅读权限");
        var article = access.requireReadableArticle(articleId);
        return images ? zip(article.getTitle(), article.getContent())
                : new File(filename(article.getTitle()) + ".md", "text/markdown", markdown(article.getTitle(), article.getContent()).getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public File archive(String title, String content) {
        if (SecurityUtils.getLoginUser() == null || !SecurityUtils.hasPermission("article:create") && !SecurityUtils.hasPermission("article:update"))
            throw new AccessDeniedException("需要文章写作权限");
        return zip(title, content);
    }

    /** 只打包本地受管理的路径，每张图片沿用媒体服务权限，不抓取网络资源。 */
    private File zip(String title, String content) {
        if (content.length() > 200000) throw new BusinessException(ARTICLE_EXPORT_LIMIT);
        Set<String> paths = new LinkedHashSet<>();
        IMAGE.matcher(content).results().forEach(match -> paths.add(match.group().replaceFirst("^/", "")));
        if (paths.size() > 50) throw new BusinessException(ARTICLE_EXPORT_LIMIT);
        try (var bytes = new ByteArrayOutputStream(); var zip = new ZipOutputStream(bytes, StandardCharsets.UTF_8)) {
            long total = 0;
            Map<String, String> entries = new HashMap<>();
            for (String path : paths) {
                var asset = media.requirePathFile(path);
                Path file = storage.resolve(asset.getPath());
                if (!Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS)) throw new BusinessException(ARTICLE_EXPORT_IMAGE_MISSING);
                long size = Files.size(file); total += size;
                if (total > MAX_BYTES) throw new BusinessException(ARTICLE_EXPORT_LIMIT);
                String entry = "images/" + (entries.size() + 1) + "-" + file.getFileName();
                entries.put(path, entry);
                zip.putNextEntry(new ZipEntry(entry));
                try (InputStream input = Files.newInputStream(file)) {
                    byte[] buffer = new byte[8192]; int count; long written = 0;
                    while ((count = input.read(buffer)) != -1) {
                        written += count;
                        if (written > size || written > MAX_BYTES) throw new BusinessException(ARTICLE_EXPORT_LIMIT);
                        zip.write(buffer, 0, count);
                    }
                }
                zip.closeEntry();
            }
            content = IMAGE.matcher(content).replaceAll(match -> java.util.regex.Matcher.quoteReplacement(entries.get(match.group().replaceFirst("^/", ""))));
            zip.putNextEntry(new ZipEntry("article.md"));
            zip.write(markdown(title, content).getBytes(StandardCharsets.UTF_8)); zip.closeEntry(); zip.finish();
            return new File(filename(title) + ".zip", "application/zip", bytes.toByteArray());
        } catch (IOException e) { throw new BusinessException(ARTICLE_EXPORT_IMAGE_MISSING); }
    }

    /** 标题单独写入文档首行，保持正文原有Markdown结构。 */
    private String markdown(String title, String content) { return "# " + title.replace('\r', ' ').replace('\n', ' ') + "\n\n" + content; }
    /** 下载名称移除路径分隔符、控制字符和Windows保留字符，最终文件扩展名固定。 */
    private String filename(String title) {
        String name = title.replaceAll("[\\p{Cntrl}\\\\/:*?\"<>|]", "_").strip().replaceAll("[. ]+$", "");
        return name.isBlank() ? "nebula-article" : name.substring(0, Math.min(80, name.length()));
    }
}
