package com.github.galdosd.betamax.opengl;

import lombok.RequiredArgsConstructor;
import org.lwjgl.opengl.*;
import org.lwjgl.system.APIUtil;
import org.lwjgl.system.Callback;

import java.io.PrintStream;
import java.util.function.Consumer;

import static org.lwjgl.opengl.AMDDebugOutput.*;
import static org.lwjgl.opengl.ARBDebugOutput.*;
import static org.lwjgl.opengl.GL11.glEnable;
import static org.lwjgl.opengl.GL11.glGetInteger;
import static org.lwjgl.opengl.GL30.GL_CONTEXT_FLAGS;
import static org.lwjgl.opengl.GL43.*;
import static org.lwjgl.system.APIUtil.apiLog;
import static org.lwjgl.system.APIUtil.apiUnknownToken;
import static org.lwjgl.system.MemoryUtil.NULL;

/**
 * copy (because it's final etc) of org.lwjgl.opengl.GLUtil so we can intercept opengl debug messages
 * but you know, like, actually act based on them and log them to our favorite logger rather than stderr
 * FIXME: make sure we are complying with OSS licenses before releasing anything using this file
 * Probably just submit a patch upstream with our improvements would be ideal
 */
public final class GlDebugMessages {
    private GlDebugMessages() {/*uninstantiable*/}

    @RequiredArgsConstructor public static class Msg {
        public final int id;
        public final String source;
        public final String type;
        public final String severity;
        public final String message;
        public final String category;
        /** what API we are using to get the debug output
         * if "AMD_debug_output", category is populated but type and source are not populated
         * otherwise category is not populated but all type and source are populated
         * severity, id, message, debugFacility should always be populated
         */
        public final String debugFacility;

        public String getOriginString() {
            if(null!=category) return String.format("Category %s", category);
            else return String.format("Src %s, Type %s", source, type);
        }

        public String getLogMsg() {
            return String.format(
                    "[LWJGL] %s message (ID 0x%X, %s, Sev %s):\n\t%s",
                    debugFacility,
                    id,
                    getOriginString(),
                    severity,
                    message
            );
        }
    }

    /**
     * Detects the best debug output functionality to use and creates a callback that prints information to {@link APIUtil#DEBUG_STREAM}. The callback
     * function is returned as a {@link Callback}, that should be {@link Callback#free freed} when no longer needed.
     */
    public static Callback setupDebugMessageCallback() {
        return setupDebugMessageCallback(APIUtil.DEBUG_STREAM);
    }

    /**
     * Detects the best debug output functionality to use and creates a callback that prints information to the specified {@link PrintStream}. The callback
     * function is returned as a {@link Callback}, that should be {@link Callback#free freed} when no longer needed.
     *
     * @param stream the output {@link PrintStream}
     */
    public static Callback setupDebugMessageCallback(PrintStream stream) {
        return setupDebugMessageCallback( msg -> {
            stream.printf("[LWJGL] %s message\n", msg.debugFacility);
            printDetail(stream, "ID", String.format("0x%X", msg.id));
            printDetail(stream, "Source", msg.source);
            printDetail(stream, "Type", msg.type);
            printDetail(stream, "Category", msg.category);
            printDetail(stream, "Severity", msg.severity);
            printDetail(stream, "Message", msg.message);
        });
    }

