package com.datngo.translation.service;

import com.datngo.translation.entity.Translation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

public interface TranslationService {
    Page<Translation> getTranslations(PageRequest pageRequest);
}
