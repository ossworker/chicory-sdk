package org.extism.chicory.sdk;

import com.dylibso.chicory.runtime.HostFunction;
import com.dylibso.chicory.runtime.Memory;
import com.dylibso.chicory.wasm.types.Value;
import com.dylibso.chicory.wasm.types.ValueType;
import okhttp3.OkHttpClient;
import org.extism.chicory.sdk.http.HttpUtils;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExtismTest {



    @Test
    void testGreet() {

        Manifest manifest = Manifest.ofWasms(ManifestWasm
                                                     .fromPath(
                                                             "/Users/workoss/IDE/rustProjects/new-plugin/target/wasm32-unknown-unknown/release/rust_pdk_template.wasm")
                                                     .build())
                .build();
        Plugin plugin = Plugin.ofManifest(manifest)
                .build();
        byte[] bytes = plugin.call("greet", "张三".getBytes(StandardCharsets.UTF_8));
        System.out.println(new String(bytes, StandardCharsets.UTF_8));

    }

    @Test
    void testAdd() {
        Manifest manifest = Manifest.ofWasms(ManifestWasm
                                                     .fromPath(
                                                             "/Users/workoss/IDE/rustProjects/new-plugin/target/wasm32-unknown-unknown/release/rust_pdk_template.wasm")
                                                     .build())
                .withOptions(new Manifest.Options().withAoT())
                .build();
        Plugin plugin = Plugin.ofManifest(manifest)
                .build();
        byte[] bytes = plugin.call("add", "{\"left\":12,\"right\":3}".getBytes(StandardCharsets.UTF_8));
        System.out.println(new String(bytes, StandardCharsets.UTF_8));

    }




    @Test
    void testHttp() {
        Manifest manifest = Manifest.ofWasms(ManifestWasm
                                                     .fromPath(
                                                             "/Users/workoss/IDE/rustProjects/new-plugin/target/wasm32-wasip1/release/rust_pdk_template.wasm")
                                                     .build())
                .withOptions(new Manifest.Options().withAoT())
                .withAllowHosts(List.of("www.baidu.com","httpbin.org"))
                .build();


        runHttp(manifest,0);

        long start = Instant.now().toEpochMilli();

        for (int i = 0; i < 20; i++) {
            runHttp(manifest,i+1);
        }
        long end = Instant.now().toEpochMilli();
        System.out.println("cost:"+(end-start)+"ms avg:"+(end-start)/10+"ms");

    }

    private void runHttp(Manifest manifest,int i){
        ExtismHostFunction hostFunction = ExtismHostFunction.of("load_host_data",
                                                                List.of(ValueType.I64),
                                                                List.of(ValueType.I64),
                                                                (currentPlugin, values) -> {
                                                                    for (Value value : values) {
                                                                        System.out.println(value);
                                                                    }
                                                                    long anInt = values[0].asLong();
                                                                    System.out.println("---" + anInt);

                                                                    Memory memory = currentPlugin.memory().memory();
                                                                    String input = memory.readCString((int) anInt);
                                                                    System.out.println("input:" + input);

                                                                    String inString = "测试中文wor中愛";
                                                                    long ptr = currentPlugin.memory()
                                                                            .alloc(inString.length() + 2L * countChineseCharacters(
                                                                                    inString));
                                                                    byte[] bytes = inString.getBytes(
                                                                            StandardCharsets.UTF_8);
                                                                    System.out.println(bytes);
                                                                    memory.writeCString((int) ptr, inString,
                                                                                        StandardCharsets.UTF_8);

                                                                    System.out.println("ptr:" + ptr);
                                                                    String hostOut = memory.readCString((int) ptr,
                                                                                                        StandardCharsets.UTF_8);
                                                                    System.out.println("hostOut:" + hostOut);
                                                                    return new Value[]{Value.i64(ptr)};
                                                                });


        Plugin plugin = Plugin.ofManifest(manifest)
                .withHostFunctions(hostFunction)
                .withLogger(new Slf4jWasmLogger())
                .build();

//        byte[] bytes = plugin.call("http_post_resp",
//                                   ("{\"url\": \"https://httpbin.org/post\", \"headers\":{\"x-gray\":\"1.0\"},\"method\": \"POST\",\"data\":\"{\\\"id\\\":\\\"中国china" + i + "\\\"}\"}").getBytes(
//                                           StandardCharsets.UTF_8));
        byte[] bytes = plugin.call("http_get_resp",
                                   ("{\"url\": \"https://www.baidu.com\", \"headers\":{\"x-gray\":\"1.0\"},\"method\": \"GET\"}").getBytes(
                                           StandardCharsets.UTF_8));
        System.out.println(new String(bytes, StandardCharsets.UTF_8));
    }



    @Test
    public void testHttpUtil(){
        String url = "http://httpbin.org/post";
        Map<String,String> headers = new HashMap<>();
        headers.put("x-gray", "1.0");

        String body = "{\"id\":\"中国china\"}\"}";

        long start = Instant.now().getEpochSecond();
        for (int i = 0; i < 10; i++) {
            byte[] execute = HttpUtils.execute(url, HttpUtils.HttpMehtod.POST, headers,
                                               body.getBytes(StandardCharsets.UTF_8), Duration.ofSeconds(60));
            System.out.println(new String(execute, StandardCharsets.UTF_8));
        }
        long end = Instant.now().getEpochSecond();
        System.out.println("cost:"+(end-start)+"s");

    }


    public long countChineseCharacters(String text) {
//        long count = 0;
//        for (int i = 0; i < text.length(); i++) {
//            char charAt = text.charAt(i);
//            if (charAt >= 0x4E00 && charAt <= 0x9FA5) {
//                count++;
//            }
//        }
//        return count;
        long chineseCharCount = 0;

        for (int i = 0; i < text.length(); ) {
            int codePoint = text.codePointAt(i);
            if (Character.UnicodeBlock.of(codePoint) == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS
                    || Character.UnicodeBlock.of(codePoint) == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS
                    || Character.UnicodeBlock.of(codePoint) == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A
                    || Character.UnicodeBlock.of(codePoint) == Character.UnicodeBlock.GENERAL_PUNCTUATION
                    || Character.UnicodeBlock.of(codePoint) == Character.UnicodeBlock.CJK_SYMBOLS_AND_PUNCTUATION
                    || Character.UnicodeBlock.of(codePoint) == Character.UnicodeBlock.HALFWIDTH_AND_FULLWIDTH_FORMS) {
                chineseCharCount++;
            }
            i += Character.charCount(codePoint);
        }
        System.out.println(chineseCharCount);
        return chineseCharCount;
    }


}