    /** Process debug messages in a more typical hygienic Java style-- log them to a real Logger,
     *  with the appropriate severity level (HIGH->ERROR, MEDIUM->WARN, LOW->WARN,
     *  NOTIFICATION->DEBUG, unknown->WARN) and throw an IllegalStateException for anything but
     *  NOTIFICATION. This makes it trivial to track down an error -- set a breakpoint right before
     *  the exception throw in this method
     */
    public static Callback setupJavaStyleDebugMessageCallback(org.slf4j.Logger logger) {
        // breakpoint won't work right without this
        glEnable(GL_DEBUG_OUTPUT_SYNCHRONOUS);
        return setupDebugMessageCallback( msg -> {
            // common case
            if(!logger.isDebugEnabled() && "NOTIFICATION".equals(msg.severity)) return;

            if("NOTIFICATION".equals(msg.severity)) {
                logger.debug(msg.getLogMsg());
            } else if("LOW".equals(msg.severity)) {
                logger.warn(msg.getLogMsg());
            } else if("MEDIUM".equals(msg.severity)) {
                logger.warn(msg.getLogMsg());
            } else if("HIGH".equals(msg.severity)) {
                logger.error(msg.getLogMsg());
            } else {
                // treat unknown as WARN, I guess
                logger.warn(msg.getLogMsg());
            }

            if(!"NOTIFICATION".equals(msg.severity)) {
                // set a breakpoint here to pinpoint the exact call causing your error
                throw new IllegalStateException(msg.getLogMsg());
            }

        });
    }
    public static Callback setupDebugMessageCallback(Consumer<Msg> debugConsumer) {
        GLCapabilities caps = GL.getCapabilities();

        if (caps.OpenGL43) {
            apiLog("[GL] Using OpenGL 4.3 for error logging.");
            GLDebugMessageCallback proc = GLDebugMessageCallback.create((source, type, id, severity, length, message, userParam) -> {
                Msg msg = new Msg(
                        id,
                        getDebugSource(source),
                        getDebugType(type),
                        getDebugSeverity(severity),
                        GLDebugMessageCallback.getMessage(length, message),
                        null,
                        "OpenGL 4.3 debug"
                );
                debugConsumer.accept(msg);
            });
            glDebugMessageCallback(proc, NULL);
            if ((glGetInteger(GL_CONTEXT_FLAGS) & GL_CONTEXT_FLAG_DEBUG_BIT) == 0) {
                apiLog("[GL] Warning: A non-debug context may not produce any debug output.");
                glEnable(GL_DEBUG_OUTPUT);
            }
            return proc;
        }

        if (caps.GL_KHR_debug) {
            apiLog("[GL] Using KHR_debug for error logging.");
            GLDebugMessageCallback proc = GLDebugMessageCallback.create((source, type, id, severity, length, message, userParam) -> {
                Msg msg = new Msg(
                        id,
                        getDebugSource(source),
                        getDebugType(type),
                        getDebugSeverity(severity),
                        GLDebugMessageCallback.getMessage(length, message),
                        null,
                        "OpenGL (KHR_debug)"
                );
                debugConsumer.accept(msg);
            });
            KHRDebug.glDebugMessageCallback(proc, NULL);
            if (caps.OpenGL30 && (glGetInteger(GL_CONTEXT_FLAGS) & GL_CONTEXT_FLAG_DEBUG_BIT) == 0) {
                apiLog("[GL] Warning: A non-debug context may not produce any debug output.");
                glEnable(GL_DEBUG_OUTPUT);
            }
            return proc;
        }

        if (caps.GL_ARB_debug_output) {
            apiLog("[GL] Using ARB_debug_output for error logging.");
            GLDebugMessageARBCallback proc = GLDebugMessageARBCallback.create((source, type, id, severity, length, message, userParam) -> {
                Msg msg = new Msg(
                        id,
                        getSourceARB(source),
                        getTypeARB(type),
                        getSeverityARB(severity),
                        GLDebugMessageARBCallback.getMessage(length, message),
                        null,
                        "ARB_debug_output"
                );
                debugConsumer.accept(msg);
            });
            glDebugMessageCallbackARB(proc, NULL);
            return proc;
        }

        if (caps.GL_AMD_debug_output) {
            apiLog("[GL] Using AMD_debug_output for error logging.");
            GLDebugMessageAMDCallback proc = GLDebugMessageAMDCallback.create((id, category, severity, length, message, userParam) -> {
                Msg msg = new Msg(
                        id,
                        null,
                        null,
                        getSeverityAMD(severity),
                        GLDebugMessageAMDCallback.getMessage(length, message),
                        getCategoryAMD(category),
                        "AMD_debug_output"
                );
                debugConsumer.accept(msg);
            });
            glDebugMessageCallbackAMD(proc, NULL);
            return proc;
        }

        apiLog("[GL] No debug output implementation is available.");
        return null;
    }

    private static void printDetail(PrintStream stream, String type, String message) {
        if(null!=message) stream.printf("\t%s: %s\n", type, message);
    }

    private static String getDebugSource(int source) {
        switch (source) {
            case GL_DEBUG_SOURCE_API:
                return "API";
            case GL_DEBUG_SOURCE_WINDOW_SYSTEM:
                return "WINDOW SYSTEM";
            case GL_DEBUG_SOURCE_SHADER_COMPILER:
                return "SHADER COMPILER";
            case GL_DEBUG_SOURCE_THIRD_PARTY:
                return "THIRD PARTY";
            case GL_DEBUG_SOURCE_APPLICATION:
                return "APPLICATION";
            case GL_DEBUG_SOURCE_OTHER:
                return "OTHER";
            default:
                return apiUnknownToken(source);
        }
    }

