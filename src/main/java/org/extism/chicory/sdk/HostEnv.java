package org.extism.chicory.sdk;

import com.dylibso.chicory.log.Logger;
import com.dylibso.chicory.runtime.HostFunction;
import com.dylibso.chicory.runtime.Instance;
import com.dylibso.chicory.wasm.types.Value;
import com.dylibso.chicory.wasm.types.ValueType;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.dylibso.chicory.wasm.types.Value.i64;

public class HostEnv {

    private final Kernel kernel;
    private final Memory memory;
    private final Logger logger;
    private final Log log;
    private final Var var;
    private final Config config;

    public HostEnv(Kernel kernel, Map<String, String> config, Logger logger) {
        this.kernel = kernel;
        this.memory = new Memory();
        this.logger = logger;
        this.config = new Config(config);
        this.var = new Var();
        this.log = new Log();
    }

    public Log log() {
        return log;
    }

    public Var var() {
        return var;
    }

    public Config config() {
        return config;
    }

    public HostFunction[] toHostFunctions() {
        return concat(
                kernel.toHostFunctions(),
                log.toHostFunctions(),
                var.toHostFunctions(),
                config.toHostFunctions());
    }

    private HostFunction[] concat(HostFunction[]... hfs) {
        return Arrays.stream(hfs).flatMap(Arrays::stream).toArray(HostFunction[]::new);
    }

    public void setInput(byte[] input) {
        kernel.setInput(input);
    }

    public byte[] getOutput() {
        return kernel.getOutput();
    }

    public Memory memory() {
        return this.memory;
    }

    public class Memory {

        public long length(long offset) {
            return kernel.length.apply(i64(offset))[0].asLong();
        }

        public com.dylibso.chicory.runtime.Memory memory() {
            return kernel.instanceMemory;
        }

        public long alloc(long size) {
            return kernel.alloc.apply(i64(size))[0].asLong();
        }

        public byte[] readBytes(long offset) {
            long length = length(offset);
            return memory().readBytes((int) offset, (int) length);
        }

        public String readString(long offset) {
            return new String(readBytes(offset), StandardCharsets.UTF_8);
        }

        public long writeBytes(byte[] bytes) {
            long ptr = alloc(bytes.length);
            memory().write((int) ptr, bytes);
            return ptr;
        }

        public long writeString(String s) {
            return writeBytes(s.getBytes(StandardCharsets.UTF_8));
        }
    }

    public class Log {
        private Log(){}

        public void log(LogLevel level, String message) {
            logger.log(level.toChicoryLogLevel(), message, null);
        }

        public void logf(LogLevel level, String format, Object args) {
            logger.log(level.toChicoryLogLevel(), String.format(format, args), null);
        }

        private Value[] logTrace(Instance instance, Value... args) {
            return log(LogLevel.TRACE, args[0].asLong());
        }

        private Value[] logDebug(Instance instance, Value... args) {
            return log(LogLevel.DEBUG, args[0].asLong());
        }

        private Value[] logInfo(Instance instance, Value... args) {
            return log(LogLevel.INFO, args[0].asLong());
        }

        private Value[] logWarn(Instance instance, Value... args) {
            return log(LogLevel.WARN, args[0].asLong());
        }

        private Value[] logError(Instance instance, Value... args) {
            return log(LogLevel.ERROR, args[0].asLong());
        }


        private Value[] log(LogLevel level, long offset) {
            String msg = memory().readString(offset);
            log(level, msg);
            return new Value[0];
        }

        HostFunction[] toHostFunctions() {
            return new HostFunction[]{
                    new HostFunction(this::logTrace, Kernel.IMPORT_MODULE_NAME, "log_trace", List.of(ValueType.I64), List.of()),
                    new HostFunction(this::logDebug, Kernel.IMPORT_MODULE_NAME, "log_debug", List.of(ValueType.I64), List.of()),
                    new HostFunction(this::logInfo, Kernel.IMPORT_MODULE_NAME, "log_info", List.of(ValueType.I64), List.of()),
                    new HostFunction(this::logWarn, Kernel.IMPORT_MODULE_NAME, "log_warn", List.of(ValueType.I64), List.of()),
                    new HostFunction(this::logError, Kernel.IMPORT_MODULE_NAME, "log_error", List.of(ValueType.I64), List.of())};
        }
    }

    public class Var {
        private final Map<String, byte[]> vars =  new ConcurrentHashMap<>();

        private Var() {}

        public byte[] get(String key) {
            return vars.get(key);
        }

        public void set(String key, byte[] value) {
            this.vars.put(key, value);
        }

        private Value[] varGet(Instance instance, Value... args) {
            // FIXME: should check MaxVarBytes to see if vars are disabled.

            long ptr = args[0].asLong();
            String key = memory().readString(ptr);
            byte[] value = get(key);
            Value result;
            if (value == null) {
                // Value not found
                result = i64(0);
            } else {
                long rPtr = memory().writeBytes(value);
                result = i64(rPtr);
            }
            return new Value[]{result};
        }

        private Value[] varSet(Instance instance, Value... args) {
            // FIXME: should check MaxVarBytes before committing.

            long keyPtr = args[0].asLong();
            long valuePtr = args[1].asLong();
            String key = memory().readString(keyPtr);

            // Remove if the value offset is 0
            if (valuePtr == 0) {
                vars.remove(key);
            } else {
                byte[] value = memory().readBytes(valuePtr);
                set(key, value);
            }
            return new Value[0];
        }


        HostFunction[] toHostFunctions() {
            return new HostFunction[]{
                    new HostFunction(this::varGet, Kernel.IMPORT_MODULE_NAME, "var_get", List.of(ValueType.I64), List.of(ValueType.I64)),
                    new HostFunction(this::varSet, Kernel.IMPORT_MODULE_NAME, "var_set", List.of(ValueType.I64, ValueType.I64), List.of()),
            };
        }
    }

    public class Config {

        private final Map<String, String> config;

        private Config(Map<String, String> config) {
            this.config = config;
        }

        public String get(String key) {
            return config.get(key);
        }

        private Value[] configGet(Instance instance, Value... args) {
            long ptr = args[0].asLong();
            String key = memory().readString(ptr);
            String value = get(key);
            Value result;
            if (value == null) {
                // Value not found
                result = i64(0);
            } else {
                long rPtr = memory().writeString(value);
                result = i64(rPtr);
            }
            return new Value[]{result};
        }

        HostFunction[] toHostFunctions() {
            return new HostFunction[]{
                    new HostFunction(this::configGet, Kernel.IMPORT_MODULE_NAME, "config_get", List.of(ValueType.I64), List.of(ValueType.I64))
            };
        }

    }




}
