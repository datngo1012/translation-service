package com.datngo.translation.service.impl;

import com.datngo.translation.entity.Translation;
import com.datngo.translation.repository.TranslationRepository;
import com.datngo.translation.service.TranslationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Service
public class TranslationServiceImpl implements TranslationService {
    @Autowired
    private TranslationRepository translationRepository;

    @Override
    public Page<Translation> getTranslations(PageRequest pageRequest) {
        return translationRepository.findAll(pageRequest);
    }
}
