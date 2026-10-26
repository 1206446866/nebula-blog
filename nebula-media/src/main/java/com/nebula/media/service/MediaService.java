package com.nebula.media.service;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.query.QueryCondition;
import com.nebula.common.exception.BusinessException;
import com.nebula.common.util.PageLimits;
import com.nebula.common.util.SecurityUtils;
import com.nebula.media.MediaError;
import com.nebula.media.entity.MediaAsset;
import com.nebula.media.mapper.MediaAssetMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.regex.Pattern;
import static com.nebula.media.entity.table.MediaAssetTableDef.MEDIA_ASSET;

@Slf4j
@Service
@RequiredArgsConstructor
public class MediaService {
    private final MediaAssetMapper mapper;
    private final ImageStorage storage;
    private final MediaOwnerResolver owners;
    private final List<MediaReferences> referenceProviders;
    private final MediaQuotaService quota;
    private final ThumbnailService thumbnails;
    private static final Pattern ARTICLE_PATH = Pattern.compile("upload/article/[A-Za-z0-9_-]{1,64}/[0-9]{8}/[a-f0-9]{32}\\.(?:jpg|png|gif|webp)");

    public record View(MediaAsset asset, List<MediaReferences.Reference> references) {}

    @Transactional(rollbackFor = Exception.class, timeout = 10)
    public MediaAsset uploadArticle(MultipartFile file) throws IOException {
        requireWriter();
        return upload("article", SecurityUtils.getUserId(), file);
    }

