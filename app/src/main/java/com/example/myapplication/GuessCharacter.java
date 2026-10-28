package com.example.myapplication;

public final class GuessCharacter {

    public enum Gender {
        MAN, WOMAN
    }

    public enum EyeColor {
        BROWN, BLUE, GREEN
    }

    public enum HairStyle {
        SHORT, LONG, CURLY, BALD
    }

    public enum HairColor {
        BLACK, BROWN, BLONDE, RED, GREY, NONE
    }

    public enum SkinTone {
        LIGHT, MEDIUM, DARK
    }

    public final String id;
    public final String name;
    public final Gender gender;
    public final EyeColor eyeColor;
    public final HairStyle hairStyle;
    public final HairColor hairColor;
    public final SkinTone skinTone;

    public final boolean hasGlasses;
    public final boolean hasHat;
    public final boolean hasEarrings;
    public final boolean hasBeard;
    public final boolean hasMoustache;

    public GuessCharacter(
            String id,
            String name,
            Gender gender,
            EyeColor eyeColor,
            HairStyle hairStyle,
            HairColor hairColor,
            SkinTone skinTone,
            boolean hasGlasses,
            boolean hasHat,
            boolean hasEarrings,
            boolean hasBeard,
            boolean hasMoustache
    ) {
        if (id == null || id.trim().isEmpty()
                || name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "Every character needs an ID and name."
            );
        }

        if (gender == null || eyeColor == null
                || hairStyle == null || hairColor == null
                || skinTone == null) {
            throw new IllegalArgumentException(
                    "Character attributes cannot be missing."
            );
        }

        if ((hairStyle == HairStyle.BALD)
                != (hairColor == HairColor.NONE)) {
            throw new IllegalArgumentException(
                    "Bald characters must use hair color NONE."
            );
        }

        this.id = id;
        this.name = name;
        this.gender = gender;
        this.eyeColor = eyeColor;
        this.hairStyle = hairStyle;
        this.hairColor = hairColor;
        this.skinTone = skinTone;
        this.hasGlasses = hasGlasses;
        this.hasHat = hasHat;
        this.hasEarrings = hasEarrings;
        this.hasBeard = hasBeard;
        this.hasMoustache = hasMoustache;
    }
}