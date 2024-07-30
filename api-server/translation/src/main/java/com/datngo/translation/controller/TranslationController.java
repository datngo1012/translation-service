package com.datngo.translation.controller;

import com.datngo.translation.entity.Translation;
import com.datngo.translation.service.TranslationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class TranslationController {

    @Autowired
    private TranslationService translationService;

    @GetMapping("/translations")
    public Page<Translation> getTranslations(@RequestParam(name = "page_number", defaultValue = "0") int page,
                                             @RequestParam(name = "page_size", defaultValue = "10") int pageSize) {
        return translationService.getTranslations(PageRequest.of(page, pageSize));
    }
}
