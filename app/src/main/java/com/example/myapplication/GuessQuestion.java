package com.example.myapplication;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public final class GuessQuestion {

    public enum Category {
        GENDER("Gender"),
        EYE_COLOR("Eye Color"),
        HAIR("Hair"),
        HAIR_COLOR("Hair Color"),
        SKIN_TONE("Skin Tone"),
        ACCESSORIES("Accessories"),
        FACIAL_HAIR("Facial Hair");

        public final String label;

        Category(String label) {
            this.label = label;
        }
    }

    public final String id;
    public final Category category;
    public final String text;

    private final String attribute;

    private GuessQuestion(
            Category category,
            String attribute,
            String text
    ) {
        this.id = category.name() + ":" + attribute;
        this.category = category;
        this.attribute = attribute;
        this.text = text;
    }

    // Returns true for YES and false for NO.
    public boolean matches(GuessCharacter character) {
        if (character == null) {
            throw new IllegalArgumentException(
                    "A character is required to answer a question."
            );
        }

        switch (category) {
            case GENDER:
                return character.gender.name().equals(attribute);

            case EYE_COLOR:
                return character.eyeColor.name().equals(attribute);

            case HAIR:
                return character.hairStyle.name().equals(attribute);

            case HAIR_COLOR:
                return character.hairColor.name().equals(attribute);

            case SKIN_TONE:
                return character.skinTone.name().equals(attribute);

            case ACCESSORIES:
                switch (attribute) {
                    case "GLASSES":
                        return character.hasGlasses;
                    case "HAT":
                        return character.hasHat;
                    case "EARRINGS":
                        return character.hasEarrings;
                    default:
                        throw new IllegalStateException(
                                "Unknown accessory: " + attribute
                        );
                }

            case FACIAL_HAIR:
                switch (attribute) {
                    case "BEARD":
                        return character.hasBeard;
                    case "MOUSTACHE":
                        return character.hasMoustache;
                    case "NONE":
                        return !character.hasBeard
                                && !character.hasMoustache;
                    default:
                        throw new IllegalStateException(
                                "Unknown facial hair: " + attribute
                        );
                }

            default:
                throw new IllegalStateException(
                        "Unknown question category."
                );
        }
    }

    // A candidate stays if their answer matches the secret's answer.
    public boolean keepsCandidate(
            GuessCharacter candidate,
            boolean secretAnswer
    ) {
        return matches(candidate) == secretAnswer;
    }

    public static List<GuessQuestion> getForCategory(
            Category category
    ) {
        List<GuessQuestion> questions = new ArrayList<>();

        switch (category) {
            case GENDER:
                add(questions, category, "MAN",
                        "Is your person a man?");
                add(questions, category, "WOMAN",
                        "Is your person a woman?");
                break;

            case EYE_COLOR:
                for (GuessCharacter.EyeColor color
                        : GuessCharacter.EyeColor.values()) {
                    add(questions, category, color.name(),
                            "Does your person have "
                                    + lower(color.name()) + " eyes?");
                }
                break;

            case HAIR:
                add(questions, category, "SHORT",
                        "Does your person have short hair?");
                add(questions, category, "LONG",
                        "Does your person have long hair?");
                add(questions, category, "CURLY",
                        "Does your person have curly hair?");
                add(questions, category, "BALD",
                        "Is your person bald?");
                break;

            case HAIR_COLOR:
                for (GuessCharacter.HairColor color
                        : GuessCharacter.HairColor.values()) {
                    if (color == GuessCharacter.HairColor.NONE) {
                        continue;
                    }

                    add(questions, category, color.name(),
                            "Does your person have "
                                    + lower(color.name()) + " hair?");
                }
                break;

            case SKIN_TONE:
                for (GuessCharacter.SkinTone tone
                        : GuessCharacter.SkinTone.values()) {
                    add(questions, category, tone.name(),
                            "Does your person have a "
                                    + lower(tone.name()) + " skin tone?");
                }
                break;

            case ACCESSORIES:
                add(questions, category, "GLASSES",
                        "Does your person wear glasses?");
                add(questions, category, "HAT",
                        "Does your person wear a hat?");
                add(questions, category, "EARRINGS",
                        "Does your person wear earrings?");
                break;

            case FACIAL_HAIR:
                add(questions, category, "BEARD",
                        "Does your person have a beard?");
                add(questions, category, "MOUSTACHE",
                        "Does your person have a moustache?");
                add(questions, category, "NONE",
                        "Does your person have no facial hair?");
                break;
        }

        return Collections.unmodifiableList(questions);
    }

    private static void add(
            List<GuessQuestion> questions,
            Category category,
            String attribute,
            String text
    ) {
        questions.add(new GuessQuestion(category, attribute, text));
    }

    private static String lower(String value) {
        return value.toLowerCase(Locale.ROOT);
    }
}