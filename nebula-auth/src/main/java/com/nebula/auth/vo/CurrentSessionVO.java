package com.nebula.auth.vo;

import com.nebula.user.vo.UserVO;
import java.util.List;

/** Current identity and authorization; never includes a token or password. */
public record CurrentSessionVO(UserVO user, List<String> roles, List<String> permissions) {}
