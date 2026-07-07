package com.ragnarok.idle.repository;

import com.ragnarok.idle.domain.Avatar;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AvatarRepository extends JpaRepository<Avatar, Long> {
}
