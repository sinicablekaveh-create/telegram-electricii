package com.telegram.electric.core;

public class MessageComposer {
    private final WordBankManager wordBank;

    public MessageComposer(WordBankManager wordBank) {
        this.wordBank = wordBank;
    }

    public String compose(String prefix) {
        if (wordBank.getWords().isEmpty()) {
            return prefix;
        }
        return prefix + " - " + wordBank.getWords().get(0);
    }
}
