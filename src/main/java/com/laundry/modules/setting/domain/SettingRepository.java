package com.laundry.modules.setting.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SettingRepository extends JpaRepository<Setting, SettingKey> {}
