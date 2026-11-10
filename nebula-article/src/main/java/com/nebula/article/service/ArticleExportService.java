package com.nebula.article.service;

/** 在文章和媒体读取权限内导出Markdown及本地图片，不抓取外部资源。 */
public interface ArticleExportService {
    record File(String name, String contentType, byte[] bytes) {}
    /**
     * 导出当前有权阅读的文章；打包图片时逐张检查读取权限，不获取外部地址。
     * @param articleId 文章编号
     * @param images 是否打包本地管理的文章图片
     * @return Markdown 文件或含图片的ZIP，最多50张、合计40MiB
     */
    File export(long articleId, boolean images);
    /**
     * 打包编辑器当前内容，要求写作权限；不会保存、发布或读取任意磁盘路径。
     * @param title 文件标题
     * @param content 当前Markdown正文
     * @return 含有权限读取的本地图片的ZIP
     */
    File archive(String title, String content);
}
