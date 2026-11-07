package dev.nyx.utils.features.commands;

import dev.nyx.utils.util.chat.Signature;
import dev.nyx.utils.util.chat.SimpleSignature;

public class MessageSignatures {
    public static final Signature GENERAL = SimpleSignature.fromLong(-1L);
    public static final Signature SUCCESS = SimpleSignature.fromLong(0L);
    public static final Signature FAIL    = SimpleSignature.fromLong(1L);
}