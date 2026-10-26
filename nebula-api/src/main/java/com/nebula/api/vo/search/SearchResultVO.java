package com.nebula.api.vo.search;

import com.mybatisflex.core.paginate.Page;
import java.util.Map;

public record SearchResultVO(Page<SearchItemVO> page, Map<String, Long> counts) {}
