package com.chatmind.repository;

import com.chatmind.entity.PmUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 用户数据访问层
 */
@Repository
public interface PmUserRepository extends JpaRepository<PmUser, Long> {
    
    /**
     * 根据用户名查询用户
     */
    PmUser findByName(String name);
}
