package com.ragnarok.idle.repository;

import com.ragnarok.idle.domain.EventMonth;
import com.ragnarok.idle.domain.SeasonalEvent;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SeasonalEventRepository extends JpaRepository<SeasonalEvent, Long> {

    List<SeasonalEvent> findByEventMonth(EventMonth eventMonth);
}
