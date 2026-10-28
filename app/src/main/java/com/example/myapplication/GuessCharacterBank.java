package com.example.myapplication;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.example.myapplication.GuessCharacter.Gender.*;
import static com.example.myapplication.GuessCharacter.HairStyle.*;
import static com.example.myapplication.GuessCharacter.SkinTone.*;

public final class GuessCharacterBank {

    private GuessCharacterBank() {
        // Prevent creating unnecessary instances.
    }

    private static final List<GuessCharacter> CHARACTERS =
            Collections.unmodifiableList(Arrays.asList(

                    // Accessories: glasses, hat, earrings, beard, moustache

                    character("alex", "Alex", MAN,
                            "BROWN", SHORT, "BLACK", LIGHT,
                            true, false, false, false, false),

                    character("maya", "Maya", WOMAN,
                            "GREEN", LONG, "BROWN", MEDIUM,
                            false, false, true, false, false),

                    character("leo", "Leo", MAN,
                            "BLUE", CURLY, "BLONDE", LIGHT,
                            false, false, false, false, false),

                    character("zoe", "Zoe", WOMAN,
                            "BROWN", SHORT, "RED", DARK,
                            true, false, true, false, false),

                    character("omar", "Omar", MAN,
                            "BROWN", BALD, "NONE", MEDIUM,
                            false, false, false, true, true),

                    character("ava", "Ava", WOMAN,
                            "BLUE", LONG, "BLONDE", LIGHT,
                            false, true, false, false, false),

                    character("noah", "Noah", MAN,
                            "GREEN", SHORT, "BROWN", DARK,
                            false, true, false, false, true),

                    character("mia", "Mia", WOMAN,
                            "BROWN", CURLY, "BLACK", MEDIUM,
                            true, false, false, false, false),

                    character("ethan", "Ethan", MAN,
                            "BLUE", SHORT, "GREY", LIGHT,
                            true, false, false, true, true),

                    character("lily", "Lily", WOMAN,
                            "GREEN", LONG, "RED", LIGHT,
                            false, false, true, false, false),

                    character("arjun", "Arjun", MAN,
                            "BROWN", CURLY, "BLACK", DARK,
                            false, false, false, true, false),

                    character("sara", "Sara", WOMAN,
                            "BLUE", SHORT, "BROWN", MEDIUM,
                            false, true, true, false, false),

                    character("ben", "Ben", MAN,
                            "GREEN", BALD, "NONE", LIGHT,
                            true, true, false, false, true),

                    character("isha", "Isha", WOMAN,
                            "BROWN", LONG, "BLACK", DARK,
                            false, false, true, false, false),

                    character("lucas", "Lucas", MAN,
                            "BLUE", LONG, "BROWN", MEDIUM,
                            false, false, true, false, false),

                    character("ella", "Ella", WOMAN,
                            "GREEN", CURLY, "BLONDE", LIGHT,
                            true, true, false, false, false),

                    character("kai", "Kai", MAN,
                            "BROWN", SHORT, "RED", MEDIUM,
                            false, true, false, true, true),

                    character("nina", "Nina", WOMAN,
                            "BLUE", BALD, "NONE", DARK,
                            true, false, true, false, false),

                    character("sam", "Sam", MAN,
                            "GREEN", CURLY, "GREY", DARK,
                            true, false, false, false, true),

                    character("ruby", "Ruby", WOMAN,
                            "BROWN", SHORT, "BLONDE", MEDIUM,
                            false, false, false, false, false),

                    character("finn", "Finn", MAN,
                            "BLUE", LONG, "BLACK", LIGHT,
                            false, true, false, true, false),

                    character("tara", "Tara", WOMAN,
                            "GREEN", LONG, "GREY", DARK,
                            true, false, true, false, false),

                    character("adam", "Adam", MAN,
                            "BROWN", SHORT, "BROWN", MEDIUM,
                            true, false, true, false, false),

                    character("rosa", "Rosa", WOMAN,
                            "BLUE", CURLY, "RED", LIGHT,
                            false, true, true, false, false)
            ));

    public static List<GuessCharacter> getAll() {
        return CHARACTERS;
    }

    public static GuessCharacter findById(String id) {
        for (GuessCharacter character : CHARACTERS) {
            if (character.id.equals(id)) {
                return character;
            }
        }

        throw new IllegalArgumentException(
                "Unknown character ID: " + id
        );
    }

    private static GuessCharacter character(
            String id,
            String name,
            GuessCharacter.Gender gender,
            String eyeColor,
            GuessCharacter.HairStyle hairStyle,
            String hairColor,
            GuessCharacter.SkinTone skinTone,
            boolean glasses,
            boolean hat,
            boolean earrings,
            boolean beard,
            boolean moustache
    ) {
        return new GuessCharacter(
                id,
                name,
                gender,
                GuessCharacter.EyeColor.valueOf(eyeColor),
                hairStyle,
                GuessCharacter.HairColor.valueOf(hairColor),
                skinTone,
                glasses,
                hat,
                earrings,
                beard,
                moustache
        );
    }
}