package com.netomedia.exam.hangman.player;

import com.google.common.base.CharMatcher;
import com.netomedia.exam.hangman.model.ServerResponse;
import com.netomedia.exam.hangman.server.HangmanServer;

import java.io.*;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

public class HangmanPlayer {

    private static HangmanServer server = new HangmanServer();

    public static void main(String[] args) throws Exception {
        System.out.println("Starting game...");
        ServerResponse response = server.startNewGame();
        System.out.println("Game started successfully");
        ServerResponse finalResponse = response;
        //load dictionary to memory and remove words that does not match the length of hangman
        List<String> dictionary = loadDictionary()
                .stream()
                .filter(word-> word.length()== finalResponse.getHangman().length())
                .collect(Collectors.toList());
        int numberOfGuess = 0;
        while (response.getGameEnded()) {
            String token = response.getToken();
            String hangman = response.getHangman();
            String guess = makeAGuess(dictionary, hangman);
            response = server.guess(token, guess);
            numberOfGuess++;
            if (response.getGameEnded()) {
                break;//no need to reconstruct the dict if game is over
            }
            dictionary = reconstructDictionary(dictionary, response.isCorrect(), guess);
        }
        System.out.println("Game ended...");
        if (response.isCorrect()) {
            System.out.println("You won the game after " + numberOfGuess + " attempts");
        } else {
            System.out.println("you lost the game after " + response.getFailedAttempts() + " attempts");
        }

    }

    private static List<String> reconstructDictionary(List<String> dictionary, Boolean correct, String guess) {
        if (correct) {
            return dictionary
                    .stream()
                    .filter(word -> word.contains(guess))
                    .collect(Collectors.toList());
        } else {
            return dictionary
                    .stream()
                    .filter(word -> !word.contains(guess))
                    .collect(Collectors.toList());
        }
    }

    private static String makeAGuess(List<String> dictionary, String hangman) {
        if (dictionary.size() == 1) {
            //only one word left in the dictionary so it must be it.
            return dictionary.stream().findAny().get();
        } else {
            //edge case- what happens if dict is empty? is it possible? I assume not.
            return getMostPopularAlphabet(hangman, dictionary)
                    .orElse(dictionary.stream().findAny().get());
        }
    }

    private static Optional<String> getMostPopularAlphabet(String hangman, List<String> dictionary) {
        Map<Character, Integer> letterToCount = new HashMap<>();
        countLettersInDictionary(dictionary, letterToCount);
        return letterToCount
                .entrySet()
                .parallelStream()
                .filter(x -> !hangman.contains(x.getKey().toString())) //filter out words that we already guessed
                .max(Map.Entry.comparingByValue())
                .map(entry -> entry.getKey().toString());
    }

    private static void countLettersInDictionary(List<String> dictionary, Map<Character, Integer> letterToCount) {
        for (String word : dictionary) {
            for (int i = 0; i < word.length(); i++) {
                char alphabet = word.charAt(i);
                int currentCount = letterToCount.get(alphabet);
                int newCount = CharMatcher.is(alphabet).countIn(word);
                letterToCount.put(alphabet, currentCount + newCount);
            }
        }
    }

    private static ArrayList<String> loadDictionary() throws IOException {
        File dictionaryFile = getFileFromResources();
        ArrayList<String> words = new ArrayList<>();
        try (FileReader reader = new FileReader(dictionaryFile);
             BufferedReader br = new BufferedReader(reader)) {
            String line;
            while ((line = br.readLine()) != null) {
                words.add(line);
            }
        }
        return words;
    }

    // get file from classpath, resources folder
    private static File getFileFromResources() {

        ClassLoader classLoader = HangmanPlayer.class.getClassLoader();
        URL resource = classLoader.getResource("dictionary.txt");
        if (resource == null) {
            throw new IllegalArgumentException("file is not found!");
        } else {
            return new File(resource.getFile());
        }
    }


}
