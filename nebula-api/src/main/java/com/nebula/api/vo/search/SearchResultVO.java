package com.nebula.api.vo.search;

import com.mybatisflex.core.paginate.Page;
import java.util.Map;

/** 数量可能随异步同步变化；候选尚未全部核验时明确标为估计，并限制可访问结果窗口。 */
public record SearchResultVO(Page<SearchItemVO> page, Map<String, Long> counts, boolean countsExact, boolean windowLimited) {
    public SearchResultVO(Page<SearchItemVO> page, Map<String, Long> counts) { this(page, counts, true, false); }
}