    /** 在同一事务中检查账号配额并保存图片；头像和文章上传共用累计及每日额度。 */
    @Transactional(rollbackFor = Exception.class, timeout = 10)
    public MediaAsset upload(String business, Long userId, MultipartFile file) throws IOException {
        if (!SecurityUtils.isSelf(userId) && !SecurityUtils.isAdmin()) throw new AccessDeniedException("无权上传资源");
        if (file == null) throw new BusinessException(MediaError.INVALID_IMAGE);
        quota.check(userId, file.getSize());
        var stored = storage.store(business, owners.requireNid(userId), file);
        var asset = new MediaAsset().setUserId(userId).setBusiness(business).setPath(stored.path())
                .setOriginalName(stored.name()).setContentType(stored.contentType()).setSize(stored.size())
                .setWidth(stored.width()).setHeight(stored.height()).setDeleted(0);
        try {
            if (mapper.insertSelective(asset) != 1) throw new IllegalStateException("图片记录保存失败");
            if (TransactionSynchronizationManager.isSynchronizationActive()) {
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override public void afterCompletion(int status) {
                        if (status != STATUS_COMMITTED) removeFile(asset.getPath());
                    }
                });
            }
            return asset;
        } catch (RuntimeException e) { removeFile(asset.getPath()); throw e; }
    }

    public Page<View> list(int current, int size, String business, String name, boolean all) {
        PageLimits.requireValid(current, size);
        if (all && !SecurityUtils.isAdmin()) throw new AccessDeniedException("仅管理员可以查看全部媒体");
        if (business != null && !Set.of("article", "avatar").contains(business)) throw new BusinessException(MediaError.INVALID_PATH);
        if (name != null && name.length() > 100) throw new BusinessException(MediaError.INVALID_PATH);
        var query = QueryWrapper.create().where(MEDIA_ASSET.USER_ID.eq(SecurityUtils.getUserId()).when(!all))
                .and(MEDIA_ASSET.BUSINESS.eq(business).when(business != null))
                .and(MEDIA_ASSET.ORIGINAL_NAME.like(name).when(name != null && !name.isBlank()))
                .orderBy(MEDIA_ASSET.ID.desc());
        if (all) {
            QueryCondition scope = null;
            if (SecurityUtils.hasPermission("article:list")) scope = MEDIA_ASSET.BUSINESS.eq("article");
            if (SecurityUtils.hasPermission("user:list")) scope = scope == null ? MEDIA_ASSET.BUSINESS.eq("avatar") : scope.or(MEDIA_ASSET.BUSINESS.eq("avatar"));
            if (scope == null) throw new AccessDeniedException("需要相应业务的查看权限");
            query.and(scope);
        }
        Page<MediaAsset> page = mapper.paginate(Page.of(current, size), query);
        Page<View> result = Page.of(current, size, page.getTotalRow());
        result.setRecords(page.getRecords().stream().map(asset -> new View(asset, references(asset.getPath()).stream()
                .map(ref -> ref.readable() ? ref : new MediaReferences.Reference(ref.business(), null, "受限内容", false, false))
                .toList())).toList());
        return result;
    }

    public MediaAsset requireFile(Long id) {
        var asset = mapper.selectOneById(id);
        if (asset == null || !canRead(asset)) throw new BusinessException(MediaError.NOT_FOUND);
        return asset;
    }

    public MediaAsset requirePublicPath(String path) {
        if (!ImageStorage.PATH.matcher(path).matches()) throw new BusinessException(MediaError.NOT_FOUND);
        var asset = mapper.selectOneByQuery(QueryWrapper.create().where(MEDIA_ASSET.PATH.eq(path)));
        if (asset == null || references(path).stream().noneMatch(MediaReferences.Reference::publiclyReadable))
            throw new BusinessException(MediaError.NOT_FOUND);
        return asset;
    }

    public MediaAsset requirePathFile(String path) {
        if (!ImageStorage.PATH.matcher(path).matches()) throw new BusinessException(MediaError.NOT_FOUND);
        var asset = mapper.selectOneByQuery(QueryWrapper.create().where(MEDIA_ASSET.PATH.eq(path)));
        if (asset == null || !canRead(asset)) throw new BusinessException(MediaError.NOT_FOUND);
        return asset;
    }

    /** 按资源归属、对应管理权限及业务引用判断当前用户是否有权读取图片。 */
    private boolean canRead(MediaAsset asset) {
        return SecurityUtils.isSelf(asset.getUserId()) || canAdminister(asset, false)
                || references(asset.getPath()).stream().anyMatch(MediaReferences.Reference::readable);
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        var asset = mapper.selectOneByQuery(QueryWrapper.create().where(MEDIA_ASSET.ID.eq(id)).forUpdate());
        if (asset == null || (!SecurityUtils.isSelf(asset.getUserId()) && !canAdminister(asset, true)))
            throw new BusinessException(MediaError.NOT_FOUND);
        if (!references(asset.getPath()).isEmpty()) throw new BusinessException(MediaError.IN_USE);
        if (mapper.deleteById(id) != 1) throw new BusinessException(MediaError.NOT_FOUND);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCommit() { removeFile(asset.getPath()); thumbnails.evict(asset.getPath()); }
        });
    }

    /** Lock references with the same row lock as deletion to prevent save/delete races. */
    public void lockArticleImages(String content) {
        if (content == null) return;
        var paths = new TreeSet<String>();
        ARTICLE_PATH.matcher(content).results().forEach(match -> paths.add(match.group()));
        for (String path : paths) {
            var asset = mapper.selectOneByQuery(QueryWrapper.create().where(MEDIA_ASSET.PATH.eq(path)).forUpdate());
            if (asset == null || !canRead(asset)) throw new BusinessException(MediaError.NOT_FOUND);
        }
    }

    /** 合并各业务模块报告的有效资源引用，供公开访问和删除保护共同使用。 */
    private List<MediaReferences.Reference> references(String path) {
        return referenceProviders.stream().flatMap(provider -> provider.find(path).stream()).toList();
    }
    /** 要求当前用户具有文章创建或编辑权限，防止无写作权限的图片上传。 */
    private void requireWriter() {
        if (SecurityUtils.getLoginUser() == null || (!SecurityUtils.hasPermission("article:create") && !SecurityUtils.hasPermission("article:update")))
            throw new AccessDeniedException("需要文章写作权限");
    }
    /** 管理员还需具备对应业务的查看或删除权限，角色本身不直接放行。 */
    private boolean canAdminister(MediaAsset asset, boolean delete) {
        String permission = "article".equals(asset.getBusiness())
                ? (delete ? "article:delete" : "article:list")
                : (delete ? "user:update" : "user:list");
        return SecurityUtils.isAdmin() && SecurityUtils.hasPermission(permission);
    }
    /** 清理受管目录中的资源文件，IO失败只记录日志，不推翻已提交的数据库事务。 */
    private void removeFile(String path) {
        try { Files.deleteIfExists(storage.resolve(path)); }
        catch (IOException e) { log.warn("媒体文件清理失败，路径={}", path, e); }
    }
}
