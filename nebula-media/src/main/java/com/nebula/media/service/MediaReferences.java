package com.nebula.media.service;

import java.util.List;

/** 由资源引用所属业务提供引用关系和可见性，供媒体模块统一校验。 */
public interface MediaReferences {
    record Reference(String business, Long id, String title, boolean publiclyReadable, boolean readable) {}
    /**
     * 查询资源仍有效的业务引用，并标明公开访问和当前用户的可读范围。
     *
     * @param path upload开头的资源相对路径
     * @return 业务引用列表，无引用时为空
     */
    List<Reference> find(String path);
}
