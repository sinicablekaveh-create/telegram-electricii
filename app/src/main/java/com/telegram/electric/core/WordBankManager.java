package com.telegram.electric.core;

import java.util.ArrayList;
import java.util.List;

public class WordBankManager {
    private final List<String> words = new ArrayList<>();

    public WordBankManager() {
        words.add("کابل");
        words.add("تابلو برق");
        words.add("فیوز");
        words.add("کلید مینیاتوری");
        words.add("سیم کشی");
    }

    public void addWord(String word) {
        if (word != null && !word.isEmpty() && !words.contains(word)) {
            words.add(word);
        }
    }

    public List<String> getWords() {
        return words;
    }
}
