package com.joowest.noticebot.repository;

import com.joowest.noticebot.domain.GlobalNoticeSource;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GlobalNoticeSourceRepository extends JpaRepository<GlobalNoticeSource, Long> {
    List<GlobalNoticeSource> findByEnabledTrueOrderBySortOrderAscSourceNameAsc();
}
