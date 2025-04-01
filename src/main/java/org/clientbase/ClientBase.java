package org.clientbase;

import lombok.Getter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.clientbase.event.EventManager;
import org.clientbase.module.ModuleManager;

/**
 * @author LangYa466
 * @since 4/2/2025 12:40 AM
 */
@Getter
public class ClientBase {
    public static final ClientBase INSTANCE = new ClientBase();

    public static final String name = "ClientBase";
    private static final Logger logger = LogManager.getLogger();
    private boolean initiated = false;

    private EventManager eventManager;
    private ModuleManager moduleManager;

    public void init() {
        if (initiated) return;

        logger.info("initiating");
        eventManager = new EventManager();
        moduleManager = new ModuleManager();
        logger.info("initiated");

        initiated = true;
    }
}
