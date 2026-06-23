package com.api.llm.repository;

import com.api.llm.entity.Briefing;
import com.api.llm.entity.BriefingId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BriefingRepository extends JpaRepository<Briefing, BriefingId> {

}
