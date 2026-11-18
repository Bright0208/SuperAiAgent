package com.bright.superaiagent.chatmemory;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import org.objenesis.strategy.StdInstantiatorStrategy;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Stores each conversation in a separate Kryo file. */
public class FileBasedChatMemory implements ChatMemory {
    private static final Object FILE_LOCK = new Object();
    private final Path baseDir;

    public FileBasedChatMemory(String dir) {
        baseDir = Path.of(Objects.requireNonNull(dir, "dir")).toAbsolutePath().normalize();
        try {
            Files.createDirectories(baseDir);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot create chat memory directory: " + baseDir, e);
        }
    }

    private Kryo newKryo() {
        Kryo kryo = new Kryo();
        kryo.setRegistrationRequired(false);
        kryo.setInstantiatorStrategy(new StdInstantiatorStrategy());
        return kryo;
    }

    @Override
    public void add(String conversationId, Message message) {
        add(conversationId, List.of(Objects.requireNonNull(message, "message")));
    }

    @Override
    public void add(String conversationId, List<Message> messages) {
        List<Message> additions = List.copyOf(messages);
        synchronized (FILE_LOCK) {
            Path file = getConversationFile(conversationId);
            List<Message> history = read(file);
            history.addAll(additions);
            save(file, history);
        }
    }

    @Override
    public List<Message> get(String conversationId) {
        synchronized (FILE_LOCK) {
            return read(getConversationFile(conversationId));
        }
    }

    public List<Message> get(String conversationId, int lastN) {
        if (lastN < 0) {
            throw new IllegalArgumentException("lastN must be non-negative");
        }
        List<Message> messages = get(conversationId);
        return new ArrayList<>(messages.subList(Math.max(messages.size() - lastN, 0), messages.size()));
    }

    @Override
    public void clear(String conversationId) {
        synchronized (FILE_LOCK) {
            Path file = getConversationFile(conversationId);
            try {
                Files.deleteIfExists(file);
            } catch (IOException e) {
                throw new IllegalStateException("Cannot delete chat memory: " + file, e);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private List<Message> read(Path file) {
        if (!Files.exists(file)) {
            return new ArrayList<>();
        }
        try (Input input = new Input(Files.newInputStream(file))) {
            return newKryo().readObject(input, ArrayList.class);
        } catch (IOException | RuntimeException e) {
            throw new IllegalStateException("Cannot read chat memory: " + file, e);
        }
    }

    private void save(Path file, List<Message> messages) {
        Path temporary = null;
        try {
            temporary = Files.createTempFile(baseDir, "chat-", ".tmp");
            try (Output output = new Output(Files.newOutputStream(temporary))) {
                newKryo().writeObject(output, new ArrayList<>(messages));
            }
            try {
                Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException | RuntimeException e) {
            throw new IllegalStateException("Cannot save chat memory: " + file, e);
        } finally {
            if (temporary != null) {
                try {
                    Files.deleteIfExists(temporary);
                } catch (IOException ignored) {
                    // A leftover temporary file does not invalidate the saved conversation.
                }
            }
        }
    }

    private Path getConversationFile(String conversationId) {
        if (conversationId == null || conversationId.isBlank()
                || conversationId.contains("/") || conversationId.contains("\\")
                || conversationId.contains(":")) {
            throw new IllegalArgumentException("conversationId must be a non-blank file name");
        }
        return baseDir.resolve(conversationId + ".kryo");
    }
}
