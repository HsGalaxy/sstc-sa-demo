package com.sstc.platform.identity.service;

import com.sstc.platform.common.security.CurrentUserHolder;
import com.sstc.platform.identity.domain.AccountStatus;
import com.sstc.platform.identity.domain.User;
import com.sstc.platform.identity.repo.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Shared read model of the accounts. All sub-systems resolve actor names through this
 * component instead of touching the S01 repositories directly.
 */
@Service
public class UserDirectory {

    private final UsersRepo users;

    public UserDirectory(UsersRepo users) {
        this.users = users;
    }

    @Transactional(readOnly = true)
    public List<User> all() {
        return users.findAllByOrderByCreatedAtAsc();
    }

    @Transactional(readOnly = true)
    public User get(Long id) {
        return id == null ? null : users.findById(id).orElse(null);
    }

    @Transactional(readOnly = true)
    public User require(Long id) {
        User user = get(id);
        if (user == null) {
            throw com.sstc.platform.common.web.AppException.notFound("Account " + id);
        }
        return user;
    }

    @Transactional(readOnly = true)
    public User current() {
        Long id = CurrentUserHolder.id();
        return id == null ? null : get(id);
    }

    @Transactional(readOnly = true)
    public User byUsername(String username) {
        return users.findByUsername(username).orElse(null);
    }

    @Transactional(readOnly = true)
    public Map<Long, User> index() {
        Map<Long, User> map = new HashMap<Long, User>();
        for (User user : all()) {
            map.put(user.getId(), user);
        }
        return map;
    }

    @Transactional(readOnly = true)
    public List<User> activeMembers() {
        return users.findByStatusOrderByDisplayNameAsc(AccountStatus.ACTIVE);
    }

    /** Id -> display name map used to decorate views without N+1 lookups. */
    @Transactional(readOnly = true)
    public Map<Long, String> names() {
        Map<Long, String> map = new LinkedHashMap<Long, String>();
        for (User user : all()) {
            map.put(user.getId(), user.getDisplayName());
        }
        return map;
    }

    public List<String> initialsOf(List<User> members) {
        List<String> result = new ArrayList<String>();
        for (User member : members) {
            result.add(initials(member.getDisplayName()));
        }
        return result;
    }

    public static String initials(String displayName) {
        if (displayName == null || displayName.isEmpty()) {
            return "?";
        }
        String[] parts = displayName.trim().split("\\s+");
        if (parts.length == 1) {
            return parts[0].substring(0, 1).toUpperCase();
        }
        return (parts[0].substring(0, 1) + parts[parts.length - 1].substring(0, 1)).toUpperCase();
    }
}
