package xyz.rebasing.rebot.plugin.notion;

import javax.inject.Inject;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import xyz.rebasing.rebot.api.domain.Chat;
import xyz.rebasing.rebot.api.domain.From;
import xyz.rebasing.rebot.api.domain.Message;
import xyz.rebasing.rebot.api.domain.MessageUpdate;
import xyz.rebasing.rebot.service.persistence.repository.NotionRepository;

@QuarkusTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class NotionPluginTest {

    @Inject
    NotionPlugin notionPlugin;

    @Inject
    NotionRepository notionRepository;

    @BeforeAll
    public void start() {
        System.setProperty("xyz.rebasing.rebot.plugin.notion.timeout", "0");
        notionPlugin.load();
    }

    @Test
    @Order(1)
    public void testSimpleNotionIncrease() {
        notionPlugin.process(getSimpleMessage("test++"), "en_US");
        Assertions.assertEquals(1, notionRepository.get("test"));
    }

    @Test
    @Order(2)
    public void testSimpleNotionDecreaseDownToZero() {
        notionPlugin.process(getSimpleMessage("test--"), "en_US");
        Assertions.assertEquals(0, notionRepository.get("test"));
    }

    @Test
    @Order(3)
    public void testSimpleNotionDecreaseToNegative() {
        notionPlugin.process(getSimpleMessage("test--"), "en_US");
        Assertions.assertEquals(-1, notionRepository.get("test"));
    }

    @Test
    @Order(4)
    public void testSimpleNotionIncreaseToZero() {
        notionPlugin.process(getSimpleMessage("test++"), "en_US");
        Assertions.assertEquals(0, notionRepository.get("test"));
    }

    @Test
    @Order(5)
    public void testSimpleNotionIncreaseToOne() {
        notionPlugin.process(getSimpleMessage("test++"), "en_US");
        Assertions.assertEquals(1, notionRepository.get("test"));
    }

    private MessageUpdate getSimpleMessage(String text) {
        MessageUpdate messageUpdate = new MessageUpdate();
        messageUpdate.setEdited(false);
        Chat chat = new Chat(-101010, "testChat");
        From from = new From();
        from.setUsername("test-username");
        from.setFirstName("FirstName");
        Message message = new Message(10L, chat, text);
        message.setFrom(from);
        messageUpdate.setMessage(message);
        messageUpdate.setEdited(false);
        return messageUpdate;
    }
}