    private static String getDebugType(int type) {
        switch (type) {
            case GL_DEBUG_TYPE_ERROR:
                return "ERROR";
            case GL_DEBUG_TYPE_DEPRECATED_BEHAVIOR:
                return "DEPRECATED BEHAVIOR";
            case GL_DEBUG_TYPE_UNDEFINED_BEHAVIOR:
                return "UNDEFINED BEHAVIOR";
            case GL_DEBUG_TYPE_PORTABILITY:
                return "PORTABILITY";
            case GL_DEBUG_TYPE_PERFORMANCE:
                return "PERFORMANCE";
            case GL_DEBUG_TYPE_OTHER:
                return "OTHER";
            case GL_DEBUG_TYPE_MARKER:
                return "MARKER";
            default:
                return apiUnknownToken(type);
        }
    }

    private static String getDebugSeverity(int severity) {
        switch (severity) {
            case GL_DEBUG_SEVERITY_HIGH:
                return "HIGH";
            case GL_DEBUG_SEVERITY_MEDIUM:
                return "MEDIUM";
            case GL_DEBUG_SEVERITY_LOW:
                return "LOW";
            case GL_DEBUG_SEVERITY_NOTIFICATION:
                return "NOTIFICATION";
            default:
                return apiUnknownToken(severity);
        }
    }

    private static String getSourceARB(int source) {
        switch (source) {
            case GL_DEBUG_SOURCE_API_ARB:
                return "API";
            case GL_DEBUG_SOURCE_WINDOW_SYSTEM_ARB:
                return "WINDOW SYSTEM";
            case GL_DEBUG_SOURCE_SHADER_COMPILER_ARB:
                return "SHADER COMPILER";
            case GL_DEBUG_SOURCE_THIRD_PARTY_ARB:
                return "THIRD PARTY";
            case GL_DEBUG_SOURCE_APPLICATION_ARB:
                return "APPLICATION";
            case GL_DEBUG_SOURCE_OTHER_ARB:
                return "OTHER";
            default:
                return apiUnknownToken(source);
        }
    }

    private static String getTypeARB(int type) {
        switch (type) {
            case GL_DEBUG_TYPE_ERROR_ARB:
                return "ERROR";
            case GL_DEBUG_TYPE_DEPRECATED_BEHAVIOR_ARB:
                return "DEPRECATED BEHAVIOR";
            case GL_DEBUG_TYPE_UNDEFINED_BEHAVIOR_ARB:
                return "UNDEFINED BEHAVIOR";
            case GL_DEBUG_TYPE_PORTABILITY_ARB:
                return "PORTABILITY";
            case GL_DEBUG_TYPE_PERFORMANCE_ARB:
                return "PERFORMANCE";
            case GL_DEBUG_TYPE_OTHER_ARB:
                return "OTHER";
            default:
                return apiUnknownToken(type);
        }
    }

    private static String getSeverityARB(int severity) {
        switch (severity) {
            case GL_DEBUG_SEVERITY_HIGH_ARB:
                return "HIGH";
            case GL_DEBUG_SEVERITY_MEDIUM_ARB:
                return "MEDIUM";
            case GL_DEBUG_SEVERITY_LOW_ARB:
                return "LOW";
            default:
                return apiUnknownToken(severity);
        }
    }

    private static String getCategoryAMD(int category) {
        switch (category) {
            case GL_DEBUG_CATEGORY_API_ERROR_AMD:
                return "API ERROR";
            case GL_DEBUG_CATEGORY_WINDOW_SYSTEM_AMD:
                return "WINDOW SYSTEM";
            case GL_DEBUG_CATEGORY_DEPRECATION_AMD:
                return "DEPRECATION";
            case GL_DEBUG_CATEGORY_UNDEFINED_BEHAVIOR_AMD:
                return "UNDEFINED BEHAVIOR";
            case GL_DEBUG_CATEGORY_PERFORMANCE_AMD:
                return "PERFORMANCE";
            case GL_DEBUG_CATEGORY_SHADER_COMPILER_AMD:
                return "SHADER COMPILER";
            case GL_DEBUG_CATEGORY_APPLICATION_AMD:
                return "APPLICATION";
            case GL_DEBUG_CATEGORY_OTHER_AMD:
                return "OTHER";
            default:
                return apiUnknownToken(category);
        }
    }

    private static String getSeverityAMD(int severity) {
        switch (severity) {
            case GL_DEBUG_SEVERITY_HIGH_AMD:
                return "HIGH";
            case GL_DEBUG_SEVERITY_MEDIUM_AMD:
                return "MEDIUM";
            case GL_DEBUG_SEVERITY_LOW_AMD:
                return "LOW";
            default:
                return apiUnknownToken(severity);
        }
    }

}
