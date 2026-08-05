package me.eldodebug.soar.logger;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class GlideLogger {
	
    public static final Logger logger = LogManager.getLogger("Glide Client");
    
    public static void info(String message) {
    	logger.info("[GC/INFO] " + message);
    }
    
    public static void warn(String message) {
    	logger.warn("[GC/WARN] " + message);
    }

    public static void error(String message) {
    	logger.error("[GC/ERROR] " + message);
    }
    
    public static void error(String message, Exception e) {
    	logger.error("[GC/ERROR] " + message, e);
    }

	public static Logger getLogger() {
		return logger;
	}
}
