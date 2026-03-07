package com.thespawnpoint.backend.entity.user;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Language {
    UA("Українська"),
    EN("English"),
    PL("Polski"),
    DE("Deutsch"),
    FR("Français"),
    ES("Español"),
    PT("Português"),
    TR("Türkçe"),
    KO("한국어"),
    ZH("中文"),
    JA("日本語"),
    IT("Italiano"),
    NL("Nederlands"),
    SV("Svenska"),
    NO("Norsk"),
    DA("Dansk"),
    FI("Suomi"),
    CS("Čeština"),
    SK("Slovenčina"),
    HU("Magyar"),
    RO("Română"),
    BG("Български"),
    HR("Hrvatski"),
    SR("Srpski"),
    AR("العربية"),
    HI("हिन्दी"),
    VI("Tiếng Việt"),
    TH("ภาษาไทย"),
    ID("Bahasa Indonesia");

    private final String nativeName;
}